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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import ch.nic.reg.delegation.Result;
import ch.nic.reg.delegation.Severity;
import ch.nic.reg.delegation.TestCategory;

/**
 * Name: ResultListActivity<br>
 * Description: <br>
 * 
 * Creation date: Feb 15, 2010<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class ResultListActivity extends ExpandableListActivity {

    private ExpandableListAdapter list;
    private Map<Enum<TestCategory>,List<Result>> results;
    private List<Map<String, String>> groupData;
    private List<List<Map<String, Result>>> childData;
    private static final String NAME = "NAME";
    private static final String RESULT = "RESULT";
    private static final int MENU_ITEM_BACK = Menu.FIRST;
    private static final int MENU_ITEM_SHARE = Menu.FIRST + 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		results = DetailTestResultList.results;

		if(results != null){
			this.setTitle(DetailTestResultList.domain);
	        groupData = new ArrayList<Map<String, String>>();
	        childData = new ArrayList<List<Map<String, Result>>>();

	        if(results.containsKey(TestCategory.Generic)){
	        	showResults(TestCategory.Generic);	
	        }
	        showResults(TestCategory.SOA);
	        showResults(TestCategory.Delegation);
	        showResults(TestCategory.Dnssec);
	        showResults(TestCategory.Nameserver);
	        showResults(TestCategory.Address);
	        if(results.containsKey(TestCategory.Keyplan)){
	        	showResults(TestCategory.Keyplan);	
	        }

			final LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    
	        // Set up our adapter
	        list = new SimpleExpandableListAdapter(
	                this,
	                groupData,
	                0, //R.layout.result_list_item_1,
	                null, //new String[] { NAME, RESULT },
	                new int[] {}, //new int[] { R.id.cat_text1, R.id.cat_image1 },
	                childData,
	                0, //android.R.layout.simple_expandable_list_item_2,
	                null, //new String[] { NAME, DATA },
	                new int[] {} //new int[] { android.R.id.text1, android.R.id.text2 }
	                ) {
	
				public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
	        		final View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
	
	        		@SuppressWarnings("unchecked") Map<String, String> map = (Map<String,String>)getGroup(groupPosition);
					int severity = Severity.toInt(map.get(RESULT));
					String cat = (String) map.get(NAME); 
					
	        		((TextView)v.findViewById(R.id.cat_text1)).setText(cat);
	        		if(cat.contains("Dnssec")){
	        			if(severity == Severity.OK){
		        			((ImageView)v.findViewById(R.id.cat_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.dnssec_on)));	        				
	        			}else{
		        			((ImageView)v.findViewById(R.id.cat_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.dnssec_off)));		        				
	        			}
	        		}else{
	        			switch(severity){
	        			case Severity.OK:
	        			case Severity.INFO:
		        			((ImageView)v.findViewById(R.id.cat_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_ok_large)));
		        			break;
	        			case Severity.WARNING:
		        			((ImageView)v.findViewById(R.id.cat_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_warning_large)));
		        			break;
	        			case Severity.ERROR:
	        			case Severity.FATAL:
		        			((ImageView)v.findViewById(R.id.cat_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_error_large)));        			
		        			break;
		        		default:
		        			((ImageView)v.findViewById(R.id.cat_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_no_info_large)));        			        			
		        			break;
	        			}
	        		}
	        		return v;
	        	}
	
	        	public View newGroupView(boolean isLastChild, ViewGroup parent) {
	        		return layoutInflater.inflate(R.layout.result_list_item_1, null, false);
	        	}
	
				public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	        		final View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
	
	        		@SuppressWarnings("unchecked") Map<String, Result> map = (Map<String,Result>)getChild(groupPosition, childPosition);
					Result result = map.get(RESULT);	        		
					String text = getDescription(result);;  
					((TextView)v.findViewById(R.id.result_text1)).setText(text);
					switch(result.getSeverity()){
					case Severity.OK:
					case Severity.INFO:
	        			((ImageView)v.findViewById(R.id.result_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_ok)));
	        			break;
					case Severity.WARNING:
	        			((ImageView)v.findViewById(R.id.result_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_warning)));
	        			break;
					case Severity.ERROR:
					case Severity.FATAL:
	        			((ImageView)v.findViewById(R.id.result_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_error)));        			
	        			break;
	        		default:
	        			((ImageView)v.findViewById(R.id.result_image1)).setImageDrawable( (Drawable) (getResources().getDrawable(R.drawable.result_no_info)));        			        			
	        			break;
					}
	        		return v;
	        	}
	
	        	public View newChildView(boolean isLastChild, ViewGroup parent) {
	        		return layoutInflater.inflate(R.layout.result_list_item_2, null, false);
	        	}
	    
	        };
	        
	        setListAdapter(list);
		}
    }

    private String getResultsAsText(){
    	StringBuffer t = new StringBuffer();
    	t.append(getResources().getString(R.string.share_header) + " " + DetailTestResultList.domain + "\n\n");
        if(results.containsKey(TestCategory.Generic)){
        	t.append(getCategoryAsText(TestCategory.Generic));	
        }
        t.append(getCategoryAsText(TestCategory.SOA));
        t.append(getCategoryAsText(TestCategory.Delegation));
        t.append(getCategoryAsText(TestCategory.Dnssec));
        t.append(getCategoryAsText(TestCategory.Nameserver));
        t.append(getCategoryAsText(TestCategory.Address));
        t.append("\n" + getResources().getString(R.string.share_footer) + "\n\n");
    	return t.toString();
    }
    
    private String getCategoryAsText(TestCategory category){
    	StringBuffer t = new StringBuffer();
    	t.append(category.name() + ":\n");
		for(Result result : results.get(category)){
			t.append(Severity.toString(result.getSeverity()) + "\t"); 
			t.append(getDescription(result));
			t.append("\n");
		}
		t.append("\n");
    	return t.toString();
    }
    
    private String getDescription(Result result){
		int txtId = getResources().getIdentifier(result.getDescription(),"string","ch.geoid.android.delegation");
		String text;  
		if(txtId > 0){
			if(result.getParameters().size() > 0){
				String params = "";
				for(String param : result.getParameters()){
					if(result.getParameters().indexOf(param) == 0){
						params = param;
					}else{
						params = params + ", " + param;
					}
				}
				String rtext = getResources().getString(txtId);
				text = rtext.replace("{param}",params);
			}else{
				text = getResources().getString(txtId);							
			}
		}else{
			if(result.getParameters().size() > 0){
				text = result.getDescription() + " " + result.getParameters();
			}else{
				text = result.getDescription();
			}
		}
		return text;
    }
    
    private void showResults(TestCategory category){
		Map<String, String> curGroupMap = new HashMap<String, String>();
		groupData.add(curGroupMap);
		curGroupMap.put(NAME,category.name());
		int overCategorySeverity = 0; 
		List<Map<String, Result>> children = new ArrayList<Map<String, Result>>();
		for(Result result : results.get(category)){
			if(result.getSeverity() > overCategorySeverity){
				overCategorySeverity = result.getSeverity();
			}
            Map<String, Result> curChildMap = new HashMap<String, Result>();
            children.add(curChildMap);
            curChildMap.put(RESULT,result);
		}
		curGroupMap.put(RESULT,Severity.toString(overCategorySeverity));
        childData.add(children);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ITEM_BACK, 0, R.string.menu_back).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(0, MENU_ITEM_SHARE, 1, R.string.menu_share).setIcon(android.R.drawable.ic_menu_share);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_BACK:
            startActivity(new Intent(ResultListActivity.this, DelegationCheckActivity.class));
            return true;
        case MENU_ITEM_SHARE:
        	Intent share = new Intent(Intent.ACTION_SEND);
        	share.setType("text/plain");
        	share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_header) + " " + DetailTestResultList.domain);
        	share.putExtra(Intent.EXTRA_TEXT, getResultsAsText());
        	startActivity(Intent.createChooser(share, getResources().getString(R.string.menu_share) + " " + DetailTestResultList.domain));
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
