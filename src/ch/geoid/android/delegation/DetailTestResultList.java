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
public class DetailTestResultList {

	/**
	 * Holds the Results static
	 */
	public static Map<Enum<TestCategory>, List<Result>> results;
	
	/**
	 * Holds the domain name
	 */
	public static String domain;
}
