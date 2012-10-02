package ws.palladian.classification.dt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance2;
import ws.palladian.classification.Predictor;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Simple Bagging for {@link DecisionTreeClassifier} with bootstrap sampling and majority voting. Could be extended to
 * allow for the usage of arbitrary {@link Predictor} implementations.
 * </p>
 * 
 * @author Philipp Katz
 */
public class BaggedDecisionTreeClassifier implements Predictor<String> {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaggedDecisionTreeClassifier.class);

    private static final long serialVersionUID = 1L;
    
    private final List<Predictor<String>> predictors;
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
        this.predictors = new ArrayList<Predictor<String>>();
        this.numClassifiers = numClassifiers;
        this.random = new Random();
    }

    @Override
    public void learn(List<Instance2<String>> instances) {
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < numClassifiers; i++) {
            List<Instance2<String>> sampling = getBagging(instances);
            DecisionTreeClassifier newClassifier = new DecisionTreeClassifier();
            newClassifier.learn(sampling);
            predictors.add(newClassifier);
            ProgressHelper.showProgress(i, numClassifiers, 0, LOGGER, stopWatch);
        }
    }

    @Override
    public CategoryEntries predict(FeatureVector vector) {
        Bag<String> categories = new HashBag<String>();
        for (Predictor<String> predictor : predictors) {
            CategoryEntries entriesResult = predictor.predict(vector);
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
    private List<Instance2<String>> getBagging(List<Instance2<String>> instances) {
        List<Instance2<String>> result = new ArrayList<Instance2<String>>();
        for (int i = 0; i < instances.size(); i++) {
            int sample = random.nextInt(instances.size());
            result.add(instances.get(sample));
        }
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder buildToString = new StringBuilder();
        buildToString.append("BaggedDecisionTreeClassifier").append('\n');
        buildToString.append("# classifiers: ").append(predictors.size()).append('\n'); 
        for (int i = 0; i < predictors.size(); i++) {
            buildToString.append("classifier ").append(i).append(":").append('\n');
            buildToString.append(predictors.get(i));
            buildToString.append('\n');
        }
        return buildToString.toString();
    }

}
