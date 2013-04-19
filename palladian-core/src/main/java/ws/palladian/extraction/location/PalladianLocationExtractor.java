package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Given a text, the LocationDetector finds mentioned locations and returns annotations.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLocationExtractor.class);

    // words that are unlikely to be a location
    private static final Set<String> skipWords;

    private final LocationSource locationSource;

    private final StopTokenRemover stopTokenRemover = new StopTokenRemover(Language.ENGLISH);

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();

    static {
        skipWords = new HashSet<String>();

        FileHelper.performActionOnEveryLine(
                PalladianLocationExtractor.class.getResourceAsStream("/locationsBlacklist.txt"), new LineAction() {
                    @Override
                    public void performAction(String line, int lineNumber) {
                        if (line.isEmpty() || line.startsWith("#")) {
                            return;
                        }
                        skipWords.add(line);
                    }
                });

    }

    public PalladianLocationExtractor(LocationSource locationSource) {
        this.locationSource = locationSource;
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {

        List<LocationAnnotation> locationEntities = CollectionHelper.newArrayList();

        Set<Location> anchorLocations = CollectionHelper.newHashSet();

        List<Annotated> taggedEntities = tagger.getAnnotations(text);
        filterPersonEntities(taggedEntities);

        Set<Collection<Location>> ambiguousLocations = CollectionHelper.newHashSet();

        MultiMap<String, Location> locationMap = MultiMap.create();

        MultiMap<String, Location> cache = fetchLocations(taggedEntities);

        // try to find them in the database
        for (Annotated locationCandidate : taggedEntities) {

            String entityValue = locationCandidate.getValue();

            entityValue = cleanName(entityValue);

            if (!StringHelper.isCompletelyUppercase(entityValue) && stopTokenRemover.isStopword(entityValue)) {
                continue;
            }

            if (skipWords.contains(entityValue)) {
                continue;
            }

            // search entities by name
            // Collection<Location> retrievedLocations = locationSource.getLocations(entityValue,
            // EnumSet.of(Language.ENGLISH));
            Collection<Location> retrievedLocations = cache.get(StringUtils.stripAccents(entityValue.toLowerCase()));
            
            // FIXME make nicer
            // XXX check total length, and avoid checking long capitalized words which are no acronyms (AMERICA)
            if (StringHelper.isCompletelyUppercase(entityValue) || isAcronymSeparated(entityValue)) {

                LOGGER.debug("**** Acronym treatment : " + entityValue);

                String temp1 = entityValue.replace(".", "");
                String temp2 = makeAcronymSeparated(temp1);
                retrievedLocations = CollectionHelper.newHashSet();
                List<Location> temp = cache.get(StringUtils.stripAccents(temp1.toLowerCase()));
                if (temp != null) {
                    retrievedLocations.addAll(temp);
                }
                temp = cache.get(StringUtils.stripAccents(temp2.toLowerCase()));
                if (temp != null) {
                    retrievedLocations.addAll(temp);
                }
            }

            if (retrievedLocations == null) {
                continue;
            }

            // if we retrieved locations with AND without coordinates, only keep those WITH coordinates
            Filter<Location> coordFilter = new Filter<Location>() {
                @Override
                public boolean accept(Location item) {
                    return item.getLatitude() != null && item.getLongitude() != null;
                }
            };
            HashSet<Location> temp = CollectionHelper
                    .filter(retrievedLocations, coordFilter, new HashSet<Location>());
            if (temp.size() > 0) {
                retrievedLocations = temp;
            }

            if (retrievedLocations.isEmpty()) {
                continue;
            }
            for (Location location : retrievedLocations) {
                if (EnumSet.of(LocationType.CONTINENT, LocationType.COUNTRY).contains(location.getType())) {
                    anchorLocations.add(location);
                }
                // XXX experimental : add places with high population count to
                // anchor locations. we should determine how to set a good threshold here.
                // improves recall/f1, slightly drops precision
                if (location.getPopulation() > 500000) {
                    LOGGER.debug("High prob location " + location);
                    anchorLocations.add(location);
                }

            }

            boolean ambiguous = checkAmbiguity(retrievedLocations);
            if (ambiguous) {
                ambiguousLocations.add(retrievedLocations);
                LOGGER.debug("- " + entityValue + " is ambiguous!");
            } else {
                LOGGER.debug("+ " + entityValue + " is not amiguous: " + retrievedLocations);
            }

            if (!locationMap.containsKey(entityValue)) {
                locationMap.addAll(entityValue, retrievedLocations);
            }

            Location location = selectLocation(retrievedLocations);

            LocationAnnotation locationAnnotation = new LocationAnnotation(locationCandidate, location);
            locationEntities.add(locationAnnotation);

            if (!ambiguous && entityValue.split("\\s").length >= 3) {
                LOGGER.debug("Adding {} to anchor locations, because of long name", location.getPrimaryName());
                anchorLocations.add(location);
            }
        }


        disambiguate(new HashSet<Location>(anchorLocations), locationMap);

        Set<Location> consolidatedLocations = CollectionHelper.newHashSet();
        consolidatedLocations.addAll(anchorLocations);
        for (List<Location> temp : locationMap.values()) {
            consolidatedLocations.addAll(temp);
        }

        Map<String, Location> finalResultsForCheck = CollectionHelper.newHashMap();

        Iterator<LocationAnnotation> iterator = locationEntities.iterator();
        Set<LocationAnnotation> toRemove = CollectionHelper.newHashSet();
        List<LocationAnnotation> toAdd = CollectionHelper.newArrayList();
        while (iterator.hasNext()) {
            LocationAnnotation annotation = iterator.next();
            String entityValue = annotation.getValue();

            entityValue = cleanName(entityValue);

            if (!locationMap.containsKey(entityValue)) {
                iterator.remove();
                continue;
            }
            if (locationMap.get(entityValue).size() == 0) {
                iterator.remove();
                continue;
            }
            if (locationMap.get(entityValue).size() > 1) {
                LOGGER.debug("Ambiguity for {}", entityValue);
            }
            Location loc = selectLocation(locationMap.get(entityValue));
            toRemove.add(annotation);

            toAdd.add(new LocationAnnotation(annotation, loc));

            finalResultsForCheck.put(annotation.getValue(), loc);
        }

        locationEntities.removeAll(toRemove);
        locationEntities.addAll(toAdd);

        Map<String, Location> clearMap = checkFinalResults(finalResultsForCheck, anchorLocations);
        iterator = locationEntities.iterator();
        while (iterator.hasNext()) {
            LocationAnnotation current = iterator.next();
            if (clearMap.containsKey(current.getValue())) {
                LOGGER.debug("- remove - " + current);
                iterator.remove();
            }
        }

        // last step, recognize streets. For also extracting ZIP codes, this needs to be better integrated into above's
        // workflow. We should use the CITY annotations, to search for neighboring ZIP codes.
        AddressTagger addressTagger = new AddressTagger();
        List<LocationAnnotation> annotatedStreets = addressTagger.getAnnotations(text);
        locationEntities.addAll(annotatedStreets);

        return locationEntities;
    }

    private MultiMap<String, Location> fetchLocations(List<? extends Annotated> annotations) {
        Set<String> valuesToRetrieve = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String entityValue = annotation.getValue();
            entityValue = cleanName(entityValue);

            if (!StringHelper.isCompletelyUppercase(entityValue) && stopTokenRemover.isStopword(entityValue)) {
                continue;
            }

            if (skipWords.contains(entityValue)) {
                continue;
            }
            if (StringHelper.isCompletelyUppercase(entityValue) || isAcronymSeparated(entityValue)) {
                valuesToRetrieve.add(entityValue.replace(".", ""));
                valuesToRetrieve.add(makeAcronymSeparated(entityValue.replace(".", "")));
                continue;
            }
            valuesToRetrieve.add(entityValue);
        }
        Collection<Location> locations = locationSource.getLocations(valuesToRetrieve, EnumSet.of(Language.ENGLISH));
        // FIXME multimap ---> Map<String, Set<>>
        MultiMap<String, Location> result = MultiMap.create();
        for (Location location : locations) {
            result.add(StringUtils.stripAccents(location.getPrimaryName().toLowerCase()), location);
            Collection<AlternativeName> alternativeNames = location.getAlternativeNames();
            for (AlternativeName alternativeName : alternativeNames) {
                if (alternativeName.getLanguage() == null || alternativeName.getLanguage() == Language.ENGLISH) {
                    result.add(StringUtils.stripAccents(alternativeName.getName().toLowerCase()), location);
                }
            }
        }
        return result;
    }

    private Map<String, Location> checkFinalResults(Map<String, Location> finalResultsForCheck,
            Set<Location> anchorLocations) {
        List<Entry<String, Location>> locationList = new ArrayList<Entry<String, Location>>(
                finalResultsForCheck.entrySet());
        Map<String, Location> toClear = CollectionHelper.newHashMap();
        for (int i = 0; i < locationList.size(); i++) {
            Location l1 = locationList.get(i).getValue();
            if (l1.getType() == LocationType.CONTINENT || l1.getType() == LocationType.COUNTRY
                    || l1.getType() == LocationType.REGION) {
                continue;
            }
            if (anchorLocations.contains(l1)) {
                continue; // always accepted.
            }
            double smallestDistance = Double.MAX_VALUE;
            Location smallestLoc = null;
            for (int j = 0; j < locationList.size(); j++) {
                Location l2 = locationList.get(j).getValue();
                if (l1.equals(l2)) {
                    continue;
                }
                double distance = getDistance(l1, l2);
                if (smallestDistance > distance) {
                    smallestDistance = distance;
                    smallestLoc = l2;
                }
            }
            if (l1.getPopulation() == null || l1.getPopulation() < 5000) {
                LOGGER.debug(l1.getPrimaryName() + " : " + smallestDistance + " --- " + smallestLoc);
                if (smallestDistance > 250) {
                    toClear.put(locationList.get(i).getKey(), l1);
                }
            }
        }
        return toClear;
    }

    public String cleanName(String entityValue) {
        entityValue = entityValue.replace("®", "");
        entityValue = entityValue.replace("™", "");
        entityValue = entityValue.replace("\\s+", " ");
        entityValue = entityValue.trim();
        return entityValue;
    }

    public static boolean isAcronymSeparated(String string) {
        return string.matches("([A-Z]\\.)+");
    }

    private static String makeAcronymSeparated(String entityValue) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < entityValue.length(); i++) {
            result.append(entityValue.charAt(i));
            result.append('.');
        }
        return result.toString();
    }

    private void disambiguate(Set<Location> anchorLocations, MultiMap<String, Location> ambiguousLocations) {

        Set<Location> toAdd = CollectionHelper.newHashSet();
        for (Location location : anchorLocations) {
            List<Location> hierarchy = locationSource.getLocations(location.getAncestorIds());
            for (Location currentLocation : hierarchy) {
                if (currentLocation.getPrimaryName().equalsIgnoreCase("earth")) {
                    continue;
                }
                toAdd.add(currentLocation);
            }
        }
        anchorLocations.addAll(toAdd);

        // if we have countries as anchors, we remove the continents, to be more precise.
        LocationTypeFilter countryFilter = new LocationTypeFilter(LocationType.COUNTRY);
        if (CollectionHelper.filter(anchorLocations, countryFilter, new HashSet<Location>()).size() > 0) {
            CollectionHelper.filter(anchorLocations, countryFilter);
        }

        if (anchorLocations.size() == 0) {
            LOGGER.debug("No anchor locations");
            return;
        }

        LOGGER.debug("Anchor locations: {}", anchorLocations);

        // go through each group
        for (String locationName : ambiguousLocations.keySet()) {

            LOGGER.debug(locationName);

            List<Location> list = ambiguousLocations.get(locationName);

            // check each location in group
            Iterator<Location> it = list.iterator();
            while (it.hasNext()) {

                Location location = it.next();

                boolean anchored = false;
                List<Integer> hierarchyIds = location.getAncestorIds();

                // XXX experimental code; also keep locations without hierarchy
                if (hierarchyIds.isEmpty()) {
                    anchored = true;
                }

                for (Location anchorLocation : anchorLocations) {
                    if (hierarchyIds.contains(anchorLocation.getId())) {
                        anchored = true;
                    }
                }

                // trivial case
                if (anchorLocations.contains(location)) {
                    anchored = true;
                }

                if (location.getType() == LocationType.CONTINENT) {
                    anchored = true;
                }

                LOGGER.debug(anchored + " -> " + location);

                if (!anchored) {
                    it.remove();
                }

            }
            LOGGER.debug("-----------");
        }

    }

    /**
     * Check, if a Collection of {@link ImmutableLocation}s are "ambiguous". The condition of ambiguity is fulfilled, if two
     * given Locations in the Collection have a greater distance then a specified threshold.
     * 
     * @param locations
     * @return
     */
    private boolean checkAmbiguity(Collection<Location> locations) {
        if (locations.size() <= 1) {
            return false;
        }
        List<Location> temp = new ArrayList<Location>(locations);
        for (int i = 0; i < temp.size(); i++) {
            Location location1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                Location location2 = temp.get(j);
                double distance = getDistance(location1, location2);
                if (distance > 50) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Select one location when multiple were retrieved.
     * 
     * @param retrievedLocations
     * @return
     */
    private Location selectLocation(Collection<Location> retrievedLocations) {
        List<Location> temp = new ArrayList<Location>(retrievedLocations);
        Collections.sort(temp, new Comparator<Location>() {
            @Override
            public int compare(Location l1, Location l2) {
                if (l1.getType() != l2.getType()) {
                    if (l1.getType() == LocationType.CONTINENT) {
                        return -1;
                    }
                    if (l2.getType() == LocationType.CONTINENT) {
                        return 1;
                    }
                }
                Long l1Population = l1.getPopulation();
                Long l2Population = l2.getPopulation();
                if (l1.getType() == LocationType.CITY) {
                    l1Population *= 2;
                }
                if (l2.getType() == LocationType.CITY) {
                    l2Population *= 2;
                }
                return l2Population.compareTo(l1Population);
            }
        });
        return CollectionHelper.getFirst(temp);
    }

    private double getDistance(Location city, Location country) {
        double distance = Integer.MAX_VALUE;

        try {

            Double lat1 = city.getLatitude();
            Double lng1 = city.getLongitude();
            Double lat2 = country.getLatitude();
            Double lng2 = country.getLongitude();

            distance = MathHelper.computeDistanceBetweenWorldCoordinates(lat1, lng1, lat2, lng2);

        } catch (Exception e) {
        }

        return distance;
    }

    // FIXME -> not cool, NER learns that stuff and many more
    private static final List<String> PREFIXES = Arrays.asList("Mrs.", "Mrs", "Mr.", "Mr", "Ms.", "Ms", "President",
            "Minister", "General", "Sir", "Lady", "Democrat", "Republican", "Senator", "Chief", "Whip", "Reverend",
            "Detective", "Det", "Superintendent", "Supt", "Chancellor", "Cardinal", "Premier", "Representative",
            "Governor", "Minister", "Dr.", "Dr", "Professor", "Prof.", "Prof", "Lawyer", "Inspector", "Admiral",
            "Officer", "Cyclist", "Commissioner", "Olympian", "Sergeant", "Shareholder", "Coroner", "Constable",
            "Magistrate", "Judge", "Futurist", "Recorder", "Councillor", "Councilor", "King", "Reporter", "Leader",
            "Executive", "Justice", "Secretary", "Prince", "Congressman", "Skipper", "Liberal", "Analyst", "Major",
            "Writer", "Ombudsman", "Examiner");

    private void filterPersonEntities(List<? extends Annotated> annotations) {
        Set<String> blacklist = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String value = annotation.getValue().toLowerCase();
            for (String prefix : PREFIXES) {
                if (value.contains(prefix.toLowerCase() + " ")) {
                    blacklist.addAll(Arrays.asList(annotation.getValue().toLowerCase().split("\\s")));
                }
                if (value.endsWith(" gmbh") || value.endsWith(" inc.") || value.endsWith(" co.")
                        || value.endsWith(" corp.")) {
                    blacklist.addAll(Arrays.asList(annotation.getValue().toLowerCase().split("\\s")));
                }
            }
        }
        Iterator<? extends Annotated> iterator = annotations.iterator();
        while (iterator.hasNext()) {
            Annotation annotation = (Annotation)iterator.next();
            String value = annotation.getValue().toLowerCase();
            boolean remove = blacklist.contains(value);
            for (String blacklistedItem : blacklist) {
                if (StringHelper.containsWord(blacklistedItem, value)) {
                    remove = true;
                    break;
                }
            }
            if (remove) {
                LOGGER.debug("Remove " + annotation);
                iterator.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "PalladianLocationExtractor";
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

    public static void main(String[] args) throws PageContentExtractorException {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        PalladianLocationExtractor extractor = new PalladianLocationExtractor(database);
        String rawText = FileHelper
                .readFileToString("/Users/pk/Desktop/LocationLab/LocationExtractionDataset/text40.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);
        List<LocationAnnotation> locations = extractor.getAnnotations(cleanText);
        CollectionHelper.print(locations);
    }

}
