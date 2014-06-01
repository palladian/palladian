package ws.palladian.extraction.location.evaluation;

import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.MUC;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.CORRECT;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR2;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR3;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR4;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR5;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.ANCHOR_DISTANCE_THRESHOLD;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.ANCHOR_POPULATION_THRESHOLD;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.LASSO_DISTANCE_THRESHOLD;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.LOWER_POPULATION_THRESHOLD;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.LOWER_UNLIKELY_POPULATION_THRESHOLD;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.SAME_DISTANCE_THRESHOLD;
import static ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation.TOKEN_THRESHOLD;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.FeatureBasedDisambiguation;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * Evaluation script for {@link LocationExtractor}s.
 * </p>
 * 
 * @author Philipp Katz
 */
@SuppressWarnings(value="unused")
public final class LocationExtractionEvaluator {
    
    // TODO combine all evaluation data (includes class GeoEvaluationResult)
    public static final class LocationEvaluationResult {
        
        final double mucPr;
        final double mucRc;
        final double mucF1;
        final double geoPr;
        final double geoRc;
        final double geoF1;

        LocationEvaluationResult(double mucPr, double mucRc, double mucF1, double geoPr, double geoRc, double geoF1) {
            this.mucPr = mucPr;
            this.mucRc = mucRc;
            this.mucF1 = mucF1;
            this.geoPr = geoPr;
            this.geoRc = geoRc;
            this.geoF1 = geoF1;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LocationEvaluationResult [mucPr=");
            builder.append(mucPr);
            builder.append(", mucRc=");
            builder.append(mucRc);
            builder.append(", mucF1=");
            builder.append(mucF1);
            builder.append(", geoPr=");
            builder.append(geoPr);
            builder.append(", geoRc=");
            builder.append(geoRc);
            builder.append(", geoF1=");
            builder.append(geoF1);
            builder.append("]");
            return builder.toString();
        }

    }

    private final List<File> datasetPaths = CollectionHelper.newArrayList();

    private final List<LocationExtractor> extractors = CollectionHelper.newArrayList();

    /**
     * <p>
     * Add a dataset for evaluation. The dataset must conform to the TUD-Loc scheme, i.e. tagged files plus coordinates
     * file (see {@link LocationExtractorUtils#readCoordinates(File)} for an explanation about the format).
     * </p>
     * 
     * @param datasetPath Path to the directory with the dataset.
     */
    public void addDataset(String datasetPath) {
        File temp = new File(datasetPath);
        if (!temp.isDirectory()) {
            throw new IllegalArgumentException(datasetPath + " is not a directory.");
        }
        datasetPaths.add(temp);
    }

    public void addExtractor(LocationExtractor extractor) {
        extractors.add(extractor);
    }

    public void addExtractors(Collection<? extends LocationExtractor> e) {
        extractors.addAll(e);
    }

    public void runAll(boolean detailedReport) {
        int numIterations = datasetPaths.size() * extractors.size();
        ProgressReporter progress = new ProgressMonitor();
        progress.startTask("LocationExtractionEvaluation", numIterations);
        for (File datasetPath : datasetPaths) {
            for (LocationExtractor extractor : extractors) {
                run(extractor, datasetPath, detailedReport);
                progress.increment();
            }
        }
    }

