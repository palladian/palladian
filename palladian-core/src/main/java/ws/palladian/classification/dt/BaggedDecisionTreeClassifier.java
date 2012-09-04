package ws.palladian.classification.dt;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.Predictor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Simple Bagging for {@link DecisionTreeClassifier} with bootstrap sampling and majority voting. Could be extended to
 * allow for the usage of arbitrary {@link Predictor} implementations.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BaggedDecisionTreeClassifier implements Predictor<BaggedDecisionTreeModel> {
    
    /** The default number of classifiers to create, in case it is not specified explicitly. */
    public static final int DEFAULT_NUM_CLASSIFIERS = 10;

    public BaggedDecisionTreeModel learn(List<NominalInstance> instances, int numClassifiers) {
        Validate.isTrue(numClassifiers > 0, "numClassifiers must be greater than zero.");
        Random random = new Random();
        List<DecisionTreeModel> decisionTreeModels = CollectionHelper.newArrayList();
        for (int i = 0; i < numClassifiers; i++) {
            List<NominalInstance> sampling = getBagging(instances, random);
            DecisionTreeClassifier newClassifier = new DecisionTreeClassifier();
            DecisionTreeModel model = newClassifier.learn(sampling);
            decisionTreeModels.add(model);
        }
        return new BaggedDecisionTreeModel(decisionTreeModels);
    }

    @Override
    public BaggedDecisionTreeModel learn(List<NominalInstance> instances) {
        return learn(instances, DEFAULT_NUM_CLASSIFIERS);
    }

    @Override
    public CategoryEntries predict(FeatureVector vector, BaggedDecisionTreeModel model) {
        DecisionTreeClassifier classifier = new DecisionTreeClassifier();
        CountMap<String> categories = CountMap.create();
        for (DecisionTreeModel decisionTreeModel : model.getModels()) {
            CategoryEntries entriesResult = classifier.predict(vector, decisionTreeModel);
            CategoryEntry categoryResult = entriesResult.get(0);
            String category = categoryResult.getCategory().getName();
            categories.add(category);
        }

        CategoryEntries result = new CategoryEntries();
        for (String categoryName : categories.uniqueItems()) {
            double confidence = (double)categories.get(categoryName) / categories.totalSize();;
            result.add(new CategoryEntry(result, new Category(categoryName), confidence));
        }
        return result;
    }

    /**
     * <p>
     * Get a bootstrap sampling drawn at random with replacement.
     * </p>
     * 
     * @param instances
     * @param random
     * @return
     */
    private List<NominalInstance> getBagging(List<NominalInstance> instances, Random random) {
        List<NominalInstance> result = CollectionHelper.newArrayList();
        for (int i = 0; i < instances.size(); i++) {
            int sample = random.nextInt(instances.size());
            result.add(instances.get(sample));
        }
        return result;
    }

}
