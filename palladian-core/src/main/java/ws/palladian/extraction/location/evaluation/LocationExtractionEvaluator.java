package ws.palladian.extraction.location.evaluation;

import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.CORRECT;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR2;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR3;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR4;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR5;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationDocument;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.features.Annotation;

public final class LocationExtractionEvaluator {

    public static void evaluate(LocationExtractor extractor, String goldStandardFileFolderPath) {
        Validate.notNull(extractor, "extractor must not be null");
        Validate.notEmpty(goldStandardFileFolderPath, "goldStandardFileFolderPath must not be empty");

        if (!new File(goldStandardFileFolderPath).isDirectory()) {
            throw new IllegalArgumentException("The provided path to the gold standard '" + goldStandardFileFolderPath
                    + "' does not exist or is no directory.");
        }

        Map<ResultType, Map<String, Collection<Annotation>>> errors = new LinkedHashMap<ResultType, Map<String, Collection<Annotation>>>();
        errors.put(CORRECT, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR1, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR2, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR3, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR4, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR5, new HashMap<String, Collection<Annotation>>());

        Iterator<LocationDocument> goldStandard = LocationExtractorUtils.iterateDataset(new File(
                goldStandardFileFolderPath));

        // for macro averaging
        double precisionMuc = 0;
        double precisionExact = 0;
        double recallMuc = 0;
        double recallExact = 0;

        EvaluationResult micro = new EvaluationResult(Collections.<Annotation> emptyList());
        GeoEvaluationResult geoResult = new GeoEvaluationResult(extractor.getName(), goldStandardFileFolderPath);

        StopWatch stopWatch = new StopWatch();
        int count = 0;
        while (goldStandard.hasNext()) {
            LocationDocument locationDocument = goldStandard.next();

            List<LocationAnnotation> extractionResult = extractor.getAnnotations(locationDocument.getText());
            EvaluationResult result = NamedEntityRecognizer.evaluate(locationDocument.getAnnotations(),
                    extractionResult, Collections.<String> emptySet());

            // write major error log
            errors.get(CORRECT).put(locationDocument.getFileName(), result.getAnnotations(CORRECT));
            errors.get(ERROR1).put(locationDocument.getFileName(), result.getAnnotations(ERROR1));
            errors.get(ERROR2).put(locationDocument.getFileName(), result.getAnnotations(ERROR2));
            errors.get(ERROR3).put(locationDocument.getFileName(), result.getAnnotations(ERROR3));
            errors.get(ERROR4).put(locationDocument.getFileName(), result.getAnnotations(ERROR4));
            errors.get(ERROR5).put(locationDocument.getFileName(), result.getAnnotations(ERROR5));

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
            count++;

            // coordinates
            geoResult.addResultFromDocument(locationDocument, extractionResult);
        }

        precisionExact /= count;
        recallExact /= count;
        precisionMuc /= count;
        recallMuc /= count;

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

        // "recognition only" evaluates whether we recognized a toponym as such, but does not evaluate whether we tagged
        // it correctly (i.e. tagging "New York" as COUNTRY is still correct) in this case.
        summary.append("============ recognition only ============\n\n");

        int correctlyRecognized = micro.getAnnotations(CORRECT).size() + micro.getAnnotations(ERROR3).size();
        int recognized = micro.getAnnotations(CORRECT).size() + micro.getAnnotations(ERROR3).size()
                + micro.getAnnotations(ERROR1).size() + micro.getAnnotations(ERROR4).size()
                + micro.getAnnotations(ERROR5).size();
        int relevant = micro.getAnnotations(CORRECT).size() + micro.getAnnotations(ERROR3).size()
                + micro.getAnnotations(ERROR2).size();
        double recognitionPrecision = (double)correctlyRecognized / recognized;
        double recognitionRecall = (double)correctlyRecognized / relevant;
        summary.append("Precision:").append(recognitionPrecision).append('\n');
        summary.append("Recall:").append(recognitionRecall).append('\n');
        double recognitionF1 = 2 * recognitionPrecision * recognitionRecall
                / (recognitionPrecision + recognitionRecall);
        summary.append("F1:").append(recognitionF1).append("\n\n");

        summary.append("Elapsed time:").append(stopWatch.getTotalElapsedTimeString()).append('\n');

        StringBuilder detailedOutput = new StringBuilder();
        detailedOutput.append(summary.toString().replace(':', ';'));
        detailedOutput.append("\n\n\n");

        // detailed error stats
        for (Entry<ResultType, Map<String, Collection<Annotation>>> entry : errors.entrySet()) {
            ResultType resultType = entry.getKey();
            int errorTypeCount = 0;
            for (Collection<Annotation> errorEntry : entry.getValue().values()) {
                errorTypeCount += errorEntry.size();
            }
            detailedOutput.append(resultType.getDescription()).append(";").append(errorTypeCount).append("\n");
            for (Entry<String, Collection<Annotation>> errorEntry : entry.getValue().entrySet()) {
                for (Annotation annotation : errorEntry.getValue()) {
                    String fileName = errorEntry.getKey();
                    detailedOutput.append("\t").append(annotation).append(";").append(fileName).append("\n");
                }
            }
            detailedOutput.append("\n\n");
        }

        FileHelper.writeToFile("data/temp/" + System.currentTimeMillis() + "_allErrors.csv", detailedOutput);

        // write summary to summary.csv
        StringBuilder summaryCsv = new StringBuilder();
        summaryCsv.append(goldStandardFileFolderPath).append(';');
        summaryCsv.append(extractor.getName()).append(';');
        summaryCsv.append(micro.getPrecision(EvaluationMode.EXACT_MATCH)).append(';');
        summaryCsv.append(micro.getRecall(EvaluationMode.EXACT_MATCH)).append(';');
        summaryCsv.append(micro.getF1(EvaluationMode.EXACT_MATCH)).append(';');
        summaryCsv.append(micro.getPrecision(EvaluationMode.MUC)).append(';');
        summaryCsv.append(micro.getRecall(EvaluationMode.MUC)).append(';');
        summaryCsv.append(micro.getF1(EvaluationMode.MUC)).append(';');
        summaryCsv.append(recognitionPrecision).append(';');
        summaryCsv.append(recognitionRecall).append(';');
        summaryCsv.append(recognitionF1).append(';');
        // geo evaluation result
        summaryCsv.append(geoResult.getPrecision()).append(';');
        summaryCsv.append(geoResult.getRecall()).append(';');
        summaryCsv.append(geoResult.getF1()).append(';').append('\n');

        FileHelper.appendFile("data/temp/locationsSummary.csv", summaryCsv);

        System.out.println(summary);
        System.out.println("======= geo =========");
        System.out.println(geoResult.getSummary());

        // write coordinates results
        geoResult.writeDetailedReport(new File("data/temp/" + System.currentTimeMillis() + "_distances.csv"));
    }

