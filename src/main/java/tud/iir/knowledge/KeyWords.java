package tud.iir.knowledge;

import tud.iir.helper.ArrayHelper;

public final class KeyWords {

    public static final byte FIRST_PRIORITY = 1;
    public static final byte SECOND_PRIORITY = 2;
    public static final byte THIRD_PRIORITY = 3;

    /** Keyowrds found in HTTP-header. */
    public static final String[] HTPP_KEYWORDS = { "date", "Date", "DATE", "last-modified", "Last-Modified",
            "Last-modified", "LAST-MODIFIED" };
    /** Keywords found in HTTP header of connections. */
    public static final String[] HEAD_KEYWORDS = { "published", "publish", "pubdate", "posted", "released", "release",
            "displaydate", "update", "updated", "last-modified", "modified", "pdate", "date", "change" };
    /** Keywords found in HTML structure of documents. */
    public static final String[] DATE_BODY_STRUC = { "published", "publish", "posted", "released", "pubdate", "pdate",
            "date_first_released", "date_last_published", "displaydate", "last-modified", "date-last-modified",
            "update", "dc:date", "xsd:date", "date", "time", "datetime" };
    /** Keywords found in HTML content of documents. */
    public static final String[] BODY_CONTENT_KEYWORDS = { "published", "pubdate", "posted", "released", "release",
            "updated", "update" };

    public static final String[] firstPriorityKeywords = { "published", "publish", "posted", "released", "release",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate" };

    public static final String[] secondPriorityKeywords = { "last-modified", "date-last-modified", "updated", "update",
            "change", "modified" };

    public static final String[] thirdPriorityKexwords = { "date", "time", "datetime", "dc:date", "xsd:date" };

    public static final String[] allKeywords = ArrayHelper.concat(firstPriorityKeywords, ArrayHelper.concat(
            secondPriorityKeywords, thirdPriorityKexwords));

}
