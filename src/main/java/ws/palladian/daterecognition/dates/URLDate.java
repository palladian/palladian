package ws.palladian.daterecognition.dates;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public class URLDate extends ExtractedDate {

    private DateType dateType = DateType.UrlDate;

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
    public String toString() {
        return super.toString();// + " url: " + getUrl() + "<<";
    }
}
