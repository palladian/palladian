package ws.palladian.classification.nb;

import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.NominalValue;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.LazyMatrix;
import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.collection.Matrix;
import ws.palladian.helper.collection.Matrix.MatrixVector;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.SlimStats;
import ws.palladian.helper.math.Stats;

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
    public NaiveBayesModel train(Iterable<? extends Instance> instances) {

        // store the counts of different categories
        Bag<String> categories = Bag.create();
        // store the counts of nominal features (name, value, category)
        LazyMatrix<String, Bag<String>> nominalCounts = LazyMatrix.create(new Bag.BagFactory<String>());
        // store mean and standard deviation for numeric features (name, category)
        Matrix<String, Stats> stats = LazyMatrix.create(SlimStats.FACTORY);

        for (Instance instance : instances) {
            String category = instance.getCategory();
            categories.add(category);

            for (VectorEntry<String, Value> entry : instance.getVector()) {
                String featureName = entry.key();
                Value value = entry.value();

                if (value instanceof NominalValue) {
                    String nominalValue = ((NominalValue)value).getString();
                    nominalCounts.get(featureName, nominalValue).add(category);
                } else if (value instanceof NumericValue) {
                    double numericValue = ((NumericValue)value).getDouble();
                    stats.get(featureName, category).add(numericValue);
                }
            }
        }

        Matrix<String, Double> sampleMeans = MapMatrix.create();
        Matrix<String, Double> standardDeviations = MapMatrix.create();

        for (MatrixVector<String, Stats> row : stats.rows()) {
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
