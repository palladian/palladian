package ws.palladian.classification;

public class ClassifierPerformanceResult {

    private double precision = -1.0;
    private double recall = -1.0;
    private double f1 = -1.0;

    private double sensitivity = -1.0;
    private double specificity = -1.0;
    private double accuracy = -1.0;

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

    @Override
    public String toString() {
        StringBuilder csv = new StringBuilder();

        csv.append(precision).append(";");
        csv.append(recall).append(";");
        csv.append(f1).append(";");

        csv.append(sensitivity).append(";");
        csv.append(specificity).append(";");
        csv.append(accuracy);

        return csv.toString();
    }

}
