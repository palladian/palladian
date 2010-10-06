/**
 * 
 */
package tud.iir.daterecognition.dates;

/**
 * 
 * Represents a date found in HTTP-connection.
 * 
 * @author Martin Gregor
 * 
 */
public class HTTPDate extends KeywordDate {

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

}
