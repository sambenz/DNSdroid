package ch.geoid.android.delegation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		results = DetailTestResultList.results;

		if(results != null){
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
}
