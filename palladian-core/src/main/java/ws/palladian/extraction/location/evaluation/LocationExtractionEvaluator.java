package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class LocationExtractionEvaluator {

    public Map<String, Double> evaluateAll(LocationExtractor extractor, String goldStandardFileFolderPath) {

        Map<ResultType, Map<String, Annotations>> errors = new LinkedHashMap<ResultType, Map<String, Annotations>>();
        errors.put(ResultType.CORRECT, new HashMap<String, Annotations>());
        errors.put(ResultType.ERROR1, new HashMap<String, Annotations>());
        errors.put(ResultType.ERROR2, new HashMap<String, Annotations>());
        errors.put(ResultType.ERROR3, new HashMap<String, Annotations>());
        errors.put(ResultType.ERROR4, new HashMap<String, Annotations>());
        errors.put(ResultType.ERROR5, new HashMap<String, Annotations>());
        Map<String, Double> averageResult = CollectionHelper.newHashMap();

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
            String path = "data/temp/" + file.getName();
            File file1 = new File(path);
            FileHelper.writeToFile(path, FileHelper.readFileToString(file).replace(" role=\"main\"", ""));
            EvaluationResult result = extractor.evaluate(file1.getAbsolutePath(), TaggingFormat.XML);

            // write major error log
            Map<ResultType, Annotations> fileErrors = result.getErrorAnnotations();
            errors.get(ResultType.CORRECT).put(file.getName(), fileErrors.get(ResultType.CORRECT));
            errors.get(ResultType.ERROR1).put(file.getName(), fileErrors.get(ResultType.ERROR1));
            errors.get(ResultType.ERROR2).put(file.getName(), fileErrors.get(ResultType.ERROR2));
            errors.get(ResultType.ERROR3).put(file.getName(), fileErrors.get(ResultType.ERROR3));
            errors.get(ResultType.ERROR4).put(file.getName(), fileErrors.get(ResultType.ERROR4));
            errors.get(ResultType.ERROR5).put(file.getName(), fileErrors.get(ResultType.ERROR5));

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

        averageResult.put("Precision-MUC", precisionMuc / files.length);
        averageResult.put("Precision-Exact", precisionExact / files.length);
        averageResult.put("Recall-MUC", recallMuc / files.length);
        averageResult.put("Recall-Exact", recallExact / files.length);
        averageResult.put("F1-MUC", f1Muc / files.length);
        averageResult.put("F1-Exact", f1Exact / files.length);

        StringBuilder allErrors = new StringBuilder();
        for (Entry<ResultType, Map<String, Annotations>> entry : errors.entrySet()) {
            ResultType resultType = entry.getKey();
            int errorTypeCount = 0;
            for (Annotations errorEntry : entry.getValue().values()) {
                errorTypeCount += errorEntry.size();
            }
            allErrors.append(getErrorTypeLine(resultType)).append(";").append(errorTypeCount).append("\n");
            for (Entry<String, Annotations> errorEntry : entry.getValue().entrySet()) {
                for (Annotation annotation : errorEntry.getValue()) {
                    String fileName = errorEntry.getKey();
                    allErrors.append("\t").append(annotation).append(";").append(fileName).append("\n");
                }
            }
            allErrors.append("\n\n");
        }
        FileHelper.writeToFile("data/temp/"+System.currentTimeMillis()+"_allErrors.csv", allErrors);

        return averageResult;
    }

    private String getErrorTypeLine(ResultType resultType) {
        switch (resultType) {
            case CORRECT:
                return "CORRECT (tag and boundaries correct)";
            case ERROR1:
                return "ERROR 1 (tagged something that should not have been tagged, false positive - bad for precision)";
            case ERROR2:
                return "ERROR 2 (completely missed a location, false negative - bad for recall)";
            case ERROR3:
                return "ERROR 3 (incorrect location type but correct boundaries - bad for precision and recall)";
            case ERROR4:
                return "ERROR 4 (correct location type but incorrect boundaries - bad for precision and recall)";
            case ERROR5:
                return "ERROR 5 (incorrect location type and incorrect boundaries - bad for precision and recall)";
        }
        return "FAIL";
    }

    // public EvaluationResult evaluate(LocationExtractor extractor, String goldStandardTaggedTextPath) {
    //
    // EvaluationResult result = locationExtractor.evaluate(goldStandardTaggedTextPath, TaggingFormat.XML);
    // System.out.println(result.getF1(EvaluationResult.EXACT_MATCH));
    //
    // return averageResult;
    // }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        String DATASET_LOCATION = "/Users/pk/Desktop/LocationLab/LocationExtractionDataset";
        // String DATASET_LOCATION = "/Users/pk/Desktop/Test";
        // String DATASET_LOCATION = "C:\\Users\\Sky\\Desktop\\LocationExtractionDatasetSmall";
        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        // Map<String, Double> results = evaluator.evaluateAll(
        // new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new AlchemyLocationExtractor(
        // "b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new YahooLocationExtractor(), DATASET_LOCATION);
        Map<String, Double> results = evaluator.evaluateAll(new PalladianLocationExtractor(database), DATASET_LOCATION);
        // "C:\\Users\\Sky\\Desktop\\LocationExtractionDataset");
        // Map<String, Double> results = evaluator.evaluateAll(new YahooLocationExtractor(), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(
        // new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"),
        // "C:\\Users\\Sky\\Desktop\\LocationExtractionDataset");

        CollectionHelper.print(results);

        // EvaluationResult result = extractor.evaluate("text5.txt", TaggingFormat.XML);
    }

}
