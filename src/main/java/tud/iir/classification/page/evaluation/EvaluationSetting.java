package tud.iir.classification.page.evaluation;

import java.util.ArrayList;
import java.util.List;

public class EvaluationSetting {

    // //////// presets //////////
    /** Evaluate quickly. */
    public static final int PRESET_SIMPLE_EVALUATION = 1;

    /** Evaluate moderately. */
    public static final int PRESET_MODERATE_EVALUATION = 2;

    /** Evaluate intensively. */
    public static final int PRESET_INTENSE_EVALUATION = 3;

    /** Number of folds for cross validation. */
    private int kFolds = 10;

    /** Minimum percentage of training data, testing percentage is 100 - training percentage. */
    private double trainingPercentageMin = 20;

    /** Maximum percentage of training data, testing percentage is 100 - training percentage. */
    private double trainingPercentageMax = 80;

    /** Steps in percent to increase the training percentage from min to max. */
    private double trainingPercentageStep = 10;

    /** If true, samples will be taken randomly. */
    private boolean random = true;

    /** List of datasets for the evaluation. */
    private List<Dataset> datasets = new ArrayList<Dataset>();

    public EvaluationSetting() {
    }

    public EvaluationSetting(int preset) {
        setPreset(preset);
    }

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

    private void setPreset(int preset) {

        switch (preset) {
            case PRESET_SIMPLE_EVALUATION:
                setkFolds(1);
                setRandom(false);
                setTrainingPercentageMin(50);
                setTrainingPercentageMax(50);
                setTrainingPercentageStep(10);
                break;
            case PRESET_MODERATE_EVALUATION:
                setkFolds(3);
                setRandom(true);
                setTrainingPercentageMin(30);
                setTrainingPercentageMax(70);
                setTrainingPercentageStep(10);
                break;
            case PRESET_INTENSE_EVALUATION:
                setkFolds(10);
                setRandom(true);
                setTrainingPercentageMin(10);
                setTrainingPercentageMax(90);
                setTrainingPercentageStep(10);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EvaluationSetting [datasets=");
        builder.append(datasets);
        builder.append(", kFolds=");
        builder.append(kFolds);
        builder.append(", random=");
        builder.append(random);
        builder.append(", trainingPercentageMax=");
        builder.append(trainingPercentageMax);
        builder.append(", trainingPercentageMin=");
        builder.append(trainingPercentageMin);
        builder.append(", trainingPercentageStep=");
        builder.append(trainingPercentageStep);
        builder.append("]");
        return builder.toString();
    }

}