package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;

/**
 * @author Martin Gregor
 * 
 */
public final class StructureDate extends AbstractBodyDate {

    public StructureDate(ExtractedDate date) {
        super(date);
    }

    public StructureDate(ExtractedDate date, String keyword) {
        super(date, keyword);
    }

}
