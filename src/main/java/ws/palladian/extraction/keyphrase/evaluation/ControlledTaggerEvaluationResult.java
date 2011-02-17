/**
 * 
 */
package ws.palladian.extraction.keyphrase.evaluation;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import ws.palladian.helper.StopWatch;

/**
 * Keeps results concerning the Tagger evaluation for specific {@link ControlledTaggerEvaluationSettings} like
 * Pr/Rc/F1, etc.
 * 
 * @author Philipp Katz
 * 
 */
public class ControlledTaggerEvaluationResult {

    private int taggedEntryCount;
    private double precisionSum;
    private double recallSum;
    private double tagSum;

    private StopWatch trainStop;
    private StopWatch testStop;

    private NumberFormat format = new DecimalFormat("0.0000");

    public void addTestResult(double precision, double recall, int assignedTags) {
        precisionSum += precision;
        recallSum += recall;
        tagSum += assignedTags;
        taggedEntryCount++;
    }

    public double getAvgPrecision() {
        return precisionSum / taggedEntryCount;
    }

    public double getAvgRecall() {
        return recallSum / taggedEntryCount;
    }

    public double getAvgFOne() {
        return 2 * getAvgPrecision() * getAvgRecall() / (getAvgPrecision() + getAvgRecall());
    }

    public double getAvgTagCount() {
        return tagSum / taggedEntryCount;
    }

    public int getTaggedEntryCount() {
        return taggedEntryCount;
    }

    public void startTraining() {
        trainStop = new StopWatch();
    }

    public void stopTraining() {
        trainStop.stop();
    }

    public void startTesting() {
        testStop = new StopWatch();
    }

    public void stopTesting() {
        testStop.stop();
    }

    public StopWatch getTestStop() {
        return testStop;
    }

    public StopWatch getTrainStop() {
        return trainStop;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ControlledTaggerEvaluationResult:");
        sb.append("taggedEntries:").append(taggedEntryCount);
        if (trainStop != null) sb.append(",timeForTraining:").append(trainStop.getElapsedTimeString());
        if (testStop != null) sb.append(",timeForTesting:").append(testStop.getElapsedTimeString());
        sb.append(",averageTagCount:").append(getAvgTagCount());
        sb.append(",averagePr:").append(getAvgPrecision());
        sb.append(",averageRc:").append(getAvgRecall());
        sb.append(",averageF1:").append(getAvgFOne());
        return sb.toString();
    }

    public void printStatistics() {

        System.out.println("---------------------------------------------------------------------");
        System.out.println("average pr: " + format.format(getAvgPrecision()) + " rc: " + format.format(getAvgRecall())
                + " f1: " + format.format(getAvgFOne()));
        System.out.println("average # assigned tags: " + format.format(getAvgTagCount()));
        System.out.println("tagged entries: " + getTaggedEntryCount());

        if (getTrainStop() != null) System.out.println("time for training " + getTrainStop().getElapsedTimeString());
        if (getTestStop() != null) System.out.println("time for testing " + getTestStop().getElapsedTimeString());

    }
}