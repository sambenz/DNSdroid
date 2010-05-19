package ch.geoid.android.delegation;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

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
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
    	setContentView(R.layout.about);
    }
}
