package ws.palladian.extraction.date;

import java.util.Arrays;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * 
 * All keywords for different techniques. TODO: refactor for not having a
 * redundancy of keywords .
 * 
 * @author Martin Gregor
 * 
 */
public final class KeyWords {

    public static final String[] ARFF_KEYWORDS = {"date", "null", "posted", "update", "release", "added", "updated",
            "create", "publish", "released", "published", "revised", "created", "pdate", "revise", "last modified",
            "date-header", "pubdate", "datetime", "geschrieben"};

    public static final byte PUBLISH_KEYWORD = 1;
    public static final byte MODIFIED_KEYWORD = 2;
    public static final byte OTHER_KEYWORD = 3;

    /** Keywords found in HTTP-header. */
    public static final String[] HTTP_KEYWORDS = {"date", "last-modified" };
//    public static final String[] HTTP_KEYWORDS = {"date", "Date", "DATE", "last-modified", "Last-Modified",
//        "Last-modified", "LAST-MODIFIED"};

    /** Keywords found in HTTP header of connections. */
    public static final String[] HEAD_KEYWORDS = {"published", "publish", "pubdate", "posted", "released", "release",
            "displaydate", "create", "update", "updated", "last-modified", "modified", "pdate", "date", "change"};

    /** Keywords found in HTML structure of documents. */
    public static final String[] DATE_BODY_STRUC = {"published", "publish", "posted", "create", "created", "released",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate", "display_date",
            "last-modified", "last modified", "date-last-modified", "update", "dc:date", "xsd:date", "xsd:dateTime",
            "date", "time", "datetime", "datestamp", "date-header", "revised", "revise"};

//    /** Keywords found in HTML content of documents. */
//    public static final String[] BODY_CONTENT_KEYWORDS_FIRST = {"published", "pubdate", "posted", "released",
//            "release", "updated", "update", "veröffentlicht", "create", "created", "revised", "revise", "aktualisiert",
//            "added", "geschrieben"};

    /** Kewords found in content of documents */
    public static final String[] BODY_CONTENT_KEYWORDS_ALL = {"published", "publish", "posted", "created", "create",
            "released", "release", "pubdate", "veröffentlicht", "geschrieben", "added", "updated", "update", "pdate",
            "revised", "revise", "aktualisiert", "date_first_released", "date_last_published", "displaydate",
            "display_date", "date-last-modified", "last-modified", "last modified", "datetime", "datestamp",
            "date-header", "date"};

    /** Keywords belonging to first priority class. */
    private static final String[] FIRST_PRIORITY_KEYWORDS = {"published", "publish", "posted", "released", "release",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate", "display_date",
            "Veröffentlicht", "create", "created", "added", "geschrieben"};
//    /** Keywords belonging to second priority class. */
//    private static final String[] SECOND_PRIORITY_KEYWORDS = {"last-modified", "last modified", "date-last-modified",
//            "updated", "update", "change", "modified", "revised", "revise", "aktualisiert"};
    /** Keywords belonging to second priority class. */
    private static final String[] THIRD_PRIORITY_KEYWORDS = {"date", "time", "datetime", "datestamp", "dc:date",
            "xsd:date", "xsd:dateTime", "date-header"};

//    /** All keywords. */
//    private static final String[] ALL_KEYWORDS = CollectionHelper.concat(FIRST_PRIORITY_KEYWORDS,
//            CollectionHelper.concat(SECOND_PRIORITY_KEYWORDS, THIRD_PRIORITY_KEYWORDS));

    /**
     * Returns the classpriority of a keyword. If a date has no keyword -1 will
     * be returned.<br>
     * Otherwise returning values are equal to {@link KeyWords} static values.
     * 
     * @param date
     * @return
     */
    public static byte getKeywordPriority(String keyword) {
        byte keywordPriority = -1;
        if (keyword != null) {
            keyword = keyword.toLowerCase();
            if (Arrays.asList(FIRST_PRIORITY_KEYWORDS).contains(keyword)) {
                keywordPriority = KeyWords.PUBLISH_KEYWORD;
            } else if (Arrays.asList(MODIFIED_KEYWORD).contains(keyword)) {
                keywordPriority = KeyWords.MODIFIED_KEYWORD;
            } else if (Arrays.asList(THIRD_PRIORITY_KEYWORDS).contains(keyword)) {
                keywordPriority = KeyWords.OTHER_KEYWORD;
            }
        }
        return keywordPriority;
    }

    /**
     * Check a string for keywords. Used to look in tag-values for date-keys.
     * 
     * @param text string with possible keywords.
     * @param keys a array of keywords.
     * @return the found keyword.
     */
    public static String searchKeyword(String text, String[] keys) {
        text = text.toLowerCase();
        for (String key : keys) {
            if (text.contains(key.toLowerCase())) {
                return key;
            }
        }
        return null;
    }

}
