package ws.palladian.extraction.date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public static final String[] ARFF_KEYWORDS = { "date", "null", "posted", "update",
			"release", "added", "updated", "create", "publish", "released",
			"published", "revised", "created", "pdate", "revise",
			"last modified", "date-header", "pubdate", "datetime",
			"geschrieben" };

	public static final byte PUBLISH_KEYWORD = 1;
	public static final byte MODIFIED_KEYWORD = 2;
	public static final byte OTHER_KEYWORD = 3;

    /** Keywords found in HTTP-header. */
	public static final String[] HTTP_KEYWORDS = { "date", "Date", "DATE",
			"last-modified", "Last-Modified", "Last-modified", "LAST-MODIFIED" };

	/** Keywords found in HTTP header of connections. */
	public static final String[] HEAD_KEYWORDS = { "published", "publish",
			"pubdate", "posted", "released", "release", "displaydate",
			"create", "update", "updated", "last-modified", "modified",
			"pdate", "date", "change" };

	/** Keywords found in HTML structure of documents. */
	public static final String[] DATE_BODY_STRUC = { "published", "publish",
			"posted", "create", "created", "released", "pubdate", "pdate",
			"date_first_released", "date_last_published", "displaydate",
			"display_date", "last-modified", "last modified",
			"date-last-modified", "update", "dc:date", "xsd:date",
			"xsd:dateTime", "date", "time", "datetime", "datestamp",
			"date-header", "revised", "revise" };

	/** Keywords found in HTML content of documents. */
	public static final String[] BODY_CONTENT_KEYWORDS_FIRST = { "published",
			"pubdate", "posted", "released", "release", "updated", "update",
			"veröffentlicht", "create", "created", "revised", "revise",
			"aktualisiert", "added", "geschrieben" };

	/** Kewords found in content of documents */
	public static final String[] BODY_CONTENT_KEYWORDS_ALL = { "published",
			"publish", "posted", "created", "create", "released", "release",
			"pubdate", "veröffentlicht", "geschrieben", "added", "updated",
			"update", "pdate", "revised", "revise", "aktualisiert",
			"date_first_released", "date_last_published", "displaydate",
			"display_date", "date-last-modified", "last-modified",
			"last modified", "datetime", "datestamp", "date-header", "date" };

	/** Keywords belonging to first priority class. */
	public static final String[] FIRST_PRIORITY_KEYWORDS = { "published",
			"publish", "posted", "released", "release", "pubdate", "pdate",
			"date_first_released", "date_last_published", "displaydate",
			"display_date", "Veröffentlicht", "create", "created", "added",
			"geschrieben" };
	/** Keywords belonging to second priority class. */
	public static final String[] SECOND_PRIORITY_KEYWORDS = { "last-modified",
			"last modified", "date-last-modified", "updated", "update",
			"change", "modified", "revised", "revise", "aktualisiert" };
	/** Keywords belonging to second priority class. */
	public static final String[] THIRD_PRIORITY_KEYWORDS = { "date", "time",
			"datetime", "datestamp", "dc:date", "xsd:date", "xsd:dateTime",
			"date-header" };

	/** All keywords. */
	public static final String[] ALL_KEYWORDS = CollectionHelper.concat(
			FIRST_PRIORITY_KEYWORDS, CollectionHelper.concat(
					SECOND_PRIORITY_KEYWORDS, THIRD_PRIORITY_KEYWORDS));

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
			if (hasKeyword(keyword, KeyWords.FIRST_PRIORITY_KEYWORDS)) {
				keywordPriority = KeyWords.PUBLISH_KEYWORD;
			} else if (hasKeyword(keyword, KeyWords.SECOND_PRIORITY_KEYWORDS)) {
				keywordPriority = KeyWords.MODIFIED_KEYWORD;
			} else if (hasKeyword(keyword, KeyWords.THIRD_PRIORITY_KEYWORDS)) {
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
	public static boolean hasKeyword(String keyword, String[] keywords) {
		boolean hasKeyword = false;
		for (int i = 0; i < keywords.length; i++) {
			if (keyword.equalsIgnoreCase(keywords[i])) {
				hasKeyword = true;
				break;
			}
		}
		return hasKeyword;
	}
	
    /**
     * Check a string for keywords. Used to look in tag-values for date-keys.
     * 
     * @param text string with possible keywords.
     * @param keys a array of keywords.
     * @return the found keyword.
     */
    public static String searchKeyword(String text, String[] keys) {
        String keyword = null;
        // FIXME this implementations looks horribly inefficient
        for (String key : keys) {
            Pattern pattern = Pattern.compile(key.toLowerCase());
            Matcher matcher = pattern.matcher(text.toLowerCase());
            if (matcher.find()) {
                keyword = key;
                break;
            }
        }
        return keyword;
    }

}