    /**
     * <p>
     * Run one evaluation circle consisting of a {@link LocationExtractor} and a dataset. The evaluation measures the
     * NER performance for locations (MUC and exact match scheme, see {@link EvaluationResult} for more information),
     * recognition-only performance (i.e. only checking if toponyms were marked, not taking the tags into
     * consideration), and Geo/Disambiguation performance (i.e. were the correct spots on the map identified). The
     * following files will be written:
     * </p>
     * 
     * <ul>
     * <li><code>timestamp_allErrors.csv</code>: A CSV file containing detailed NER evaluation results, useful for
     * debugging (different error types for each annotation, and detailed NER evaluation measures).</li>
     * <li><code>timestamp_distances.csv</code>: A CSV file containing detailed Geo evaluation results, useful for
     * debugging (for each annotation the spatial distance between the gold standard and the performed annotation, and
     * detailed summarized evaluation measures).</li>
     * <li><code>_locationsSummary.csv</code>: A summary file which is appended for each run and contains overview
     * evaluation measures, useful for creating the graphs.</li>
     * </ul>
     * 
     * @param extractor The extractor, not <code>null</code>.
     * @param datasetDirectory The directory with the dataset, not <code>null</code>.
     * @param detailedReport <code>true</code> to write detailed evaluation results (*_allErrors.csv, *_distances.csv),
     *            <code>false</code> to only write the _locations_summary.csv.
     * @return Result with summary evaluation measures.
     */
    public static LocationEvaluationResult run(LocationExtractor extractor, File datasetDirectory, boolean detailedReport) {
        Validate.notNull(extractor, "extractor must not be null");
        Validate.notNull(datasetDirectory, "datasetDirectory must not be null");

        if (!datasetDirectory.isDirectory()) {
            throw new IllegalArgumentException("The provided path to the gold standard '" + datasetDirectory
                    + "' does not exist or is no directory.");
        }

        Map<ResultType, Map<String, Collection<Annotation>>> errors = new LinkedHashMap<ResultType, Map<String, Collection<Annotation>>>();
        errors.put(CORRECT, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR1, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR2, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR3, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR4, new HashMap<String, Collection<Annotation>>());
        errors.put(ERROR5, new HashMap<String, Collection<Annotation>>());

        Iterator<LocationDocument> goldStandard = new TudLoc2013DatasetIterable(datasetDirectory).iterator();

        // for macro averaging
        double precisionMuc = 0;
        double precisionExact = 0;
        double recallMuc = 0;
        double recallExact = 0;

        EvaluationResult micro = new EvaluationResult(Collections.<Annotation> emptyList());
        GeoEvaluationResult geoResult = new GeoEvaluationResult(extractor.getName(), datasetDirectory.getPath());

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
        summary.append("Using dataset:").append(datasetDirectory.getPath()).append("\n\n");

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
        
        detailedOutput.append("Per-type stats:\n\n");
        
        // per-tag-level stats
        for (LocationType type : LocationType.values()) {
            // XXX does it make sense to score type-specified using MUC? At least, there is a bug in the
            // EvaluationResult class, causing type specific MUC precision above one!
            
            double typePrExact = micro.getPrecisionFor(type.toString(), EvaluationMode.EXACT_MATCH);
            double typeRcExact = micro.getRecallFor(type.toString(), EvaluationMode.EXACT_MATCH);
            double typeF1Exact = micro.getF1For(type.toString(), EvaluationMode.EXACT_MATCH);
//            double typePrMuc = micro.getPrecisionFor(type.toString(), EvaluationMode.MUC);
//            double typeRcMuc = micro.getRecallFor(type.toString(), EvaluationMode.MUC);
//            double typeF1Muc = micro.getF1For(type.toString(), EvaluationMode.MUC);
            detailedOutput.append("Type:").append(type.toString()).append("\n");
            detailedOutput.append("Precision Exact:").append(typePrExact).append("\n");
            detailedOutput.append("Recall Exact:").append(typeRcExact).append("\n");
            detailedOutput.append("F1 Exact:").append(typeF1Exact).append("\n");
//            detailedOutput.append("Precision MUC:").append(typePrMuc).append("\n");
//            detailedOutput.append("Recall MUC:").append(typeRcMuc).append("\n");
//            detailedOutput.append("F1 MUC:").append(typeF1Muc).append("\n").append("\n");
        }

        long timestamp = System.currentTimeMillis();
        
        if (detailedReport) {
            FileHelper.writeToFile("data/temp/" + timestamp + "_allErrors.csv", detailedOutput);
        }

        // write summary to summary.csv
        StringBuilder summaryCsv = new StringBuilder();

        File summaryFile = new File("data/temp/_locationsSummary.csv");
        if (!summaryFile.exists()) {
            // write header
            summaryCsv
                    .append("timestamp;dataset;extractor;prExact;rcExact;f1Exact;prMUC;rcMUC;f1MUC;prRec;rcRec;f1Rec;prGeo;rcGeo;f1Geo;time\n");
        }
        summaryCsv.append(timestamp).append(';');
        summaryCsv.append(datasetDirectory.getPath()).append(';');
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
        summaryCsv.append(geoResult.getF1()).append(';');
        // elapsed time
        summaryCsv.append(stopWatch.getTotalElapsedTime()).append('\n');

        FileHelper.appendFile(summaryFile.getPath(), summaryCsv);

        System.out.println(summary);
        System.out.println("======= geo =========");
        System.out.println(geoResult.getSummary());

        // write coordinates results
        if (detailedReport) {
            geoResult.writeDetailedReport(new File("data/temp/" + timestamp + "_distances.csv"));
        }
        
        return new LocationEvaluationResult(micro.getPrecision(MUC), micro.getRecall(MUC), micro.getF1(MUC),
                geoResult.getPrecision(), geoResult.getRecall(), geoResult.getF1());
    }

