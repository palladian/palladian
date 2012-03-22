package ws.palladian.helper.date.dates;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public class UrlDate extends ExtractedDate {

    public UrlDate() {
    }

    public UrlDate(String dateString) {
        super(dateString);
    }

    public UrlDate(String dateString, String format) {
        super(dateString, format);
    }

    @Override
    public String toString() {
        return super.toString();// + " url: " + getUrl() + "<<";
    }
}
