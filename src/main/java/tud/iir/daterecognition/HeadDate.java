/**
 * 
 */
package tud.iir.daterecognition;

/**
 * @author salco
 * 
 */
public class HeadDate extends ExtractedDate {
    /**
     * Context, in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     */
    private String keyword = null;

    /**
     * 
     */
    public HeadDate() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     */
    public HeadDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     * @param format
     */
    public HeadDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.daterecognition.ExtractedDate#getType()
     */
    @Override
    public int getType() {
        return TECH_HTML_HEAD;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + "Keyword: " + keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

}
