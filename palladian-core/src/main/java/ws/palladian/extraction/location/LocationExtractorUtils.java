package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;

/**
 * @author Philipp Katz
 */
final class LocationExtractorUtils {

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

    public static boolean isChildOf(Location child, Location parent) {
        return child.getAncestorIds().contains(parent.getId());
    }

    public static boolean isDirectChildOf(Location child, Location parent) {
        Integer firstId = CollectionHelper.getFirst(child.getAncestorIds());
        if (firstId == null) {
            return false;
        }
        return firstId == parent.getId();
    }

    public static Location getBiggest(Collection<Location> locations) {
        Location biggest = null;
        for (Location location : locations) {
            Long population = location.getPopulation();
            if (population == null) {
                continue;
            }
            if (biggest == null || population > biggest.getPopulation()) {
                biggest = location;
            }
        }
        return biggest;
    }

    public static double getLargestDistance(Collection<Location> locations) {
        double largestDistance = 0;
        List<Location> temp = new ArrayList<Location>(locations);
        for (int i = 0; i < temp.size(); i++) {
            Location l1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                Location l2 = temp.get(j);
                largestDistance = Math.max(largestDistance, GeoUtils.getDistance(l1, l2));
            }
        }
        return largestDistance;
    }

    public static <T> Set<T> filterConditionally(Collection<T> set, Filter<T> filter) {
        Set<T> temp = new HashSet<T>(set);
        CollectionHelper.filter(temp, filter);
        return temp.size() > 0 ? temp : new HashSet<T>(set);
    }

    /**
     * <p>
     * Check, whether two {@link Location}s share a common name. Names are normalized according to the rules given in
     * {@link #normalizeName(String)}.
     * </p>
     * 
     * @param l1 First location, not <code>null</code>.
     * @param l2 Second location, not <code>null</code>.
     * @return <code>true</code>, if a common name exists, <code>false</code> otherwise.
     */
    public static boolean commonName(Location l1, Location l2) {
        Set<String> names1 = collectNames(l1);
        Set<String> names2 = collectNames(l2);
        names1.retainAll(names2);
        return names1.size() > 0;
    }

    private static Set<String> collectNames(Location location) {
        Set<String> names = CollectionHelper.newHashSet();
        names.add(normalizeName(location.getPrimaryName()));
        for (AlternativeName alternativeName : location.getAlternativeNames()) {
            names.add(normalizeName(alternativeName.getName()));
        }
        return names;
    }

    public static class LocationTypeFilter implements Filter<Location> {

        private final LocationType type;

        public LocationTypeFilter(LocationType type) {
            this.type = type;
        }

        @Override
        public boolean accept(Location item) {
            return item.getType() == type;
        }

    }

    public static class CoordinateFilter implements Filter<Location> {
        @Override
        public boolean accept(Location item) {
            return item.getLatitude() != null && item.getLongitude() != null;
        }

    }

    private LocationExtractorUtils() {

    }

}
