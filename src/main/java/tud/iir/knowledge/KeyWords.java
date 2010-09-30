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
            "displaydate", "create", "update", "updated", "last-modified", "modified", "pdate", "date", "change" };
    /** Keywords found in HTML structure of documents. */
    public static final String[] DATE_BODY_STRUC = { "published", "publish", "posted", "create", "created", "released",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate", "last-modified",
            "date-last-modified", "update", "dc:date", "xsd:date", "xsd:dateTime", "date", "time", "datetime",
            "dateStamp", "date-header", "revised", "revise" };
    /** Keywords found in HTML content of documents. */
    public static final String[] BODY_CONTENT_KEYWORDS = { "published", "pubdate", "posted", "released", "release",
            "updated", "update", "Veröffentlicht", "create", "created", "revised", "revise", "aktualisiert", "Added",
            "Geschrieben" };

    public static final String[] firstPriorityKeywords = { "published", "publish", "posted", "released", "release",
            "pubdate", "pdate", "date_first_released", "date_last_published", "displaydate", "Veröffentlicht",
            "create", "created", "added", "geschrieben" };

    public static final String[] secondPriorityKeywords = { "last-modified", "date-last-modified", "updated", "update",
            "change", "modified", "revised", "revise", "aktualisiert" };

    public static final String[] thirdPriorityKexwords = { "date", "time", "datetime", "dateStamp", "dc:date",
            "xsd:date", "xsd:dateTime", "date-header" };

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
                keywordPriority = KeyWords.FIRST_PRIORITY;
            } else if (hasKeyword(keyword, KeyWords.secondPriorityKeywords)) {
                keywordPriority = KeyWords.SECOND_PRIORITY;
            } else if (hasKeyword(keyword, KeyWords.thirdPriorityKexwords)) {
                keywordPriority = KeyWords.THIRD_PRIORITY;
            }
        }
        return keywordPriority;
    }

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
