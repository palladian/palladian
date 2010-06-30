package tud.iir.classification.page.evaluation;

/**
 * This class is a container for the averaged classifier performance.
 * 
 * @author David Urbansky
 * 
 */
public class AverageClassifierPerformance {

    private double precision = -1.0;
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

    public double getF1() {
        return 2 * getPrecision() * getRecall() / (getPrecision() + getRecall());
    }

}
