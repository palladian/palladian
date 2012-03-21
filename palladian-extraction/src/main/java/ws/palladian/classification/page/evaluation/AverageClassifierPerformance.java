package ws.palladian.classification.page.evaluation;

/**
 * This class is a container for the averaged classifier performance.
 * 
 * @author David Urbansky
 * 
 */
public class AverageClassifierPerformance {

    /** The average precision of the classifier. */
    private double precision = -1.0;

    /** The average recall of the classifier. */
    private double recall = -1.0;

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

    /**
     * Calculate the F1 score.
     * 
     * @return The F1 score.
     */
    public double getF1() {
        return 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
    }

}