    private static List<LocationExtractor> createForParameterOptimization(LocationDatabase database) {
        List<LocationExtractor> extractors = CollectionHelper.newArrayList();
        for (int anchorDistanceThreshold : Arrays.asList(0, 10, 100, 1000, 10000, 100000, 1000000)) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    anchorDistanceThreshold, //
                    LOWER_POPULATION_THRESHOLD, //
                    ANCHOR_POPULATION_THRESHOLD, //
                    SAME_DISTANCE_THRESHOLD, //
                    LASSO_DISTANCE_THRESHOLD, //
                    LOWER_UNLIKELY_POPULATION_THRESHOLD, //
                    TOKEN_THRESHOLD)));
        }
        for (int lowerPopulationThreshold = 0; lowerPopulationThreshold <= 20000; lowerPopulationThreshold += 1000) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    ANCHOR_DISTANCE_THRESHOLD, //
                    lowerPopulationThreshold, //
                    ANCHOR_POPULATION_THRESHOLD, //
                    SAME_DISTANCE_THRESHOLD, //
                    LASSO_DISTANCE_THRESHOLD, //
                    LOWER_UNLIKELY_POPULATION_THRESHOLD, //
                    TOKEN_THRESHOLD)));
        }
        for (int anchorPopulationThreshold = 0; anchorPopulationThreshold <= 9; anchorPopulationThreshold++) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    ANCHOR_DISTANCE_THRESHOLD, //
                    LOWER_POPULATION_THRESHOLD, //
                    (int)Math.pow(10, anchorPopulationThreshold), //
                    SAME_DISTANCE_THRESHOLD, //
                    LASSO_DISTANCE_THRESHOLD, //
                    LOWER_UNLIKELY_POPULATION_THRESHOLD, //
                    TOKEN_THRESHOLD)));
        }
        for (int sameDistanceThreshold = 0; sameDistanceThreshold <= 200; sameDistanceThreshold += 10) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    ANCHOR_DISTANCE_THRESHOLD, //
                    LOWER_POPULATION_THRESHOLD, //
                    ANCHOR_POPULATION_THRESHOLD, //
                    sameDistanceThreshold, //
                    LASSO_DISTANCE_THRESHOLD, //
                    LOWER_UNLIKELY_POPULATION_THRESHOLD, //
                    TOKEN_THRESHOLD)));
        }
        for (int lassoDistanceThreshold = 0; lassoDistanceThreshold <= 200; lassoDistanceThreshold += 10) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    ANCHOR_DISTANCE_THRESHOLD, //
                    LOWER_POPULATION_THRESHOLD, //
                    ANCHOR_POPULATION_THRESHOLD, //
                    SAME_DISTANCE_THRESHOLD, //
                    lassoDistanceThreshold, //
                    LOWER_UNLIKELY_POPULATION_THRESHOLD, //
                    TOKEN_THRESHOLD)));
        }
        for (int lowerUnlikelyPopulationThreshold = 0; lowerUnlikelyPopulationThreshold <= 9; lowerUnlikelyPopulationThreshold++) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    ANCHOR_DISTANCE_THRESHOLD, //
                    LOWER_POPULATION_THRESHOLD, //
                    ANCHOR_POPULATION_THRESHOLD, //
                    SAME_DISTANCE_THRESHOLD, //
                    LASSO_DISTANCE_THRESHOLD, //
                    (int)Math.pow(10, lowerUnlikelyPopulationThreshold), //
                    TOKEN_THRESHOLD)));
        }
        for (int tokenThreshold = 0; tokenThreshold <= 10; tokenThreshold++) {
            extractors.add(new PalladianLocationExtractor(database, new HeuristicDisambiguation(
                    ANCHOR_DISTANCE_THRESHOLD, //
                    LOWER_POPULATION_THRESHOLD, //
                    ANCHOR_POPULATION_THRESHOLD, //
                    SAME_DISTANCE_THRESHOLD, //
                    LASSO_DISTANCE_THRESHOLD, //
                    LOWER_UNLIKELY_POPULATION_THRESHOLD, //
                    tokenThreshold)));
        }
        return extractors;
    }

    private static List<LocationExtractor> createForThresholdAnalysis(LocationDatabase database, QuickDtModel model) {
        List<LocationExtractor> extractors = CollectionHelper.newArrayList();
        for (int i = 0; i <= 10; i++) {
            double threshold = i / 10.;
            FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model, threshold,
                    FeatureBasedDisambiguation.CONTEXT_SIZE);
            extractors.add(new PalladianLocationExtractor(database, disambiguation));
        }
        return extractors;
    }

    private static List<LocationExtractor> createForContextAnalysis(LocationDatabase database, QuickDtModel model) {
        List<LocationExtractor> extractors = CollectionHelper.newArrayList();
        for (int i = 0; i <= 5000; i += 100) {
            FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model,
                    FeatureBasedDisambiguation.PROBABILITY_THRESHOLD, i);
            extractors.add(new PalladianLocationExtractor(database, disambiguation));
        }
        return extractors;
    }

    public static void main(String[] args) throws IOException {

        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        // evaluator.addDataset("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/2-validation");
        // evaluator.addDataset("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/2-validation");
        // evaluator.addDataset("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/CLUST-converted/2-validation");

        evaluator.addDataset("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/3-test");
        evaluator.addDataset("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/3-test");
        evaluator.addDataset("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/CLUST-converted/3-test");

        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // evaluator.addExtractor(new PalladianLocationExtractor(database, new BaselineDisambiguation()));
        evaluator.addExtractor(new PalladianLocationExtractor(database, new HeuristicDisambiguation()));
        // BaggedDecisionTreeModel model = FileHelper.deserialize("data/temp/fd_tud_train_1375884663191.model");
        // BaggedDecisionTreeModel model = FileHelper.deserialize("data/temp/fd_lgl_train_1375884760443.model");
        // BaggedDecisionTreeModel model = FileHelper.deserialize("data/temp/fd_clust_train_1375885091622.model");
        QuickDtModel model;
        // model = FileHelper.deserialize("data/temp/location_disambiguation_tud.model");
        // evaluator.addExtractor(new PalladianLocationExtractor(database, new FeatureBasedDisambiguation(model)));
        // model = FileHelper.deserialize("data/temp/location_disambiguation_lgl.model");
        // evaluator.addExtractor(new PalladianLocationExtractor(database, new FeatureBasedDisambiguation(model)));
        model = FileHelper.deserialize("data/temp/location_disambiguation_clust.model");
        // evaluator.addExtractor(new PalladianLocationExtractor(database, new FeatureBasedDisambiguation(model)));
        // model = FileHelper.deserialize("data/temp/location_disambiguation_all.model");
        // evaluator.addExtractor(new PalladianLocationExtractor(database, new FeatureBasedDisambiguation(model)));
        // evaluator.addExtractor(new PalladianLocationExtractor(database, new CombinedDisambiguation(model)));

        // perform threshold analysis ////////////////////////////////
        // List<LocationExtractor> extractors = createForThresholdAnalysis(database, model);
        // evaluator.addExtractors(extractors);

        // parameter tuning for heuristic; vary one parameter at once ////////////////////////////
        // List<LocationExtractor> extractors = createForParameterOptimization(database);
        // evaluator.addExtractors(extractors);

        // analyse impact of disambiguation window size /////////////////////////
        // List<LocationExtractor> extractors = createForContextAnalysis(database, model);
        // evaluator.addExtractors(extractors);

        // comparison with others /////////////////////////////
        // evaluator.addExtractor(new YahooLocationExtractor());
        // evaluator.addExtractor(new AlchemyLocationExtractor("b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"));
        // evaluator.addExtractor(new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"));
        // evaluator.addExtractor(new ExtractivLocationExtractor());
        // File pathToTexts = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2-cleanTexts");
        // File pathToJsonResults = new File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/UnlockTextResults");
        // evaluator.addExtractor(new UnlockTextMockExtractor(pathToTexts, pathToJsonResults));

        evaluator.runAll(true);
    }

}
