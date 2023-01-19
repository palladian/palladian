package ws.palladian.classification.utils;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Model;

import java.util.Objects;

/**
 * Store a {@link Model} together with its {@link Classifier}.
 *
 * @param <M> Type of the model
 * @author Philipp Katz
 */
public class ClassifierWithModel<M extends Model> {
    private final Classifier<M> classifier;
    private final M model;

    public ClassifierWithModel(Classifier<M> classifier, M model) {
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    public CategoryEntries classify(FeatureVector featureVector) {
        return classifier.classify(featureVector, model);
    }
}
