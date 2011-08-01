package ch.geoid.android.delegation;
/* 
 * Copyright (c) 2011 Samuel Benz <benz@geoid.ch>
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

import java.net.InetAddress;

import org.xbill.DNS.Name;
import org.xbill.DNS.ResolverConfig;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Name: Preferences<br>
 * Description: <br>
 * 
 * Creation date: Jul 29, 2011<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class Preferences extends PreferenceActivity implements Runnable {

	private final String TAG = "dnsdroid";
	private ProgressDialog progress;
	private String current_resolver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(TAG);
		addPreferencesFromResource(R.xml.preferences);

        final CheckBoxPreference default_resolver = (CheckBoxPreference) findPreference("default_resolver");
    	final EditTextPreference resolver = (EditTextPreference) findPreference("resolver");
        default_resolver.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				if(((CheckBoxPreference)preference).isChecked()){
					resolver.setEnabled(false);
				}else{
					resolver.setEnabled(true);
				}
				return true;
			}

		});

        resolver.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object object) {
				EditTextPreference p = (EditTextPreference) preference;
				p.setSummary((String) object);
				return true;
			}

		});

    	final ListPreference retries = (ListPreference) findPreference("retries");
    	retries.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object object) {
				ListPreference p = (ListPreference) preference;
				p.setSummary((String) object);
				return true;
			}

		});
    	
    	final ListPreference timeout = (ListPreference) findPreference("timeout");
    	timeout.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object object) {
				ListPreference p = (ListPreference) preference;
				p.setSummary((String) object + " s");
				return true;
			}

		});

    	final ListPreference interval = (ListPreference) findPreference("interval");
    	interval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object object) {
				ListPreference p = (ListPreference) interval;
				String[] values = getResources().getStringArray(R.array.intervalValues);
				int id = 0;
				for(String s : values){
					if(s.equals((String)object)){
						break;
					}
					id++;
				}
				p.setSummary(getResources().getStringArray(R.array.interval)[id]);
				return true;
			}

		});
    	
		final Preference dnssec = (Preference) findPreference("dnssec");
		dnssec.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				onDnssecButtonClicked();
				return true;
			}

		});
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
        ResolverConfig.refresh();
        final CheckBoxPreference default_resolver = (CheckBoxPreference) findPreference("default_resolver");
        default_resolver.setSummary(ResolverConfig.getCurrentConfig().server());
    	final EditTextPreference resolver = (EditTextPreference) findPreference("resolver");
    	resolver.setSummary(resolver.getText());
    	if(default_resolver.isChecked()){
        	resolver.setEnabled(false);
        }else{
        	resolver.setEnabled(true);        	
        }
    	final ListPreference retries = (ListPreference) findPreference("retries");
    	retries.setSummary(retries.getEntry());
    	final ListPreference timeout = (ListPreference) findPreference("timeout");
    	timeout.setSummary(timeout.getEntry());    	
    	final ListPreference interval = (ListPreference) findPreference("interval");
    	interval.setSummary(interval.getEntry());    	
    }

    private void onDnssecButtonClicked() {
    	progress = ProgressDialog.show(Preferences.this, "",getResources().getString(R.string.progress_test), true);
    	Thread thread = new Thread(this);
    	thread.start();			
    }

    private String getCurrentResolver(){
        final CheckBoxPreference default_resolver = (CheckBoxPreference) findPreference("default_resolver");
    	final EditTextPreference resolver = (EditTextPreference) findPreference("resolver");
    	if(default_resolver.isChecked()){
    		return ResolverConfig.getCurrentConfig().server();
    	}else{
    		return resolver.getText();
    	}
    }
    
	@Override
	public void run() {
		try {
			current_resolver = getCurrentResolver();
			SimpleCacheTester test = new SimpleCacheTester();
			test.setVerbose(true);
			test.addAddresse(InetAddress.getByName(current_resolver));
			test.testAll(new Name("ch",Name.root));
			handler.sendEmptyMessage(0);
		}catch(NoDNSSECException e){
			handler.sendEmptyMessage(1);
		}catch(Exception e){
			Log.e(TAG,"SimpleCacheTester Exception!");
			Log.d(TAG,"Exception:",e);
			handler.sendEmptyMessage(2);
		}
	}

	private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		progress.dismiss();
    		if(msg.what == 0){
    			AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
    			builder.setMessage(getResources().getString(R.string.pref_test_dnssec));
    			AlertDialog alert = builder.create();
    			alert.show();
    		}else if(msg.what == 1){
    			AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
    			builder.setMessage(current_resolver + " " + getResources().getString(R.string.pref_test_nodnssec));
    			AlertDialog alert = builder.create();
    			alert.show();			    			    			
    		}else if(msg.what > 1){
    			AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
    			builder.setMessage(getResources().getString(R.string.pref_test_fail) + " " + current_resolver);
    			AlertDialog alert = builder.create();
    			alert.show();			    			
    		}
    	}
    };

}