package ws.palladian.extraction.location;

public class LocationExtractorUtils {

    public static String cleanName(String value) {
        value = value.replaceAll("[©®™]", "");
        value = value.replaceAll("\\s+", " ");
        return value;
    }

    public static String normalize(String value) {
        if (value.matches("([A-Z]\\.)+")) {
            value = value.replace(".", "");
        }
        value = cleanName(value);
        if (value.equals("US")) {
            value = "U.S.";
        }
        return value;
    }

}
