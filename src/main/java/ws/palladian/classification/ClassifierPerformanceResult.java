package ws.palladian.classification;

import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.Matrix;

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
