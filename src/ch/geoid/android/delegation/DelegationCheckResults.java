package ch.geoid.android.delegation;

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
