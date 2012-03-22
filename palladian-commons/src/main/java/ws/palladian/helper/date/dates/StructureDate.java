/**
 * 
 */
package ws.palladian.helper.date.dates;

/**
 * @author salco
 * 
 */
public class StructureDate extends AbstractBodyDate {
	
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

}
