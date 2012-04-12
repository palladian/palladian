package ws.palladian.helper.date;

import ws.palladian.helper.date.dates.ArchiveDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.DateType;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;
import ws.palladian.helper.date.dates.ReferenceDate;
import ws.palladian.helper.date.dates.StructureDate;
import ws.palladian.helper.date.dates.UrlDate;

public class DateConverter {
    

    /**
     * Converts an extracted date into a specific one. <br>
     * Use technique flags of this class or {@linkplain ExtractedDate}.
     * 
     * @param <T>
     * @param date
     * @param techniqueFlag
     * @return
     */
    public static <T> T convert(ExtractedDate date, DateType techniqueFlag) {
        T newDate = null;
        if (date != null) {
            switch (techniqueFlag) {
                case ArchiveDate:
                    newDate = (T) new ArchiveDate();
                    ((ExtractedDate)newDate).setType(DateType.ArchiveDate);
                    break;
                case UrlDate:
                    newDate = (T) new UrlDate();
                    ((ExtractedDate)newDate).setType(DateType.UrlDate);
                    break;
                case MetaDate:
                    newDate = (T) new MetaDate();
                    ((ExtractedDate)newDate).setType(DateType.MetaDate);
                    break;
                case StructureDate:
                    newDate = (T) new StructureDate();
                    ((ExtractedDate)newDate).setType(DateType.StructureDate);
                    break;
                case ContentDate:
                    newDate = (T) new ContentDate();
                    ((ExtractedDate)newDate).setType(DateType.ContentDate);
                    break;
                case ReferenceDate:
                    newDate = (T) new ReferenceDate();
                    ((ExtractedDate)newDate).setType(DateType.ReferenceDate);
                    break;

            }
            ((ExtractedDate) newDate).setAll(date.getAll());
        }
        return newDate;
    }
}
