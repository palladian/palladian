package ws.palladian.classification.quickml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import quickml.data.AttributesMap;
import quickml.data.instances.ClassifierInstance;
import quickml.supervised.PredictiveModelBuilder;
import quickml.supervised.classifier.Classifier;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForestBuilder;
import quickml.supervised.tree.decisionTree.DecisionTreeBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;

/**
 * <p>
 * A classifier based on <a href="http://quickml.org">QuickML</a> by Ian Clarke.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class QuickMlLearner implements Learner<QuickMlModel> {

    /** The builder used for creating the predictive mode. */
    private final PredictiveModelBuilder<? extends Classifier, ClassifierInstance> builder;

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
        DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
        RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(numTrees);
        return new QuickMlLearner(randomForestBuilder);
    }

    /**
     * @return A new QuickMlLearner creating a single tree.
     */
    public static QuickMlLearner tree() {
        return new QuickMlLearner(new DecisionTreeBuilder<>());
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
    public QuickMlLearner(PredictiveModelBuilder<? extends Classifier, ClassifierInstance> builder) {
        Validate.notNull(builder, "builder must not be null");
        this.builder = builder;
    }

    @Override
    public QuickMlModel train(Iterable<? extends Instance> instances) {
        Validate.notNull(instances, "instances must not be null");
		Set<String> featureNames = ClassificationUtils
				.getFeatureNames(ClassificationUtils.unwrapInstances(instances));
        FlyweightAttributesMap.Builder mapBuilder = new FlyweightAttributesMap.Builder(featureNames);
        List<ClassifierInstance> trainingInstances = new ArrayList<>();
        Set<String> classes = new HashSet<>();
        for (Instance instance : instances) {
            AttributesMap input = mapBuilder.create(instance.getVector());
            trainingInstances.add(new ClassifierInstance(input, instance.getCategory()));
            classes.add(instance.getCategory());
        }
        Classifier classifier = builder.buildPredictiveModel(trainingInstances);
        return new QuickMlModel(classifier, classes);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + builder.getClass().getSimpleName() + ")";
    }

}
