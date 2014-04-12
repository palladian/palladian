package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.discretization.Discretization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Value;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * A {@link FeatureRanker} applying the information gain selection criterion as explained by Yang, Y., & Pedersen, J. O.
 * (1997). A comparative study on feature selection in text categorization. ICML. Retrieved from <a
 * href="http://faculty.cs.byu.edu/~ringger/Winter2007-CS601R-2/papers/yang97comparative.pdf">here</a>.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.2.0
 */
public final class InformationGainFeatureRanker extends AbstractFeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InformationGainFeatureRanker.class);
    
    private final Discretization discretization = new Discretization();

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
     * @param dataset The collection of instances to select features for.
     * @return A mapping from features to their information gain score. This score is zero for features that are
     *         equally distributed over all target classes but can take on negative and positive values. Higher scores
     *         mean the feature provides much information about the distribution of the target classes and about
     *         which target class an instance belongs to.
     */
    private Map<String, Double> calculateInformationGain(Collection<? extends Instance> dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        Map<String, Double> ret = CollectionHelper.newHashMap();
        if (dataset.isEmpty()) {
            LOGGER.warn("Dataset for feature selection is empty. No feature selection is carried out.");
            return ret;
        }

        List<Instance> preparedData = prepare(dataset);

        int workItems = preparedData.get(0).getVector().size() * preparedData.size();
        ProgressMonitor monitor = new ProgressMonitor(workItems, 0.5, "Ranking Features");
        InformationGainFormula formula = new InformationGainFormula(preparedData, monitor);
        
        // TODO This is evil since it assumes the first Trainable in the preparedData list contains all features. Again
        // a schema would help.
        for (VectorEntry<String, Value> preparedFeature : preparedData.get(0).getVector()) {
//            if (preparedFeature instanceof ListFeature) {
//                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)preparedFeature;
//                Map<Feature<?>, Double> gains = formula.calculateGains(listFeature);
//                ret.putAll(gains);
//            } else {
                double gain = formula.calculateGain(preparedFeature.key());
                ret.put(preparedFeature.key(), gain);
//            }
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
     *         features from that instance's {@link FeatureVector} and the instance's target class.
     */
    private List<Instance> prepare(Collection<? extends Instance> dataset) {
        List<Instance> ret = CollectionHelper.newArrayList();

        for (Instance instance : dataset) {
            FeatureVector features = discretization.discretize(instance.getVector(), dataset);
            Instance preparedInstance = new InstanceBuilder().add(features).create(instance.getCategory());
            ret.add(preparedInstance );
        }

        return ret;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset) {
        FeatureRanking ranking = new FeatureRanking();
        Map<String, Double> informationGainValues = calculateInformationGain(dataset);
        LOGGER.debug(informationGainValues.toString());

        // Dense features will have one score per value. This must be averaged to calculate a complete score for the
        // whole feature.
        Map<String, List<Double>> scores = LazyMap.create(new Factory<List<Double>>() {
            @Override
            public List<Double> create() {
                return CollectionHelper.newArrayList();
            }
        });
        for (Entry<String, Double> entry : informationGainValues.entrySet()) {
            String name = entry.getKey();
//            List<Double> featureScores = scores.get(name);
//            if (featureScores == null) {
//                featureScores = CollectionHelper.newArrayList();
//            }
//            featureScores.add(entry.getValue());
//            scores.put(name, featureScores);
            scores.get(name).add(entry.getValue());
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
