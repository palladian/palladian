package tud.iir.daterecognition;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.dates.URLDate;

public class DateConverter {

    public static URLDate convertToURLDate(ExtractedDate date) {
        URLDate urlDate = null;
        if (date != null) {
            urlDate = new URLDate(date.getDateString(), date.getFormat());
        }
        return urlDate;
    }

    public static StructureDate convertToStructureDate(ExtractedDate date) {
        StructureDate structureDate = null;
        if (date != null) {
            structureDate = new StructureDate(date.getDateString(), date.getFormat());
        }
        return structureDate;
    }

    public static ContentDate convertToContentDate(ExtractedDate date) {
        ContentDate contentDate = null;
        if (date != null) {
            contentDate = new ContentDate(date.getDateString(), date.getFormat());
        }
        return contentDate;
    }

    public static HTTPDate convertToHTTPDate(ExtractedDate date) {
        HTTPDate httpDate = null;
        if (date != null) {
            httpDate = new HTTPDate(date.getDateString(), date.getFormat());
        }
        return httpDate;
    }

}
