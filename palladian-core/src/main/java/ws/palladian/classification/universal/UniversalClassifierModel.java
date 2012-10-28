package ws.palladian.classification.universal;

import java.util.Arrays;

import ws.palladian.classification.Model;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.text.DictionaryModel;

public class UniversalClassifierModel implements Model {

    private static final long serialVersionUID = 1L;

    private final NaiveBayesModel bayesModel;
    private final KnnModel knnModel;
    private final DictionaryModel dictionaryModel;

    private final double[] weights;

    public UniversalClassifierModel(NaiveBayesModel bayesModel, KnnModel knnModel, DictionaryModel dictionaryModel) {
        this.bayesModel = bayesModel;
        this.knnModel = knnModel;
        this.dictionaryModel = dictionaryModel;

        weights = new double[3];

        weights[0] = 1.0;
        weights[1] = 1.0;
        weights[2] = 1.0;
    }

    public NaiveBayesModel getBayesModel() {
        return bayesModel;
    }

    public KnnModel getKnnModel() {
        return knnModel;
    }

    public DictionaryModel getTextClassifier() {
        return dictionaryModel;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UniversalClassifierModel [bayesModel=");
        builder.append(bayesModel);
        builder.append(", knnModel=");
        builder.append(knnModel);
        builder.append(", dictionaryModel=");
        builder.append(dictionaryModel);
        builder.append(", weights=");
        builder.append(Arrays.toString(weights));
        builder.append("]");
        return builder.toString();
    }

    public double[] getWeights() {
        return weights;
    }

    void setWeights(double... weights) {
        this.weights[0] = weights[0];
        this.weights[1] = weights[1];
        this.weights[2] = weights[2];
    }

}
