package ws.palladian.extraction.keyphrase.evaluation;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * <p>
 * Keeps results concerning the keyphrase extraction evaluation like precision, recall, f1.
 * </p>
 * 
 * @author Philipp Katz
 */
public class KeyphraseExtractorEvaluationResult {

    // private static final char NEWLINE = '\n';
    private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("0.0000");

    private int itemCount;
    private double precisionSum;
    private double recallSum;
    private int keyphraseCount;

    public void addTestResult(double precision, double recall, int assignedKeyphrases) {
        precisionSum += precision;
        recallSum += recall;
        keyphraseCount += assignedKeyphrases;
        itemCount++;
    }

    public double getAvgPrecision() {
        return precisionSum / itemCount;
    }

    public double getAvgRecall() {
        return recallSum / itemCount;
    }

    public double getAvgFOne() {
        return 2 * getAvgPrecision() * getAvgRecall() / (getAvgPrecision() + getAvgRecall());
    }

    public double getAvgKeyphraseCount() {
        return (double)keyphraseCount / itemCount;
    }

    public int getItemCount() {
        return itemCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("average precision: ").append(format(getAvgPrecision())).append(" ");
        sb.append("average recall: ").append(format(getAvgRecall())).append(" ");
        sb.append("average f1: ").append(format(getAvgFOne())).append(" ");
        sb.append("average assigned count: ").append(format(getAvgKeyphraseCount())).append(" ");
        sb.append("processed items: ").append(getItemCount());
        return sb.toString();
    }

    private static final String format(double value) {
        return NUMBER_FORMAT.format(value);
    }

}