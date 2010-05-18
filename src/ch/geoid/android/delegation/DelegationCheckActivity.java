package ch.geoid.android.delegation;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import ch.geoid.android.delegation.DelegationCheckResults.Results;
import ch.nic.reg.delegation.CheckDelegation;
import ch.nic.reg.delegation.CheckDelegationException;
import ch.nic.reg.delegation.Result;
import ch.nic.reg.delegation.Severity;
import ch.nic.reg.delegation.TestCategory;
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
public class DelegationCheckActivity extends ListActivity implements Runnable {
	
	private final String TAG = "dnsdroid";
	private CheckDelegation test;
	private ProgressDialog progress;
	private Intent intent;
	private Intent resultIntent;
    public static final int MENU_ITEM_TEST = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
		
    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Results._ID, // 0
            Results.DOMAIN, // 1
            Results.RESULT, // 2
            Results.MODIFIED_DATE, // 3
    };

	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        getListView().setOnCreateContextMenuListener(this);
        
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

        //SharedPreferences settings = getSharedPreferences(TAG,0);
       
        intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Results.CONTENT_URI);
        }

        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null, Results.DEFAULT_SORT_ORDER);
        SimpleCursorAdapter list = new SimpleCursorAdapter(
        											this, 
        											R.layout.domain_list_item_1, 
        											cursor, 
        											new String[] { Results.DOMAIN, Results.MODIFIED_DATE, Results.RESULT }, 
        											new int[] { R.id.res_text1, R.id.res_text2, R.id.res_image1 }
        											) {
        	@Override
        	public void bindView(View view, Context context, Cursor cursor){
        		String domain = cursor.getString(1);
        		int severity = Severity.toInt(cursor.getString(2));
				long date = Long.parseLong(cursor.getString(3));			
				
                ((TextView) view.findViewById(R.id.res_text1)).setText(domain);
                ((TextView)view.findViewById(R.id.res_text2)).setText(FuzzyDate(date));
				switch(severity){
				case Severity.OK:
				case Severity.INFO:
        			((ImageView)view.findViewById(R.id.res_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_ok)));
					break;
				case Severity.WARNING:
        			((ImageView)view.findViewById(R.id.res_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_warning)));					
        			break;
				case Severity.ERROR:
				case Severity.FATAL:
					((ImageView)view.findViewById(R.id.res_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_error)));        			
	        		break;
				default:
        			((ImageView)view.findViewById(R.id.res_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_no_info)));        			        			
        			break;	
				}
        	}
        };
        setListAdapter(list);

    }
    
    private void onTestButtonClicked() {
    	EditText domain = (EditText) findViewById(R.id.DomainText);
    	testDomain(domain.getText().toString());
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        Cursor cursor = managedQuery(uri, PROJECTION, null, null, null);
        if(cursor.getCount() > 0){
        	cursor.moveToFirst();
        	testDomain(cursor.getString(1));
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(1));

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_TEST, 0, "test");
        menu.add(0, MENU_ITEM_DELETE, 1, "delete");
        
    }
        
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                getContentResolver().delete(noteUri, null, null);
                return true;
            }
            case MENU_ITEM_TEST: {
                Uri uri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                Cursor cursor = managedQuery(uri, PROJECTION, null, null, null);
                if(cursor.getCount() > 0){
                	cursor.moveToFirst();
                	testDomain(cursor.getString(1));
                }
                return true;
            }
        }
        return false;
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
			// It seems to be more efficient to transfer the data via static ResultList than Bundle
			DetailTestResultList.results =  test.testZone();
			updateDB(test);
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
        values.put(Results.RESULT,Severity.toString(severity));
        values.put(Results.MODIFIED_DATE,System.currentTimeMillis());

        Cursor cur = getContentResolver().query(intent.getData(), PROJECTION,"domain = \"" + domain + "\"",null, Results.DEFAULT_SORT_ORDER);
        if(cur.getCount() > 0){
        	cur.moveToFirst();
        	getContentResolver().update(intent.getData(), values, "domain = \"" + domain + "\"", null);
        }else{
        	getContentResolver().insert(intent.getData(), values);        	
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
    	
	private String FuzzyDate(long date){
		String s = "";
		int d,delta = (int)((System.currentTimeMillis() - date) / 1000);
		
		if((int) delta/2073600 >= 1){
			d = (int) delta/2073600;
			s = d + " month ago";
		}else if((int) delta/604800 >= 1){
			d = (int) delta/604800;
			s = d + " week ago";						
		}else if((int)delta/86400 >= 1){
			d = (int) delta/86400;
			s = d + " day ago";			
		}else if((int) delta/3600 >= 1){
			d = (int) delta/3600;
			s = d + " hour ago";						
		}else {
			d = (int) delta / 60;
			if(d > 1){
				s = d + " min ago";
			}else{
				s = "<1 min ago";
			}
		}
		return s;
	}
}
