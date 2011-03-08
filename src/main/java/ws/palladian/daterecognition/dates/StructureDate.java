/**
 * 
 */
package ws.palladian.daterecognition.dates;

/**
 * @author salco
 * 
 */
public class StructureDate extends AbstractBodyDate {

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

    /*
     * (non-Javadoc)
     * @see tud.iir.daterecognition.ExtractedDate#getType()
     */
    @Override
    public int getType() {
        // TODO Auto-generated method stub
        return TECH_HTML_STRUC;
    }

}
