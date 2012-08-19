package ws.palladian.classification.dt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
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
import ws.palladian.classification.Instance2;
import ws.palladian.classification.Predictor;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A Decision Tree classifier based on <a href="https://github.com/sanity/quickdt">quickdt</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DecisionTreeClassifier implements Predictor<String> {

    private final int maxDepth;

    private final double minProbability;

    private final Set<Instance> trainingInstances;

    private Node tree;

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
        this.trainingInstances = new HashSet<Instance>();
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
    public void learn(List<Instance2<String>> instances) {
        for (Instance2<String> instance2 : instances) {
            addTrainingInstance(instance2);
        }
        build();
        
        // added to save memory
        trainingInstances.clear();
    }

    private void addTrainingInstance(Instance2<String> instance) {
        Serializable[] input = getInput(instance.featureVector);
        trainingInstances.add(Attributes.create(input).classification(instance.target));
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

    private void build() {
        tree = new TreeBuilder().buildTree(trainingInstances, maxDepth, minProbability);
    }

    @Override
    public CategoryEntries predict(FeatureVector featureVector) {
        Leaf leaf = tree.getLeaf(Attributes.create(getInput(featureVector)));
        CategoryEntries categoryEntries = new CategoryEntries();
        Category category = new Category((String)leaf.classification);
        CategoryEntry categoryEntry = new CategoryEntry(categoryEntries, category, leaf.probability);
        categoryEntries.add(categoryEntry);
        return categoryEntries;
    }

    @Override
    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        tree.dump(printStream);
        return out.toString();
    }

}
