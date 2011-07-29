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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Name: DelegationCheckResults<br>
 * Description: <br>
 * 
 * Creation date: Apr 12, 2010<br>
 * $Id$
 * 
 * @author google.com; benz@geoid.ch
 */
public final class DelegationCheckResults {

	/**
	 * DelegationCheckResults AUTHORITY
	 */
	public static final String AUTHORITY = "ch.geoid.android.delegation.DelegationCheckResults";

    // This class cannot be instantiated
    private DelegationCheckResults() {}
    
    /**
     * Results table
     */
    public static final class Results implements BaseColumns {
        // This class cannot be instantiated
        private Results() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/results");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.geoid.delegation.results";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.geoid.delegation.result";

        /**
         * The domain name of the result
         * <P>Type: TEXT</P>
         */
        public static final String DOMAIN = "domain";

        /**
         * The result itself
         * <P>Type: TEXT</P>
         */
        public static final String RESULT = "result";

        /**
         * The timestamp for when the result was last modified
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String MODIFIED_DATE = "modified";
    }
}
