package ws.palladian.classification.webpage;

public enum ContentType {

    /** Pages with (little) textual content that point to several articles/pages. */
    OVERVIEW,

    /** Pages that have textual content representing one article. */
    CONTENT,

    /** Search result pages. */
    SEARCH_RESULTS,

    /** If the page has no _useful_ content, e.g. error pages, pages without or very little text. */
    SPAM,

    /** If the content type could not be detected properly. */
    UNKNOWN
}

