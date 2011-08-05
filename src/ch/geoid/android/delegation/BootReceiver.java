package ch.geoid.android.delegation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

/**
 * Name: BootReceiver<br>
 * Description: <br>
 * 
 * Creation date: Aug 5, 2011<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class BootReceiver extends BroadcastReceiver {

	private final String TAG = "dnsdroid";

	@Override
	public void onReceive(Context context, Intent intent) {
		final SharedPreferences settings = context.getSharedPreferences(TAG,0);
        if(settings.contains("interval")){
        	String interval = settings.getString("interval","0");
            int seconds = Integer.parseInt(interval);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(context, DelegationCheckService.class);
            PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
            am.cancel(pi);
            if (seconds > 0) {
                am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + 120*1000L,seconds*1000L, pi);
            }
        }
	}

}
