/**
 * 
 */
package tud.iir.daterecognition.dates;

/**
 * @author salco
 * 
 */
public class HeadDate extends KeywordDate {

    private String tag = null;

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

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
