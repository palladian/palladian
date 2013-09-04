/**
 * Created on: 05.02.2013 15:59:33
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;

/**
 * <p>
 * An implementation of the chi squared feature selection method. This method calculates the probability that the null
 * hypothesis is wrong for the correlation between a feature and a target class.
 * </p>
 * <p>
 * Further details are available for example in C. D. Manning, P. Raghavan, and H. Schütze, An introduction to
 * information retrieval, no. c. New York: Cambridge University Press, 2009, Page 275.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class ChiSquaredFeatureRanker extends AbstractFeatureRanker {

    /**
     * <p>
     * The logger for objects of this class. Configure it using <tt>/src/main/resources/log4j.properties</tt>
     * </p>
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChiSquaredFeatureRanker.class);

    /**
     * <p>
     * A strategy describing how feature rankings for different classes are merged.
     * </p>
     */
    private final SelectedFeatureMergingStrategy mergingStrategy;

    /**
     * <p>
     * Creates a new completely initialized {@link FeatureRanker}.
     * </p>
     * 
     * @param mergingStrategy
     *            A strategy describing how feature rankings for different
     *            classes are merged.
     */
    public ChiSquaredFeatureRanker(SelectedFeatureMergingStrategy mergingStrategy) {
        this.mergingStrategy = mergingStrategy;
    }

    /**
     * <p>
     * This is the core method calculating the raw chi squared scores. Only call it directly if you know what you are
     * doing. Otherwise use the {@link FeatureRanker} interface.
     * </p>
     * 
     * @param featureName
     *            The name of or path to the features to calculate the chi
     *            squared values for.
     * @param featureType
     *            The implementation class of the features at the provided path.
     *            this is necessary to get the correct features from the
     *            provided instances' {@link FeatureVector}.
     * @param dataset
     *            The dataset to use to calculate chi squared values for. The
     *            instances should actually contain the feature provided by {@code featurePath} and it needs to be of
     *            the correct type
     *            (i.e. {@code featureType}).
     * @return A mapping with the first key being a feature mapped to a map
     *         where the key is a target class from the {@code instances} and
     *         the value is the chi squared score for the feature with that
     *         class.
     */
    public Map<String, Map<String, Double>> calculateChiSquareValues(final Collection<? extends Trainable> dataset) {
        Map<Feature<?>, Map<String, Long>> termClassCorrelationMatrix = CollectionHelper.newHashMap();
        Map<String, Long> classCounts = CollectionHelper.newHashMap();
        Map<String, Map<String, Double>> ret = CollectionHelper.newHashMap();

        for (Trainable instance : dataset) {
            Set<Feature<?>> features = convertToSet(instance.getFeatureVector());
            features.addAll(discretize(features, dataset));
            LOGGER.trace(features.toString());
            for (Feature<?> value : features) {
                addCooccurence(value, instance.getTargetClass(), termClassCorrelationMatrix);
            }
            Long count = classCounts.get(instance.getTargetClass());
            if (count == null) {
                count = 0L;
            }
            classCounts.put(instance.getTargetClass(), ++count);
        }

        for (Map.Entry<Feature<?>, Map<String, Long>> termOccurence : termClassCorrelationMatrix.entrySet()) {
            // The following variables are uppercase because that is the way
            // they are used in the literature.
            int N = dataset.size();
            for (Map.Entry<String, Long> currentClassCount : classCounts.entrySet()) {
                String className = currentClassCount.getKey();
                Long classCount = currentClassCount.getValue();
                Long termClassCoocurrence = termOccurence.getValue().get(className);
                if (termClassCoocurrence == null) {
                    termClassCoocurrence = 0L;
                }
                //LOGGER.trace("Calculating Chi² for feature {} in class {}.", termOccurence.getKey(), className);
                long N_11 = termClassCoocurrence;
                long N_10 = sumOfRowExceptOne(termOccurence.getKey(), className, termClassCorrelationMatrix);
                long N_01 = classCount - termClassCoocurrence;
                long N_00 = N - (N_10 + N_01 + N_11);
                //LOGGER.trace("Using N_11 {}, N_10 {}, N_01 {}, N_00 {}", N_11, N_10, N_01, N_00);

                double numerator = Double.valueOf(N_11 + N_10 + N_01 + N_00) * Math.pow(N_11 * N_00 - N_10 * N_01, 2);
                long denominatorInt = (N_11 + N_01) * (N_11 + N_10) * (N_10 + N_00) * (N_01 + N_00);
                double denominator = Double.valueOf(denominatorInt);
                double chiSquare = numerator / denominator;

                //LOGGER.trace("Chi² value is {}", chiSquare);
                Map<String, Double> chiSquaresForCurrentTerm = ret.get(termOccurence.getKey());
                if (chiSquaresForCurrentTerm == null) {
                    chiSquaresForCurrentTerm = new HashMap<String, Double>();
                }
                chiSquaresForCurrentTerm.put(className, chiSquare);
                ret.put(termOccurence.getKey().getName(), chiSquaresForCurrentTerm);
            }
        }

        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     *
     * @param featureVector
     * @return
     */
    private Set<Feature<?>> convertToSet(Iterable<Feature<?>> featureVector) {
        Set<Feature<?>> ret = CollectionHelper.newHashSet();
        for(Feature<?> feature:featureVector) {
            if(feature instanceof ListFeature) {
                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
                for(Feature<?> listFeatureElement:listFeature) {
                    ret.add(listFeatureElement);
                }
            } else {
                ret.add(feature);
            }
        }
        return ret;
    }

    /**
     * <p>
     * Sums up a row of a matrix leaving one column out. This is required to calculate N01 and N10 for Chi² test. N01 is
     * the amount of documents of a class without a certain term, while N10 is the amount of documents with a certain
     * term but not of the specified class.
     * </p>
     * 
     * @param rowValue
     *            The value of the row to create the sum for.
     * @param exception
     *            The column to leave out of the summation.
     * @param correlationMatrix
     *            A matrix where cells are the counts of how often a the row
     *            value and the column value occur together.
     * @return The sum of the class occurrence without the term.
     */
    private static long sumOfRowExceptOne(Feature<?> rowValue, String exception,
            Map<Feature<?>, Map<String, Long>> correlationMatrix) {
        Map<String, Long> occurencesOfClass = correlationMatrix.get(rowValue);
        // add up all occurrences of the current class
        long ret = 0;
        for (Map.Entry<String, Long> occurence : occurencesOfClass.entrySet()) {
            if (!occurence.getKey().equals(exception)) {
                ret += occurence.getValue();
            }
        }
        return ret;
    }

    /**
     * <p>
     * Increases the coocurrence of the row value and the column value in the provided correlation matrix by one.
     * </p>
     *
     * @param row The value of the row of the matrix.
     * @param column The value of the column of the matrix.
     * @param correlationMatrix The correlation matrix to add the correlation to.
     */
    private static void addCooccurence(Feature<?> row, String column, Map<Feature<?>, Map<String, Long>> correlationMatrix) {

        Map<String, Long> correlations = correlationMatrix.get(row);
        if (correlations == null) {
            correlations = new HashMap<String, Long>();
        }
        Long occurenceCount = correlations.get(column);
        if (occurenceCount == null) {
            occurenceCount = 0L;
        }
        occurenceCount++;
        correlations.put(column, occurenceCount);
        correlationMatrix.put(row, correlations);

    }

    @Override
    public FeatureRanking rankFeatures(Collection<Trainable> dataset) {
        Map<String,Map<String,Double>> scoredFeatures = calculateChiSquareValues(dataset);
        return mergingStrategy.merge(dataset,scoredFeatures);
    }
}
