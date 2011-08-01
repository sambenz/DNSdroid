package ch.geoid.android.delegation;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.IBinder;
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
public class DelegationCheckService extends Service implements OnSharedPreferenceChangeListener {

	private static final String TAG = "dnsdroid";

	private Timer timer;

	private TimerTask getTask(){
		return new TimerTask() {
		@Override
		public void run() {
			final Cursor c = getContentResolver().query(Results.CONTENT_URI, Results.PROJECTION, null, null,Results.DEFAULT_SORT_ORDER);
			try {
				while(c.moveToNext()){
					CharSequence domain = c.getString(c.getColumnIndex(Results.DOMAIN));
					try {
						CheckDelegation test = new CheckDelegation(domain.toString());
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
					}
				}
			} finally {
				c.close();
			}
		}
	};
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
        //TODO: send message if error or warning
        
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

	@Override
	public void onCreate() {
		super.onCreate();
		final SharedPreferences settings = getSharedPreferences(TAG,0);
		settings.registerOnSharedPreferenceChangeListener(this);
		String interval = settings.getString("interval", "0");
		Long i = Long.parseLong(interval);
		if(i > 0){
			timer = new Timer();	
			timer.schedule(getTask(), 1000L, i * 1000L);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(timer != null){
			timer.cancel();
		}
		timer = null;
	}
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		// this will only work if this service runs in the same process than the app!
		final SharedPreferences settings = getSharedPreferences(TAG,0);
		String interval = settings.getString("interval", "0");
		Long i = Long.parseLong(interval);
		if(i > 0){
			if(timer != null){
				timer.cancel();
			}			
			timer = new Timer();
			timer.schedule(getTask(), 1000L, i * 1000L);
		}else{
			onDestroy();
		}
	}

}
