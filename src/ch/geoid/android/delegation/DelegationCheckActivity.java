package ch.geoid.android.delegation;

import java.io.InputStream;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import ch.nic.reg.delegation.CheckDelegation;
import ch.nic.reg.delegation.CheckDelegationException;
import ch.nic.reg.delegation.Zone;
import ch.nic.reg.delegation.utils.KeyPlan;

/**
 * Name: DelegationCheckActivity<br>
 * Description: <br>
 * 
 * Creation date: Feb 15, 2010<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class DelegationCheckActivity extends Activity implements Runnable{
	
	private final String TAG = "dnsdroid";
	private CheckDelegation test;
	private ProgressDialog progress;
	private Intent resultIntent;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final Button button = (Button) findViewById(R.id.StartTest);
        button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onTestButtonClicked();
                }
            });
        
        final EditText domain_input = (EditText)findViewById(R.id.DomainText);
        domain_input.setOnEditorActionListener(new OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                	if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                		testDomain(domain_input.getText().toString());
                        return true;
                	}
                	return false;
                }
        	});
    }
    
    private void onTestButtonClicked() {
    	EditText domain = (EditText) findViewById(R.id.DomainText);
    	testDomain(domain.getText().toString());
    }
    
    private void testDomain(String domain){
		try {
			test = new CheckDelegation(domain);
			// start testing ...
	    	progress = ProgressDialog.show(DelegationCheckActivity.this, "",getResources().getString(R.string.progress_test), true);
	    	Thread thread = new Thread(this);
	    	thread.start();			
		} catch (CheckDelegationException e) {
			Log.e(TAG,"CheckDelegation Exception!");
			Log.d(TAG,"Exception:",e);
			if(e.getInitalException() != null){
				Log.d(TAG,"Initial Exception:",e.getInitalException());
			}			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getResources().getString(R.string.error_valid_domain));
			AlertDialog alert = builder.create();
			alert.show();			
		}
    }

	@Override
	public void run() {
		try {
			Zone zone = test.getZone();
			zone.setNameserver(zone.getNameserverByResolver());
			Log.d(TAG,"found name server for " + test.getZone().getNameAsString() + " " + test.getZone().getNameserver().toString());
			zone.setDNSKey(zone.getDNSKeyByResolver());
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
			resultIntent = new Intent(DelegationCheckActivity.this, ResultListActivity.class);
			Log.d(TAG,"start test for " + test.getZone().getNameAsString());
			// It is more efficient to transfer the data via static than Bundle			
			ResultList.results =  test.testZone();
			handler.sendEmptyMessage(0);
		}catch(CheckDelegationException e){
			Log.e(TAG,"CheckDelegation Exception!");
			Log.d(TAG,"Exception:",e);
			if(e.getInitalException() != null){
				Log.d(TAG,"Initial Exception:",e.getInitalException());
			}
			handler.sendEmptyMessage(1);
		}	
	}
	
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		progress.dismiss();
    		if(msg.what == 0){
    			DelegationCheckActivity.this.startActivity(resultIntent);
    		}else if(msg.what > 0){
    			AlertDialog.Builder builder = new AlertDialog.Builder(DelegationCheckActivity.this);
    			builder.setMessage(getResources().getString(R.string.error_no_ns_found) + " \"" + test.getZone().getNameAsString() + "\"");
    			AlertDialog alert = builder.create();
    			alert.show();			    			
    		}
    	}
    };
}
