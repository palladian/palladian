package ws.palladian.classification.dt;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import quickdt.HashMapAttributes;
import quickdt.Instance;
import quickdt.PredictiveModel;
import quickdt.PredictiveModelBuilder;
import quickdt.TreeBuilder;
import quickdt.randomForest.RandomForestBuilder;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Learner;
import ws.palladian.core.NominalValue;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * A Decision Tree classifier based on <a href="https://github.com/sanity/quickdt">quickdt</a> by Ian Clarke. The
 * classifier supports plain decision trees, and Random Forests (use {@link #QuickDtClassifier(PredictiveModelBuilder)}
 * with {@link RandomForestBuilder}).
 * </p>
 * 
 * @author Philipp Katz
 */
public final class QuickDtLearner implements Learner<QuickDtModel> {

    /** The builder used for creating the predictive mode. */
    private final PredictiveModelBuilder<? extends PredictiveModel> builder;

    /**
     * @return A new QuickDtLearner creating a random forest with ten trees.
     */
    public static QuickDtLearner randomForest() {
        return randomForest(10);
    }

    /**
     * @param numTrees The number of trees to grow, greater zero.
     * @return A new QuickDtLearner creating a random forest with the specified number of trees.
     */
    public static QuickDtLearner randomForest(int numTrees) {
        Validate.isTrue(numTrees > 0, "numTrees must be greater zero");
        // "random subspace" method; 0.7 denotes the probability, that an attribute at a node will be ignored
        // see: Tin K. Ho; The random subspace method for constructing decision forests; 1998
        // and mail Ian, 2013-12-29 -- Philipp
        TreeBuilder treeBuilder = new TreeBuilder().ignoreAttributeAtNodeProbability(0.7);
        return new QuickDtLearner(new RandomForestBuilder(treeBuilder).numTrees(numTrees));
    }

    /**
     * @return A new QuickDtLearner creating a single tree.
     */
    public static QuickDtLearner tree() {
        return new QuickDtLearner(new TreeBuilder());
    }

    /**
     * <p>
     * Create a new QuickDtLearner with the specified {@link PredictiveModelBuilder}. (currently, quickdt offers a
     * standard {@link TreeBuilder}, and a {@link RandomForestBuilder}).
     * </p>
     * 
     * @param builder The builder to use, not <code>null</code>.
     * @see {@link #tree()} and {@link #randomForest()} for a predefined learner.
     * @deprecated Use {@link #tree()} or {@link #randomForest()} to create instances.
     */
    @Deprecated
    public QuickDtLearner(PredictiveModelBuilder<? extends PredictiveModel> builder) {
        Validate.notNull(builder, "builder must not be null");
        this.builder = builder;
    }

    @Override
    public QuickDtModel train(Iterable<? extends ws.palladian.core.Instance> instances) {
        Set<Instance> trainingInstances = CollectionHelper.newHashSet();
        Set<String> classes = CollectionHelper.newHashSet();
        for (ws.palladian.core.Instance instance : instances) {
            Serializable[] input = getInput(instance.getVector());
            trainingInstances.add(HashMapAttributes.create(input).classification(instance.getCategory()));
            classes.add(instance.getCategory());
        }
        PredictiveModel tree = builder.buildPredictiveModel(trainingInstances);
        return new QuickDtModel(tree, classes);
    }

    static Serializable[] getInput(FeatureVector featureVector) {
        List<Serializable> inputs = CollectionHelper.newArrayList();
        for (VectorEntry<String, Value> feature : featureVector) {
            inputs.add(feature.key());
            Value value = feature.value();
            if (value instanceof NominalValue) {
                inputs.add(((NominalValue)value).getString());
            } else if (value instanceof NumericValue) {
                inputs.add(((NumericValue)value).getDouble());
            }
        }
        return inputs.toArray(new Serializable[inputs.size()]);
    }

    @Override
    public String toString() {
        return "QuickDtLearner (" + builder + ")";
    }

}
