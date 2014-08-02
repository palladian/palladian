package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.discretization.Discretization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.value.Value;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.collection.CountMatrix.IntegerMatrixVector;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.NumericMatrix;

/**
 * <p>
 * An implementation of the chi squared feature selection method. This method calculates the probability that the null
 * hypothesis is wrong for the correlation between a feature and a target class. Further details are available for
 * example in C. D. Manning, P. Raghavan, and H. Schütze, An introduction to information retrieval, no. c. New York:
 * Cambridge University Press, 2009, Page 275.
 * </p>
 * 
 * @author Klemens Muthmann
 */
public final class ChiSquaredFeatureRanker extends AbstractFeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChiSquaredFeatureRanker.class);

    /** A strategy describing how feature rankings for different classes are merged. */
    private final SelectedFeatureMergingStrategy mergingStrategy;

    /**
     * <p>
     * Creates a new completely initialized {@link FeatureRanker}.
     * </p>
     * 
     * @param mergingStrategy A strategy describing how feature rankings for different classes are merged.
     */
    public ChiSquaredFeatureRanker(SelectedFeatureMergingStrategy mergingStrategy) {
        Validate.notNull(mergingStrategy, "mergingStrategy must not be null");
        this.mergingStrategy = mergingStrategy;
    }

    /**
     * <p>
     * This is the core method calculating the raw chi squared scores. Only call it directly if you know what you are
     * doing. Otherwise use the {@link FeatureRanker} interface.
     * </p>
     * 
     * @param dataset The dataset for which to calculate chi squared values, not <code>null</code>.
     * @param progress A {@link ProgressReporter}, or <code>null</code> in case no progress should be reported.
     * @return Matrix with the chi squared values. Each row in the matrix represents a feature, each column a class.
     */
    public static NumericMatrix<String> calculateChiSquareValues(Iterable<? extends Instance> dataset,
            ProgressReporter progress) {
        Validate.notNull(dataset, "dataset must not be null");

        if (progress == null) {
            progress = NoProgress.INSTANCE;
        }

        progress.startTask("Calculating chi² ranking", -1);

        int N = CollectionHelper.count(dataset.iterator());
        ProgressReporter cooccurrenceProgress = progress.createSubProgress(0.5);
        cooccurrenceProgress.startTask("Counting cooccurrences.", N);
        CountMatrix<String> termCategoryCorrelations = CountMatrix.create();
        Bag<String> categoryCounts = Bag.create();

        Discretization discretization = new Discretization(dataset, NoProgress.INSTANCE);
        Iterable<Instance> discretizedDataset = discretization.discretize(dataset);

        for (Instance instance : discretizedDataset) {
            FeatureVector featureVector = instance.getVector();
            String category = instance.getCategory();
            for (VectorEntry<String, Value> feature : featureVector) {
                String featureValueIdentifier = feature.key() + "###" + feature.value().toString();
                termCategoryCorrelations.add(category, featureValueIdentifier);
            }
            categoryCounts.add(category);
            cooccurrenceProgress.increment();
        }

        ProgressReporter chiSquareProgress = progress.createSubProgress(0.5);
        chiSquareProgress.startTask("Calculating chi² values.", termCategoryCorrelations.rowCount());
        NumericMatrix<String> result = new NumericMatrix<String>();
        for (IntegerMatrixVector<String> termOccurence : termCategoryCorrelations.rows()) {
            String featureName = termOccurence.key();
            IntegerMatrixVector<String> categoryCorrelations = termCategoryCorrelations.getRow(featureName);
            for (Entry<String, Integer> categoryCountEntry : categoryCounts.unique()) {
                String categoryName = categoryCountEntry.getKey();
                Integer categoryCount = categoryCountEntry.getValue();
                LOGGER.trace("Calculating Chi² for feature {} in class {}.", featureName, categoryName);
                int N_10 = categoryCorrelations.getSum() - categoryCorrelations.get(categoryName);
                int N_11 = termOccurence.get(categoryName);
                int N_01 = categoryCount - N_11;
                int N_00 = N - (N_10 + N_01 + N_11);
                LOGGER.trace("Using N_11 {}, N_10 {}, N_01 {}, N_00 {}", N_11, N_10, N_01, N_00);
                double numerator = (N_11 + N_10 + N_01 + N_00) * Math.pow(N_11 * N_00 - N_10 * N_01, 2);
                int denominator = (N_11 + N_01) * (N_11 + N_10) * (N_10 + N_00) * (N_01 + N_00);
                double chiSquare = numerator / denominator;
                LOGGER.trace("Chi² value is {}", chiSquare);
                result.set(categoryName, featureName, chiSquare);
            }
            chiSquareProgress.increment();
        }
        return result;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress) {
        Validate.notNull(dataset, "dataset must not be null");
        NumericMatrix<String> chiSquareMatrix = calculateChiSquareValues(dataset, progress);
        return mergingStrategy.merge(chiSquareMatrix);
    }

}
