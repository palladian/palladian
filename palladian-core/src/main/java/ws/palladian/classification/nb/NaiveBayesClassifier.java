package ws.palladian.classification.nb;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;

/**
 * <p>
 * A simple implementation of the Naive Bayes Classifier. This classifier supports nominal and numeric input. The output
 * (prediction) is nominal. More information about Naive Bayes can be found <a
 * href="http://www.pierlucalanzi.net/wp-content/teaching/dmtm/DMTM0809-13-ClassificationIBLNaiveBayes.pdf">here</a>.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NaiveBayesClassifier implements Classifier<NaiveBayesModel> {

    /** The default value for the Laplace smoothing. */
    public static final double DEFAULT_LAPLACE_CORRECTOR = 0.00001;

    /** The corrector for the Laplace smoothing. */
    private final double laplace;

    /** Flag to denote whether to score in log. space. */
    private final boolean logSpace;

    /**
     * <p>
     * Create a new Naive Bayes classifier with a value of {@value #DEFAULT_LAPLACE_CORRECTOR} for the Laplace
     * smoothing.
     * </p>
     */
    public NaiveBayesClassifier() {
        this(DEFAULT_LAPLACE_CORRECTOR);
    }

    /**
     * <p>
     * Create a new Naive Bayes classifier.
     * </p>
     *
     * @param laplaceCorrector The Laplace corrector for smoothing. Must be greater or equal to zero. A value of zero
     *                         means no smoothing.
     */
    public NaiveBayesClassifier(double laplaceCorrector) {
        this(DEFAULT_LAPLACE_CORRECTOR, true);
    }

    /**
     * Create a new Naive Bayes classifier.
     *
     * @param laplaceCorrector The Laplace corrector for smoothing. Must be greater or equal
     *                         to zero. A value of zero means no smoothing.
     * @param logSpace         Set to <code>false</code> in order to perform scoring
     *                         calculations as addition; this will give better distributed
     *                         probability estimates. If <code>true</code>, the calculation
     *                         will be done in log space in order to avoid underflows.
     */
    public NaiveBayesClassifier(double laplaceCorrector, boolean logSpace) {
        Validate.isTrue(laplaceCorrector >= 0, "The Laplace corrector must be equal or greater than zero.");
        this.laplace = laplaceCorrector;
        this.logSpace = logSpace;
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, NaiveBayesModel model) {

        CategoryEntriesBuilder categoryEntriesBuilder = new CategoryEntriesBuilder();

        for (String category : model.getCategories()) {

            // initially set all category probabilities to their priors
            double probability = model.getPrior(category);
            if (logSpace) {
                probability = FastMath.log(probability);
            }

            for (String featureName : model.getLearnedFeatures()) {
                Value value = featureVector.get(featureName);
                if (value instanceof NominalValue) {
                    String nominalValue = ((NominalValue) value).getString();
                    double currentProbability = model.getProbability(featureName, nominalValue, category, laplace);
                    if (logSpace) {
                        probability += FastMath.log(currentProbability);
                    } else {
                        probability *= currentProbability;
                    }
                } else if (value instanceof NumericValue) {
                    double doubleValue = ((NumericValue) value).getDouble();
                    double density = model.getDensity(featureName, doubleValue, category);
                    if (density > 0) {
                        if (logSpace) {
                            probability += FastMath.log(density);
                        } else {
                            probability *= density;
                        }
                    }
                }
            }
            categoryEntriesBuilder.set(category, probability);
        }

        return categoryEntriesBuilder.create();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (laplace=" + laplace + ")";
    }

}
