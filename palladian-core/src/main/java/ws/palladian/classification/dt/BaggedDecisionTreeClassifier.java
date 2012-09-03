package ws.palladian.classification.dt;

import java.util.List;
import java.util.Random;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.Predictor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Simple Bagging for {@link DecisionTreeClassifier} with bootstrap sampling and majority voting. Could be extended to
 * allow for the usage of arbitrary {@link Predictor} implementations.
 * </p>
 * 
 * @author Philipp Katz
 */
public class BaggedDecisionTreeClassifier implements Predictor<BaggedDecisionTreeModel> {

    private static final long serialVersionUID = 1L;
    
    // private final List<Predictor<String>> predictors;
    private transient final int numClassifiers;
    private transient final Random random;

    /**
     * <p>
     * Create a new {@link BaggedDecisionTreeClassifier} with the specified number of decision trees.
     * </p>
     * 
     * @param numClassifiers The number of decision trees to use, must be greater than zero.
     */
    public BaggedDecisionTreeClassifier(int numClassifiers) {
        Validate.isTrue(numClassifiers > 0, "numClassifiers must be greater than zero.");
        // this.predictors = new ArrayList<Predictor<String>>();
        this.numClassifiers = numClassifiers;
        this.random = new Random();
    }

    @Override
    public BaggedDecisionTreeModel learn(List<NominalInstance> instances) {
        List<DecisionTreeModel> classifiers = CollectionHelper.newArrayList();
        BaggedDecisionTreeModel model = new BaggedDecisionTreeModel(classifiers);
        
        for (int i = 0; i < numClassifiers; i++) {
            List<NominalInstance> sampling = getBagging(instances);
            DecisionTreeClassifier newClassifier = new DecisionTreeClassifier();
            DecisionTreeModel model2 = newClassifier.learn(sampling);
            classifiers.add(model2);
        }
        
        return model;
        
    }

    @Override
    public CategoryEntries predict(FeatureVector vector, BaggedDecisionTreeModel model) {
        DecisionTreeClassifier classifier = new DecisionTreeClassifier();
        Bag<String> categories = new HashBag<String>();
        for (DecisionTreeModel predictor : model.getClassifiers()) {
            CategoryEntries entriesResult = classifier.predict(vector, predictor);
            CategoryEntry categoryResult = entriesResult.get(0);
            String category = categoryResult.getCategory().getName();
            categories.add(category);
        }

        CategoryEntries result = new CategoryEntries();
        for (String categoryName : categories.uniqueSet()) {
            double confidence = (double)categories.getCount(categoryName) / categories.size();
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
     * @return
     */
    private List<NominalInstance> getBagging(List<NominalInstance> instances) {
        List<NominalInstance> result = CollectionHelper.newArrayList();
        for (int i = 0; i < instances.size(); i++) {
            int sample = random.nextInt(instances.size());
            result.add(instances.get(sample));
        }
        return result;
    }
    
//    @Override
//    public String toString() {
//        StringBuilder buildToString = new StringBuilder();
//        buildToString.append("BaggedDecisionTreeClassifier").append('\n');
//        buildToString.append("# classifiers: ").append(predictors.size()).append('\n'); 
//        for (int i = 0; i < predictors.size(); i++) {
//            buildToString.append("classifier ").append(i).append(":").append('\n');
//            buildToString.append(predictors.get(i));
//            buildToString.append('\n');
//        }
//        return buildToString.toString();
//    }

}
