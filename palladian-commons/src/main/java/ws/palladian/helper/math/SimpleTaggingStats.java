package ws.palladian.helper.math;

/**
 * Simple stats to compute precision, recall, accuracy and F1 for tagging.
 * Created by David Urbansky on 22.05.2016.
 * 
 * @author David Urbansky
 */
public class SimpleTaggingStats {

    private int truePositives = 0;
    private int falsePositives = 0;
    private int trueNegatives = 0;
    private int falseNegatives = 0;

    public void incrementTruePositives() {
        truePositives++;
    }

    public void incrementFalsePositives() {
        falsePositives++;
    }

    public void incrementTrueNegatives() {
        trueNegatives++;
    }

    public void incrementFalseNegatives() {
        falseNegatives++;
    }

    public double getPrecision() {
        return truePositives / ((double)truePositives + falsePositives);
    }

    public double getRecall() {
        return truePositives / ((double)truePositives + falseNegatives);
    }

    public double getF1() {
        return 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
    }

    public int getTruePositives() {
        return truePositives;
    }

    public void setTruePositives(int truePositives) {
        this.truePositives = truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public void setFalsePositives(int falsePositives) {
        this.falsePositives = falsePositives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public void setTrueNegatives(int trueNegatives) {
        this.trueNegatives = trueNegatives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    public void setFalseNegatives(int falseNegatives) {
        this.falseNegatives = falseNegatives;
    }
}
