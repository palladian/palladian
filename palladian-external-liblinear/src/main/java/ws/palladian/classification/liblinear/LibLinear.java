package ws.palladian.classification.liblinear;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.NumericFeature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

/**
 * <p>
 * LIBLINEAR, A Library for Large Linear Classification. Wrapper for <a
 * href="http://liblinear.bwaldvogel.de">liblinear-java</a>. For a documentation about liblinear see <a
 * href="http://www.csie.ntu.edu.tw/~cjlin/liblinear/">here</a> and <a
 * href="http://www.csie.ntu.edu.tw/~cjlin/papers/liblinear.pdf">here</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public class LibLinear implements Learner<LibLinearModel>, Classifier<LibLinearModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LibLinear.class);

    /** The training parameters. */
    private final Parameter parameter;

    /** Bias parameter, will be set in case value is greater zero. */
    private final double bias;

    /**
     * <p>
     * Create a new {@link LibLinear} with the specified {@link Parameter} for training.
     * </p>
     * 
     * @param parameter The parameter, not <code>null</code>.
     * @param bias The value for the bias term, <code>0</code> to add no bias term.
     */
    public LibLinear(Parameter parameter, double bias) {
        Validate.notNull(parameter, "parameter must not be null");
        this.parameter = parameter;
        this.bias = bias;
    }

    public LibLinear() {
        this(new Parameter(SolverType.L2R_LR, //
                1.0, // cost of constraints violation
                0.01), // stopping criteria
                1); // bias term
    }

    @Override
    public LibLinearModel train(Iterable<? extends Trainable> trainables) {
        Validate.notNull(trainables, "trainables must not be null");
        Problem problem = new Problem();
        Set<String> featureLabels = CollectionHelper.newTreeSet();
        List<String> classIndices = CollectionHelper.newArrayList();
        for (Trainable trainable : trainables) {
            problem.l++;
            for (Feature<?> feature : trainable.getFeatureVector()) {
                if (feature instanceof NumericFeature) {
                    featureLabels.add(feature.getName());
                }
            }
            if (!classIndices.contains(trainable.getTargetClass())) {
                classIndices.add(trainable.getTargetClass());
            }
        }
        LOGGER.debug("Features = {}", featureLabels);
        problem.n = featureLabels.size();
        problem.x = new de.bwaldvogel.liblinear.Feature[problem.l][];
        problem.y = new double[problem.l];
        if (bias > 0) {
            LOGGER.debug("Add bias correction {}", bias);
            problem.bias = bias; // bias feature
            problem.n++; // add one for bias term
        }
        int index = 0;
        for (Trainable trainable : trainables) {
            problem.x[index] = makeInstance(featureLabels, trainable, bias);
            problem.y[index] = classIndices.indexOf(trainable.getTargetClass());
            index++;
        }
        LOGGER.debug("n={}, l={}", problem.n, problem.l);
        Model model = Linear.train(problem, parameter);
        return new LibLinearModel(model, featureLabels, classIndices);
    }

    private static de.bwaldvogel.liblinear.Feature[] makeInstance(Set<String> labels, Classifiable trainable,
            double bias) {
        List<de.bwaldvogel.liblinear.Feature> features = CollectionHelper.newArrayList();
        int index = 0; // 1-indexed
        for (String label : labels) {
            index++;
            NumericFeature feature = trainable.getFeatureVector().get(NumericFeature.class, label);
            if (feature == null) {
                LOGGER.trace("Feature {}@{} not present", label, index);
                continue;
            }
            Double value = feature.getValue();
            if (value == 0) {
                continue;
            }
            features.add(new FeatureNode(index, value));
        }
        if (bias > 0) {
            features.add(new FeatureNode(index + 1, bias)); // bias term
        }
        return features.toArray(new de.bwaldvogel.liblinear.Feature[0]);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, LibLinearModel model) {
        Validate.notNull(classifiable, "classifiable must not be null");
        Validate.notNull(model, "model must not be null");
        de.bwaldvogel.liblinear.Feature[] instance = makeInstance(model.getFeatureLabels(), classifiable, model
                .getLLModel().getBias());
        double[] probabilities = new double[model.getCategories().size()];
        Linear.predictProbability(model.getLLModel(), instance, probabilities);
        CategoryEntriesMap categoryEntries = new CategoryEntriesMap();
        for (int i = 0; i < probabilities.length; i++) {
            categoryEntries.add(model.getCategoryForIndex(i), probabilities[i]);
        }
        return categoryEntries;
    }

}
