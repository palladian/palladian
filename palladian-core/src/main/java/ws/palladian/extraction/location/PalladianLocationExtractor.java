package ws.palladian.extraction.location;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private final LocationDisambiguation disambiguation;

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
        // this.disambiguation = new FirstDisambiguation(locationSource);
        // this.disambiguation = new BaselineDisambiguation();
        this.disambiguation = new ProximityDisambiguation();
        // this.disambiguation = new ClusteringDisambiguation();
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<Annotated> taggedEntities = tagger.getAnnotations(text);
        filterPersonEntities(taggedEntities);
        clean2(taggedEntities);

        LocationLookup cache = fetchLocations(taggedEntities);

        List<LocationAnnotation> locationEntities = disambiguation.disambiguate(taggedEntities, cache);

        // last step, recognize streets. For also extracting ZIP codes, this needs to be better integrated into above's
        // workflow. We should use the CITY annotations, to search for neighboring ZIP codes.
        AddressTagger addressTagger = new AddressTagger();
        List<LocationAnnotation> annotatedStreets = addressTagger.getAnnotations(text);
        locationEntities.addAll(annotatedStreets);

        return locationEntities;
    }

    private void clean2(List<Annotated> taggedEntities) {
        Iterator<Annotated> iterator = taggedEntities.iterator();
        while (iterator.hasNext()) {
            Annotated annotation = iterator.next();
            String entityValue = annotation.getValue();
            entityValue = cleanName(entityValue);
            boolean remove = !StringHelper.isCompletelyUppercase(entityValue)
                    && stopTokenRemover.isStopword(entityValue);
            remove |= skipWords.contains(entityValue);
            if (remove) {
                iterator.remove();
            }
        }
    }

    private LocationLookup fetchLocations(List<Annotated> annotations) {
        Set<String> query = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String entityValue = cleanName(annotation.getValue());
            if (StringHelper.isCompletelyUppercase(entityValue) || isAcronymSeparated(entityValue)) {
                query.add(entityValue.replace(".", ""));
                query.add(makeAcronymSeparated(entityValue.replace(".", "")));
                continue;
            }
            query.add(entityValue);
        }
        return LocationLookup.lookupLocations(locationSource, query, EnumSet.of(Language.ENGLISH));
    }

    static class LocationLookup {

        private final MultiMap<String, Location> map;

        public static LocationLookup lookupLocations(LocationSource source, Set<String> query, Set<Language> languages) {
            Collection<Location> locations = source.getLocations(query, languages);
            MultiMap<String, Location> map = MultiMap.create();
            for (Location location : locations) {
                map.add(StringUtils.stripAccents(location.getPrimaryName().toLowerCase()), location);
                for (AlternativeName altName : location.getAlternativeNames()) {
                    if (altName.getLanguage() == null || languages.contains(altName.getLanguage())) {
                        map.add(StringUtils.stripAccents(altName.getName().toLowerCase()), location);
                    }
                }
            }
            return new LocationLookup(map);
        }

        private LocationLookup(MultiMap<String, Location> map) {
            this.map = map;
        }

        public Collection<Location> get(String name) {
            Collection<Location> result = CollectionHelper.newHashSet();
            List<Location> temp = map.get(cleanName(StringUtils.stripAccents(name.toLowerCase())));
            if (temp != null) {
                result.addAll(temp);
            }
            if (StringHelper.isCompletelyUppercase(name)) {
                List<Location> temp2 = map.get(makeAcronymSeparated(name.toLowerCase()));
                if (temp2 != null) {
                    result.addAll(temp2);
                }
            }
            if (isAcronymSeparated(name)) {
                List<Location> temp2 = map.get(name.replace(".", "").toLowerCase());
                if (temp2 != null) {
                    result.addAll(temp2);
                }
            }
            return result;
        }

        public Collection<Location> getAll() {
            return map.allValues();
        }

        public Collection<Collection<Location>> getClusters() {
            Collection<Collection<Location>> clusters = CollectionHelper.newHashSet();
            for (List<Location> cluster : map.values()) {
                if (!cluster.isEmpty()) {
                    clusters.add(cluster);
                }
            }
            return clusters;
        }
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

    public static String cleanName(String entityValue) {
        entityValue = entityValue.replace("®", "");
        entityValue = entityValue.replace("™", "");
        entityValue = entityValue.replace("\\s+", " ");
        entityValue = entityValue.trim();
        return entityValue;
    }

    // FIXME -> not cool, NER learns that stuff and many more
    private static final List<String> PREFIXES = Arrays.asList("Mrs.", "Mrs", "Mr.", "Mr", "Ms.", "Ms", "President",
            "Minister", "General", "Sir", "Lady", "Democrat", "Republican", "Senator", "Chief", "Whip", "Reverend",
            "Detective", "Det", "Superintendent", "Supt", "Chancellor", "Cardinal", "Premier", "Representative",
            "Governor", "Minister", "Dr.", "Dr", "Professor", "Prof.", "Prof", "Lawyer", "Inspector", "Admiral",
            "Officer", "Cyclist", "Commissioner", "Olympian", "Sergeant", "Shareholder", "Coroner", "Constable",
            "Magistrate", "Judge", "Futurist", "Recorder", "Councillor", "Councilor", "King", "Reporter", "Leader",
            "Executive", "Justice", "Secretary", "Prince", "Congressman", "Skipper", "Liberal", "Analyst", "Major",
            "Writer", "Ombudsman", "Examiner", "Mayor");

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
        String rawText = FileHelper.readFileToString("/Users/pk/Desktop/LocationLab/TUD-Loc-2013_V2/text54.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38822240.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38765806.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38812825.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38543488.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38543534.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38543581.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);
        List<LocationAnnotation> locations = extractor.getAnnotations(cleanText);
        CollectionHelper.print(locations);
    }

}
