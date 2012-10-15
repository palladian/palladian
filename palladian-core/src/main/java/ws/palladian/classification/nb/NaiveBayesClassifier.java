package ws.palladian.classification.nb;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
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

    @Override
    public NaiveBayesModel train(List<Instance> instances) {

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

        for (Instance instance : instances) {
            String category = instance.getTargetClass();
            categories.add(category);

            for (Feature<?> feature : instance.getFeatureVector()) {
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
    public CategoryEntries classify(FeatureVector vector, NaiveBayesModel model) {

        Map<String, Double> probabilities = CollectionHelper.newHashMap();

        // for normalization, so that category probabilities sum up to one at the end
        double evidence = 0;
        for (String category : model.getCategoryNames()) {

            // initially set all category probabilities to their priors
            double probability = model.getPrior(category);

            for (Feature<?> feature : vector) {
                String featureName = feature.getName();

                if (feature instanceof NominalFeature) {
                    NominalFeature nominalFeature = (NominalFeature)feature;
                    probability *= model.getProbability(featureName, nominalFeature.getValue(), category);
                }

                if (feature instanceof NumericFeature) {
                    NumericFeature numericFeature = (NumericFeature)feature;
                    probability *= model.getDensity(featureName, numericFeature.getValue(), category);
                }
            }

            probabilities.put(category, probability);
            evidence += probability;
        }

        // create the result with normalized probabilities
        CategoryEntries categoryEntries = new CategoryEntries();
        for (Entry<String, Double> entry : probabilities.entrySet()) {
            categoryEntries.add(new CategoryEntry(entry.getKey(), entry.getValue() / evidence));
        }

        return categoryEntries;
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

    @Override
    public NaiveBayesModel train(Dataset dataset) {
        // FIXME
        return null;
    }

}