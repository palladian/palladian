package ws.palladian.helper.date.dates;

/**
 * Template for all dates having a keyword.
 * 
 * @author Martin Gregor
 * 
 */
public abstract class KeywordDate extends ExtractedDate {
    /**
     * Context, in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     */
    private String keyword = null;

    /**
     * @param dateString
     */
    public KeywordDate(String dateString) {
        super(dateString);
    }

    /**
     * @param dateString
     * @param format
     */
    public KeywordDate(String dateString, String format) {
        super(dateString, format);
    }

    public KeywordDate(ExtractedDate date) {
        super(date);
    }

    public KeywordDate(ExtractedDate date, String keyword) {
        super(date);
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + "Keyword: " + keyword;
    }

    /**
     * 
     * @param keyword
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * All techniques except extracted-date and url-date have keywords.<br>
     * Should be set direct after founding this date.
     */
    @Override
    public String getKeyword() {
        return keyword;
    }

}
