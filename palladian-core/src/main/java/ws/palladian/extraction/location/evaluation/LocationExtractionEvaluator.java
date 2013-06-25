package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.YahooLocationExtractor;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.Annotated;

public final class LocationExtractionEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationExtractionEvaluator.class);

    public static void evaluate(LocationExtractor extractor, String goldStandardFileFolderPath) {
        Validate.notNull(extractor, "extractor must not be null");
        Validate.notEmpty(goldStandardFileFolderPath, "goldStandardFileFolderPath must not be empty");

        if (!new File(goldStandardFileFolderPath).isDirectory()) {
            throw new IllegalArgumentException("The provided path to the gold standard '" + goldStandardFileFolderPath
                    + "' does not exist or is no directory.");
        }

        Map<ResultType, Map<String, Collection<Annotated>>> errors = new LinkedHashMap<ResultType, Map<String, Collection<Annotated>>>();
        errors.put(ResultType.CORRECT, new HashMap<String, Collection<Annotated>>());
        errors.put(ResultType.ERROR1, new HashMap<String, Collection<Annotated>>());
        errors.put(ResultType.ERROR2, new HashMap<String, Collection<Annotated>>());
        errors.put(ResultType.ERROR3, new HashMap<String, Collection<Annotated>>());
        errors.put(ResultType.ERROR4, new HashMap<String, Collection<Annotated>>());
        errors.put(ResultType.ERROR5, new HashMap<String, Collection<Annotated>>());


        File[] files = FileHelper.getFiles(goldStandardFileFolderPath, "text");

        // for macro averaging
        double precisionMuc = 0;
        double precisionExact = 0;
        double recallMuc = 0;
        double recallExact = 0;

        EvaluationResult micro = new EvaluationResult(Collections.<Annotated> emptyList());

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < files.length; i++) {
            ProgressHelper.printProgress(i, files.length, 1, stopWatch);

            File file = files[i];
            File file1 = new File(FileHelper.getTempDir(), file.getName());
            FileHelper.writeToFile(file1.getPath(), FileHelper.readFileToString(file).replace(" role=\"main\"", ""));
            EvaluationResult result = extractor.evaluate(file1.getAbsolutePath(), TaggingFormat.XML);

            // write major error log
            errors.get(ResultType.CORRECT).put(file.getName(), result.getAnnotations(ResultType.CORRECT));
            errors.get(ResultType.ERROR1).put(file.getName(), result.getAnnotations(ResultType.ERROR1));
            errors.get(ResultType.ERROR2).put(file.getName(), result.getAnnotations(ResultType.ERROR2));
            errors.get(ResultType.ERROR3).put(file.getName(), result.getAnnotations(ResultType.ERROR3));
            errors.get(ResultType.ERROR4).put(file.getName(), result.getAnnotations(ResultType.ERROR4));
            errors.get(ResultType.ERROR5).put(file.getName(), result.getAnnotations(ResultType.ERROR5));

            Double precision = result.getPrecision(EvaluationMode.MUC);
            if (!precision.equals(Double.NaN)) {
                precisionMuc += precision;
            }
            Double precision2 = result.getPrecision(EvaluationMode.EXACT_MATCH);
            if (!precision2.equals(Double.NaN)) {
                precisionExact += precision2;
            }
            Double recall = result.getRecall(EvaluationMode.MUC);
            if (!recall.equals(Double.NaN)) {
                recallMuc += recall;
            }
            Double recall2 = result.getRecall(EvaluationMode.EXACT_MATCH);
            if (!recall2.equals(Double.NaN)) {
                recallExact += recall2;
            }

            micro.merge(result);

        }

        precisionExact /= files.length;
        recallExact /= files.length;
        precisionMuc /= files.length;
        recallMuc /= files.length;

        // summary
        StringBuilder summary = new StringBuilder();

        summary.append("Result for:").append(extractor.getName()).append("\n\n");
        summary.append("Using dataset:").append(goldStandardFileFolderPath).append("\n\n");

        summary.append("============ macro average ============\n\n");

        summary.append("Precision-Exact:").append(precisionExact).append('\n');
        summary.append("Recall-Exact:").append(recallExact).append('\n');
        summary.append("F1-Exact:").append(2 * precisionExact * recallExact / (precisionExact + recallExact))
                .append('\n');
        summary.append('\n');
        summary.append("Precision-MUC:").append(precisionMuc).append('\n');
        summary.append("Recall-MUC:").append(recallMuc).append('\n');
        summary.append("F1-MUC:").append(2 * precisionMuc * recallMuc / (precisionMuc + recallMuc)).append("\n\n");

        summary.append("============ micro average ============\n\n");

        summary.append("Precision-Exact:").append(micro.getPrecision(EvaluationMode.EXACT_MATCH)).append('\n');
        summary.append("Recall-Exact:").append(micro.getRecall(EvaluationMode.EXACT_MATCH)).append('\n');
        summary.append("F1-Exact:").append(micro.getF1(EvaluationMode.EXACT_MATCH)).append('\n');
        summary.append('\n');
        summary.append("Precision-MUC:").append(micro.getPrecision(EvaluationMode.MUC)).append('\n');
        summary.append("Recall-MUC:").append(micro.getRecall(EvaluationMode.MUC)).append('\n');
        summary.append("F1-MUC:").append(micro.getF1(EvaluationMode.MUC)).append("\n\n");

        summary.append("Elapsed time:").append(stopWatch.getTotalElapsedTimeString()).append('\n');

        StringBuilder detailedOutput = new StringBuilder();
        detailedOutput.append(summary.toString().replace(':', ';'));
        detailedOutput.append("\n\n\n");

        // detailed error stats
        for (Entry<ResultType, Map<String, Collection<Annotated>>> entry : errors.entrySet()) {
            ResultType resultType = entry.getKey();
            int errorTypeCount = 0;
            for (Collection<Annotated> errorEntry : entry.getValue().values()) {
                errorTypeCount += errorEntry.size();
            }
            detailedOutput.append(resultType.getDescription()).append(";").append(errorTypeCount).append("\n");
            for (Entry<String, Collection<Annotated>> errorEntry : entry.getValue().entrySet()) {
                for (Annotated annotation : errorEntry.getValue()) {
                    String fileName = errorEntry.getKey();
                    detailedOutput.append("\t").append(annotation).append(";").append(fileName).append("\n");
                }
            }
            detailedOutput.append("\n\n");
        }

        FileHelper.writeToFile("data/temp/" + System.currentTimeMillis() + "_allErrors.csv", detailedOutput);
        System.out.println(summary);
    }

    public static String evaluateCoordinates(LocationExtractor extractor, String goldStandardFileFolderPath) {
        Validate.notNull(extractor, "extractor must not be null");
        Validate.notEmpty(goldStandardFileFolderPath, "goldStandardFileFolderPath must not be empty");

        if (!new File(goldStandardFileFolderPath).isDirectory()) {
            throw new IllegalArgumentException("The provided path to the gold standard '" + goldStandardFileFolderPath
                    + "' does not exist or is no directory.");
        }

        Map<String, SortedMap<Integer, GeoCoordinate>> coordinatesMap = readCoordinatesCsv(new File(
                goldStandardFileFolderPath, "coordinates.csv"));

        File[] files = FileHelper.getFiles(goldStandardFileFolderPath, "text");
        StopWatch stopWatch = new StopWatch();
        StringBuilder evaluationDetails = new StringBuilder();

        evaluationDetails.append("Result for:").append(extractor.getName()).append("\n\n");
        evaluationDetails.append("Using dataset:").append(goldStandardFileFolderPath).append("\n\n");

        evaluationDetails.append("file;offset;type;value;goldLat;goldLng;taggedLat;taggedLng;distance;rmsd\n");

        for (int i = 0; i < files.length; i++) {

            ProgressHelper.printProgress(i, files.length, 1, stopWatch);

            File file = files[i];
            String inputText = FileHelper.readFileToString(file).replace(" role=\"main\"", "");
            String cleanText = HtmlHelper.stripHtmlTags(inputText);
            // evaluationDetails.append(file.getName()).append('\n');

            // get the gold standard annotations
            Annotations<ContextAnnotation> goldStandard = FileFormatParser.getAnnotationsFromXmlText(inputText);

            // annotate the document using the LocationExtractor
            List<LocationAnnotation> annotationResult = extractor.getAnnotations(cleanText);

            // the map holding the annotation index + coordinate
            SortedMap<Integer, GeoCoordinate> coordinates = coordinatesMap.get(file.getName());

            // verify, if we have data for every annotation in the gold standard
            for (ContextAnnotation annotation : goldStandard) {
                int start = annotation.getStartPosition();
                if (!coordinates.containsKey(start)) {
                    LOGGER.error("Coordinate list does not contain data for annotation with offset {}", start);
                }
            }

            double summedSquaredDistances = 0;

            // evaluate
            for (LocationAnnotation locationAnnotation : annotationResult) {

                LOGGER.debug("Check '{}'@{}", locationAnnotation.getValue(), locationAnnotation.getStartPosition());

                for (Annotated goldStandardAnnotation : goldStandard) {
                    GeoCoordinate goldStandardCoordinate = coordinates.get(goldStandardAnnotation.getStartPosition());

                    boolean samePositionAndLength = locationAnnotation.getStartPosition() == goldStandardAnnotation
                            .getStartPosition()
                            && locationAnnotation.getValue().length() == goldStandardAnnotation.getValue().length();
                    boolean overlaps = locationAnnotation.overlaps(goldStandardAnnotation);

                    if (!samePositionAndLength && !overlaps) {
                        continue;
                    }

                    LOGGER.debug("Compare with '{}'@{}", goldStandardAnnotation.getValue(),
                            goldStandardAnnotation.getStartPosition());

                    if (goldStandardCoordinate == null) {
                        LOGGER.debug("No coordinates in gold standard.");
                        continue;
                    }
                    if (locationAnnotation.getLocation().getLatitude() == null) {
                        LOGGER.debug("No coordinates from extractor.");
                        continue;
                    }

                    double distance = GeoUtils.getDistance(goldStandardCoordinate, locationAnnotation.getLocation());
                    if (samePositionAndLength) {
                        LOGGER.debug("Distance {} km, annotations are congruent", distance);
                    } else if (overlaps) {
                        LOGGER.debug("Distance {} km, annotations overlap", distance);
                    }

                    evaluationDetails.append(file.getName()).append(';');
                    evaluationDetails.append(goldStandardAnnotation.getStartPosition()).append(';');
                    evaluationDetails.append(goldStandardAnnotation.getTag()).append(';');
                    evaluationDetails.append(goldStandardAnnotation.getValue()).append(';');
                    evaluationDetails.append(goldStandardCoordinate.getLatitude()).append(';');
                    evaluationDetails.append(goldStandardCoordinate.getLongitude()).append(';');
                    evaluationDetails.append(locationAnnotation.getLocation().getLatitude()).append(';');
                    evaluationDetails.append(locationAnnotation.getLocation().getLongitude()).append(';');
                    evaluationDetails.append(distance).append('\n');
                    summedSquaredDistances += Math.pow(distance, 2);

                    if (samePositionAndLength) {
                        break;
                    }
                }
            }
            double rmsd = Math.sqrt(summedSquaredDistances / annotationResult.size());
            evaluationDetails.append(file.getName()).append(";;;;;;;;;").append(rmsd).append('\n');
        }
        FileHelper.writeToFile("data/temp/" + System.currentTimeMillis() + "_distances.csv",
                evaluationDetails.toString());
        return evaluationDetails.toString();
    }

    private static Map<String, SortedMap<Integer, GeoCoordinate>> readCoordinatesCsv(File coordinatesCsvFile) {
        final Map<String, SortedMap<Integer, GeoCoordinate>> coordinateMap = LazyMap
                .create(new Factory<SortedMap<Integer, GeoCoordinate>>() {
                    @Override
                    public SortedMap<Integer, GeoCoordinate> create() {
                        return CollectionHelper.newTreeMap();
                    }
                });
        FileHelper.performActionOnEveryLine(coordinatesCsvFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    return;
                }
                String[] split = StringUtils.splitPreserveAllTokens(line, ";");
                String documentName = split[0];
                int offset = Integer.valueOf(split[2]);
                GeoCoordinate coordinate = null;
                if (!split[3].isEmpty() && !split[4].isEmpty()) {
                    double lat = Double.valueOf(split[3]);
                    double lng = Double.valueOf(split[4]);
                    coordinate = new ImmutableGeoCoordinate(lat, lng);
                }
                coordinateMap.get(documentName).put(offset, coordinate);
            }
        });
        return coordinateMap;
    }

    private LocationExtractionEvaluator() {
        // utility class.
    }

    public static void main(String[] args) {
        String DATASET_LOCATION = "/Users/pk/Desktop/LocationLab/TUD-Loc-2013_V1";
        // String DATASET_LOCATION = "C:\\Users\\Sky\\Desktop\\LocationExtractionDatasetSmall";
        // String DATASET_LOCATION = "Q:\\Users\\David\\Desktop\\LocationExtractionDataset";
        // evaluate(new YahooLocationExtractor(), DATASET_LOCATION);
        // evaluate(new AlchemyLocationExtractor("b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        // evaluate(new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // evaluate(new ExtractivLocationExtractor(), DATASET_LOCATION);

        // LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // evaluate(new PalladianLocationExtractor(database), DATASET_LOCATION);
        // evaluateCoordinates(new PalladianLocationExtractor(database), DATASET_LOCATION);
        evaluateCoordinates(new YahooLocationExtractor(), DATASET_LOCATION);
    }

}
