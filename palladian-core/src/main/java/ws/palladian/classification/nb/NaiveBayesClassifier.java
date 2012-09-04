package ws.palladian.classification.nb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.Predictor;
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
 * is nominal. More information about Naive Bayes can be found <a
 * href="http://www.pierlucalanzi.net/wp-content/teaching/dmtm/DMTM0809-13-ClassificationIBLNaiveBayes.pdf">here</a>.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class NaiveBayesClassifier implements Predictor<NaiveBayesModel> {

    @Override
    public NaiveBayesModel learn(List<NominalInstance> instances) {

        CountMap<Triplet<String, String, String>> nominalCounts = CountMap.create();
        CountMap<String> categories = CountMap.create();
        Map<Pair<String, String>, Stats> stats = LazyMap.create(new Factory<Stats>() {
            @Override
            public Stats create() {
                return new Stats();
            }
        });

        for (NominalInstance instance : instances) {
            String category = instance.target;
            categories.increment(category);

            for (Feature<?> feature : instance.featureVector) {
                String featureName = feature.getName();

                if (feature instanceof NominalFeature) {
                    String nominalValue = ((NominalFeature)feature).getValue();
                    nominalCounts.increment(new Triplet<String, String, String>(featureName, nominalValue, category));
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
    public CategoryEntries predict(FeatureVector vector, NaiveBayesModel model) {

        // set all category probabilities to one initially
        Map<String, Double> probabilities = LazyMap.create(new Factory<Double>() {
            @Override
            public Double create() {
                return 1.;
            }
        });

        for (String category : model.getCategories().uniqueItems()) {

            for (Feature<?> feature : vector) {

                if (feature instanceof NominalFeature) {
                    NominalFeature nominalFeature = (NominalFeature)feature;
                    int count = model.getNominalCounts()
                            .get(new Triplet<String, String, String>(feature.getName(), nominalFeature.getValue(),
                                    category));
                    double probability = (double)count / (model.getCategories().get(category) + 1);
                    probabilities.put(category, probabilities.get(category) * probability);
                }

                if (feature instanceof NumericFeature) {
                    NumericFeature numericFeature = (NumericFeature)feature;
                    double standardDeviation = model.getStandardDeviations().get(
                            new Pair<String, String>(feature.getName(), category));
                    double mean = model.getSampleMeans().get(new Pair<String, String>(feature.getName(), category));

                    double density = (double)1
                            / (Math.sqrt(2 * Math.PI) * standardDeviation)
                            * Math.pow(
                                    Math.E,
                                    -Math.pow(numericFeature.getValue() - mean, 2)
                                            / (2 * Math.pow(standardDeviation, 2)));

                    probabilities.put(category, probabilities.get(category) * density);
                }
            }
        }

        // multiply with prior probabilities, determine sum for normalization
        double sum = 0;
        for (String category : model.getCategories().uniqueItems()) {
            double probability = probabilities.get(category) * model.getPrior(category);
            sum += probability;
            probabilities.put(category, probability);
        }

        // create the result with normalized probabilities
        CategoryEntries categoryEntries = new CategoryEntries();
        for (Entry<String, Double> entry : probabilities.entrySet()) {
            categoryEntries
                    .add(new CategoryEntry(categoryEntries, new Category(entry.getKey()), entry.getValue() / sum));
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
            this.values = new ArrayList<Double>();
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
}