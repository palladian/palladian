/**
 * 
 */
package ws.palladian.extraction.date.dates;

/**
 * @author salco
 * 
 */
public class StructureDate extends AbstractBodyDate {

	private DateType dateType = DateType.StructureDate;
	
    /**
     * 
     */
    public StructureDate() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     */
    public StructureDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     * @param format
     */
    public StructureDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

}
