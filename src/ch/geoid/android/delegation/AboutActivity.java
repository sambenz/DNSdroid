package ch.geoid.android.delegation;

import android.app.Activity;
import android.os.Bundle;

/**
 * Name: AboutActivity<br>
 * Description: Simple activity to show the about screen<br>
 * 
 * Creation date: May 18, 2010<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class AboutActivity extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.about);
    }
}
