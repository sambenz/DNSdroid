package ch.geoid.android.delegation;
/* 
 * Copyright (c) 2010 Samuel Benz <benz@geoid.ch>
 * 
 * This file is part of DNSdroid.
 *
 * DNSdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DNSdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DNSdroid.  If not, see <http://www.gnu.org/licenses/>.
 */

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
