package ws.palladian.classification.text.evaluation;

import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.math.ConfusionMatrix;

public class ClassifierPerformanceResult {

    private final double precision;
    private final double recall;
    private final double f1;

    private final double sensitivity;
    private final double specificity;
    private final double accuracy;

    private final double correctlyClassified;
    
    /** Superiority is the factor with which the classifier is better than the highest prior in the dataset: Superiority = correctlyClassified / percentHighestPrior. A superiority of 1 means it doesn't make sense classifying at all since we could simply always take the category with the highest prior. A superiority smaller 1 means the classifier is harmful. */
    private final double superiority;
    
    private final ConfusionMatrix confusionMatrix;

    /**
     * Hold the thresholds in buckets 0-0.1,0.1-0.2... with the buckets correctlyClassified. We will be able to see how
     * correct classified items are when having a trust within the bucket's threshold.
     * 
     * <pre>
     * threshold bucket | correctly classified % | number of documents in the bucket 
     * -----------------|------------------------|---------------------------------
     * 0.1-0.2          |                        |
     * ...              |                        |
     * 0.9-1.00         |                        |
     * </pre>
     */
    private final Map<Double, Double[]> thresholdBucketMap;

    /**
     * Hold the thresholds in .01 steps with the correctlyClassified value of all documents with a threshold >= the
     * threshold.
     * 
     * <pre>
     * threshold    | correctly classified  | number of documents >= threshold 
     * -------------|-----------------------|---------------------------------
     * 0.01         |                       |
     * ...          |                       |
     * 1.00         |                       |
     * </pre>
     */
    private final Map<Double, Double[]> thresholdAccumulativeMap;

    /**
     * @param precision
     * @param recall
     * @param f1
     * @param sensitivity
     * @param specificity
     * @param accuracy
     * @param correctlyClassified
     * @param superiority
     * @param confusionMatrix
     * @param thresholdBucketMap
     * @param thresholdAccumulativeMap
     */
    ClassifierPerformanceResult(double precision, double recall, double f1, double sensitivity,
            double specificity, double accuracy, double correctlyClassified, double superiority,
            ConfusionMatrix confusionMatrix, Map<Double, Double[]> thresholdBucketMap,
            Map<Double, Double[]> thresholdAccumulativeMap) {
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
        this.sensitivity = sensitivity;
        this.specificity = specificity;
        this.accuracy = accuracy;
        this.correctlyClassified = correctlyClassified;
        this.superiority = superiority;
        this.confusionMatrix = confusionMatrix;
        this.thresholdBucketMap = thresholdBucketMap;
        this.thresholdAccumulativeMap = thresholdAccumulativeMap;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getF1() {
        return f1;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public double getSpecificity() {
        return specificity;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getCorrectlyClassified() {
        return correctlyClassified;
    }

    public double getSuperiority() {
        return superiority;
    }
    
    public ConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }

    public Map<Double, Double[]> getThresholdBucketMap() {
        return thresholdBucketMap;
    }

    public String getThresholdBucketMapAsCsv() {
        StringBuilder csv = new StringBuilder();

        for (Entry<Double, Double[]> bucket : thresholdBucketMap.entrySet()) {
            csv.append(bucket.getKey());

            for (Double value : bucket.getValue()) {
                csv.append(";").append(value);
            }

            csv.append("\n");
        }

        return csv.toString();
    }

    public Map<Double, Double[]> getThresholdAccumulativeMap() {
        return thresholdAccumulativeMap;
    }

    public String getThresholdAccumulativeMapAsCsv() {
        StringBuilder csv = new StringBuilder();

        for (Entry<Double, Double[]> bucket : thresholdAccumulativeMap.entrySet()) {
            csv.append(bucket.getKey());

            for (Double value : bucket.getValue()) {
                csv.append(";").append(value);
            }

            csv.append("\n");
        }

        return csv.toString();
    }

    @Override
    public String toString() {
        StringBuilder csv = new StringBuilder();

        csv.append(precision).append(";");
        csv.append(recall).append(";");
        csv.append(f1).append(";");

        csv.append(sensitivity).append(";");
        csv.append(specificity).append(";");
        csv.append(accuracy).append(";");

        csv.append(correctlyClassified).append(";");

        csv.append(superiority);

        return csv.toString();
    }


}
