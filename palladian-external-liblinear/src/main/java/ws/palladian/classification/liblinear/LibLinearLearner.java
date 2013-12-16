package ws.palladian.classification.liblinear;

import java.io.PrintStream;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Learner;
import ws.palladian.classification.utils.DummyVariableCreator;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.classification.utils.Normalizer;
import ws.palladian.classification.utils.ZScoreNormalizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.Slf4JOutputStream;
import ws.palladian.helper.io.Slf4JOutputStream.Level;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
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
 * href="http://www.csie.ntu.edu.tw/~cjlin/papers/liblinear.pdf">here</a>. In addition, to the pure LIBLINEAR
 * classifier, this wrapper adds the following functionality: a) Numerical data can be normalized using a
 * {@link Normalizer}, b) nominal data is transformed to a numerical representation, using dummy coding (see
 * {@link DummyVariableCreator} for more information).
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LibLinearLearner implements Learner<LibLinearModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LibLinearLearner.class);

    /** The training parameters. */
    private final Parameter parameter;

    /** Bias parameter, will be set in case value is greater zero. */
    private final double bias;

    /** The normalizer for the numeric features. */
    private final Normalizer normalizer;

    static {
        // redirect debug output to logger.
        Linear.setDebugOutput(new PrintStream(new Slf4JOutputStream(LOGGER, Level.DEBUG)));
    }

    /**
     * <p>
     * Create a new {@link LibLinearLearner} with the specified {@link Parameter} for training.
     * </p>
     * 
     * @param parameter The parameter, not <code>null</code>.
     * @param bias The value for the bias term, <code>0</code> to add no bias term.
     * @param normalizer The normalizer to use, not <code>null</code>. Use a {@link NoNormalizer} to skip
     *            normalization.
     */
    public LibLinearLearner(Parameter parameter, double bias, Normalizer normalizer) {
        Validate.notNull(parameter, "parameter must not be null");
        Validate.notNull(normalizer, "normalizer must not be null");
        this.parameter = parameter;
        this.bias = bias;
        this.normalizer = normalizer;
    }

    /**
     * <p>
     * Create a new {@link LibLinearLearner} with 'L2-regularized logistic regression', a cost value of 1.0 for
     * constraints violation, a value of 0.01 as stopping criterion, a bias term of one, and Z-Score normalization for
     * features.
     * </p>
     */
    public LibLinearLearner() {
        this(new Parameter(SolverType.L2R_LR, //
                1.0, // cost of constraints violation
                0.01), // stopping criteria
                1, // bias term
                new ZScoreNormalizer()); // normalizer
    }

    @Override
    public LibLinearModel train(Iterable<? extends Trainable> trainables) {
        Validate.notNull(trainables, "trainables must not be null");
        Normalization normalization = normalizer.calculate(trainables);
        DummyVariableCreator dummyCoder = new DummyVariableCreator(trainables);
        Problem problem = new Problem();
        List<String> featureLabels = CollectionHelper.newArrayList();
        List<String> classIndices = CollectionHelper.newArrayList();
        for (Trainable trainable : trainables) {
            problem.l++;
            FeatureVector featureVector = dummyCoder.convert(trainable).getFeatureVector();
            for (Feature<?> feature : featureVector) {
                if (feature instanceof NumericFeature) {
                    if (!featureLabels.contains(feature.getName())) {
                        featureLabels.add(feature.getName());
                    }
                }
            }
            if (!classIndices.contains(trainable.getTargetClass())) {
                classIndices.add(trainable.getTargetClass());
            }
        }
        LOGGER.debug("Features = {}", featureLabels);
        LOGGER.debug("Classes = {}", classIndices);
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
            normalization.normalize(trainable);
            Classifiable converted = dummyCoder.convert(trainable);
            problem.x[index] = makeInstance(featureLabels, converted, bias);
            problem.y[index] = classIndices.indexOf(trainable.getTargetClass());
            index++;
        }
        LOGGER.debug("n={}, l={}", problem.n, problem.l);
        Model model = Linear.train(problem, parameter);
        return new LibLinearModel(model, featureLabels, classIndices, normalization, dummyCoder);
    }

    static de.bwaldvogel.liblinear.Feature[] makeInstance(List<String> labels, Classifiable trainable, double bias) {
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

}
