package ws.palladian.daterecognition.dates;

/**
 * Represents a date, found in a reference.
 * 
 * @author Martin Gregor
 * 
 */
public class ReferenceDate extends ExtractedDate {

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

    @Override
    public int getType() {
        return TECH_REFERENCE;
    }

}
