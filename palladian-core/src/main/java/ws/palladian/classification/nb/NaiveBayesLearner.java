package ws.palladian.classification.nb;

import ws.palladian.classification.Learner;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.LazyMatrix;
import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.collection.Matrix;
import ws.palladian.helper.collection.Matrix.MatrixEntry;
import ws.palladian.helper.collection.Vector.VectorEntry;
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
        LazyMatrix<String, Bag<String>> nominalCounts = LazyMatrix.create(new Bag.BagFactory<String>());
        // store mean and standard deviation for numeric features (name, category)
        Matrix<String, Stats> stats = LazyMatrix.create(SlimStats.FACTORY);

        for (Trainable trainable : trainables) {
            String category = trainable.getTargetClass();
            categories.add(category);

            for (Feature<?> feature : trainable.getFeatureVector()) {
                String featureName = feature.getName();

                if (feature instanceof NominalFeature) {
                    String nominalValue = ((NominalFeature)feature).getValue();
                    nominalCounts.get(featureName, nominalValue).add(category);
                } else if (feature instanceof NumericFeature) {
                    Double numericValue = ((NumericFeature)feature).getValue();
                    stats.get(featureName, category).add(numericValue);
                }
            }
        }

        Matrix<String, Double> sampleMeans = MapMatrix.create();
        Matrix<String, Double> standardDeviations = MapMatrix.create();

        for (MatrixEntry<String, Stats> row : stats.rows()) {
            String category = row.key();
            for (VectorEntry<String, Stats> cell : row) {
                String featureName = cell.key();
                sampleMeans.set(featureName, category, cell.value().getMean());
                standardDeviations.set(featureName, category, cell.value().getStandardDeviation());
            }
        }

        return new NaiveBayesModel(nominalCounts.getMatrix(), categories, sampleMeans, standardDeviations);
    }

}
