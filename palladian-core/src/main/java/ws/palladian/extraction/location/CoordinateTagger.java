package ws.palladian.extraction.location;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;

/**
 * <p>
 * {@link Tagger} for extracting geographic coordinates from text. Supported are coordinates in DMS format (e.g.
 * '40°26′47″N'), in decimal format (e.g. '40.446195N'), and combinations thereof (e.g. '40° 26.7717'). The coordinates
 * are transformed to decimal format and can be obtained from {@link LocationAnnotation#getLocation()}.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Geographic_coordinate_conversion">Geographic coordinate conversion</a>
 * @author Philipp Katz
 */
public final class CoordinateTagger implements Tagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateTagger.class);

    /** The name of the tag to be assigned. */
    public static final String TAG_NAME = "geoCoordinate";

    private static final String LEFT = "(?<=^|\\s)";
    private static final String RIGHT = "\\b";
    private static final String DEG = "([-+]?\\d{1,3}\\.\\d{1,10})([NSWE])?";
    private static final String DMS = "([-+]?\\d{1,3})[°d:](?:\\s?(\\d{2}(?:\\.\\d{1,10})?))?['′:]?(?:\\s?(\\d{2}(?:\\.\\d{1,10})?))?[\"″]?(?:\\s?([NSWE]))?";
    private static final String SEP = "(?:,\\s?|\\s)";

    /** Only degrees, as real number. */
    private static final Pattern PATTERN_DEG = Pattern.compile(LEFT + "(" + DEG + ")" + SEP + "(" + DEG + ")" + RIGHT);

    /** DMS scheme, and/or combination with degrees. */
    private static final Pattern PATTERN_DMS = Pattern.compile(LEFT + "(" + DMS + ")" + SEP + "(" + DMS + ")" + RIGHT);

    /** For parsing a single DMS expression. */
    private static final Pattern PATTERN_PARSE_DMS = Pattern.compile(DMS);

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
        Matcher matcher = PATTERN_DEG.matcher(text);
        while (matcher.find()) {
            try {
                double lat = Double.valueOf(matcher.group(2));
                double lng = Double.valueOf(matcher.group(5));
                int sgnLat = "S".equals(matcher.group(3)) ? -1 : 1;
                int sgnLng = "W".equals(matcher.group(6)) ? -1 : 1;
                annotations.add(createAnnotation(matcher.start(), matcher.group(), sgnLat * lat, sgnLng * lng));
            } catch (NumberFormatException e) {
                LOGGER.debug("NumberFormatException while parsing " + matcher.group() + ": " + e.getMessage());
            }
        }

        matcher = PATTERN_DMS.matcher(text);
        while (matcher.find()) {
            try {
                double lat = dmsToDecimal(matcher.group(1));
                double lng = dmsToDecimal(matcher.group(6));
                annotations.add(createAnnotation(matcher.start(), matcher.group(), lat, lng));
            } catch (NumberFormatException e) {
                LOGGER.debug("NumberFormatException while parsing " + matcher.group() + ": " + e.getMessage());
            }
        }
        return annotations;
    }

    private static final LocationAnnotation createAnnotation(int start, String value, double latitude, double longitude) {
        Location location = new ImmutableLocation(0, value, LocationType.UNDETERMINED, latitude, longitude, null);
        return new LocationAnnotation(start, start + value.length(), value, location);
    }

    /**
     * <p>
     * Convert a DMS coordinate (degrees, minutes, seconds) to decimal degree.
     * </p>
     * 
     * @param dmsString The string with the DMS coordinate, not <code>null</code>.
     * @return The double value with decimal degree.
     * @throws NumberFormatException in case the string could not be parsed.
     */
    public static final double dmsToDecimal(String dmsString) {
        Matcher matcher = PATTERN_PARSE_DMS.matcher(dmsString);
        if (!matcher.matches()) {
            throw new NumberFormatException("The string " + dmsString + " could not be parsed in DMS format.");
        }
        int degrees = Integer.valueOf(matcher.group(1)); // degree value, including sign
        int sign; // the sign, determined either from hemisphere/meridien, or degree sign
        if (matcher.group(4) != null) {
            sign = "W".equals(matcher.group(4)) || "S".equals(matcher.group(4)) ? -1 : 1;
        } else {
            sign = matcher.group(1).startsWith("-") ? -1 : 1;
        }
        double minutes = matcher.group(2) != null ? Double.valueOf(matcher.group(2)) : 0;
        double seconds = matcher.group(3) != null ? Double.valueOf(matcher.group(3)) : 0;
        return sign * (Math.abs(degrees) + minutes / 60. + seconds / 3600.);
    }

    @SuppressWarnings("unused")
    private static final void printGroups(Matcher matcher) {
        for (int i = 0; i <= matcher.groupCount(); i++) {
            System.out.println(i + ":" + matcher.group(i));
        }
    }

}
