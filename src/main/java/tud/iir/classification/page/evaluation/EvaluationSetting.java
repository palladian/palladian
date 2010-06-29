package tud.iir.classification.page.evaluation;

import java.util.ArrayList;
import java.util.List;

public class EvaluationSetting {

    /** number of folds for cross validation */
    private int kFolds = 10;

    /** minimum percentage of training data, testing percentage is 100 - training percentage */
    private double trainingPercentageMin = 20;

    /** maximum percentage of training data, testing percentage is 100 - training percentage */
    private double trainingPercentageMax = 80;

    /** steps in percent to increase the training percentage from min to max */
    private double trainingPercentageStep = 10;

    /** if true, samples will be taken randomly */
    private boolean random = true;

    /** List of datasets for the evaluation */
    private List<Dataset> datasets = new ArrayList<Dataset>();

    public int getkFolds() {
        return kFolds;
    }

    public void setkFolds(int kFolds) {
        this.kFolds = kFolds;
    }

    public boolean isRandom() {
        return random;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    public void setTrainingPercentageMin(double trainingPercentageMin) {
        this.trainingPercentageMin = trainingPercentageMin;
    }

    public double getTrainingPercentageMin() {
        return trainingPercentageMin;
    }

    public void setTrainingPercentageMax(double trainingPercentageMax) {
        this.trainingPercentageMax = trainingPercentageMax;
    }

    public double getTrainingPercentageMax() {
        return trainingPercentageMax;
    }

    public void setTrainingPercentageStep(double trainingPercentageStep) {
        this.trainingPercentageStep = trainingPercentageStep;
    }

    public double getTrainingPercentageStep() {
        return trainingPercentageStep;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
    }

}
