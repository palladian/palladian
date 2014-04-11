package ws.palladian.classification.universal;

import java.util.Set;

import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.core.Model;
import ws.palladian.helper.collection.CollectionHelper;

public class UniversalClassifierModel implements Model {

    private static final long serialVersionUID = 1L;

    private final NaiveBayesModel bayesModel;
    private final KnnModel knnModel;
    private final DictionaryModel dictionaryModel;

    UniversalClassifierModel(NaiveBayesModel bayesModel, KnnModel knnModel, DictionaryModel dictionaryModel) {
        this.bayesModel = bayesModel;
        this.knnModel = knnModel;
        this.dictionaryModel = dictionaryModel;
    }

    public NaiveBayesModel getBayesModel() {
        return bayesModel;
    }

    public KnnModel getKnnModel() {
        return knnModel;
    }

    public DictionaryModel getDictionaryModel() {
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
        builder.append("]");
        return builder.toString();
    }

    @Override
    public Set<String> getCategories() {
        Set<String> result = CollectionHelper.newHashSet();
        if (bayesModel != null) {
            result.addAll(bayesModel.getCategories());
        }
        if (knnModel != null) {
            result.addAll(knnModel.getCategories());
        }
        if (dictionaryModel != null) {
            result.addAll(dictionaryModel.getCategories());
        }
        return result;
    }

}
