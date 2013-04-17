package ws.palladian.extraction.location;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;

public final class CoordinateTagger implements Tagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateTagger.class);

    public static final String TAG_NAME = "geoCoordinate";

    private static final Pattern PATTERN = Pattern
            .compile("([-+]?\\d{1,3}.\\d+)(N|S)?(?:,\\s|,|\\s)([-+]?\\d{1,3}.\\d+)(W|E)?");

    // 40°26′21″N 079°58′36″W
    private static final Pattern PATTERN_2 = Pattern
            .compile("(\\d{2}°\\d{2}['′]\\d{2}[\"″][NS])\\s?(\\d{3}°\\d{2}['′]\\d{2}[\"″][WE])");

    private static final Pattern PATTERN_DMS = Pattern.compile("(\\d{2})°(\\d{2})['′](\\d{2})[\"″]([NSWE])");

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                double lat = Double.valueOf(matcher.group(1));
                double lng = Double.valueOf(matcher.group(3));
                boolean negLat = matcher.group(2) != null && matcher.group(2).equals("S");
                boolean negLng = matcher.group(4) != null && matcher.group(4).equals("W");
                lat = negLat ? -lat : lat;
                lng = negLng ? -lng : lng;
                annotations.add(createAnnotation(matcher.start(), matcher.group(), lat, lng));
            } catch (NumberFormatException e) {
                LOGGER.debug("NumberFormatException while parsing " + matcher.group() + ": " + e.getMessage());
            }
        }

        matcher = PATTERN_2.matcher(text);
        while (matcher.find()) {
            double latitude = parseDms(matcher.group(1));
            double longitude = parseDms(matcher.group(2));
            annotations.add(createAnnotation(matcher.start(), matcher.group(), latitude, longitude));
        }
        return annotations;
    }

    private static final LocationAnnotation createAnnotation(int start, String value, double latitude, double longitude) {
        Location location = new ImmutableLocation(0, value, LocationType.UNDETERMINED, latitude, longitude, null);
        return new LocationAnnotation(start, start + value.length(), value, location);
    }

    static final double parseDms(String dmsString) {
        Matcher matcher = PATTERN_DMS.matcher(dmsString);
        if (!matcher.find()) {
            throw new NumberFormatException(dmsString + " could not be parsed in DMS format.");
        }
        int degrees = Integer.valueOf(matcher.group(1));
        int minutes = Integer.valueOf(matcher.group(2));
        int seconds = Integer.valueOf(matcher.group(3));
        int sign = "W".equals(matcher.group(4)) || "S".equals(matcher.group(4)) ? -1 : 1;

        return sign * (degrees + minutes / 60. + seconds / 3600.);
    }

}
