package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateImpl;

/**
 * Template for all dates having a keyword.
 * 
 * @author Martin Gregor
 * 
 */
public abstract class KeywordDate extends ExtractedDateImpl {
    /**
     * Context, in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     */
    private String keyword = null;

    public KeywordDate(ExtractedDate date) {
        super(date);
    }

    public KeywordDate(ExtractedDate date, String keyword) {
        super(date);
        this.keyword = keyword;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(" keyword=").append(keyword);
        return builder.toString();
    }

    /**
     * 
     * @param keyword
     */
    @Deprecated
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * All techniques except extracted-date and url-date have keywords.<br>
     * Should be set direct after founding this date.
     */
    public String getKeyword() {
        return keyword;
    }

}
