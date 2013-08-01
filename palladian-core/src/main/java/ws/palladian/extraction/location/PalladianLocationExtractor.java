package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.location.ContextClassifier.ClassificationMode;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

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

    private final ContextClassifier contextClassifier = new ContextClassifier(ClassificationMode.PROPAGATION);

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
        List<Annotation> taggedEntities = tagger.getAnnotations(text);
        taggedEntities = filter.filter(taggedEntities);
        List<ClassifiedAnnotation> classifiedEntities = contextClassifier.classify(taggedEntities, text);

        CollectionHelper.remove(classifiedEntities, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation item) {
                String value = item.getValue();
                // the probability, that we are wrong when tagging one or two-letter abbreviations is very high, so we
                // discard them here, except for "US" and "UK".
                return value.equals("US") || value.equals("UK") || !value.matches("[A-Z]{1,2}|[A-Z]\\.");
            }
        });

        MultiMap<ClassifiedAnnotation, Location> locations = fetchLocations(locationSource, classifiedEntities);

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

    public static <A extends Annotation> MultiMap<A, Location> fetchLocations(LocationSource source, List<A> annotations) {
        Set<String> valuesToRetrieve = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue()).toLowerCase();
            valuesToRetrieve.add(entityValue);
        }
        MultiMap<String, Location> lookup = source.getLocations(valuesToRetrieve, EnumSet.of(Language.ENGLISH));
        MultiMap<A, Location> result = DefaultMultiMap.createWithSet();
        for (A annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue()).toLowerCase();
            Collection<Location> locations = lookup.get(entityValue);
            if (locations.size() > 0) {
                result.addAll(annotation, locations);
//            } else if (greedyRetrieval) {
//                greedyRetrieve(source, annotation, result);
            } else {
                result.addAll(annotation, Collections.<Location> emptySet());
            }
        }
        return result;
    }

    // XXX experimental; commit to history and delete again.

    private static final AnnotationFilter filterCached = new AnnotationFilter();

    private static void greedyRetrieve(LocationSource source, Annotation annotation, MultiMap<Annotation, Location> result) {
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
        MultiMap<Annotation, Location> additionalLocations = DefaultMultiMap.createWithSet();
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
                Annotation newAnnotation = new ImmutableAnnotation(annotation.getStartPosition(), trimmedValue,
                        StringTagger.CANDIDATE_TAG);
                additionalLocations.addAll(newAnnotation, lookup);
                break;
            }
        }
        List<Annotation> filtered = filterCached.filter(new ArrayList<Annotation>(additionalLocations.keySet()));
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
        // .readFileToString("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/0-all/text74.txt");
                .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_40866507.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_38765806.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_38812825.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41521706.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_38543534.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_38543581.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_40996796.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_43664193.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41205662.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_38543581.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41840564.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_34647085.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_41298996.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/text_38551711.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);
        List<LocationAnnotation> locations = extractor.getAnnotations(cleanText);
        CollectionHelper.print(locations);
    }

}
