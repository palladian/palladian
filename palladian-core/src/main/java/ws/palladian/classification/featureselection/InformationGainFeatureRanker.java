package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.discretization.DatasetStatistics;
import ws.palladian.classification.discretization.Discretization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

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
public final class InformationGainFeatureRanker implements FeatureRanker {

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

        List<Instance> preparedData = prepare(dataset);

        InformationGainFormula formula = new InformationGainFormula(preparedData);
        
        Set<String> featureNames = new DatasetStatistics(dataset).getFeatureNames();
        
        System.out.println("feature names  = " + featureNames);
        
        for (String featureName : featureNames) {
            double gain = formula.calculateGain(featureName);
            System.out.println(featureName + "=" + gain);
            ret.put(featureName, gain);
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
        Discretization discretization = new Discretization(dataset);
        for (Instance instance : dataset) {
            FeatureVector features = discretization.discretize(instance.getVector());
            Instance preparedInstance = new InstanceBuilder().add(features).create(instance.getCategory());
            ret.add(preparedInstance );
        }
        return ret;
    }

    @Override
    public FeatureRanking rankFeatures(Collection<? extends Instance> dataset) {
        Map<String, Double> informationGainValues = calculateInformationGain(dataset);
        return new FeatureRanking(informationGainValues);
    }

}
