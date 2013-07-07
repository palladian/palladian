package ws.palladian.extraction.location;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.classification.featureselection.BackwardFeatureElimination;
import ws.palladian.classification.featureselection.FeatureRanker;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.featureselection.InformationGainFeatureRanker;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Annotated;

public class FeatureBasedDisambiguationTrainer {

    static LocationSource locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
    static EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();
    static AnnotationFilter filter = new AnnotationFilter();
    static FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation();

    public static void main(String[] args) {
        // String csvFilePath = "/Users/pk/Code/palladian/palladian-core/location_disambiguation_1373097352488.csv";
        // List<Trainable> dataset = ClassificationUtils.readCsv(csvFilePath, true);
        // dataset = ClassificationUtils.filterFeatures(dataset, InverseFilter.create(new RegexFilter("marker=.*")));
        // performFeatureSelection(dataset);
        // performBackwardElimination(dataset);

        StopWatch stopWatch = new StopWatch();
        File goldStandardFileFolderPath = new File("/Users/pk/Desktop/TUD-Loc-2013_V2_train");

        File[] files = FileHelper.getFiles(goldStandardFileFolderPath.getPath(), "text");
        Map<String, SortedMap<Integer, GeoCoordinate>> coordinates = LocationExtractionEvaluator
                .readCoordinatesCsv(new File(goldStandardFileFolderPath, "coordinates.csv"));

        // addFile(coordinates, new File(goldStandardFileFolderPath, "text7.txt"));
        // System.exit(0);

        for (int i = 0; i < files.length; i++) {
            ProgressHelper.printProgress(i, files.length, 10, stopWatch);
            File file = files[i];

            addFile(coordinates, file);
        }

        disambiguation.buildModel();
    }

    static void addFile(Map<String, SortedMap<Integer, GeoCoordinate>> coordinates, File file) {
        String rawText = FileHelper.readFileToString(file);
        String cleanText = HtmlHelper.stripHtmlTags(rawText);
        List<Annotated> taggedEntities = tagger.getAnnotations(cleanText);
        taggedEntities = filter.filter(taggedEntities);
        MultiMap<String, Location> locations = fetchLocations(taggedEntities);

        SortedMap<Integer, GeoCoordinate> fileCoordinates = coordinates.get(file.getName());
        Set<Location> positive = getPositiveLocations(rawText, fileCoordinates);
        disambiguation.addTrainData(cleanText, taggedEntities, locations, positive, file.getName());
    }

    private static Set<Location> getPositiveLocations(String rawText, SortedMap<Integer, GeoCoordinate> coordinates) {
        rawText = rawText.replace(" role=\"main\"", "");
        Annotations<ContextAnnotation> anootations = FileFormatParser.getAnnotationsFromXmlText(rawText);

        Set<Location> result = CollectionHelper.newHashSet();
        for (ContextAnnotation annotation : anootations) {
            GeoCoordinate coordinate = coordinates.get(annotation.getStartPosition());
            Double longitude = coordinate != null ? coordinate.getLongitude() : null;
            Double latitude = coordinate != null ? coordinate.getLatitude() : null;
            Location location = new ImmutableLocation(annotation.getValue().hashCode(), annotation.getValue(),
                    LocationType.map(annotation.getTag()), latitude, longitude, 0l);
            result.add(location);
        }
        return result;
    }

    private static MultiMap<String, Location> fetchLocations(List<Annotated> annotations) {
        Set<String> valuesToRetrieve = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue());
            valuesToRetrieve.add(entityValue);
        }
        return locationSource.getLocations(valuesToRetrieve, EnumSet.of(Language.ENGLISH));
    }

    static void performFeatureSelection(List<Trainable> dataset) {
        FeatureRanker ranker = new InformationGainFeatureRanker();
        FeatureRanking featureRanking = ranker.rankFeatures(dataset);
        System.out.println(featureRanking);
    }
    
    static void performBackwardElimination(List<Trainable> dataset) {
        BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();
        Function<ConfusionMatrix, Double> scorer = new Function<ConfusionMatrix, Double>() {
            @Override
            public Double compute(ConfusionMatrix input) {
                return input.getF(1.0, "true");
            }
        };
        BackwardFeatureElimination<BaggedDecisionTreeModel> elimination = new BackwardFeatureElimination<BaggedDecisionTreeModel>(
                classifier, classifier, scorer);
        FeatureRanking featureRanking = elimination.rankFeatures(dataset);
        System.out.println(featureRanking);
    }

}
