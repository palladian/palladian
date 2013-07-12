package ws.palladian.extraction.location.evaluation;

import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.CORRECT;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR2;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR3;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR4;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR5;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationDocument;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
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
        errors.put(CORRECT, new HashMap<String, Collection<Annotated>>());
        errors.put(ERROR1, new HashMap<String, Collection<Annotated>>());
        errors.put(ERROR2, new HashMap<String, Collection<Annotated>>());
        errors.put(ERROR3, new HashMap<String, Collection<Annotated>>());
        errors.put(ERROR4, new HashMap<String, Collection<Annotated>>());
        errors.put(ERROR5, new HashMap<String, Collection<Annotated>>());

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
            errors.get(CORRECT).put(file.getName(), result.getAnnotations(CORRECT));
            errors.get(ERROR1).put(file.getName(), result.getAnnotations(ERROR1));
            errors.get(ERROR2).put(file.getName(), result.getAnnotations(ERROR2));
            errors.get(ERROR3).put(file.getName(), result.getAnnotations(ERROR3));
            errors.get(ERROR4).put(file.getName(), result.getAnnotations(ERROR4));
            errors.get(ERROR5).put(file.getName(), result.getAnnotations(ERROR5));

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

        // write summary to summary.csv
        StringBuilder summaryCsv = new StringBuilder();
        summaryCsv.append(extractor.getName()).append(';');
        summaryCsv.append(micro.getPrecision(EvaluationMode.EXACT_MATCH)).append(';');
        summaryCsv.append(micro.getRecall(EvaluationMode.EXACT_MATCH)).append(';');
        summaryCsv.append(micro.getF1(EvaluationMode.EXACT_MATCH)).append(';');
        summaryCsv.append(micro.getPrecision(EvaluationMode.MUC)).append(';');
        summaryCsv.append(micro.getRecall(EvaluationMode.MUC)).append(';');
        summaryCsv.append(micro.getF1(EvaluationMode.MUC)).append(';').append('\n');
        FileHelper.appendFile("data/temp/locationsSummary.csv", summaryCsv);

        System.out.println(summary);
    }

    private static final class EvaluationItem implements Comparable<EvaluationItem> {

        public EvaluationItem(String file, Annotated annotation, ResultType resultType, GeoCoordinate goldCoordinate,
                GeoCoordinate taggedCoordinate) {
            this.file = file;
            this.annotation = annotation;
            this.resultType = resultType;
            this.goldCoord = goldCoordinate;
            this.taggedCoord = taggedCoordinate;
        }

        String file;
        Annotated annotation;
        ResultType resultType;
        GeoCoordinate goldCoord;
        GeoCoordinate taggedCoord;

        @Override
        public int compareTo(EvaluationItem other) {
            int result = this.file.compareTo(other.file);
            if (result != 0) {
                return result;
            }
            return Integer.valueOf(this.annotation.getStartPosition()).compareTo(other.annotation.getStartPosition());
        }

        private Double getDistance() {
            if (goldCoord == null || goldCoord.getLatitude() == null || goldCoord.getLongitude() == null) {
                return null;
            }
            if (taggedCoord == null || taggedCoord.getLatitude() == null || taggedCoord.getLongitude() == null) {
                return null;
            }
            return GeoUtils.getDistance(goldCoord, taggedCoord);
        }

        public String toCsvLine() {
            StringBuilder builder = new StringBuilder();
            builder.append(file).append(';');
            builder.append(annotation.getStartPosition()).append(';');
            builder.append(annotation.getTag()).append(';');
            builder.append(annotation.getValue()).append(';');
            builder.append(resultType).append(';');
            builder.append(goldCoord != null ? goldCoord.getLatitude() : "").append(';');
            builder.append(goldCoord != null ? goldCoord.getLongitude() : "").append(';');
            builder.append(taggedCoord != null ? taggedCoord.getLatitude() : "").append(';');
            builder.append(taggedCoord != null ? taggedCoord.getLongitude() : "").append(';');
            Double distance = getDistance();
            builder.append(distance != null ? getDistance() : "");
            return builder.toString();
        }
    }

    /**
     * Evaluate the location disambiguation. This step considers locations of type CITY and POI and evaluates, whether
     * the coordinates from the gold standard match the extracted coordinates within a given threshold (100 km).
     * Locations without coordinates in the gold standard are skipped.
     * 
     * @param extractor
     * @param goldStandardFileFolderPath
     * @return
     */
    public static String evaluateCoordinates(LocationExtractor extractor, String goldStandardFileFolderPath) {
        Validate.notNull(extractor, "extractor must not be null");
        Validate.notEmpty(goldStandardFileFolderPath, "goldStandardFileFolderPath must not be empty");

        if (!new File(goldStandardFileFolderPath).isDirectory()) {
            throw new IllegalArgumentException("The provided path to the gold standard '" + goldStandardFileFolderPath
                    + "' does not exist or is no directory.");
        }

        Iterator<LocationDocument> goldStandard = LocationExtractorUtils.iterateDataset(new File(
                goldStandardFileFolderPath));
        StopWatch stopWatch = new StopWatch();
        StringBuilder evaluationDetails = new StringBuilder();

        evaluationDetails.append("# Result for:").append(extractor.getName()).append('\n');
        evaluationDetails.append("# Using dataset:").append(goldStandardFileFolderPath).append('\n');

        List<EvaluationItem> completeEvaluationList = CollectionHelper.newArrayList();

        while (goldStandard.hasNext()) {

            LocationDocument goldStandardDocument = goldStandard.next();
            String fileName = goldStandardDocument.getFileName();

            // ProgressHelper.printProgress(i, files.length, 1, stopWatch);

            // annotate the document using the LocationExtractor
            List<LocationAnnotation> annotationResult = extractor.getAnnotations(goldStandardDocument.getText());

            List<EvaluationItem> evaluationList = CollectionHelper.newArrayList();
            Set<Annotated> taggedAnnotations = CollectionHelper.newHashSet();

            // evaluate
            for (LocationAnnotation assignedAnnotation : annotationResult) {

                boolean taggedOverlap = false;
                int counter = 0;

                for (LocationAnnotation goldAnnotation : goldStandardDocument.getAnnotations()) {
                    counter++;

                    GeoCoordinate goldCoordinate = goldAnnotation.getLocation();

                    boolean congruent = assignedAnnotation.getStartPosition() == goldAnnotation.getStartPosition()
                            && assignedAnnotation.getEndPosition() == goldAnnotation.getEndPosition();
                    boolean overlaps = assignedAnnotation.overlaps(goldAnnotation);

                    if (congruent) {
                        // same start and end
                        taggedAnnotations.add(goldAnnotation);
                        evaluationList.add(new EvaluationItem(fileName, goldAnnotation, CORRECT, goldCoordinate,
                                assignedAnnotation.getLocation()));
                        break;
                    } else if (overlaps) {
                        // overlap
                        taggedOverlap = true;
                        taggedAnnotations.add(goldAnnotation);
                        evaluationList.add(new EvaluationItem(fileName, goldAnnotation, ERROR4, goldCoordinate,
                                assignedAnnotation.getLocation()));
                    } else if (assignedAnnotation.getStartPosition() < goldAnnotation.getEndPosition()
                            || counter == goldStandardDocument.getAnnotations().size()) {
                        if (!taggedOverlap) {
                            // false alarm
                            evaluationList.add(new EvaluationItem(fileName, assignedAnnotation, ERROR1, null,
                                    assignedAnnotation.getLocation()));
                        }
                        break;
                    } else {
                        continue;
                    }
                }
            }

            // check which gold standard annotations have not been found by the NER (error2)
            for (LocationAnnotation goldAnnotation : goldStandardDocument.getAnnotations()) {
                if (!taggedAnnotations.contains(goldAnnotation)) {
                    GeoCoordinate goldCooardinate = goldAnnotation.getLocation();
                    evaluationList.add(new EvaluationItem(fileName, goldAnnotation, ERROR2, goldCooardinate, null));
                }
            }
            completeEvaluationList.addAll(evaluationList);
        }


        int correct = 0;
        int retrieved = 0;
        int relevant = 0;
        for (EvaluationItem item : completeEvaluationList) {

            // ignore other types than CITY and POI because they are too broad
            if (!Arrays.asList("CITY", "POI").contains(item.annotation.getTag())) {
                continue;
            }

            // when gold standard has no location, ignore
            if (item.goldCoord == null) {
                continue;
            }

            Double distance = item.getDistance();
            if (distance != null && distance < 100) {
                correct++;
            }
            if (Arrays.asList(CORRECT, ERROR4, ERROR1).contains(item.resultType)) {
                retrieved++;
            }
            if (Arrays.asList(CORRECT, ERROR4, ERROR2).contains(item.resultType)) {
                relevant++;
            }
        }

        evaluationDetails.append("#\n");
        evaluationDetails.append("#\n");

        evaluationDetails.append("# num correct: " + correct).append('\n');
        evaluationDetails.append("# num retrieved: " + retrieved).append('\n');
        evaluationDetails.append("# relevant: " + relevant).append('\n');
        float precision = (float)correct / retrieved;
        float recall = (float)correct / relevant;
        float f1 = 2 * precision * recall / (precision + recall);

        evaluationDetails.append("# precision: " + precision).append('\n');
        evaluationDetails.append("# recall: " + recall).append('\n');
        evaluationDetails.append("# f1: " + f1).append('\n');
        evaluationDetails.append("#\n");
        evaluationDetails.append("#\n");

        evaluationDetails
                .append("file;offset;type;value;annotationResult;goldLat;goldLng;taggedLat;taggedLng;distance\n");
        Collections.sort(completeEvaluationList);
        for (EvaluationItem item : completeEvaluationList) {
            evaluationDetails.append(item.toCsvLine()).append('\n');
        }

        FileHelper.writeToFile("data/temp/" + System.currentTimeMillis() + "_distances.csv",
                evaluationDetails.toString());

        return evaluationDetails.toString();
    }

    private LocationExtractionEvaluator() {
        // utility class.
    }

    public static void main(String[] args) {
        // String DATASET_LOCATION = "/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted";
        String DATASET_LOCATION = "/Users/pk/Desktop/TUD-Loc-2013/TUD-Loc-2013_V2/2-validation";
        // String DATASET_LOCATION = "/Users/pk/Desktop/TUD-Loc-2013/TUD-Loc-2013_V2/3-test";
        // String DATASET_LOCATION = "/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2";
        // String DATASET_LOCATION = "/Users/pk/Desktop/TUD-Loc-2013_V2_test";
        // String DATASET_LOCATION = "C:\\Users\\Sky\\Desktop\\LocationExtractionDatasetSmall";
        // String DATASET_LOCATION = "Q:\\Users\\David\\Desktop\\LocationExtractionDataset";
        // evaluate(new YahooLocationExtractor(), DATASET_LOCATION);
        // evaluate(new AlchemyLocationExtractor("b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        // evaluate(new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // evaluate(new ExtractivLocationExtractor(), DATASET_LOCATION);

        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");

        // ///////////////////// baseline //////////////////////
        // LocationDisambiguation disambiguation = new BaselineDisambiguation();

        // ///////////////////// anchor heuristic //////////////////////
        // LocationDisambiguation disambiguation = new HeuristicDisambiguation();

        // ///////////////////// feature based //////////////////////
        // String modelFilePath = "data/temp/location_disambiguation_1373659810968.model";
        // BaggedDecisionTreeModel model = FileHelper.deserialize(modelFilePath);
        // FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model);

        // evaluate(new PalladianLocationExtractor(database, disambiguation), DATASET_LOCATION);

        // parameter tuning for heuristic; vary one parameter at once ////////////////////////////
        // for (int sameDistanceThreshold : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 50, 60, 70, 80,
        // 90, 100, 200, 300, 400, 500)) {
        // LocationDisambiguation disambiguation = new HeuristicDisambiguation(
        // HeuristicDisambiguation.ANCHOR_DISTANCE_THRESHOLD,
        // HeuristicDisambiguation.LOWER_POPULATION_THRESHOLD,
        // HeuristicDisambiguation.ANCHOR_POPULATION_THRESHOLD, sameDistanceThreshold);
        // evaluate(new PalladianLocationExtractor(database, disambiguation), DATASET_LOCATION);
        // }

        // evaluateCoordinates(new PalladianLocationExtractor(database, disambiguation), DATASET_LOCATION);
        // evaluateCoordinates(new YahooLocationExtractor(), DATASET_LOCATION);
        // File pathToTexts = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2-cleanTexts");
        // File pathToJsonResults = new File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/UnlockTextResults");
        // evaluate(new UnlockTextMockExtractor(pathToTexts, pathToJsonResults), DATASET_LOCATION);
        // evaluateCoordinates(new UnlockTextMockExtractor(pathToTexts, pathToJsonResults), DATASET_LOCATION);
        // evaluateCoordinates(new PalladianLocationExtractor(database), DATASET_LOCATION);
        // evaluateCoordinates(new OpenCalaisLocationExtractor2("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
    }

}
