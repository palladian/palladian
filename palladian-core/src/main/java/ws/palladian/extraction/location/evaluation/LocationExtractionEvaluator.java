package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.Map;

import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.YahooLocationExtractor;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class LocationExtractionEvaluator {

    public Map<String, Double> evaluateAll(LocationExtractor extractor, String goldStandardFileFolderPath) {

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

            Double precision = result.getPrecision(EvaluationResult.MUC);
            if (!precision.equals(Double.NaN)) {
                precisionMuc += precision;
            }
            Double precision2 = result.getPrecision(EvaluationResult.EXACT_MATCH);
            if (!precision2.equals(Double.NaN)) {
                precisionExact += precision2;
            }
            Double recall = result.getRecall(EvaluationResult.MUC);
            if (!recall.equals(Double.NaN)) {
                recallMuc += recall;
            }
            Double recall2 = result.getRecall(EvaluationResult.EXACT_MATCH);
            if (!recall2.equals(Double.NaN)) {
                recallExact += recall2;
            }
            Double f1 = result.getF1(EvaluationResult.MUC);
            if (!f1.equals(Double.NaN)) {
                f1Muc += f1;
            }
            Double f12 = result.getF1(EvaluationResult.EXACT_MATCH);
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

        return averageResult;
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
        // String DATASET_LOCATION = "C:\\Users\\Sky\\Desktop\\LocationExtractionDataset";
        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        // Map<String, Double> results = evaluator.evaluateAll(
        // new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new AlchemyLocationExtractor(
        // "b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"), DATASET_LOCATION);
        Map<String, Double> results = evaluator.evaluateAll(new YahooLocationExtractor(), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(new PalladianLocationExtractor(
        // PalladianNerExperiments.WX_API_KEY, database), DATASET_LOCATION);
        // "C:\\Users\\Sky\\Desktop\\LocationExtractionDataset");
        // Map<String, Double> results = evaluator.evaluateAll(new YahooLocationExtractor(), DATASET_LOCATION);
        // Map<String, Double> results = evaluator.evaluateAll(
        // new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5"),
        // "C:\\Users\\Sky\\Desktop\\LocationExtractionDataset");

        CollectionHelper.print(results);

        // EvaluationResult result = extractor.evaluate("text5.txt", TaggingFormat.XML);
    }

}
