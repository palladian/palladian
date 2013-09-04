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
        InformationGainFormula formula = new InformationGainFormula();
        // TODO This is evil since it assumes the first Trainable in the preparedData list contains all features. Again
        // a schema would help.
        for (Feature<?> preparedFeature : preparedData.get(0).getFeatureVector()) {
            if (preparedFeature instanceof ListFeature) {
                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)preparedFeature;
                Map<Feature<?>, Double> gains = formula.calculateGains(preparedData, listFeature);
                ret.putAll(gains);
            } else {
                double gain = formula.calculateGain(preparedData, preparedFeature.getName());
                ret.put(preparedFeature, gain);
            }
        }

        // Map<String, Double> classPriors = calculateTargetClassPriors(dataset);
        // // All occurrences of feature in the dataset
        // Map<Feature<?>, Integer> absoluteOccurences = CollectionHelper.newHashMap();
        // // Occurrences of feature together with a certain target class.
        // Map<Feature<?>, Map<String, Integer>> absoluteConditionalOccurences = CollectionHelper.newHashMap();
        // // the following two lines: vocabulary size required for smoothing
        // Map<String, Integer> featuresInClass = CollectionHelper.newHashMap();
        // Integer sumOfFeaturesInAllClasses = 0;
        //
        // for (Pair<Set<Feature<?>>, String> instance : preparedData) {
        //
        // for (Feature<?> feature : instance.getLeft()) {
        //
        // // count absolute occurrences
        // Integer absoluteOccurrencesOfFeature = absoluteOccurences.get(feature);
        // if (absoluteOccurrencesOfFeature == null) {
        // absoluteOccurrencesOfFeature = 0;
        // }
        // absoluteOccurrencesOfFeature++;
        // absoluteOccurences.put(feature, absoluteOccurrencesOfFeature);
        //
        // // count conditional occurrences with all classes
        // Map<String, Integer> absoluteConditionalOccurence = absoluteConditionalOccurences.get(feature);
        // if (absoluteConditionalOccurence == null) {
        // absoluteConditionalOccurence = CollectionHelper.newHashMap();
        // }
        // Integer occurrenceInTargetClass = absoluteConditionalOccurence.get(instance.getRight());
        // if (occurrenceInTargetClass == null) {
        // occurrenceInTargetClass = 0;
        // }
        // occurrenceInTargetClass++;
        // absoluteConditionalOccurence.put(instance.getRight(), occurrenceInTargetClass);
        // absoluteConditionalOccurences.put(feature, absoluteConditionalOccurence);
        //
        // // calculate the vocabulary size for smoothing.
        // Integer countOfFeaturesInClass = featuresInClass.get(instance.getRight());
        // if (countOfFeaturesInClass == null) {
        // countOfFeaturesInClass = 0;
        // }
        // featuresInClass.put(instance.getRight(), ++countOfFeaturesInClass);
        // sumOfFeaturesInAllClasses++;
        // }
        // }
        //
        // // calculate dataset constant class probability (first summand)
        // double classProb = 0.0d;
        // for (Entry<String, Double> classCount : classPriors.entrySet()) {
        // double Prci = classCount.getValue();
        // classProb += Prci * ld(Prci);
        // }
        //
        // // calculate information gain.
        // for (Entry<Feature<?>, Integer> absoluteOccurrence : absoluteOccurences.entrySet()) {
        // ret.put(absoluteOccurrence.getKey(),
        // gain(classProb, absoluteOccurrence.getValue(),
        // absoluteConditionalOccurences.get(absoluteOccurrence.getKey()), dataset, featuresInClass,
        // sumOfFeaturesInAllClasses));
        // }
        return ret;
    }

    // /**
    // * <p>
    // *
    // * </p>
    // *
    // * @param value
    // * @param map
    // * @return
    // */
    // private double gain(Double classProbability, Integer value, Map<String, Integer> map,
    // Collection<? extends Trainable> dataset, Map<String, Integer> featuresInClass, int sumOfFeaturesInAllClasses) {
    // double termClassCoocurrence = 0.0d;
    // double termClassNonCoocurrence = 0.0d;
    // for (Entry<String, Integer> absoluteConditionalOccurence : map.entrySet()) {
    // // Probability for class ci containing term t
    // double Prcit = laplaceSmooth(map.keySet().size(),
    // featuresInClass.get(absoluteConditionalOccurence.getKey()), absoluteConditionalOccurence.getValue());
    // termClassCoocurrence += Prcit * ld(Prcit);
    //
    // // Probability for class ci not containing term t
    // double Prcint = laplaceSmooth(map.keySet().size(),
    // sumOfFeaturesInAllClasses - featuresInClass.get(absoluteConditionalOccurence.getKey()),
    // dataset.size() - absoluteConditionalOccurence.getValue());
    // termClassNonCoocurrence += Prcint * ld(Prcint);
    // }
    // double termProb = value.doubleValue() / dataset.size() * termClassCoocurrence;
    //
    // double nonTermProb = (double)(dataset.size() - value) / dataset.size() * termClassNonCoocurrence;
    //
    // return -classProbability + termProb + nonTermProb;
    // // double gain = .0;
    // // for(Entry<String, Integer> entry:map.entrySet()) {
    // // int probability = entry.getValue()/value;
    // // gain = gain - probability * ld(probability);
    // // }
    // // return gain;
    // }

    // /**
    // * <p>
    // * Calculates the base 2 logarithm for the provided argument.
    // * </p>
    // *
    // * @param arg The argument to calculate the logarithm for.
    // * @return The base 2 logarithm for the provided argument.
    // */
    // private double ld(double arg) {
    // return Math.log(arg) / Math.log(2);
    // }

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

    // /**
    // * <p>
    // * Calculates the priors for all target classes occurring in the dataset.
    // * </p>
    // *
    // * @param dataset The dataset to calculate the target class priors for.
    // * @return A mapping from target class to prior.
    // */
    // private Map<String, Double> calculateTargetClassPriors(final Collection<? extends Trainable> dataset) {
    // Map<String, Double> ret = CollectionHelper.newHashMap();
    // Map<String, Integer> absoluteOccurrences = CollectionHelper.newHashMap();
    //
    // for (Trainable instance : dataset) {
    // Integer absoluteOccurrenceOfClass = absoluteOccurrences.get(instance.getTargetClass());
    // if (absoluteOccurrenceOfClass == null) {
    // absoluteOccurrenceOfClass = 0;
    // }
    // absoluteOccurrenceOfClass++;
    // absoluteOccurrences.put(instance.getTargetClass(), absoluteOccurrenceOfClass);
    // }
    //
    // for (Entry<String, Integer> absoluteOccurrenceOfClass : absoluteOccurrences.entrySet()) {
    // ret.put(absoluteOccurrenceOfClass.getKey(),
    // absoluteOccurrenceOfClass.getValue().doubleValue() / dataset.size());
    // }
    //
    // return ret;
    // }
    //
    // private static double laplaceSmooth(int vocabularySize, int countOfFeature, int countOfCoocurence) {
    // return (1.0d + countOfCoocurence) / (vocabularySize + countOfFeature);
    // }

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
