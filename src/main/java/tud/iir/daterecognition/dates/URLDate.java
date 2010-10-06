package tud.iir.daterecognition.dates;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public class URLDate extends ExtractedDate {

    public URLDate() {
        // TODO Auto-generated constructor stub
    }

    public URLDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    public URLDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getType() {
        // TODO Auto-generated method stub
        return TECH_URL;
    }

    @Override
    public String toString() {
        return super.toString();// + " url: " + getUrl() + "<<";
    }
}
