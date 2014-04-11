package ws.palladian.classification.nb;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.NominalValue;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

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
    private static final double DEFAULT_LAPLACE_CORRECTOR = 0.00001;

    /** The corrector for the Laplace smoothing. */
    private final double laplace;

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
     *            means no smoothing.
     */
    public NaiveBayesClassifier(double laplaceCorrector) {
        Validate.isTrue(laplaceCorrector >= 0, "The Laplace corrector must be equal or greater than zero.");
        this.laplace = laplaceCorrector;
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, NaiveBayesModel model) {

        CategoryEntriesBuilder categoryEntriesBuilder = new CategoryEntriesBuilder();

        for (String category : model.getCategories()) {

            if (category.isEmpty()) {
                continue;
            }

            // initially set all category probabilities to their priors
            double probability = model.getPrior(category);

            for (VectorEntry<String, Value> feature : featureVector) {
                String featureName = feature.key();
                Value value = feature.value();
                if (value instanceof NominalValue) {
                    String nominalValue = ((NominalValue)value).getString();
                    probability *= model.getProbability(featureName, nominalValue, category, laplace);
                } else if (value instanceof NumericValue) {
                    double doubleValue = ((NumericValue)value).getDouble();
                    double density = model.getDensity(featureName, doubleValue, category);
                    if (density > 0) {
                        probability *= density;
                    }
                }
            }
            categoryEntriesBuilder.set(category, probability);
        }

        return categoryEntriesBuilder.create();
    }

}
