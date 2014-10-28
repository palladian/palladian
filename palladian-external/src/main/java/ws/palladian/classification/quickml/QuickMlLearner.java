package ws.palladian.classification.quickml;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickml.data.AttributesMap;
import quickml.data.InstanceImpl;
import quickml.supervised.PredictiveModelBuilder;
import quickml.supervised.classifier.Classifier;
import quickml.supervised.classifier.decisionTree.TreeBuilder;
import quickml.supervised.classifier.randomForest.RandomForestBuilder;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * A classifier based on <a href="http://quickml.org">QuickML</a> by Ian Clarke.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class QuickMlLearner implements Learner<QuickMlModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickMlLearner.class);

    /** The builder used for creating the predictive mode. */
    private final PredictiveModelBuilder<AttributesMap, ? extends Classifier> builder;

    /**
     * @return A new QuickMlLearner creating a random forest with ten trees.
     */
    public static QuickMlLearner randomForest() {
        return randomForest(10);
    }

    /**
     * @param numTrees The number of trees to grow, greater zero.
     * @return A new QuickMlLearner creating a random forest with the specified number of trees.
     */
    public static QuickMlLearner randomForest(int numTrees) {
        Validate.isTrue(numTrees > 0, "numTrees must be greater zero");
        // "random subspace" method; 0.7 denotes the probability, that an attribute at a node will be ignored
        // see: Tin K. Ho; The random subspace method for constructing decision forests; 1998
        // and mail Ian, 2013-12-29 -- Philipp
        TreeBuilder treeBuilder = new TreeBuilder().ignoreAttributeAtNodeProbability(0.7);
        return new QuickMlLearner(new RandomForestBuilder(treeBuilder).numTrees(numTrees));
    }

    /**
     * @return A new QuickMlLearner creating a single tree.
     */
    public static QuickMlLearner tree() {
        return new QuickMlLearner(new TreeBuilder());
    }

    /**
     * <p>
     * Create a new QuickMlLearner with the specified {@link PredictiveModelBuilder}.
     * </p>
     * 
     * @param builder The builder to use, not <code>null</code>.
     * @see {@link #tree()} and {@link #randomForest()} for a predefined learner.
     * @deprecated Use {@link #tree()} or {@link #randomForest()} to create instances.
     */
    @Deprecated
    public QuickMlLearner(PredictiveModelBuilder<AttributesMap, ? extends Classifier> builder) {
        Validate.notNull(builder, "builder must not be null");
        this.builder = builder;
    }

    @Override
    public QuickMlModel train(Iterable<? extends Instance> instances) {
        Validate.notNull(instances, "instances must not be null");
        Set<quickml.data.Instance<AttributesMap>> trainingInstances = CollectionHelper.newHashSet();
        Set<String> classes = CollectionHelper.newHashSet();
        for (ws.palladian.core.Instance instance : instances) {
            AttributesMap input = getInput(instance.getVector());
            trainingInstances.add(new InstanceImpl<AttributesMap>(input, instance.getCategory()));
            classes.add(instance.getCategory());
        }
        Classifier classifier = builder.buildPredictiveModel(trainingInstances);
        return new QuickMlModel(classifier, classes);
    }

    static AttributesMap getInput(FeatureVector featureVector) {
        Map<String, Serializable> inputs = CollectionHelper.newHashMap();
        for (VectorEntry<String, Value> feature : featureVector) {
            Value value = feature.value();
            if (value instanceof NominalValue) {
                inputs.put(feature.key(), ((NominalValue)value).getString());
            } else if (value instanceof NumericValue) {
                inputs.put(feature.key(), ((NumericValue)value).getDouble());
            } else {
                LOGGER.trace("Unsupported type for {}: {}", feature.key(), value.getClass().getName());
            }
        }
        return new AttributesMap(inputs);
    }

    @Override
    public String toString() {
        return "QuickMlLearner (" + builder + ")";
    }

}
