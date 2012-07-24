package ws.palladian.helper.date.dates;

/**
 * Represents a date, found in a reference.
 * 
 * @author Martin Gregor
 * 
 */
public final class ReferenceDate extends ExtractedDate {

    public ReferenceDate() {
    }

    public ReferenceDate(String dateString) {
        super(dateString);
    }

    public ReferenceDate(String dateString, String format) {
        super(dateString, format);
    }

    public ReferenceDate(ExtractedDate extractedDate) {
        super(extractedDate);
    }

}
