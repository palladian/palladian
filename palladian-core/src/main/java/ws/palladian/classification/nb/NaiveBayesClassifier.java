package ws.palladian.classification.nb;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
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
public final class NaiveBayesClassifier implements Learner, Classifier<NaiveBayesModel> {

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
    public NaiveBayesModel train(Iterable<? extends Trainable> trainables) {

        // store the counts of different categories
        CountMap<String> categories = CountMap.create();
        // store the counts of nominal features (name, value, category)
        CountMap<Triplet<String, String, String>> nominalCounts = CountMap.create();
        // store mean and standard deviation for numeric features (name, category)
        Map<Pair<String, String>, Stats> stats = LazyMap.create(new Factory<Stats>() {
            @Override
            public Stats create() {
                return new Stats();
            }
        });

        for (Trainable trainable : trainables) {
            String category = trainable.getTargetClass();
            categories.add(category);

            for (Feature<?> feature : trainable.getFeatureVector()) {
                String featureName = feature.getName();

                if (feature instanceof NominalFeature) {
                    String nominalValue = ((NominalFeature)feature).getValue();
                    nominalCounts.add(new Triplet<String, String, String>(featureName, nominalValue, category));
                }

                if (feature instanceof NumericFeature) {
                    Stats stat = stats.get(new Pair<String, String>(featureName, category));
                    Double numericValue = ((NumericFeature)feature).getValue();
                    stat.add(numericValue);
                }
            }
        }

        Map<Pair<String, String>, Double> sampleMeans = CollectionHelper.newHashMap();
        Map<Pair<String, String>, Double> standardDeviations = CollectionHelper.newHashMap();

        for (Entry<Pair<String, String>, Stats> entry : stats.entrySet()) {
            sampleMeans.put(entry.getKey(), entry.getValue().getMean());
            standardDeviations.put(entry.getKey(), entry.getValue().getStandardDeviation());
        }

        return new NaiveBayesModel(nominalCounts, categories, sampleMeans, standardDeviations);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, NaiveBayesModel model) {

        Map<String, Double> probabilities = CollectionHelper.newHashMap();

        for (String category : model.getCategoryNames()) {

            // initially set all category probabilities to their priors
            double probability = model.getPrior(category);

            for (Feature<?> feature : classifiable.getFeatureVector()) {
                String featureName = feature.getName();

                if (feature instanceof NominalFeature) {
                    NominalFeature nominalFeature = (NominalFeature)feature;
                    probability *= model.getProbability(featureName, nominalFeature.getValue(), category, laplace);
                }

                if (feature instanceof NumericFeature) {
                    NumericFeature numericFeature = (NumericFeature)feature;
                    double density = model.getDensity(featureName, numericFeature.getValue(), category);
                    if (density > 0) {
                        probability *= density;
                    }
                }
            }

            probabilities.put(category, probability);
        }

        return new CategoryEntriesMap(probabilities);
    }

    /**
     * <p>
     * Keep mathematical stats such as mean and standard deviation for a series of numbers.
     * </p>
     * 
     * @author Philipp Katz
     */
    private static final class Stats {

        private final List<Double> values;

        public Stats() {
            this.values = CollectionHelper.newArrayList();
        }

        public void add(Double value) {
            values.add(value);
        }

        public double getMean() {
            double mean = 0;
            for (double value : values) {
                mean += value;
            }
            return mean / values.size();
        }

        public double getStandardDeviation() {
            if (values.size() == 1) {
                return 0.;
            }
            double mean = getMean();
            double standardDeviation = 0;
            for (double value : values) {
                standardDeviation += Math.pow(value - mean, 2);
            }
            standardDeviation /= values.size() - 1;
            standardDeviation = Math.sqrt(standardDeviation);
            return standardDeviation;
        }

    }

}
