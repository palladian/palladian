package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class LocationExtractionEvaluator {

    public void evaluateAll(LocationExtractor extractor, String goldStandardFileFolderPath) {
        Validate.notNull(extractor, "extractor must not be null");
        Validate.notEmpty(goldStandardFileFolderPath, "goldStandardFileFolderPath must not be empty");

        if (!new File(goldStandardFileFolderPath).isDirectory()) {
            throw new IllegalArgumentException("The provided path to the gold standard '" + goldStandardFileFolderPath
                    + "' does not exist or is no directory.");
        }

        Map<ResultType, Map<String, List<Annotation>>> errors = new LinkedHashMap<ResultType, Map<String, List<Annotation>>>();
        errors.put(ResultType.CORRECT, new HashMap<String, List<Annotation>>());
        errors.put(ResultType.ERROR1, new HashMap<String, List<Annotation>>());
        errors.put(ResultType.ERROR2, new HashMap<String, List<Annotation>>());
        errors.put(ResultType.ERROR3, new HashMap<String, List<Annotation>>());
        errors.put(ResultType.ERROR4, new HashMap<String, List<Annotation>>());
        errors.put(ResultType.ERROR5, new HashMap<String, List<Annotation>>());

        File[] files = FileHelper.getFiles(goldStandardFileFolderPath, "text");

        double precisionMuc = 0;
        double precisionExact = 0;
        double recallMuc = 0;
        double recallExact = 0;
        double f1Muc = 0;
        double f1Exact = 0;

        for (int i = 0; i < files.length; i++) {
            ProgressHelper.printProgress(i, files.length, 1);

            File file = files[i];
            // String path = "data/temp/" + file.getName();
            File file1 = new File(FileHelper.getTempDir(), file.getName());
            FileHelper.writeToFile(file1.getPath(), FileHelper.readFileToString(file).replace(" role=\"main\"", ""));
            EvaluationResult result = extractor.evaluate(file1.getAbsolutePath(), TaggingFormat.XML);

            // write major error log
            // Map<ResultType, Annotations> fileErrors = result.getErrorAnnotations();
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
            Double f1 = result.getF1(EvaluationMode.MUC);
            if (!f1.equals(Double.NaN)) {
                f1Muc += f1;
            }
            Double f12 = result.getF1(EvaluationMode.EXACT_MATCH);
            if (!f12.equals(Double.NaN)) {
                f1Exact += f12;
            }

        }

        // summary
        StringBuilder summary = new StringBuilder();
        summary.append("Precision-Exact:").append(precisionExact / files.length).append('\n');
        summary.append("Recall-Exact:").append(recallExact / files.length).append('\n');
        summary.append("F1-Exact:").append(f1Exact / files.length).append('\n');
        summary.append('\n');
        summary.append("Precision-MUC:").append(precisionMuc / files.length).append('\n');
        summary.append("Recall-MUC:").append(recallMuc / files.length).append('\n');
        summary.append("F1-MUC:").append(f1Muc / files.length).append('\n');

        StringBuilder detailedOutput = new StringBuilder();
        detailedOutput.append(summary.toString().replace(':', ';'));
        detailedOutput.append("\n\n\n");

        // detailed error stats
        for (Entry<ResultType, Map<String, List<Annotation>>> entry : errors.entrySet()) {
            ResultType resultType = entry.getKey();
            int errorTypeCount = 0;
            for (List<Annotation> errorEntry : entry.getValue().values()) {
                errorTypeCount += errorEntry.size();
            }
            detailedOutput.append(resultType.getDescription()).append(";").append(errorTypeCount).append("\n");
            for (Entry<String, List<Annotation>> errorEntry : entry.getValue().entrySet()) {
                for (Annotation annotation : errorEntry.getValue()) {
                    String fileName = errorEntry.getKey();
                    detailedOutput.append("\t").append(annotation).append(";").append(fileName).append("\n");
                }
            }
            detailedOutput.append("\n\n");
        }

        FileHelper.writeToFile("data/temp/" + System.currentTimeMillis() + "_allErrors.csv", detailedOutput);
        System.out.println(summary);
    }

    public static void main(String[] args) {
        String DATASET_LOCATION = "/Users/pk/Desktop/LocationLab/LocationExtractionDataset";
        // String DATASET_LOCATION = "/Users/pk/Desktop/tmp";
        // String DATASET_LOCATION = "C:\\Users\\Sky\\Desktop\\LocationExtractionDatasetSmall";
        // String DATASET_LOCATION = "Q:\\Users\\David\\Desktop\\LocationExtractionDataset";
        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        // Map<String, Double> results = evaluator.evaluateAll(
        // new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new AlchemyLocationExtractor(
        // "b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new YahooLocationExtractor(), DATASET_LOCATION);

        StopWatch stopWatch = new StopWatch();
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // LocationSource database = new NewsSeecrLocationSource("tr1dn3mc0bdhzzjngkvzahqloxph0e");
        evaluator.evaluateAll(new PalladianLocationExtractor(database), DATASET_LOCATION);

        System.out.println(stopWatch.getElapsedTimeString());
    }

}
