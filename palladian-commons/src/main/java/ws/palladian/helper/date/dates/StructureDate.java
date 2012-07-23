package ws.palladian.helper.date.dates;

/**
 * @author Martin Gregor
 * 
 */
public final class StructureDate extends AbstractBodyDate {
	
    /**
     * 
     */
    public StructureDate() {
    }

    /**
     * @param dateString
     */
    public StructureDate(String dateString) {
        super(dateString);
    }

    /**
     * @param dateString
     * @param format
     */
    public StructureDate(String dateString, String format) {
        super(dateString, format);
    }

    public StructureDate(ExtractedDate date) {
        super(date, DateType.StructureDate);
    }

}
