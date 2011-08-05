package ch.geoid.android.delegation;

import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import ch.geoid.android.delegation.DelegationCheckResults.Results;
import ch.nic.reg.delegation.Severity;

/**
 * Name: ResultWidget<br>
 * Description: <br>
 * 
 * Creation date: Jul 29, 2011<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class ResultWidget extends AppWidgetProvider {
	
	private final String TAG = "dnsdroid";
	
	private final List<DisplayResult> results = new ArrayList<DisplayResult>();
	
	private static Thread thread = null;
	
	@Override
	public void onEnabled(Context context){
		super.onEnabled(context);
		final SharedPreferences settings = context.getSharedPreferences(TAG,0);
		String s = settings.getString("interval", "empty");
		if(s.equals("empty")){
			SharedPreferences.Editor edit = settings.edit();
			edit.putString("interval", "86400");
			edit.commit();
		}
    	String interval = settings.getString("interval","0");
        int seconds = Integer.parseInt(interval);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, DelegationCheckService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        am.cancel(pi);
        if (seconds > 0) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + 10*1000L,seconds*1000L, pi);
        }
	}
	
	@Override
	public void onDisabled(Context context){
		super.onDisabled(context);
		if(thread != null){
			thread.interrupt();
		}
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final Cursor c = context.getContentResolver().query(Results.CONTENT_URI, Results.PROJECTION, null, null,Results.DEFAULT_SORT_ORDER);
		results.clear();
		try {
			while(c.moveToNext()){
				String domain = c.getString(c.getColumnIndex(Results.DOMAIN));
				CharSequence modifiedAt = DateUtils.getRelativeTimeSpanString(context,c.getLong(c.getColumnIndex(Results.MODIFIED_DATE)));//DelegationCheckActivity.FuzzyDate(context.getResources(), c.getLong(c.getColumnIndex(Results.MODIFIED_DATE)));
				String result = c.getString(c.getColumnIndex(Results.RESULT));
				results.add(new DisplayResult(domain,(String)modifiedAt,result));
			}
		} finally {
			c.close();
		}
		if(thread != null){
			thread.interrupt();
		}
		if(results.size()>1){
			thread = new Thread(new UpdateRunnable(context,appWidgetManager,appWidgetIds,results));
			thread.start();
		}else if(results.size()>0){
			updateWidget(context,appWidgetManager,appWidgetIds,results.get(0));
		}else{
			for (int appWidgetId : appWidgetIds) {
				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
				views.setOnClickPendingIntent(R.id.widget,PendingIntent.getActivity(context, 0, new Intent(context, DelegationCheckActivity.class), 0));
				appWidgetManager.updateAppWidget(appWidgetId, views);
			}
		}
	}
	
	private static void updateWidget(Context context,AppWidgetManager appWidgetManager,int[] appWidgetIds,DisplayResult r){
		// Loop through all instances of this widget
		for (int appWidgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			if(r.severity.equals(Severity.toString(Severity.OK)) || r.severity.equals(Severity.toString(Severity.INFO))){
				views.setImageViewResource(R.id.widget_severity_img, R.drawable.result_ok_large);
			}else if(r.severity.equals(Severity.toString(Severity.WARNING))){
				views.setImageViewResource(R.id.widget_severity_img, R.drawable.result_warning_large);
			}else if(r.severity.equals(Severity.toString(Severity.ERROR)) || r.severity.equals(Severity.toString(Severity.FATAL))){
				views.setImageViewResource(R.id.widget_severity_img, R.drawable.result_error_large);
			}
			views.setTextViewText(R.id.widget_domain, r.domain);
			views.setTextViewText(R.id.widget_date, r.date);
			views.setOnClickPendingIntent(R.id.widget,PendingIntent.getActivity(context, 0, new Intent(context, DelegationCheckActivity.class), 0));
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.getAction().equals("ch.geoid.android.delegation.DBUpdate")) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			this.onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(new ComponentName(context,ResultWidget.class)));
		}
	}
	    
    /**
     * Name: UpdateRunnable<br>
     * Description: <br>
     * 
     * Creation date: Aug 3, 2011<br>
     * $Id$
     * 
     * @author benz@geoid.ch
     */
    public class UpdateRunnable implements Runnable {
    	final Context context;
    	final AppWidgetManager appWidgetManager;
    	final int[] appWidgetIds;
    	final List<DisplayResult> results;
    	UpdateRunnable(Context context,AppWidgetManager appWidgetManager,int[] appWidgetIds,List<DisplayResult> results){
    		this.context = context;
    		this.appWidgetManager = appWidgetManager;
    		this.appWidgetIds = appWidgetIds;
    		this.results = results;
    	}
    	
    	@Override
		public void run() {
        	while(!Thread.currentThread().isInterrupted()){
        		for(DisplayResult r : results){
        			updateWidget(context,appWidgetManager,appWidgetIds,r);
                	try {
    					Thread.sleep(7000);
    				} catch (InterruptedException e) {
    					Thread.currentThread().interrupt();
    					break;
    				}
        		}
        	}
    	}
    }

	private class DisplayResult {
		final String domain;
		final String date;
		final String severity;
		DisplayResult(String domain,String date,String severity){
			this.domain = domain;
			this.date = date;
			this.severity = severity;
		}
	}

}
