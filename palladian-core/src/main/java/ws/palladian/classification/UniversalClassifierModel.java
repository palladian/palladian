package ws.palladian.classification;

import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;

public class UniversalClassifierModel implements Model {

    private static final long serialVersionUID = 1L;

    private final NaiveBayesModel bayesModel;
    private final KnnModel knnModel;
    private final DictionaryClassifier textClassifier;

    public UniversalClassifierModel(NaiveBayesModel bayesModel, KnnModel knnModel, DictionaryClassifier textClassifier) {
        this.bayesModel = bayesModel;
        this.knnModel = knnModel;
        this.textClassifier = textClassifier;
    }

    public NaiveBayesModel getBayesModel() {
        return bayesModel;
    }

    public KnnModel getKnnModel() {
        return knnModel;
    }

    public DictionaryClassifier getTextClassifier() {
        return textClassifier;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UniversalClassifierModel [bayesModel=");
        builder.append(bayesModel);
        builder.append(", knnModel=");
        builder.append(knnModel);
        builder.append(", textClassifier=");
        builder.append(textClassifier);
        builder.append(", dictionary#documents=").append(textClassifier.getDictionary().getNumberOfDocuments());
        builder.append(", dictionary#entries=").append(textClassifier.getDictionary().size());
        builder.append("]");
        return builder.toString();
    }
}
