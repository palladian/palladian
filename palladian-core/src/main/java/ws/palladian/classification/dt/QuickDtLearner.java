package ws.palladian.classification.dt;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import quickdt.HashMapAttributes;
import quickdt.PredictiveModel;
import quickdt.PredictiveModelBuilder;
import quickdt.TreeBuilder;
import quickdt.randomForest.RandomForestBuilder;
import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;

/**
 * <p>
 * A Decision Tree classifier based on <a href="https://github.com/sanity/quickdt">quickdt</a> by Ian Clarke. The
 * classifier supports plain decision trees, and Random Forests (use {@link #QuickDtClassifier(PredictiveModelBuilder)}
 * with {@link RandomForestBuilder}).
 * </p>
 * 
 * @author Philipp Katz
 */
public class QuickDtLearner implements Learner<QuickDtModel> {

    /** The builder used for creating the predictive mode. */
    private final PredictiveModelBuilder<? extends PredictiveModel> builder;

    // removed zero-arg constructor, because one might erroneously instantiate a single tree instead of a random forest
//    /**
//     * <p>
//     * Create a new DecisionTreeClassifier with unlimited ({@link Integer#MAX_VALUE}) size and minimum probability of 1.
//     * </p>
//     */
//    public QuickDtLearner() {
//        this(new TreeBuilder());
//    }

    /**
     * <p>
     * Create a new QuickDtClassifier with the specified {@link PredictiveModelBuilder}. (currently, quickdt offers a
     * standard {@link TreeBuilder}, and a {@link RandomForestBuilder}).
     * </p>
     * 
     * @param builder The builder to use, not <code>null</code>.
     */
    public QuickDtLearner(PredictiveModelBuilder<? extends PredictiveModel> builder) {
        Validate.notNull(builder, "builder must not be null");
        this.builder = builder;
    }

    @Override
    public QuickDtModel train(Iterable<? extends Trainable> trainables) {
        Set<quickdt.Instance> trainingInstances = CollectionHelper.newHashSet();
        Set<String> classes = CollectionHelper.newHashSet();
        for (Trainable instance : trainables) {
            Serializable[] input = getInput(instance);
            trainingInstances.add(HashMapAttributes.create(input).classification(instance.getTargetClass()));
            classes.add(instance.getTargetClass());
        }
        PredictiveModel tree = builder.buildPredictiveModel(trainingInstances);
        return new QuickDtModel(tree, classes);
    }

    static Serializable[] getInput(Classifiable classifiable) {
        List<Serializable> inputs = CollectionHelper.newArrayList();
        for (Feature<?> feature : classifiable.getFeatureVector()) {
            inputs.add(feature.getName());
            inputs.add((Serializable)feature.getValue());
        }
        return inputs.toArray(new Serializable[inputs.size()]);
    }

}
