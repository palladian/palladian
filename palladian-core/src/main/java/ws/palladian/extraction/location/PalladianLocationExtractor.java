package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.Annotation;

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

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();

    private final AnnotationFilter filter = new AnnotationFilter();

    private final LocationSource locationSource;

    private final LocationDisambiguation disambiguation;

    private final AddressTagger addressTagger = new AddressTagger();


    private final static boolean greedyRetrieval = false;

    public PalladianLocationExtractor(LocationSource locationSource, LocationDisambiguation disambiguation) {
        this.locationSource = locationSource;
        this.disambiguation = disambiguation;
    }

    public PalladianLocationExtractor(LocationSource locationSource) {
        this(locationSource, new HeuristicDisambiguation());
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<Annotated> taggedEntities = tagger.getAnnotations(text);
        taggedEntities = filter.filter(taggedEntities);

        MultiMap<Annotated, Location> locations = fetchLocations(locationSource, taggedEntities);

        Annotations<LocationAnnotation> result = new Annotations<LocationAnnotation>();

        List<LocationAnnotation> locationEntities = disambiguation.disambiguate(text, locations);
        result.addAll(locationEntities);

        // last step, recognize streets. For also extracting ZIP codes, this needs to be better integrated into above's
        // workflow. We should use the CITY annotations, to search for neighboring ZIP codes.
        List<LocationAnnotation> annotatedStreets = addressTagger.getAnnotations(text);
        result.addAll(annotatedStreets);

        result.sort();

        return result;
    }

    public static MultiMap<Annotated, Location> fetchLocations(LocationSource source, List<Annotated> annotations) {
        Set<String> valuesToRetrieve = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue());
            valuesToRetrieve.add(entityValue);
        }
        MultiMap<String, Location> lookup = source.getLocations(valuesToRetrieve, EnumSet.of(Language.ENGLISH));
        MultiMap<Annotated, Location> result = DefaultMultiMap.createWithSet();
        for (Annotated annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue());
            Collection<Location> locations = lookup.get(entityValue);
            if (locations.size() > 0) {
                result.addAll(annotation, locations);
            } else if (greedyRetrieval) {
                greedyRetrieve(source, annotation, result);
            }
        }
        return result;
    }

    // XXX experimental; commit to history and delete again.

    private static final AnnotationFilter filterCached = new AnnotationFilter();

    private static void greedyRetrieve(LocationSource source, Annotated annotation, MultiMap<Annotated, Location> result) {
        String[] parts = annotation.getValue().split("\\s");
        if (parts.length == 1) {
            return;
        }
//        for (String part : parts) {
//            String entityValue = LocationExtractorUtils.normalizeName(part);
//            int startPosition = annotation.getStartPosition() + annotation.getValue().indexOf(part);
//            Annotated newAnnotation = new Annotation(startPosition, part, "DEEP");
//            Collection<Location> lookup = source.getLocations(entityValue, EnumSet.of(Language.ENGLISH));
//            if (lookup.size() > 0) {
//                System.out.println("Deep retrieval for " + annotation.getValue() + " found " + lookup.size()
//                        + " locations for part " + part + ".");
//                result.addAll(newAnnotation, lookup);
//            }
//        }
        MultiMap<Annotated, Location> additionalLocations = DefaultMultiMap.createWithSet();
        String trimmedValue = annotation.getValue();
        for (;;) {
            int idx = trimmedValue.lastIndexOf(' ');
            if (idx == -1) {
                break;
            }
            trimmedValue = trimmedValue.substring(0, idx);
            String entityValue = LocationExtractorUtils.normalizeName(trimmedValue);
            Collection<Location> lookup = source.getLocations(entityValue, EnumSet.of(Language.ENGLISH));
            if (lookup.size() > 0) {
                LOGGER.debug("Deep retrieval for {} found {} locations for part {}.", annotation.getValue(),
                        lookup.size(), trimmedValue);
                Annotated newAnnotation = new Annotation(annotation.getStartPosition(), trimmedValue,
                        StringTagger.CANDIDATE_TAG);
                additionalLocations.addAll(newAnnotation, lookup);
                break;
            }
        }
        List<Annotated> filtered = filterCached.filter(new ArrayList<Annotated>(additionalLocations.keySet()));
        additionalLocations.keySet().retainAll(filtered);
        result.addAll(additionalLocations);
    }

    @Override
    public String getName() {
        return String.format("PalladianLocationExtractor:%s", disambiguation);
    }

    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        PalladianLocationExtractor extractor = new PalladianLocationExtractor(database);
        String rawText = FileHelper
        // .readFileToString("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/0-all/text70.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38822240.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38765806.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38812825.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38543488.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38543534.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38543581.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_40996796.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_43664193.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41205662.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_38543581.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41840564.txt");
                .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41840564.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38551711.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);
        List<LocationAnnotation> locations = extractor.getAnnotations(cleanText);
        CollectionHelper.print(locations);
    }

}
