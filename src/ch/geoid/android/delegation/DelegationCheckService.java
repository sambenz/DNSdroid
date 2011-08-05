package ch.geoid.android.delegation;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.xbill.DNS.ResolverConfig;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import ch.geoid.android.delegation.DelegationCheckResults.Results;
import ch.nic.reg.delegation.CheckDelegation;
import ch.nic.reg.delegation.CheckDelegationException;
import ch.nic.reg.delegation.ResolverFactory;
import ch.nic.reg.delegation.Result;
import ch.nic.reg.delegation.Severity;
import ch.nic.reg.delegation.TestCategory;
import ch.nic.reg.delegation.Zone;
import ch.nic.reg.delegation.utils.KeyPlan;

/**
 * Name: DelegationCheckService<br>
 * Description: <br>
 * 
 * Creation date: Jul 30, 2011<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class DelegationCheckService extends Service implements Runnable {

	private static final String TAG = "dnsdroid";

	private NotificationManager notifications;
	
	private WakeLock wakelock;

	/**
	 * The notify note ID
	 */
	public static final int NOTE_ID = 34653465;

	private int warn_count;
	private int error_count;

	@Override
	public void onCreate() {
		super.onCreate();
		notifications = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();
        Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		wakelock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	@Override
	public void run() {
		final Cursor c = getContentResolver().query(Results.CONTENT_URI, Results.PROJECTION, null, null,Results.DEFAULT_SORT_ORDER);
		notifications.cancel(NOTE_ID);
		warn_count = 0;
		error_count = 0;
		try {
			while(c.moveToNext()){
				CharSequence domain = c.getString(c.getColumnIndex(Results.DOMAIN));
				CheckDelegation test;
				ResolverConfig.refresh();
				try {
					test = new CheckDelegation(domain.toString());
					Zone zone = test.getZone();
					final SharedPreferences settings = getSharedPreferences(TAG,0);
					if(settings.getBoolean("debug",false)){
						zone.setDebug(true);
					}
					if(settings.getBoolean("default_resolver",true)){
						ResolverFactory.setStdResolver("");
					}else{
						ResolverFactory.setStdResolver(settings.getString("resolver",""));
					}
					ResolverFactory.setRetries(Integer.parseInt(settings.getString("retries", "3")));
					ResolverFactory.setTimout(Integer.parseInt(settings.getString("timeout", "3")));	        
					zone.setNameserver(zone.getNameserverByResolver());
					Log.d(TAG,"found name server for " + test.getZone().getNameAsString() + " " + test.getZone().getNameserver().toString());
					zone.setDNSKey(zone.getDNSKeyByResolver());
					zone.setDS(zone.getDSByResolver());
					if(zone.getNameserver().size() < 1){
						throw new CheckDelegationException();
					}
					KeyPlan keyplan = null;
					if(zone.getName().labels() < 3 && zone.getName().getLabelString(0).toLowerCase().matches("ch")){
						try {
							InputStream resource = getResources().openRawResource(R.raw.ch_schedule);
							keyplan = new KeyPlan(resource);
							resource.close();
						} catch (Exception e) {
							Log.e(TAG,"Failed to load KeyPLan for .CH!",e);
						}
					}else if(zone.getName().labels() < 3 && zone.getName().getLabelString(0).toLowerCase().matches("li")){
						try {
							InputStream resource = getResources().openRawResource(R.raw.li_schedule);
							keyplan = new KeyPlan(resource);
							resource.close();
						} catch (Exception e) {
							Log.e(TAG,"Failed to load KeyPLan for .LI!",e);
						}
					}
					if(keyplan != null){
						if(!new Date().after(keyplan.getExpireDate())){
							zone.setKeyPlan(keyplan);
							Log.d(TAG,keyplan.toString());
						}else{
							Log.d(TAG,"KeyPlan EXPIRED: " + keyplan.getExpireDate());
						}
					}
					test.testZone();
					updateDB(test);
				} catch (CheckDelegationException e) {
					Log.e(TAG,"DelegationCheckService Error",e);
				} finally {
					test = null;
				}
			}
		} finally {
			c.close();
		}		
	}

	private void updateDB(CheckDelegation t) {
		ContentValues values = new ContentValues();
		String domain = t.getZone().getNameAsString();
		values.put(Results.DOMAIN,domain);
		int severity = -1;
		Map<Enum<TestCategory>, List<Result>> r = t.getResults();
		for(Enum<TestCategory> c : r.keySet()){
			for(Result res : r.get(c)){
				if(res.getSeverity() > severity){
					severity = res.getSeverity();
				}
			}
		}

		// send system notification
		final SharedPreferences settings = getSharedPreferences(TAG,0);
		String message = settings.getString("message", "Never");
		if (!message.startsWith("N")){
			if(message.startsWith("W") && severity == Severity.WARNING && error_count < 1){
				warn_count++;
				sendNotification(severity,t);
			}else if (severity > Severity.WARNING){
				error_count++;
				sendNotification(severity,t);
			}
		}

		values.put(Results.RESULT,Severity.toString(severity));
		values.put(Results.MODIFIED_DATE,System.currentTimeMillis());

		Cursor cursor = getContentResolver().query(Results.CONTENT_URI, Results.PROJECTION,"domain = \"" + domain + "\"",null, Results.DEFAULT_SORT_ORDER);
		if(cursor.getCount() > 0){
			cursor.moveToFirst();
			getContentResolver().update(Results.CONTENT_URI, values, "domain = \"" + domain + "\"", null);
		}else{
			getContentResolver().insert(Results.CONTENT_URI, values);        	
		}
		Intent intent = new Intent("ch.geoid.android.delegation.DBUpdate");
		sendBroadcast(intent);
		cursor.close();
	}

	private void sendNotification(int severity, CheckDelegation t) {
		final String domain = t.getZone().getNameAsString();
		final Context context = getApplicationContext();

		CharSequence title = getResources().getString(R.string.notification_title);
		CharSequence text = getResources().getString(R.string.notification_text);

		if(error_count > 0){
			if(error_count == 1){
				title = domain + " " + Severity.toString(severity);
			}else if(error_count > 1){
				title = error_count + " " + title  + " " + Severity.toString(severity); 
			}			
		}else{
			if(warn_count == 1){
				title = domain + " " + Severity.toString(severity);
			}else if(warn_count > 1){
				title = warn_count + " " + title  + " " + Severity.toString(severity);
			}
		}

		Notification notification = new Notification(android.R.drawable.stat_notify_error, title, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS; 
        notification.ledARGB = Color.GREEN; 
        notification.ledOffMS = 400; 
        notification.ledOnMS = 300; 
		Intent notificationIntent = new Intent(this, DelegationCheckActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, title, text, contentIntent);
		notifications.notify(NOTE_ID, notification);
	}

}
