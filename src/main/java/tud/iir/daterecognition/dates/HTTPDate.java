/**
 * 
 */
package tud.iir.daterecognition.dates;

/**
 * @author salco
 * 
 */
public class HTTPDate extends ExtractedDate {
    /**
     * Context, in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     */
    private String keyword = null;

    /**
     * 
     */
    public HTTPDate() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     */
    public HTTPDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     * @param format
     */
    public HTTPDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.daterecognition.ExtractedDate#getType()
     */
    @Override
    public int getType() {
        // TODO Auto-generated method stub
        return TECH_HTTP_HEADER;
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
