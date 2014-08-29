package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * @author Philipp Katz
 */
public final class LocationExtractorUtils {

    /** {@link Function} to unwrap a {@link Location} from a {@link LocationAnnotation}. */
    public static final Function<LocationAnnotation, Location> ANNOTATION_LOCATION_FUNCTION = new Function<LocationAnnotation, Location>() {
        @Override
        public Location compute(LocationAnnotation annotation) {
            return annotation.getLocation();
        }
    };

    /** {@link Function} for unwrapping a {@link GeoCoordinate} from a {@link Location}. */
    public static final Function<Location, GeoCoordinate> LOCATION_COORDINATE_FUNCTION = new Function<Location, GeoCoordinate>() {
        @Override
        public GeoCoordinate compute(Location location) {
            return location.getCoordinate();
        }
    };

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

//    /**
//     * <p>
//     * Get the biggest {@link Location} from the given {@link Collection}.
//     * </p>
//     * 
//     * @param locations The locations.
//     * @return The {@link Location} with the highest population, or <code>null</code> in case the collection was empty,
//     *         or none of the locations has a population specified.
//     */
//    public static Location getBiggest(Collection<? extends Location> locations) {
//        Validate.notNull(locations, "locations must not be null");
//        Location biggest = null;
//        for (Location location : locations) {
//            Long population = location.getPopulation();
//            if (population == null) {
//                continue;
//            }
//            if (biggest == null || population > biggest.getPopulation()) {
//                biggest = location;
//            }
//        }
//        return biggest;
//    }

//    /**
//     * <p>
//     * Get the highest population from the given {@link Collection} of {@link Location}s.
//     * </p>
//     * 
//     * @param locations The locations, not <code>null</code>.
//     * @return The count of the highest population, or zero, in case the collection was empty or non of the locations
//     *         had a population value.
//     */
//    public static long getHighestPopulation(Collection<Location> locations) {
//        Validate.notNull(locations, "locations must not be null");
//        Location biggestLocation = getBiggest(locations);
//        if (biggestLocation == null || biggestLocation.getPopulation() == null) {
//            return 0;
//        }
//        return biggestLocation.getPopulation();
//    }

    /**
     * <p>
     * For each pair in the given Collection of {@link GeoCoordinate}s determine the distance, and return the highest
     * distance.
     * </p>
     * 
     * @param locations {@link Collection} of {@link GeoCoordinate}s, not <code>null</code>.
     * @return The maximum distance between any pair in the given {@link Collection}, or zero in case the collection was
     *         empty.
     * @see #largestDistanceBelow(double, Collection) is faster, if you just care about a maximum value.
     */
    public static double getLargestDistance(Collection<? extends GeoCoordinate> coordinates) {
        Validate.notNull(coordinates, "coordinates must not be null");
        if (coordinates.contains(null) && coordinates.size() > 1) { // multiple null coordinates?
            return Double.MAX_VALUE;
        }
        double largestDistance = 0;
        List<GeoCoordinate> temp = new ArrayList<GeoCoordinate>(coordinates);
        for (int i = 0; i < temp.size(); i++) {
            GeoCoordinate c1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                GeoCoordinate c2 = temp.get(j);
                largestDistance = Math.max(largestDistance, c1.distance(c2));
            }
        }
        return largestDistance;
    }

    /**
     * <p>
     * For each pair in the given Collection of {@link GeoCoordinate}s determine, if its distance is below the specified
     * distance.
     * </p>
     * 
     * @param distance The distance threshold, larger/equal zero.
     * @param locations {@link Collection} of {@link GeoCoordinate}s, not <code>null</code>.
     * @return <code>true</code> in case the distance for each pair in the given collection is below the specified
     *         distance, <code>false</code> otherwise.
     */
    public static boolean largestDistanceBelow(double distance, Collection<? extends GeoCoordinate> coordinates) {
        Validate.isTrue(distance >= 0, "distance must be greater/equal zero.");
        Validate.notNull(coordinates, "coordinates must not be null");
        if (coordinates.contains(null) && coordinates.size() > 1) {
            return false;
        }
        List<GeoCoordinate> temp = new ArrayList<GeoCoordinate>(coordinates);
        for (int i = 0; i < temp.size(); i++) {
            GeoCoordinate c1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                GeoCoordinate c2 = temp.get(j);
                if (c1.distance(c2) >= distance) {
                    return false;
                }
            }
        }
        return true;
    }

//    public static <T> Set<T> filterConditionally(Collection<T> set, Filter<T> filter) {
//        Set<T> temp = new HashSet<T>(set);
//        CollectionHelper.remove(temp, filter);
//        return temp.size() > 0 ? temp : new HashSet<T>(set);
//    }

//    /**
//     * <p>
//     * Check, whether the given {@link Collection} contains a {@link Location} of one of the specified
//     * {@link LocationType}s.
//     * </p>
//     * 
//     * @param locations The locations, not <code>null</code>.
//     * @param types The {@link LocationType}s for which to check.
//     * @return <code>true</code> in case there is at least one location of the specified types, <code>false</code>
//     *         otherwise.
//     */
//    public static boolean containsType(Collection<Location> locations, LocationType... types) {
//        for (LocationType type : types) {
//            for (Location location : locations) {
//                if (location.getType() == type) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

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
        Set<String> allNames = CollectionHelper.newHashSet();
        for (Location location : locations) {
            Set<String> currentNames = location.collectAlternativeNames();
            if (allNames.size() > 0) {
                Set<String> tempIntersection = new HashSet<String>(allNames);
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

    private LocationExtractorUtils() {
        // thou shalt not instantiate
    }

}
