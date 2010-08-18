package tud.iir.daterecognition;

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

}
