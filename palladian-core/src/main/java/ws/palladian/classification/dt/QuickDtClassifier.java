package ws.palladian.classification.dt;

import quickdt.Attributes;
import quickdt.HashMapAttributes;
import quickdt.PredictiveModel;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Classifier;
import ws.palladian.processing.Classifiable;

/**
 * <p>
 * Classifier for models built with {@link QuickDtLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class QuickDtClassifier implements Classifier<QuickDtModel> {

    @Override
    public CategoryEntries classify(Classifiable classifiable, QuickDtModel model) {
        PredictiveModel pm = model.getModel();
        Attributes attributes = HashMapAttributes.create(QuickDtLearner.getInput(classifiable));
        CategoryEntriesMap categoryEntries = new CategoryEntriesMap();
        for (String targetClass : model.getCategories()) {
            categoryEntries.set(targetClass, pm.getProbability(attributes, targetClass));
        }
        categoryEntries.sort();
        return categoryEntries;
    }

}
