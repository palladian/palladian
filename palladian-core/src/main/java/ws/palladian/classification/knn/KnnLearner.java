package ws.palladian.classification.knn;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.classification.utils.Normalizer;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

/**
 * <p>
 * A "learner" for KNN models. It stores all supplied instances to a model which are later used for prediction.
 * </p>
 *
 * @author David Urbansky
 */
public final class KnnLearner {
    /** The normalizer for numeric values. */
    private final Normalizer normalizer;

    public KnnLearner(Normalizer normalizer) {
        this.normalizer = normalizer;
    }

    public KnnModel train(Dataset dataset, Object2FloatMap<String> numericFieldsAndWeights, Object2FloatMap<String> textualFieldsAndWeights) {
        Normalization normalization = normalizer.calculate(dataset);
        return new KnnModel(dataset, normalization, numericFieldsAndWeights, textualFieldsAndWeights);
    }

    public KnnModel train(Iterable<? extends Instance> instances, Object2FloatMap<String> numericFieldsAndWeights, Object2FloatMap<String> textualFieldsAndWeights) {
        return train(new DefaultDataset(instances), numericFieldsAndWeights, textualFieldsAndWeights);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + normalizer.getClass().getSimpleName() + ")";
    }
}
