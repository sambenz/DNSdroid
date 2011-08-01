package ch.geoid.android.delegation;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import ch.geoid.android.delegation.DelegationCheckResults.Results;

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

	@Override
	public void onEnabled(Context context){
		super.onEnabled(context);
		final SharedPreferences settings = context.getSharedPreferences(TAG,0);
		String i = settings.getString("interval", "0");
		if(i.startsWith("0")){
			SharedPreferences.Editor edit = settings.edit();
			edit.putString("interval", "86400");
			edit.commit();
		}
        context.startService(new Intent("ch.geoid.android.delegation.StartService"));
	}
	
	@Override
	public void onDisabled(Context context){
		super.onDisabled(context);
		final SharedPreferences settings = context.getSharedPreferences(TAG,0);
		SharedPreferences.Editor edit = settings.edit();
		edit.putString("interval", "0");
		edit.commit();
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final Cursor c = context.getContentResolver().query(Results.CONTENT_URI, Results.PROJECTION, null, null,Results.DEFAULT_SORT_ORDER);
		try {
			//TODO: Use here a ViewFlipper design with all results
			if (c.moveToFirst()) {
				CharSequence domain = c.getString(c.getColumnIndex(Results.DOMAIN));
				CharSequence modifiedAt = DateUtils.getRelativeTimeSpanString(context,c.getLong(c.getColumnIndex(Results.MODIFIED_DATE)));
				CharSequence result = c.getString(c.getColumnIndex(Results.RESULT));
				
				// Loop through all instances of this widget
				for (int appWidgetId : appWidgetIds) {
					RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
					views.setTextViewText(R.id.message, result + " " + domain + " " + modifiedAt);
					views.setOnClickPendingIntent(R.id.message,PendingIntent.getActivity(context, 0, new Intent(context, DelegationCheckActivity.class), 0));
					appWidgetManager.updateAppWidget(appWidgetId, views);
				}
			} else {
				Log.d(TAG, "No data to update");
			}
		} finally {
			c.close();
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

}