package ws.palladian.daterecognition.dates;

/**
 * Represents a date, found in a reference.
 * 
 * @author Martin Gregor
 * 
 */
public class ReferenceDate extends ExtractedDate {

    private DateType dateType = DateType.ReferenceDate;

    public ReferenceDate() {
        // TODO Auto-generated constructor stub
    }

    public ReferenceDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    public ReferenceDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

}
