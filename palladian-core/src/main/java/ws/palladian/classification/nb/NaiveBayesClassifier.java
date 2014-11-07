package ws.palladian.classification.nb;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

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
    public CategoryEntries classify(Classifiable classifiable, NaiveBayesModel model) {

        CategoryEntriesBuilder categoryEntriesBuilder = new CategoryEntriesBuilder();

        for (String category : model.getCategories()) {

            if (category.isEmpty()) {
                continue;
            }

            // initially set all category probabilities to their priors
            double probability = model.getPrior(category);

            for (Feature<?> feature : classifiable.getFeatureVector()) {
                String featureName = feature.getName();
                if (feature instanceof NominalFeature) {
                    NominalFeature nominalFeature = (NominalFeature)feature;
                    probability *= model.getProbability(featureName, nominalFeature.getValue(), category, laplace);
                } else if (feature instanceof NumericFeature) {
                    NumericFeature numericFeature = (NumericFeature)feature;
                    double density = model.getDensity(featureName, numericFeature.getValue(), category);
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
