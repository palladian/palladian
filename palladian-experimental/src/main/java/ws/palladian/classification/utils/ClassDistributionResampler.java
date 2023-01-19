package ws.palladian.classification.utils;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.zeror.ZeroRLearner;
import ws.palladian.classification.zeror.ZeroRModel;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.functional.Factories;

import java.util.*;

/**
 * <p>
 * Re-sample the class distribution of a given data set, so that the class counts a roughly equal. One can also specify
 * a desired class distribution, using the {@link #ClassDistributionResampler(Iterable, Map)} constructor.
 * </p>
 *
 * @author Philipp Katz
 */
public class ClassDistributionResampler implements Iterable<Instance> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassDistributionResampler.class);

    /** The random number generator. */
    private static final Random RANDOM = new Random();

    /** The re-sampled data set. */
    private Collection<Instance> sampled;

    /** The class probability distribution in the data set. */
    private final Map<String, Double> probabilities;

    /** Manually assigned weights for the classes. */
    private final Map<String, Double> weights;

    /**
     * Create a new {@link ClassDistributionResampler} for the given data set.
     *
     * @param data The data set, not <code>null</code>.
     */
    public ClassDistributionResampler(Iterable<Instance> data) {
        this(data, Collections.<String, Double>emptyMap());
    }

    /**
     * Create a new {@link ClassDistributionResampler} for the given data set, and allow to weight different classes
     * (e.g. take twice as much samples from class 'A').
     *
     * @param data    The data set, not <code>null</code>.
     * @param weights A map with weights for the classes, empty map to do no re-weighting (i.e. make class counts
     *                roughly equal). Values denote, how much the class occurrence is multiplied.
     */
    public ClassDistributionResampler(Iterable<Instance> data, Map<String, Double> weights) {
        Validate.notNull(data, "data must not be null");
        Validate.notNull(weights, "weights must not be null");
        ZeroRModel classDistribution = new ZeroRLearner().train(data);
        this.probabilities = classDistribution.getCategoryProbabilities();
        this.weights = new LazyMap<>(new HashMap<>(weights), Factories.constant(1.));
        LOGGER.info("Class probabilities : {}", probabilities);
        sampled = reSample(data);
    }

    private Collection<Instance> reSample(Iterable<Instance> data) {
        double minProbability = Double.MAX_VALUE;
        for (Double value : probabilities.values()) {
            minProbability = Math.min(minProbability, value);
        }
        List<Instance> result = new ArrayList<>();
        Bag<String> temp = new Bag<>();
        for (Instance trainable : data) {
            String targetClass = trainable.getCategory();
            // XXX use reservoir sampling to obtain exactly same amounts?
            // use MathHelper#sample
            double probability = minProbability / probabilities.get(targetClass) * weights.get(targetClass);
            if (RANDOM.nextDouble() <= probability) {
                result.add(trainable);
                temp.add(targetClass);
            }
        }
        LOGGER.info("Re-weighted class counts: {}", temp);
        return result;
    }

    @Override
    public Iterator<Instance> iterator() {
        return sampled.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClassDistributionEqualizer [#items=");
        builder.append(sampled.size());
        builder.append(", probabilities=");
        builder.append(probabilities);
        builder.append(", weights=");
        builder.append(weights);
        builder.append("]");
        return builder.toString();
    }

}
