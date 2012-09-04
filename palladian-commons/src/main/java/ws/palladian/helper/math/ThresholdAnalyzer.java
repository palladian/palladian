package ws.palladian.helper.math;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import ws.palladian.helper.collection.CountMap;

/**
 * <p>
 * This class allows threshold analysis for binary classification or retrieval results with confidence values. It allows
 * to calculate Precision, Recall and F1 measures depending on varying confidence values. Internally, the data is kept
 * in histogram-like bins, which allows the analysis of huge result sets. To regulate the granularity of the results,
 * the number of bins can be specified by using the constructor {@link #ThresholdAnalyzer(int)}.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://en.wikipedia.org/wiki/Precision_and_recall">Precision and recall</a>
 * @see <a href="http://en.wikipedia.org/wiki/F1_score">F1 score</a>
 */
public class ThresholdAnalyzer {

    private final int numBins;

    private final CountMap<Integer> truePositiveItems;

    private final CountMap<Integer> retrievedItems;

    private int relevantItems;

    /**
     * <p>
     * Initialize a new, empty {@link ThresholdAnalyzer} with five bins.
     * </p>
     */
    public ThresholdAnalyzer() {
        this(5);
    }

    /**
     * <p>
     * Initialize a new, empty {@link ThresholdAnalyzer} with the specified number of bins.
     * </p>
     * 
     * @param numBins The number of bins to use, the more, the more fine grained the result.
     */
    public ThresholdAnalyzer(int numBins) {
        if (numBins < 2) {
            throw new IllegalArgumentException("numBins must be least two, was " + numBins);
        }
        this.numBins = numBins;
        this.retrievedItems = CountMap.create();
        this.truePositiveItems = CountMap.create();
        this.relevantItems = 0;
    }

    /**
     * <p>
     * Get the precision for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the precision, must be in range [0,1].
     * @return The precision at the specified threshold.
     */
    public double getPrecision(double threshold) {
        int numRelevantRetrieved = getTruePositiveAt(threshold);
        int numRetrieved = getRetrievedAt(threshold);
        return (double)numRelevantRetrieved / numRetrieved;
    }

    /**
     * <p>
     * Get the recall for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the recall, must be in range [0,1].
     * @return The recall at the specified threshold.
     */
    public double getRecall(double threshold) {
        int numRelevantRetrieved = getTruePositiveAt(threshold);
        return (double)numRelevantRetrieved / relevantItems;
    }

    /**
     * <p>
     * Get the F1 for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the F1, must be in range [0,1].
     * @return The F1 at the specified threshold.
     */
    public double getF1(double threshold) {
        double pr = getPrecision(threshold);
        double rc = getRecall(threshold);
        return 2 * pr * rc / (pr + rc);
    }

    /**
     * <p>
     * Add a record (e.g. a document) for analysis. A record consists of the actual class (i.e. <code>relevant</code> or
     * <code>notRelevant</code> ) and a confidence value determined e.g. by a classifier, which denotes the certainty of
     * the classifier that the made decision for being <code>relevant</code> is correct.
     * </p>
     * 
     * @param relevant Whether this record is actually relevant.
     * @param confidence The confidence, determined by some algorithm to evaluate, for prediction relevant.
     */
    public void add(boolean relevant, double confidence) {
        int bin = getBin(confidence);
        if (relevant) {
            relevantItems++;
            truePositiveItems.add(bin);
        }
        retrievedItems.add(bin);
    }

    /** package private methods for unit testing. */

    int getBin(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Threshold must be in range [0,1], but was " + threshold);
        }
        return (int)Math.round(threshold * numBins);
    }

    int getRetrievedAt(double threshold) {
        int retrieved = 0;
        for (int i = getBin(threshold); i <= numBins; i++) {
            retrieved += retrievedItems.get(i);
        }
        return retrieved;
    }

    int getTruePositiveAt(double threshold) {
        int truePositive = 0;
        for (int i = getBin(threshold); i <= numBins; i++) {
            truePositive += truePositiveItems.get(i);
        }
        return truePositive;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        NumberFormat format = new DecimalFormat("#0.00");
        sb.append("t\tpr\trc\tf1\n");
        for (int i = 0; i <= numBins; i++) {
            double threshold = (double)i / numBins;
            double pr = getPrecision(threshold);
            double rc = getRecall(threshold);
            double f1 = getF1(threshold);
            sb.append(format.format(threshold)).append('\t');
            sb.append(format.format(pr)).append('\t');
            sb.append(format.format(rc)).append('\t');
            sb.append(format.format(f1)).append('\n');
        }
        return sb.toString();
    }

}
