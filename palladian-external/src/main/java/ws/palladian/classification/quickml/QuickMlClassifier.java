package ws.palladian.classification.quickml;

import org.apache.commons.lang3.Validate;

import quickml.data.AttributesMap;
import quickml.data.PredictionMap;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;

/**
 * <p>
 * Classifier for models built with {@link QuickMlLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class QuickMlClassifier implements Classifier<QuickMlModel> {

    @Override
    public CategoryEntries classify(FeatureVector featureVector, QuickMlModel model) {
        Validate.notNull(featureVector, "featureVector must not be null");
        Validate.notNull(model, "model must not be null");
        quickml.supervised.classifier.Classifier pm = model.getClassifier();
        AttributesMap attributes = QuickMlLearner.getInput(featureVector);
        PredictionMap prediction = pm.predict(attributes);
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        for (String targetClass : model.getCategories()) {
            builder.set(targetClass, prediction.get(targetClass));
        }
        return builder.create();
    }
    
    @Override
    public String toString() {
    	return getClass().getSimpleName();
    }

}
