package ws.palladian.classification.dt;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Predictor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * Simple Bagging for {@link DecisionTreeClassifier} with bootstrap sampling and majority voting. Could be extended to
 * allow for the usage of arbitrary {@link Predictor} implementations.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BaggedDecisionTreeClassifier implements Classifier<BaggedDecisionTreeModel> {

    /** The default number of classifiers to create, in case it is not specified explicitly. */
    public static final int DEFAULT_NUM_CLASSIFIERS = 10;

    public BaggedDecisionTreeModel train(Iterable<? extends Trainable> trainables, int numClassifiers) {
        Validate.isTrue(numClassifiers > 0, "numClassifiers must be greater than zero.");
        Random random = new Random();
        List<? extends Trainable> trainableList = CollectionHelper.newArrayList(trainables);
        List<DecisionTreeModel> decisionTreeModels = CollectionHelper.newArrayList();
        for (int i = 0; i < numClassifiers; i++) {
            List<Trainable> sampling = getBagging(trainableList, random);
            DecisionTreeClassifier newClassifier = new DecisionTreeClassifier();
            DecisionTreeModel model = newClassifier.train(sampling);
            decisionTreeModels.add(model);
        }
        return new BaggedDecisionTreeModel(decisionTreeModels);
    }

    @Override
    public BaggedDecisionTreeModel train(Iterable<? extends Trainable> trainables) {
        return train(trainables, DEFAULT_NUM_CLASSIFIERS);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, BaggedDecisionTreeModel model) {
        DecisionTreeClassifier classifier = new DecisionTreeClassifier();
        CountMap<String> categories = CountMap.create();
        for (DecisionTreeModel decisionTreeModel : model.getModels()) {
            CategoryEntries entriesResult = classifier.classify(classifiable, decisionTreeModel);
            CategoryEntry categoryResult = entriesResult.getMostLikelyCategoryEntry();
            categories.add(categoryResult.getName());
        }

        CategoryEntries result = new CategoryEntries();
        for (String categoryName : categories.uniqueItems()) {
            double confidence = (double)categories.getCount(categoryName) / categories.totalSize();
            result.add(new CategoryEntry(categoryName, confidence));
        }
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
