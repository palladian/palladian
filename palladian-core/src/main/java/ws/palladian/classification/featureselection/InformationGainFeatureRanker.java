package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.discretization.DatasetStatistics;
import ws.palladian.classification.discretization.Discretization;
import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.value.Value;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * A {@link FeatureRanker} applying the information gain selection criterion as explained in
 * "A Comparative Study on Feature Selection in Text Categorization", Yiming Yang, Jan O. Pedersen, 1997.
 * 
 * <p>
 * This ranker calculates the information gain score for all features within the provided dataset using the following
 * formula:
 * 
 * <pre>
 * G(t) =          -sum^m_(i=1) P_r(c_i) log P_r(c_i) 
 *        + P_r(t)  sum^m_(i=1) P_r(c_i|t) log P_r(ci|t) 
 *        + P_r(!t) sum^m_(i=1) P_r(c_i|!t) log P_r(c_i|!t)
 * </pre>
 * 
 * <p>
 * The variable <code>c_i</code> is the i-th target class. <code>sum^m_(i=1)</code> is the sum from <code>i = 1</code>
 * to <code>m</code>, where <code>m</code> equals the number of target classes in the dataset. <code>t</code> is a
 * feature and <code>Pr(t)</code> is the probability, that <code>t</code> occurs. <code>Pr(c_i|t)</code> is the
 * probability that t and ci occur together.
 * 
 * <p>
 * The result is of the provided {@link FeatureRanking} a mapping from features to their information gain score. This
 * score is zero for features that are equally distributed over all target classes, but can take on negative and
 * positive values. Higher scores mean the feature provides much information about the distribution of the target
 * classes and about which target class an instance belongs to.
 * 
 * <p>
 * The Information Gain formula is implemented as proposed by the Weka Machine Learning Framework. A description can be
 * found in <a href="http://arxiv.org/pdf/nlin/0307015v4.pdf">Methods and Techniques of Complex Systems Science: An
 * Overview</a>, page 47.
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class InformationGainFeatureRanker extends AbstractFeatureRanker {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InformationGainFeatureRanker.class);

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress) {
        Validate.notNull(dataset, "dataset must not be null");
        Map<String, Double> informationGainValues = CollectionHelper.newHashMap();

        progress.startTask("Information Gain", -1);
        LOGGER.debug("Calculating discretization");
        Discretization discretization = new Discretization(dataset, progress.createSubProgress(0.5));
        Iterable<Instance> preparedData = discretization.discretize(dataset);

        CategoryEntries categoryCounts = ClassificationUtils.getCategoryCounts(dataset);
        double entropy = ClassificationUtils.entropy(categoryCounts);
        Set<String> featureNames = new DatasetStatistics(preparedData).getFeatureNames();

        ProgressReporter informationGainProgress = progress.createSubProgress(0.5);
        LOGGER.debug("Calculating gain");
        informationGainProgress.startTask("Calculating gain", featureNames.size());
        for (String featureName : featureNames) {
            double gain = entropy - conditionalEntropy(featureName, preparedData);
            informationGainValues.put(featureName, gain);
            informationGainProgress.increment();
        }
        informationGainProgress.finishTask();
        return new FeatureRanking(informationGainValues);
    }

    /**
     * Calculates the conditional entropy of the dataset under the consideration that we know how the provided feature
     * is distributed. This is often called H(X|Y).
     * 
     * @param featureName The name of the feature.
     * @return The conditional entropy of the dataset knowing the distribution of Y.
     */
    private static double conditionalEntropy(String featureName, Iterable<? extends Instance> dataset) {
        CategoryEntries jointOccurrences = countJointOccurrences(dataset, featureName);
        CategoryEntries featureOccurrences = countFeatureOccurrences(dataset, featureName);
        return ClassificationUtils.entropy(jointOccurrences) - ClassificationUtils.entropy(featureOccurrences);
    }

    /**
     * Counts the joint occurrences of each value of the provided feature with each target class.
     * 
     * @param featureName The name of the feature.
     * @return A mapping from a pair of target class and feature value to the counter of their joint occurrences.
     */
    private static CategoryEntries countJointOccurrences(Iterable<? extends Instance> dataset, String featureName) {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Instance dataItem : dataset) {
            Value value = dataItem.getVector().get(featureName);
            builder.add(dataItem.getCategory() + "###" + value.toString(), 1);
        }
        return builder.create();
    }

    /**
     * Counts how often the values of the feature with the provided name occurs in the dataset handled by this object.
     * 
     * @param featureName The name of the feature.
     * @return A mapping from a {@link String} representation of the value to a counter of how often it occurs.
     */
    private static CategoryEntries countFeatureOccurrences(Iterable<? extends Instance> dataset, String featureName) {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Instance dataItem : dataset) {
            Value value = dataItem.getVector().get(featureName);
            builder.add(value.toString(), 1);
        }
        return builder.create();
    }

}
