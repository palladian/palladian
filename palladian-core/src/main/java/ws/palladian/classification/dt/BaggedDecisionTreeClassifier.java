package ws.palladian.classification.dt;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * Simple Bagging for {@link DecisionTreeClassifier} with bootstrap sampling and majority voting. Could be extended to
 * allow for the usage of arbitrary {@link Classifier} implementations.
 * </p>
 * 
 * @author Philipp Katz
 * @deprecated Use {@link QuickDtClassifier} instead.
 */
@Deprecated
public final class BaggedDecisionTreeClassifier implements Learner<BaggedDecisionTreeModel>,
        Classifier<BaggedDecisionTreeModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaggedDecisionTreeClassifier.class);

    /** The default number of classifiers to create, in case it is not specified explicitly. */
    public static final int DEFAULT_NUM_CLASSIFIERS = 10;

    private final int numClassifiers;

    /**
     * <p>
     * Create a new {@link BaggedDecisionTreeClassifier} using the specified number of classifiers.
     * </p>
     * 
     * @param numClassifiers The number of decision trees to grow, greater zero.
     */
    public BaggedDecisionTreeClassifier(int numClassifiers) {
        Validate.isTrue(numClassifiers > 0, "numClassifiers must be greater than zero.");
        this.numClassifiers = numClassifiers;
    }

    /**
     * <p>
     * Create a new {@link BaggedDecisionTreeClassifier} using {@value #DEFAULT_NUM_CLASSIFIERS} classifiers.
     * </p>
     */
    public BaggedDecisionTreeClassifier() {
        this(DEFAULT_NUM_CLASSIFIERS);
    }

    @Override
    public BaggedDecisionTreeModel train(Iterable<? extends Trainable> trainables) {
        Random random = new Random();
        List<? extends Trainable> trainableList = CollectionHelper.newArrayList(trainables);
        List<DecisionTreeModel> decisionTreeModels = CollectionHelper.newArrayList();
        ProgressMonitor monitor = new ProgressMonitor(numClassifiers, 1);
        DecisionTreeClassifier newClassifier = new DecisionTreeClassifier();
        for (int i = 0; i < numClassifiers; i++) {
            List<Trainable> sampling = getBagging(trainableList, random);
            DecisionTreeModel model = newClassifier.train(sampling);
            decisionTreeModels.add(model);
            String progress = monitor.incrementAndGetProgress();
            if (progress.length() > 0) {
                LOGGER.debug(progress);
            }
        }
        return new BaggedDecisionTreeModel(decisionTreeModels);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, BaggedDecisionTreeModel model) {
        DecisionTreeClassifier classifier = new DecisionTreeClassifier();
        CountMap<String> categories = CountMap.create();
        for (DecisionTreeModel decisionTreeModel : model.getModels()) {
            CategoryEntries entriesResult = classifier.classify(classifiable, decisionTreeModel);
            categories.add(entriesResult.getMostLikelyCategory());
        }

        CategoryEntriesMap result = new CategoryEntriesMap();
        for (String categoryName : categories.uniqueItems()) {
            double confidence = (double)categories.getCount(categoryName) / categories.totalSize();
            result.set(categoryName, confidence);
        }
        result.sort();
        return result;
    }

    /**
     * <p>
     * Get a bootstrap sampling drawn at random with replacement.
     * </p>
     * 
     * @param trainables
     * @param random
     * @return
     */
    private List<Trainable> getBagging(List<? extends Trainable> trainables, Random random) {
        List<Trainable> result = CollectionHelper.newArrayList();
        for (int i = 0; i < trainables.size(); i++) {
            int sample = random.nextInt(trainables.size());
            result.add(trainables.get(sample));
        }
        return result;
    }

}
