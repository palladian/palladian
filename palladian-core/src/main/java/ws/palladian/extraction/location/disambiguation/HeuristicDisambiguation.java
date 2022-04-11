package ws.palladian.extraction.location.disambiguation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.location.*;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;

import java.util.*;

import static ws.palladian.extraction.location.LocationExtractorUtils.LOCATION_COORDINATE_FUNCTION;
import static ws.palladian.extraction.location.LocationFilters.coordinate;
import static ws.palladian.extraction.location.LocationFilters.type;
import static ws.palladian.extraction.location.LocationType.*;

/**
 * <p>
 * Heuristic disambiguation strategy based on anchor locations, and proximities.
 * </p>
 *
 * @author Philipp Katz
 */
public class HeuristicDisambiguation implements LocationDisambiguation {
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicDisambiguation.class);

    public static final int ANCHOR_DISTANCE_THRESHOLD = 100;

    public static final int LOWER_POPULATION_THRESHOLD = 5000;

    public static final int ANCHOR_POPULATION_THRESHOLD = 1000000;

    public static final int SAME_DISTANCE_THRESHOLD = 50;

    public static final int LASSO_DISTANCE_THRESHOLD = 100;

    public static final int LOWER_UNLIKELY_POPULATION_THRESHOLD = 100000;

    public static final int TOKEN_THRESHOLD = 2;

    /** Maximum distance for anchoring. */
    private final int anchorDistanceThreshold;

    /** Minimum population for anchoring. */
    private final int lowerPopulationThreshold;

    /** Minimum population for a location to become anchor. */
    private final int anchorPopulationThreshold;

    /** Maximum distance between two locations with equal name, to assume they are the same. */
    private final int sameDistanceThreshold;

    /** Distance threshold when lasso heuristic stops. */
    private final int lassoDistanceThreshold;

    /** Threshold for population under which locations which are unlikely to be a location will be removed. */
    private final int lowerUnlikelyPopulationThreshold;

    private final int tokenThreshold;

    public HeuristicDisambiguation() {
        this(ANCHOR_DISTANCE_THRESHOLD, LOWER_POPULATION_THRESHOLD, ANCHOR_POPULATION_THRESHOLD, SAME_DISTANCE_THRESHOLD, LASSO_DISTANCE_THRESHOLD,
                LOWER_UNLIKELY_POPULATION_THRESHOLD, TOKEN_THRESHOLD);
    }

    /**
     * <p>
     * Create a new {@link HeuristicDisambiguation} with the specified settings.
     * </p>
     *
     * @param anchorDistanceThreshold          The maximum distance for a location to be "catched" by an anchor.
     * @param lowerPopulationThreshold         The minimum population threshold to be "catched " as child of an anchor.
     * @param anchorPopulationThreshold        The minimum population threshold for a location to become an anchor.
     * @param sameDistanceThreshold            The maximum distance between two locations with the same names to assume, that they
     *                                         are actually the same.
     * @param lassoDistanceThreshold           The distance threshold, when the lasso heuristic stops.
     * @param lowerUnlikelyPopulationThreshold The threshold below which locations will be removed, which have been
     *                                         classified as "unlikely" anyways (like person names, ...)
     * @param tokenThreshold
     */
    public HeuristicDisambiguation(int anchorDistanceThreshold, int lowerPopulationThreshold, int anchorPopulationThreshold, int sameDistanceThreshold, int lassoDistanceThreshold,
            int lowerUnlikelyPopulationThreshold, int tokenThreshold) {
        this.anchorDistanceThreshold = anchorDistanceThreshold;
        this.lowerPopulationThreshold = lowerPopulationThreshold;
        this.anchorPopulationThreshold = anchorPopulationThreshold;
        this.sameDistanceThreshold = sameDistanceThreshold;
        this.lassoDistanceThreshold = lassoDistanceThreshold;
        this.lowerUnlikelyPopulationThreshold = lowerUnlikelyPopulationThreshold;
        this.tokenThreshold = tokenThreshold;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {
        Set<Annotation> unlikelyLocations = getUnlikelyLocations(locations);
        locations.keySet().removeAll(unlikelyLocations);

        List<LocationAnnotation> result = new ArrayList<>();

        Set<Location> anchors = getAnchors(locations);

        for (Annotation annotation : locations.keySet()) {
            Collection<Location> candidates = locations.get(annotation);
            if (candidates.isEmpty()) {
                LOGGER.debug("'{}' could not be found and will be dropped", annotation.getValue());
                continue;
            }

            LOGGER.debug("'{}' has {} candidates", annotation.getValue(), candidates.size());

            // for distance checks below, only consider anchor locations, which are not in the current candidate set
            Collection<Location> currentAnchors = new HashSet<>(anchors);
            currentAnchors.removeAll(candidates);

            Set<Location> preselection = new HashSet<>();

            for (Location candidate : candidates) {
                if (anchors.contains(candidate)) {
                    LOGGER.debug("{} is in anchors", candidate);
                    preselection.add(candidate);
                    continue;
                }
                for (Location anchor : currentAnchors) {
                    if (candidate.getCoordinate() != null && anchor.getCoordinate() != null) {
                        double distance = candidate.getCoordinate().distance(anchor.getCoordinate());
                        if (distance < anchorDistanceThreshold) {
                            LOGGER.debug("Distance of {} to anchors: {}", distance, candidate);
                            preselection.add(candidate);
                            break;
                        }
                    }
                    if (Arrays.asList(CITY, UNIT, COUNTRY).contains(anchor.getType())) {
                        Long population = CollectionHelper.coalesce(candidate.getPopulation(), 0L);
                        if (candidate.descendantOf(anchor) && population > lowerPopulationThreshold) {
                            LOGGER.debug("{} is child of anchor '{}'", candidate, anchor.getPrimaryName());
                            preselection.add(candidate);
                            break;
                        }
                    }
                }
            }
            if (preselection.size() > 0) {
                Location selection = selectLocation(preselection);
                result.add(new LocationAnnotation(annotation, selection));
            }
        }
        return result;
    }

    private Set<Annotation> getUnlikelyLocations(MultiMap<ClassifiedAnnotation, Location> locations) {
        Set<Annotation> unlikelyLocations = new HashSet<>();
        for (ClassifiedAnnotation annotation : locations.keySet()) {
            LocationSet group = new LocationSet(locations.get(annotation));
            boolean likelyLocation = group.where(type(COUNTRY, CONTINENT)).size() > 0;
            boolean bigLocation = group.biggestPopulation() > lowerUnlikelyPopulationThreshold;
            if (likelyLocation || bigLocation) {
                continue;
            }
            if ("PER".equals(annotation.getCategoryEntries().getMostLikelyCategory())) {
                LOGGER.debug("{} does not seem to be a location and will be dropped", annotation);
                unlikelyLocations.add(annotation);
            }
        }
        LOGGER.debug("Spotted {} unlikely locations", unlikelyLocations.size());
        return unlikelyLocations;
    }

    /* package */ static Location selectLocation(Collection<Location> selection) {

        // if we have a continent, take the continent
        LocationSet result = new LocationSet(selection).whereConditionally(type(CONTINENT));
        if (result.size() == 1) {
            return CollectionHelper.getFirst(result);
        }

        List<Location> temp = new ArrayList<>(selection);
        
        // if locations are nested, take the "deepest" one
        List<Location> toRemove = new ArrayList<>();
        for (Location l1 : temp) {
        	for (Location l2 : temp) {
                if (l2.descendantOf(l1)) {
                    toRemove.add(l1);
                    break; // inner loop
                }
            }
        }
        temp.removeAll(toRemove);
        
        // as last step, compare by population
        temp.sort((l1, l2) -> {
            Long p1 = l1.getPopulation() != null ? l1.getPopulation() : 0;
            Long p2 = l2.getPopulation() != null ? l2.getPopulation() : 0;

            // XXX dirty hack; favor cities
            if (l1.getType() == CITY) {
                p1 *= 2;
            }
            if (l2.getType() == CITY) {
                p2 *= 2;
            }

            return p2.compareTo(p1);

        });
        return CollectionHelper.getFirst(temp);
    }

    private Set<Location> getAnchors(MultiMap<? extends Annotation, Location> locations) {
        Set<Location> anchorLocations = new HashSet<>();

        //        // check if one is contained in the other
        //        for (Annotation currentAnnotation : locations.keySet()) {
        //            Collection<Location> currentLocations = locations.get(currentAnnotation);
        //            // iterate through other locations
        //            for (Annotation otherAnnotation : locations.keySet()) {
        //                if (otherAnnotation.equals(currentAnnotation)) {
        //                    continue;
        //                }
        //                Collection<Location> otherLocations = locations.get(otherAnnotation);
        //                for (Location currentLocation : currentLocations) {
        //                    for (Location otherLocation : otherLocations) {
        //                        if (currentLocation.descendantOf(otherLocation)) {
        //                            LOGGER.debug("{} is descendant of {}", currentLocation, otherLocation);
        //                        }
        //                    }
        //                }
        //            }
        //        }

        // get prominent anchor locations; continents, countries and locations with very high population
        for (Location location : locations.allValues()) {
            LocationType type = location.getType();
            Long population = CollectionHelper.coalesce(location.getPopulation(), 0L);
            if (type == CONTINENT || type == COUNTRY || population > anchorPopulationThreshold) {
                LOGGER.debug("Prominent anchor location: {}", location);
                anchorLocations.add(location);
            }
        }

        // get unique and unambiguous locations; location whose name only occurs once, or which are very closely
        // together (because we might have multiple entries in the database with the same name which lie on a cluster)
        for (Annotation annotation : locations.keySet()) {
            LocationSet group = new LocationSet(locations.get(annotation));
            if (group.size() == 0) {
                continue;
            }
            String name = annotation.getValue();

            // in case we have locations with same name, but once with and without coordinates in the DB, we drop those
            // without coordinates
            group = group.whereConditionally(coordinate());

            if (group.largestDistance() < sameDistanceThreshold) {
                Location location = group.biggest();
                if (location == null) {
                    location = group.first();
                }
                if (location != null) {
                    Long population = CollectionHelper.coalesce(location.getPopulation(), 0l);
                    if (population > lowerPopulationThreshold || name.split("\\s").length >= tokenThreshold) {
                        anchorLocations.add(location);
                    }
                }
            } else {
                LOGGER.debug("Ambiguous location: {} ({} candidates)", name, group.size());
            }
        }

        // try the "lasso trick"; continuously remove locations which are over a specified threshold away from the
        // center of all locations; if at least two unique locations are left after this procedure, they serve as
        // anchors. This idea has been adopted from "Disambiguating Geographic Names in a Historical Digital Library" --
        // David A. Smith and Gregory Crane, 2001. They do not have such a cool name for it though.
        if (anchorLocations.isEmpty()) {
            anchorLocations.addAll(getLassoLocations(locations));
        }

        // if we could not get any anchor locations, just take the biggest one from the given candidates
        if (anchorLocations.isEmpty()) {
            Location biggest = new LocationSet(locations.allValues()).biggest();
            if (biggest != null) {
                LOGGER.debug("No anchor found, took biggest location: {}", biggest);
                anchorLocations.add(biggest);
            }
        }

        if (anchorLocations.isEmpty()) {
            LOGGER.debug("No anchor found.");
        }
        return anchorLocations;
    }

    private Set<Location> getLassoLocations(MultiMap<? extends Annotation, Location> locations) {
        Set<Location> lassoLocations = new HashSet<Location>(locations.allValues());
        while (lassoLocations.size() > 1) {
            List<GeoCoordinate> coordinates = CollectionHelper.convertList(lassoLocations, LOCATION_COORDINATE_FUNCTION);
            CollectionHelper.removeNulls(coordinates);
            if (coordinates.isEmpty()) {
                break;
            }
            GeoCoordinate midpoint = GeoUtils.getMidpoint(coordinates);
            double maxDistance = Double.MIN_VALUE;
            Location farthestLocation = null;
            for (Location location : lassoLocations) {
                if (location.getCoordinate() != null) {
                    double distance = location.getCoordinate().distance(midpoint);
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        farthestLocation = location;
                    }
                }
            }
            if (maxDistance < lassoDistanceThreshold) {
                break;
            }
            lassoLocations.remove(farthestLocation);
            if (LOGGER.isDebugEnabled()) {
                Object[] logArgs = new Object[]{farthestLocation, midpoint, maxDistance, lassoLocations.size()};
                LOGGER.debug("Removed {}, distance to center {}: {}, {} items left", logArgs);
            }
        }

        if (lassoLocations.size() < 2 || LocationExtractorUtils.sameNames(lassoLocations)) {
            LOGGER.debug("Could not identify lasso locations");
            return Collections.emptySet();
        }
        LOGGER.debug("Identified {} locations via lasso trick", lassoLocations.size());

        // add parents of the given locations
        Set<Location> parents = new HashSet<>();
        for (Location location : lassoLocations) {
            for (Location other : locations.allValues()) {
                if (location.descendantOf(other)) {
                    if (parents.add(other)) {
                        LOGGER.debug("Added {} to lassos because it is parent of {}", other, location);
                    }
                }
            }
        }
        LOGGER.debug("Adding {} parents of lasso locations", parents.size());
        lassoLocations.addAll(parents);
        return lassoLocations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HeuristicDisambiguation [anchorDistanceThreshold=");
        builder.append(anchorDistanceThreshold);
        builder.append(", lowerPopulationThreshold=");
        builder.append(lowerPopulationThreshold);
        builder.append(", anchorPopulationThreshold=");
        builder.append(anchorPopulationThreshold);
        builder.append(", sameDistanceThreshold=");
        builder.append(sameDistanceThreshold);
        builder.append(", lassoDistanceThreshold=");
        builder.append(lassoDistanceThreshold);
        builder.append(", lowerUnlikelyPopulationThreshold=");
        builder.append(lowerUnlikelyPopulationThreshold);
        builder.append(", tokenThreshold=");
        builder.append(tokenThreshold);
        builder.append("]");
        return builder.toString();
    }
}
