/**
 * Created on: 05.02.2013 15:55:54
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.ListFeature;

/**
 * <p>
 * A {@link FeatureRanker} applying the information gain selection criterion as explained by Yang, Y., & Pedersen, J. O.
 * (1997). A comparative study on feature selection in text categorization. ICML. Retrieved from
 * http://faculty.cs.byu.edu/~ringger/Winter2007-CS601R-2/papers/yang97comparative.pdf
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.2.0
 */
public final class InformationGainFeatureRanker extends AbstractFeatureRanker {

    /**
     * <p>
     * The logger for objects of this class. Configure it using <code>/src/main/resources/log4j.properties</code>.
     * </p>
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InformationGainFeatureRanker.class);

    /**
     * <p>
     * Calculates the information gain score for all features within the provided {@code dataset} using the following
     * formula: G(t) = - sum^m_i=1 Pr(ci)log Pr(ci) + Pr(t) sum^m_i=1 Pr(ci|t) log Pr(ci|t) + Pr(!t) sum^m_i=1 Pr(ci|!t)
     * log Pr(ci|!t)
     * </p>
     * <p>
     * The variable ci is the ith target class. 'sum^m_i=1 is the sum from i equals 1 to m where m equals the number of
     * target classes in the dataset. 't' is a feature and Pr(t) is the probability that 't' occurs. Pr(ci|t) is the
     * probability that t and ci occur together.
     * </p>
     * 
     * @param featurePath The feature name if you have a flat {@link BasicFeatureVectorImpl} or the featurePath otherwise.
     * @param dataset The collection of instances to select features for.
     * @return A mapping from {@link Feature}s to their information gain score. This score is zero for features that are
     *         equally distributed over all target classes but can take on negative and positive values. Higher scores
     *         mean the {@link Feature} provides much information about the distribution of the target classes and about
     *         which target class an instance belongs to.
     */
    private Map<Feature<?>, Double> calculateInformationGain(final Collection<Trainable> dataset) {
        Validate.notNull(dataset);
        Map<Feature<?>, Double> ret = CollectionHelper.newHashMap();
        if (dataset.isEmpty()) {
            LOGGER.warn("Dataset for feature selection is empty. No feature selection is carried out.");
            return ret;
        }

        List<Trainable> preparedData = prepare(dataset);
        InformationGainFormula formula = new InformationGainFormula(preparedData);
        // TODO This is evil since it assumes the first Trainable in the preparedData list contains all features. Again
        // a schema would help.
        for (Feature<?> preparedFeature : preparedData.get(0).getFeatureVector()) {
            if (preparedFeature instanceof ListFeature) {
                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)preparedFeature;
                Map<Feature<?>, Double> gains = formula.calculateGains(listFeature);
                ret.putAll(gains);
            } else {
                double gain = formula.calculateGain(preparedFeature.getName());
                ret.put(preparedFeature, gain);
            }
        }

        return ret;
    }

    /**
     * <p>
     * Prepares the dataset so feature ranking can be applied.
     * </p>
     * 
     * @param dataset The dataset to prepare.
     * @return The prepared dataset. Each entry corresponds to one instance from the dataset and contains the prepared
     *         {@link Feature}s from that instance's {@link BasicFeatureVectorImpl} and the instance's target class.
     */
    private List<Trainable> prepare(Collection<? extends Trainable> dataset) {
        List<Trainable> ret = CollectionHelper.newArrayList();

        for (Trainable instance : dataset) {
            // deduplicate // TODO is this necessary? Is it possible to include a duplicate feature in the feature
            // vector? is the same word at different positions the same feature?
            Set<Feature<?>> features = discretize(instance.getFeatureVector(), dataset);

            Instance preparedInstance = new Instance(instance.getTargetClass());
            preparedInstance.getFeatureVector().addAll(features);
            ret.add(preparedInstance);
        }

        return ret;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<Trainable> dataset) {
        FeatureRanking ranking = new FeatureRanking();
        Map<? extends Feature<?>, Double> informationGainValues = calculateInformationGain(dataset);
        LOGGER.debug(informationGainValues.toString());

        // Dense features will have one score per value. This must be averaged to calculate a complete score for the
        // whole feature.
        Map<String, List<Double>> scores = CollectionHelper.newHashMap();
        for (Entry<? extends Feature<?>, Double> entry : informationGainValues.entrySet()) {
            String name = entry.getKey().getName();
            List<Double> featureScores = scores.get(name);
            if (featureScores == null) {
                featureScores = CollectionHelper.newArrayList();
            }
            featureScores.add(entry.getValue());
            scores.put(name, featureScores);
        }

        // average scores and add to ranking
        for (Entry<String, List<Double>> featureScores : scores.entrySet()) {
            double summedScores = .0;
            for (double score : featureScores.getValue()) {
                summedScores += score;
            }
            double averageScore = summedScores / featureScores.getValue().size();
            ranking.add(featureScores.getKey(), averageScore);
        }

        return ranking;
    }
}