    private LocationExtractionEvaluator() {
        // utility class.
    }

    public static void main(String[] args) {
        // String DATASET_LOCATION = "/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/2-validation";
        // String DATASET_LOCATION = "/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/3-test";
        // String DATASET_LOCATION = "/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/2-validation";
        String DATASET_LOCATION = "/Users/pk/Dropbox/Uni/Dissertation_LocationLab/CLUST-converted/2-validation";
        // evaluate(new YahooLocationExtractor(), DATASET_LOCATION);
        // evaluate(new AlchemyLocationExtractor("b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        // evaluate(new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // evaluateCoordinates(new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // evaluate(new ExtractivLocationExtractor(), DATASET_LOCATION);
        // System.exit(0);

        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");

        // ///////////////////// baseline //////////////////////
        // LocationDisambiguation disambiguation = new BaselineDisambiguation();

        // ///////////////////// anchor heuristic //////////////////////
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();

        // ///////////////////// feature based //////////////////////
        // String modelFilePath = "data/temp/location_disambiguation_1375713579496.model";
        // BaggedDecisionTreeModel model = FileHelper.deserialize(modelFilePath);
        // FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model, 0);

        evaluate(new PalladianLocationExtractor(database, disambiguation), DATASET_LOCATION);
        // evaluateCoordinates(new PalladianLocationExtractor(database, disambiguation), DATASET_LOCATION);

        // perform threshold analysis ////////////////////////////////
        // for (double t = 0.; t <= 1.02; t += 0.02) {
        // FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model, t);
        // evaluate(new PalladianLocationExtractor(database, disambiguation), DATASET_LOCATION);
        // }

        // parameter tuning for heuristic; vary one parameter at once ////////////////////////////
        // for (int anchorDistanceThreshold : Arrays.asList(0, 10, 100, 1000, 10000, 100000, 1000000)) {
            // for (int lowerPopulationThreshold : Arrays.asList(0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000,
            // 9000,
            // 10000, 11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000)) {
        // for (int anchorPopulationThreshold : Arrays.asList(0, 10, 100, 1000, 10000, 100000, 1000000, 10000000,
        // 100000000, 1000000000)) {
            // for (int sameDistanceThreshold : Arrays.asList(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130,
            // 140,
            // 150, 160, 170, 180, 190, 200)) {
            // for (int lassoDistanceThreshold : Arrays.asList(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120,
            // 130,
            // 140,
            // 150, 160, 170, 180, 190, 200)) {
        // for (int unlikelyPopulationThreshold : Arrays.asList(0, 10, 100, 1000, 10000, 100000, 1000000, 10000000,
        // 100000000, 1000000000)) {
        // // for (int tokenThreshold : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)) {
        // LocationDisambiguation disambiguation = new HeuristicDisambiguation(//
        // HeuristicDisambiguation.ANCHOR_DISTANCE_THRESHOLD, //
        // HeuristicDisambiguation.LOWER_POPULATION_THRESHOLD, //
        // HeuristicDisambiguation.ANCHOR_POPULATION_THRESHOLD, //
        // HeuristicDisambiguation.SAME_DISTANCE_THRESHOLD, //
        // HeuristicDisambiguation.LASSO_DISTANCE_THRESHOLD, //
        // unlikelyPopulationThreshold, // HeuristicDisambiguation.LOWER_UNLIKELY_POPULATION_THRESHOLD, //
        // HeuristicDisambiguation.TOKEN_THRESHOLD);
        // PalladianLocationExtractor extractor = new PalladianLocationExtractor(source, disambiguation);
        // evaluate(extractor, DATASET_LOCATION);
        // evaluateCoordinates(extractor, DATASET_LOCATION);
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
