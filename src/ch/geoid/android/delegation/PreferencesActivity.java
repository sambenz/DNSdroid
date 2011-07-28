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

import java.net.InetAddress;

import org.xbill.DNS.Name;
import org.xbill.DNS.ResolverConfig;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * Name: PreferencesActivity<br>
 * Description: Simple activity to show the preferences screen<br>
 * 
 * Creation date: May 18, 2010<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class PreferencesActivity extends Activity implements Runnable {

	private final String TAG = "dnsdroid";
	private ProgressDialog progress;
	
	/**
	 * Preference DEFAULT_RESOLVER
	 */
	public static final String DEFAULT_RESOLVER = "default_resolver";
	/**
	 * Preference RESOLVER
	 */
	public static final String RESOLVER = "resolver";
	/**
	 * Preference RETRIES
	 */
	public static final String RETRIES = "retries";
	/**
	 * Preference TIMEOUT
	 */
	public static final String TIMEOUT = "timeout";

	/**
	 * Preference DEBUG
	 */
	public static final String DEBUG = "debug";

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.preferences);

    	final SharedPreferences settings = getSharedPreferences(TAG,0);

    	// get default resolver
    	final EditText resolver_text = (EditText)findViewById(R.id.resolverText1);
        resolver_text.setInputType(524288); // disable text suggestions
    	final CheckBox resolver_box = (CheckBox)findViewById(R.id.resolverBox1);
    	if(settings.getBoolean(DEFAULT_RESOLVER, true)){
    		resolver_box.setChecked(true);
        	resolver_text.setEnabled(false);
        	resolver_text.setText(ResolverConfig.getCurrentConfig().server());
    	}else{
        	resolver_text.setEnabled(true);
        	resolver_text.setText(settings.getString(RESOLVER,""));    		
    	}
        resolver_box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                //onCheckBoxClicked();
            	SharedPreferences.Editor edit = settings.edit();
                if(resolver_box.isChecked()){
                	edit.putBoolean(DEFAULT_RESOLVER, true);
                	resolver_text.setEnabled(false);
                	resolver_text.setText(ResolverConfig.getCurrentConfig().server());
                }else{
                	edit.putBoolean(DEFAULT_RESOLVER, false);
                	resolver_text.setEnabled(true);
                	resolver_text.setText(settings.getString(RESOLVER,""));
                }
                edit.commit();
            }
        });    	
        
    	// test resolver dnssec awareness
        final Button dnssec = (Button) findViewById(R.id.dnssecButton1);
        dnssec.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onDnssecButtonClicked();
                }
            });

    	// get retries
    	final EditText retries_text = (EditText)findViewById(R.id.retriesText1);
        retries_text.setInputType(InputType.TYPE_CLASS_NUMBER);
        retries_text.setText(settings.getString(RETRIES, "3"));

    	// get timeout
    	final EditText timeout_text = (EditText)findViewById(R.id.timeoutText1);
    	timeout_text.setInputType(InputType.TYPE_CLASS_NUMBER);
    	timeout_text.setText(settings.getString(TIMEOUT, "3"));

    	final CheckBox debug_box = (CheckBox)findViewById(R.id.debugBox1);
    	debug_box.setChecked(settings.getBoolean(DEBUG, false));
    	
    }
    
    private void onDnssecButtonClicked() {
    	progress = ProgressDialog.show(PreferencesActivity.this, "",getResources().getString(R.string.progress_test), true);
    	Thread thread = new Thread(this);
    	thread.start();			
    }

	@Override
	public void run() {
		try {
			final EditText resolver_text = (EditText)findViewById(R.id.resolverText1);
			SimpleCacheTester test = new SimpleCacheTester();
			test.setVerbose(true);
			test.addAddresse(InetAddress.getByName(resolver_text.getText().toString()));
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
    		final EditText resolver_text = (EditText)findViewById(R.id.resolverText1);
    		if(msg.what == 0){
    			AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
    			builder.setMessage(getResources().getString(R.string.pref_test_dnssec));
    			AlertDialog alert = builder.create();
    			alert.show();
    		}else if(msg.what == 1){
    			AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
    			builder.setMessage(resolver_text.getText().toString() + " " + getResources().getString(R.string.pref_test_nodnssec));
    			AlertDialog alert = builder.create();
    			alert.show();			    			    			
    		}else if(msg.what > 1){
    			AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
    			builder.setMessage(getResources().getString(R.string.pref_test_fail) + " " + resolver_text.getText().toString());
    			AlertDialog alert = builder.create();
    			alert.show();			    			
    		}
    	}
    };
    
    @Override
    protected void onResume() {
    	super.onResume();
        ResolverConfig.refresh();
    	final SharedPreferences settings = getSharedPreferences(TAG,0);
    	
        final EditText resolver_text = (EditText)findViewById(R.id.resolverText1);
    	final CheckBox resolver_box = (CheckBox)findViewById(R.id.resolverBox1);
    	if(settings.getBoolean(DEFAULT_RESOLVER, true)){
    		resolver_box.setChecked(true);
        	resolver_text.setEnabled(false);
        	resolver_text.setText(ResolverConfig.getCurrentConfig().server());
    	}else{
        	resolver_text.setEnabled(true);
        	resolver_text.setText(settings.getString(RESOLVER,""));    		
    	}
    }

    @Override
    protected void onPause(){
    	super.onPause();
    	final SharedPreferences settings = getSharedPreferences(TAG,0);
        SharedPreferences.Editor edit = settings.edit();    	
    	
        // set resolver
        if(!settings.getBoolean(DEFAULT_RESOLVER, true)){
            final EditText resolver_text = (EditText)findViewById(R.id.resolverText1);
            edit.putString(RESOLVER, resolver_text.getText().toString());
    	}

        // set retries
    	final EditText retries_text = (EditText)findViewById(R.id.retriesText1);
    	edit.putString(RETRIES, retries_text.getText().toString());

    	// set timeout
    	final EditText timeout_text = (EditText)findViewById(R.id.timeoutText1);
    	edit.putString(TIMEOUT, timeout_text.getText().toString());

    	final CheckBox debug_box = (CheckBox)findViewById(R.id.debugBox1);
    	edit.putBoolean(DEBUG, debug_box.isChecked());
    	
    	edit.commit();
    }
    
}
