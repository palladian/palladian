package ws.palladian.classification;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ws.palladian.helper.math.ConfusionMatrix;

public class ClassifierPerformanceResult {

    private double precision = -1.0;
    private double recall = -1.0;
    private double f1 = -1.0;

    private double sensitivity = -1.0;
    private double specificity = -1.0;
    private double accuracy = -1.0;

    private double correctlyClassified = -1.0;
    
    /** Superiority is the factor with which the classifier is better than the highest prior in the dataset: Superiority = correctlyClassified / percentHighestPrior. A superiority of 1 means it doesn't make sense classifying at all since we could simply always take the category with the highest prior. A superiority smaller 1 means the classifier is harmful. */
    private double superiority = -1.0;
    
    private ConfusionMatrix confusionMatrix = new ConfusionMatrix();

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
    private Map<Double, Double[]> thresholdBucketMap = new TreeMap<Double, Double[]>();

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
    private Map<Double, Double[]> thresholdAccumulativeMap = new TreeMap<Double, Double[]>();

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getF1() {
        return f1;
    }

    public void setF1(double f1) {
        this.f1 = f1;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(double sensitivity) {
        this.sensitivity = sensitivity;
    }

    public double getSpecificity() {
        return specificity;
    }

    public void setSpecificity(double specificity) {
        this.specificity = specificity;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public void setCorrectlyClassified(double correctlyClassified) {
        this.correctlyClassified = correctlyClassified;
    }

    public double getCorrectlyClassified() {
        return correctlyClassified;
    }

    public double getSuperiority() {
        return superiority;
    }

    public void setSuperiority(double superiority) {
        this.superiority = superiority;
    }
    
    public void setConfusionMatrix(ConfusionMatrix confusionMatrix) {
        this.confusionMatrix = confusionMatrix;
    }
    
    public ConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }
    
    public void setThresholdBucketMap(Map<Double, Double[]> thresholdBucketMap) {
        this.thresholdBucketMap = thresholdBucketMap;
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

    public void setThresholdAccumulativeMap(Map<Double, Double[]> thresholdAccumulativeMap) {
        this.thresholdAccumulativeMap = thresholdAccumulativeMap;
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
