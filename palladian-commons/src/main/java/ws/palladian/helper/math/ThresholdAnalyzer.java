package ws.palladian.helper.math;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.Bag;

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
public class ThresholdAnalyzer implements Iterable<ThresholdAnalyzer.ThresholdEntry> {

    /**
     * An entry for a specific threshold supplying precision, recall and f-measure.
     * 
     * @author pk
     */
    public static final class ThresholdEntry {

        /** Length in characters for the printed bar visualizing the F1 measure. */
        private static final int F1_BAR_LENGTH = 50;

        private final double t;
        private final double pr;
        private final double rc;

        ThresholdEntry(double t, double pr, double rc) {
            this.t = t;
            this.pr = pr;
            this.rc = rc;
        }
        
        public double getThreshold() {
            return t;
        }
        
        public double getPrecision() {
            return pr;
        }
        
        public double getRecall() {
            return rc;
        }

        public double getF1() {
            return 2 * pr * rc / (pr + rc);
        }

        @Override
        public String toString() {
            return internalToString("threshold=%s: pr=%s, rc=%s, f1=%s");
        }

        private String internalToString(String format) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(5);
            return String.format(format, numberFormat.format(t), numberFormat.format(pr), numberFormat.format(rc),
                    numberFormat.format(getF1()), makeBar(getF1()));
        }

        private String makeBar(double f1) {
            return StringUtils.repeat('*', (int)Math.round(F1_BAR_LENGTH * f1));
        }

    }

    private final int numBins;

    private final Bag<Integer> truePositiveItems;

    private final Bag<Integer> retrievedItems;

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
        this.retrievedItems = Bag.create();
        this.truePositiveItems = Bag.create();
        this.relevantItems = 0;
    }

    /**
     * <p>
     * Get the precision for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the precision, must be in range [0,1].
     * @return The precision at the specified threshold.
     * @deprecated Use {@link #getEntry(double)}.
     */
    @Deprecated
    public double getPrecision(double threshold) {
        return getEntry(threshold).pr;
    }

    /**
     * <p>
     * Get the recall for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the recall, must be in range [0,1].
     * @return The recall at the specified threshold.
     * @deprecated Use {@link #getEntry(double)}.
     */
    @Deprecated
    public double getRecall(double threshold) {
        return getEntry(threshold).rc;
    }

    /**
     * <p>
     * Get the F1 for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the F1, must be in range [0,1].
     * @return The F1 at the specified threshold.
     * @deprecated Use {@link #getEntry(double)}.
     */
    @Deprecated
    public double getF1(double threshold) {
        double pr = getPrecision(threshold);
        double rc = getRecall(threshold);
        return 2 * pr * rc / (pr + rc);
    }

    @Override
    public Iterator<ThresholdEntry> iterator() {
        return new AbstractIterator<ThresholdEntry>() {
            // start in the bin, where we actually have entries (everything below gives same values as here).
            int bin = Collections.min(retrievedItems.uniqueItems());

            @Override
            protected ThresholdEntry getNext() throws Finished {
                double threshold = (double)bin++ / numBins;
                if (threshold > 1) {
                    throw FINISHED;
                }
                ThresholdEntry entry = getEntry(threshold);
                if (entry.rc == 0) { // no more useful information from here, stop.
                    throw FINISHED;
                }
                return entry;
            }
        };
    }

    /**
     * <p>
     * Get a threshold entry supplying precision, recall and F1 measure for the specified threshold.
     * </p>
     * 
     * @param threshold The threshold for which to get the entry, must be in range [0,1].
     * @return The threshold entry for the specified threshold.
     */
    public ThresholdEntry getEntry(double threshold) {
        int numRelevantRetrieved = getTruePositiveAt(threshold);
        int numRetrieved = getRetrievedAt(threshold);
        double pr = (double)numRelevantRetrieved / numRetrieved;
        double rc = (double)numRelevantRetrieved / relevantItems;
        return new ThresholdEntry(threshold, pr, rc);
    }

    /**
     * <p>
     * Get the maximum achieved F1 value for the whole threshold interval.
     * </p>
     * 
     * @return The maximum F1 value.
     * @deprecated Use {@link #getMaxF1Entry()} instead.
     */
    @Deprecated
    public double getMaxF1() {
//        double maxF1 = 0;
//        for (int i = 0; i <= numBins; i++) {
//            double threshold = (double)i / numBins;
//            double f1 = getF1(threshold);
//            if (Double.isNaN(f1)) {
//                continue;
//            }
//            maxF1 = Math.max(maxF1, f1);
//        }
//        return maxF1;
        ThresholdEntry maxF1Entry = getMaxF1Entry();
        return maxF1Entry != null ? maxF1Entry.getF1() : 0;
    }

    /**
     * <p>
     * Get the threshold entry, for which a maximum F1 value is achieved.
     * </p>
     * 
     * @return ThresholdEntry with maximum F1 value.
     */
    public ThresholdEntry getMaxF1Entry() {
        ThresholdEntry entry = null;
        for (ThresholdEntry currentEntry : this) {
            if (entry == null || entry.getF1() < currentEntry.getF1()) {
                entry = currentEntry;
            }
        }
        return entry;
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
            retrieved += retrievedItems.count(i);
        }
        return retrieved;
    }

    int getTruePositiveAt(double threshold) {
        int truePositive = 0;
        for (int i = getBin(threshold); i <= numBins; i++) {
            truePositive += truePositiveItems.count(i);
        }
        return truePositive;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("t\tPr\tRc\tF1\n");
        for (ThresholdEntry entry : this) {
            sb.append(entry.internalToString("%s\t%s\t%s\t%s\t%s\n"));
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        ThresholdEntry maxF1Entry = getMaxF1Entry();
        sb.append('\n').append("Max. F1=").append(numberFormat.format(maxF1Entry.getF1())).append("@t=")
                .append(numberFormat.format(maxF1Entry.getThreshold()));
        return sb.toString();
    }

}
