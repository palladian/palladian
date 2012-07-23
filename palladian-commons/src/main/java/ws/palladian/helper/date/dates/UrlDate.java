package ws.palladian.helper.date.dates;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public final class UrlDate extends ExtractedDate {

    public UrlDate() {
    }

    public UrlDate(String dateString) {
        super(dateString);
    }

    public UrlDate(String dateString, String format) {
        super(dateString, format);
    }

    public UrlDate(ExtractedDate date) {
        super(date, DateType.UrlDate);
    }

//    @Override
//    public String toString() {
//        return super.toString();// + " url: " + getUrl() + "<<";
//    }
}
