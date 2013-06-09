package ws.palladian.extraction.location;

import ws.palladian.helper.collection.Filter;

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

    static class LocationTypeFilter implements Filter<Location> {

        private final LocationType type;

        public LocationTypeFilter(LocationType type) {
            this.type = type;
        }

        @Override
        public boolean accept(Location item) {
            return item.getType() == type;
        }

    }

}
