package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.entity.Annotations;
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

/**
 * <p>
 * Given a text, the LocationDetector finds mentioned locations and returns annotations.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianLocationExtractor extends LocationExtractor {

    /** Long annotations exceeding the specified token count, are split up and parts of them are treated as candidates. */
    public final static int LONG_ANNOTATION_SPLIT = 3;

    private static final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger(LONG_ANNOTATION_SPLIT);

    private static final AnnotationFilter filter = new AnnotationFilter();

    private final LocationSource locationSource;

    private final LocationDisambiguation disambiguation;

    private static final AddressTagger addressTagger = new AddressTagger();

    private static final ContextClassifier contextClassifier = new ContextClassifier(ClassificationMode.PROPAGATION);

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
        result.removeNested();

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
            } else {
                result.addAll(annotation, Collections.<Location> emptySet());
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return String.format("PalladianLocationExtractor:%s", disambiguation);
    }

    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        PalladianLocationExtractor extractor = new PalladianLocationExtractor(database);
        String rawText = FileHelper
                .readFileToString("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/0-all/text64.txt");
        // .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/0-all/text_44026163.txt");
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
