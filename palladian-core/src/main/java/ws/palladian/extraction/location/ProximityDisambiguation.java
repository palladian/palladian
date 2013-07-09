package ws.palladian.extraction.location;

import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.CONTINENT;
import static ws.palladian.extraction.location.LocationType.COUNTRY;
import static ws.palladian.extraction.location.LocationType.UNIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.LocationExtractorUtils.CoordinateFilter;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationTypeFilter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Disambiguation strategy based on anchor locations, and proximities.
 * </p>
 * 
 * @author Philipp Katz
 */
public class ProximityDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProximityDisambiguation.class);

    /** Maximum distance for anchoring. */
    private static final int DISTANCE_THRESHOLD = 150;

    /** Minimum population for anchoring. */
    private static final int LOWER_POPULATION_THRESHOLD = 5000;

    /** Minimum population for a location to become anchor. */
    private static final int ANCHOR_POPULATION_THRESHOLD = 1000000;

    /** Maximum distance between two locations with equal name, to assume they are the same. */
    private static final int SAME_DISTANCE_THRESHOLD = 50;

    @Override
    public List<LocationAnnotation> disambiguate(String text, List<Annotated> annotations,
            MultiMap<String, Location> locations) {

        List<LocationAnnotation> result = CollectionHelper.newArrayList();

        Set<Location> anchors = getAnchors(locations);

        for (Annotated annotation : annotations) {
            String value = LocationExtractorUtils.normalizeName(annotation.getValue());
            Collection<Location> candidates = locations.get(value);
            if (candidates.isEmpty()) {
                LOGGER.debug("'{}' could not be found and will be dropped", annotation.getValue());
                continue;
            }

            LOGGER.debug("'{}' has {} candidates", annotation.getValue(), candidates.size());

            // for distance checks below, only consider anchor locations, which are not in the current candidate set
            Collection<Location> currentAnchors = new HashSet<Location>(anchors);
            currentAnchors.removeAll(candidates);

            Set<Location> preselection = CollectionHelper.newHashSet();

            for (Location candidate : candidates) {
                if (anchors.contains(candidate)) {
                    LOGGER.debug("{} is in anchors", candidate);
                    preselection.add(candidate);
                    continue;
                }
                for (Location anchor : currentAnchors) {
                    double distance = GeoUtils.getDistance(candidate, anchor);
                    LocationType anchorType = anchor.getType();
                    if (distance < DISTANCE_THRESHOLD) {
                        LOGGER.debug("Distance of {} to anchors: {}", distance, candidate);
                        preselection.add(candidate);
                        // XXX anchor type checking does not seem to be necessary.
                    } else if (anchorType == CITY || anchorType == UNIT || anchorType == COUNTRY) {
                        if (LocationExtractorUtils.isChildOf(candidate, anchor)
                                && candidate.getPopulation() > LOWER_POPULATION_THRESHOLD) {
                            LOGGER.debug("{} is child of anchor '{}'", candidate, anchor.getPrimaryName());
                            preselection.add(candidate);
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

    private static Location selectLocation(Collection<Location> selection) {

        // if we have a continent, take the continent
        Set<Location> result = LocationExtractorUtils.filterConditionally(selection, new LocationTypeFilter(CONTINENT));
        if (result.size() == 1) {
            return CollectionHelper.getFirst(result);
        }

        List<Location> temp = new ArrayList<Location>(selection);
        Collections.sort(temp, new Comparator<Location>() {
            @Override
            public int compare(Location l1, Location l2) {

                // if locations are nested, take the "deepest" one
                if (LocationExtractorUtils.isDirectChildOf(l2, l1)) {
                    return 1;
                } else if (LocationExtractorUtils.isDirectChildOf(l1, l2)) {
                    return -1;
                }

                // as last step, compare by population
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

            }
        });
        return CollectionHelper.getFirst(temp);
    }

    private static Set<Location> getAnchors(MultiMap<String, Location> locations) {
        Set<Location> anchorLocations = CollectionHelper.newHashSet();

        // get prominent anchor locations; continents, countries and locations with very high population
        for (Location location : locations.allValues()) {
            LocationType type = location.getType();
            long population = location.getPopulation() != null ? location.getPopulation() : 0;
            if (type == CONTINENT || type == COUNTRY || population > ANCHOR_POPULATION_THRESHOLD) {
                LOGGER.debug("Prominent anchor location: {}", location);
                anchorLocations.add(location);
            }
        }

        // get unique and unambiguous locations; location whose name only occurs once, or which are very closely
        // together (because we might have multiple entries in the database with the same name which lie on a cluster)
        for (String name : locations.keySet()) {
            Collection<Location> group = locations.get(name);

            // in case we have locations with same name, but once with and without coordinates in the DB, we drop those
            // without coordinates
            group = LocationExtractorUtils.filterConditionally(group, new CoordinateFilter());

            if (LocationExtractorUtils.getLargestDistance(group) < SAME_DISTANCE_THRESHOLD) {
//                for (Location location : group) {
//                    long population = location.getPopulation() != null ? location.getPopulation() : 0;
//                    if (population > LOWER_POPULATION_THRESHOLD || name.split("\\s").length > 2) {
//                        LOGGER.debug("Unambiguous anchor location: {}", location);
//                        anchorLocations.add(location);
//                    }
//                }
                Location location = LocationExtractorUtils.getBiggest(group);
                if (location.getPopulation() > LOWER_POPULATION_THRESHOLD || name.split("\\s").length > 2) {
                    anchorLocations.add(location);
                }
            } else {
                LOGGER.debug("Ambiguous location: {} ({} candidates)", name, group.size());
            }
        }

        // if we could not get any anchor locations, just take the biggest one from the given candidates
        if (anchorLocations.isEmpty()) {
            Location biggest = LocationExtractorUtils.getBiggest(locations.allValues());
            if (biggest != null) {
                LOGGER.debug("Biggest anchor location: {}", biggest);
                anchorLocations.add(biggest);
            }
        }
        return anchorLocations;
    }

}
