package ws.palladian.classification.dt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import quickdt.Attributes;
import quickdt.Instance;
import quickdt.Leaf;
import quickdt.Node;
import quickdt.TreeBuilder;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.Predictor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A Decision Tree classifier based on <a href="https://github.com/sanity/quickdt">quickdt</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DecisionTreeClassifier implements Predictor<DecisionTreeModel> {

    private final int maxDepth;

    private final double minProbability;

    /**
     * <p>
     * Create a new DecisionTreeClassifier with the specified maximum depth and the minimum probability.
     * </p>
     * 
     * @param maxDepth The maximum depth for the created decision tree.
     * @param minProbability The minimum probability in the created decision tree.
     */
    public DecisionTreeClassifier(int maxDepth, double minProbability) {
        this.maxDepth = maxDepth;
        this.minProbability = minProbability;
    }

    /**
     * <p>
     * Create a new DecisionTreeClassifier with unlimited ({@link Integer#MAX_VALUE}) size and minimum probability of 1.
     * </p>
     */
    public DecisionTreeClassifier() {
        this(Integer.MAX_VALUE, 1);
    }

    @Override
    public DecisionTreeModel learn(List<NominalInstance> instances) {
        Set<Instance> trainingInstances = CollectionHelper.newHashSet();
        for (NominalInstance instance : instances) {
            Serializable[] input = getInput(instance.featureVector);
            trainingInstances.add(Attributes.create(input).classification(instance.target));
        }
        Node tree = new TreeBuilder().buildTree(trainingInstances, maxDepth, minProbability);
        return new DecisionTreeModel(tree);
    }

    private Serializable[] getInput(FeatureVector featureVector) {
        List<Serializable> inputs = new ArrayList<Serializable>();
        for (Feature<?> feature : featureVector.toArray()) {
            String featureName = feature.getName();
            Serializable featureValue = (Serializable)feature.getValue();
            inputs.add(featureName);
            inputs.add(featureValue);
        }
        return inputs.toArray(new Serializable[inputs.size()]);
    }

    @Override
    public CategoryEntries predict(FeatureVector featureVector, DecisionTreeModel decisionTreeModel) {
        Leaf leaf = decisionTreeModel.getTree().getLeaf(Attributes.create(getInput(featureVector)));
        CategoryEntries categoryEntries = new CategoryEntries();
        Category category = new Category((String)leaf.classification);
        CategoryEntry categoryEntry = new CategoryEntry(categoryEntries, category, leaf.probability);
        categoryEntries.add(categoryEntry);
        return categoryEntries;
    }

}
