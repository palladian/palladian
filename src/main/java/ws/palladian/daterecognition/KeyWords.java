package ws.palladian.daterecognition;

import ws.palladian.helper.ArrayHelper;

/**
 * 
 * All keywords for different techniques.
 * TODO: refactor for not having a redundancy of keywords .
 * 
 * @author Martin Gregor
 * 
 */
public final class KeyWords {

    public static final byte PUBLISH_KEYWORD = 1;
    public static final byte MODIFIED_KEYWORD = 2;
    public static final byte OTHER_KEYWORD = 3;

    /** Keyowrds found in HTTP-header. */
    public static final String[] HTPP_KEYWORDS = { "date", "Date", "DATE", "last-modified", "Last-Modified",
            "Last-modified", "LAST-MODIFIED" };

    /** Keywords found in HTTP header of connections. */
    public static final String[] HEAD_KEYWORDS = { "published", "publish", "pubdate", "posted", "released", "release",
            "displaydate", "create", "update", "updated", "last-modified", "modified", "pdate", "date", "change" };

    /** Keywords found in HTML structure of documents. */
    public static final String[] DATE_BODY_STRUC = { "published", "publish", "posted", "create", "created", "released",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate", "display_date",
            "last-modified", "last modified", "date-last-modified", "update", "dc:date", "xsd:date", "xsd:dateTime",
            "date", "time", "datetime", "datestamp", "date-header", "revised", "revise" };

    /** Keywords found in HTML content of documents. */
    public static final String[] BODY_CONTENT_KEYWORDS_FIRST = { "published", "pubdate", "posted", "released",
            "release", "updated", "update", "veröffentlicht", "create", "created", "revised", "revise", "aktualisiert",
            "added", "geschrieben" };

    /** Kewords found in content of documents */
    public static final String[] BODY_CONTENT_KEYWORDS_ALL = { "published", "publish", "posted", "create", "created",
            "released", "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate",
            "display_date", "last-modified", "last modified", "date-last-modified", "update", "updated", "date",
            "datetime", "datestamp", "date-header", "revised", "revise", "added" };

    /** Keywords belonging to first priority class. */
    public static final String[] firstPriorityKeywords = { "published", "publish", "posted", "released", "release",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate", "display_date",
            "Veröffentlicht", "create", "created", "added", "geschrieben" };
    /** Keywords belonging to second priority class. */
    public static final String[] secondPriorityKeywords = { "last-modified", "last modified", "date-last-modified",
            "updated", "update", "change", "modified", "revised", "revise", "aktualisiert" };
    /** Keywords belonging to second priority class. */
    public static final String[] thirdPriorityKexwords = { "date", "time", "datetime", "datestamp", "dc:date",
            "xsd:date", "xsd:dateTime", "date-header" };

    /** All keywords. */
    public static final String[] allKeywords = ArrayHelper.concat(firstPriorityKeywords, ArrayHelper.concat(
            secondPriorityKeywords, thirdPriorityKexwords));

    /**
     * Returns the classpriority of a keyword. If a date has no keyword -1 will be returned.<br>
     * Otherwise returning values are equal to {@link KeyWords} static values.
     * 
     * @param date
     * @return
     */
    public static byte getKeywordPriority(String keyword) {
        byte keywordPriority = -1;
        if (keyword != null) {
            if (hasKeyword(keyword, KeyWords.firstPriorityKeywords)) {
                keywordPriority = KeyWords.PUBLISH_KEYWORD;
            } else if (hasKeyword(keyword, KeyWords.secondPriorityKeywords)) {
                keywordPriority = KeyWords.MODIFIED_KEYWORD;
            } else if (hasKeyword(keyword, KeyWords.thirdPriorityKexwords)) {
                keywordPriority = KeyWords.OTHER_KEYWORD;
            }
        }
        return keywordPriority;
    }

    /**
     * Is a specific keyword in a array of strings.
     * 
     * @param keyword
     * @param keywords
     * @return
     */
    private static boolean hasKeyword(String keyword, String[] keywords) {
        boolean hasKeyword = false;
        for (int i = 0; i < keywords.length; i++) {
            if (keyword.equalsIgnoreCase(keywords[i])) {
                hasKeyword = true;
                break;
            }
        }
        return hasKeyword;
    }

}
