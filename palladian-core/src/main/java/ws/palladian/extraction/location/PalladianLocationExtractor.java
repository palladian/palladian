package ws.palladian.extraction.location;

import ws.palladian.core.ClassifyingTagger;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.persistences.sqlite.SQLiteLocationSource;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;

import java.io.File;
import java.util.*;

/**
 * <p>
 * Given a text, the LocationDetector finds mentioned locations and returns annotations.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianLocationExtractor extends LocationExtractor {
    private final LocationSource locationSource;

    private final ClassifyingTagger tagger;

    private final LocationDisambiguation disambiguation;

    private static final AddressTagger addressTagger = AddressTagger.INSTANCE;

    private static final CoordinateTagger coordinateTagger = CoordinateTagger.INSTANCE;

    public PalladianLocationExtractor(LocationSource locationSource, ClassifyingTagger tagger, LocationDisambiguation disambiguation) {
        this.locationSource = locationSource;
        this.tagger = tagger;
        this.disambiguation = disambiguation;
    }

    public PalladianLocationExtractor(LocationSource locationSource, LocationDisambiguation disambiguation) {
        this(locationSource, DefaultCandidateExtractor.INSTANCE, disambiguation);
    }

    public PalladianLocationExtractor(LocationSource locationSource) {
        this(locationSource, DefaultCandidateExtractor.INSTANCE, new HeuristicDisambiguation());
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<ClassifiedAnnotation> classifiedEntities = tagger.getAnnotations(text);

        MultiMap<ClassifiedAnnotation, Location> locations = fetchLocations(locationSource, classifiedEntities);

        Annotations<LocationAnnotation> result = new Annotations<>();

        List<LocationAnnotation> locationEntities = disambiguation.disambiguate(text, locations);
        result.addAll(locationEntities);

        // last step, recognize streets. For also extracting ZIP codes, this needs to be better integrated into above's
        // workflow. We should use the CITY annotations, to search for neighboring ZIP codes.
        List<LocationAnnotation> annotatedStreets = addressTagger.getAnnotations(text);
        result.addAll(annotatedStreets);

        // extract explicit coordinate mentions in the text
        List<LocationAnnotation> annotatedCoordinates = coordinateTagger.getAnnotations(text);
        result.addAll(annotatedCoordinates);

        result.sort();
        result.removeNested();

        return result;
    }

    public static MultiMap<ClassifiedAnnotation, Location> fetchLocations(LocationSource source, List<ClassifiedAnnotation> annotations) {
        Set<String> valuesToRetrieve = new HashSet<>();
        for (ClassifiedAnnotation annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue()).toLowerCase();
            valuesToRetrieve.add(entityValue);
        }
        MultiMap<String, Location> lookup = source.getLocations(valuesToRetrieve, EnumSet.of(Language.ENGLISH));
        MultiMap<ClassifiedAnnotation, Location> result = DefaultMultiMap.createWithSet();
        for (ClassifiedAnnotation annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue()).toLowerCase();
            Collection<Location> locations = lookup.get(entityValue);
            if (locations.size() > 0) {
                result.addAll(annotation, locations);
            } else {
                result.addAll(annotation, Collections.emptySet());
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return String.format("PalladianLocationExtractor:%s", disambiguation);
    }

    public static void main(String[] args) {
        LocationSource database = SQLiteLocationSource.open(new File("/Users/pk/Desktop/locations_final.sqlite"));
        PalladianLocationExtractor extractor = new PalladianLocationExtractor(database);
        String rawText = "Rio, New York, or Tokyo.";
        // String rawText = FileHelper.tryReadFileToString("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/0-all/text64.txt");
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
