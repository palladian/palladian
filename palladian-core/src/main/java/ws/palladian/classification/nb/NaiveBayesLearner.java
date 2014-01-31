package ws.palladian.classification.nb;

import java.util.Map;
import java.util.Map.Entry;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.math.SlimStats;
import ws.palladian.helper.math.Stats;
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
public final class NaiveBayesLearner implements Learner<NaiveBayesModel> {

    @Override
    public NaiveBayesModel train(Iterable<? extends Trainable> trainables) {

        // store the counts of different categories
        Bag<String> categories = Bag.create();
        // store the counts of nominal features (name, value, category)
        Bag<Triplet<String, String, String>> nominalCounts = Bag.create();
        // store mean and standard deviation for numeric features (name, category)
        Map<Pair<String, String>, Stats> stats = LazyMap.create(SlimStats.FACTORY);

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

}
