package ws.palladian.classification.dt;

import quickdt.Attributes;
import quickdt.HashMapAttributes;
import quickdt.PredictiveModel;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;

/**
 * <p>
 * Classifier for models built with {@link QuickDtLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class QuickDtClassifier implements Classifier<QuickDtModel> {

    @Override
    public CategoryEntries classify(FeatureVector featureVector, QuickDtModel model) {
        PredictiveModel pm = model.getModel();
        Attributes attributes = HashMapAttributes.create(QuickDtLearner.getInput(featureVector));
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        for (String targetClass : model.getCategories()) {
            builder.set(targetClass, pm.getProbability(attributes, targetClass));
        }
        return builder.create();
    }

}
