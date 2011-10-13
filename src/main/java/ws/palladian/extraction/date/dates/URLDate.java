package ws.palladian.extraction.date.dates;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public class URLDate extends ExtractedDate {

    public URLDate() {
    }

    public URLDate(String dateString) {
        super(dateString);
    }

    public URLDate(String dateString, String format) {
        super(dateString, format);
    }

    @Override
    public String toString() {
        return super.toString();// + " url: " + getUrl() + "<<";
    }
}
