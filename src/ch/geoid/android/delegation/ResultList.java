package ch.geoid.android.delegation;

import java.util.List;
import java.util.Map;

import ch.nic.reg.delegation.Result;
import ch.nic.reg.delegation.TestCategory;

/**
 * Name: ResultList<br>
 * Description: Static Class to transfer Results between Activity<br>
 * 
 * Creation date: Feb 15, 2010<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class ResultList {

	/**
	 * Holds the Results static
	 */
	public static Map<Enum<TestCategory>, List<Result>> results;
	
}
