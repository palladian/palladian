package tud.iir.knowledge;

public final class KeyWords {

    /** Keywords found in HTML head of documents. */
    public static final String[] DATE_DOC_HEAD = { "published", "pubdate", "posted", "released", "displaydate",
            "pdate", "last-modified", "modified", "update", "changed", "date" };
    /** Keywords found in HTML structure of documents. */
    public static final String[] DATE_BODY_STRUC = { "published", "posted", "released", "pubdate", "pdate",
            "date_first_released", "date_last_published", "displaydate", "last-modified", "date-last-modified",
            "update", "dc:date", "xsd:date", "date" };
    /** Keywords found in HTTP header of connections. */
    public static final String[] HEAD_KEYWORDS = { "published", "pubdate", "posted", "released", "update",
            "last-modified", "pdate", "date", "change" };
    /** Keywords found in HTML content of documents. */
    public static final String[] BODY_CONTENT_KEYWORDS = { "published", "pubdate", "posted", "released", "update" };
}
