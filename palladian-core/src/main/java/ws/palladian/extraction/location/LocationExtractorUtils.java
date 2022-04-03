package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import java.util.function.Function;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * @author Philipp Katz
 */
public final class LocationExtractorUtils {

    /** {@link Function} to unwrap a {@link Location} from a {@link LocationAnnotation}. */
    public static final Function<LocationAnnotation, Location> ANNOTATION_LOCATION_FUNCTION = LocationAnnotation::getLocation;

    /** {@link Function} for unwrapping a {@link GeoCoordinate} from a {@link Location}. */
    public static final Function<Location, GeoCoordinate> LOCATION_COORDINATE_FUNCTION = Location::getCoordinate;

    public static String normalizeName(String value) {
        if (value.matches("([A-Z]\\.)+")) {
            value = value.replace(".", "");
        }
        value = value.replaceAll("[©®™]", "");
        value = value.replaceAll("\\s+", " ");
        if (value.equals("US")) {
            value = "U.S.";
        }
        return value;
    }

    /**
     * <p>
     * Check, whether at least two of the given locations in the {@link Collection} have different names (i.e. the
     * intersection of all names of each {@link Location} is empty).
     * </p>
     * 
     * @param locations The locations, not <code>null</code>.
     * @return <code>true</code> in case there is at least one pair in the given collection which does not share at
     *         least one name.
     */
    public static boolean differentNames(Collection<Location> locations) {
        Set<String> allNames = new HashSet<>();
        for (Location location : locations) {
            Set<String> currentNames = location.collectAlternativeNames();
            if (allNames.size() > 0) {
                Set<String> tempIntersection = new HashSet<>(allNames);
                tempIntersection.retainAll(currentNames);
                if (tempIntersection.isEmpty()) {
                    return true;
                }
            }
            allNames.addAll(currentNames);
        }
        return false;
    }

    public static boolean sameNames(Collection<Location> locations) {
        return !differentNames(locations);
    }
    
    public static Comparator<Location> distanceComparator(final GeoCoordinate coordinate) {
        Validate.notNull(coordinate, "coordinate must not be null");
        return (o1, o2) -> {
            double d1 = o1.getCoordinate().distance(coordinate);
            double d2 = o2.getCoordinate().distance(coordinate);
            return Double.compare(d1, d2);
        };
    }

    private LocationExtractorUtils() {
        // thou shalt not instantiate
    }

}
