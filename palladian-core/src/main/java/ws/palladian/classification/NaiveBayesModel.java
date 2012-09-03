package ws.palladian.classification;

import java.util.List;

import org.apache.commons.collections15.Bag;

import ws.palladian.helper.math.Tensor;

public class NaiveBayesModel implements Model {
    
    public NaiveBayesModel(Tensor bayesProbabilityTensor, List<Double[]> meansAndStandardDeviations, Bag<String> categories) {
        super();
        
        this.bayesProbabilityTensor = bayesProbabilityTensor;
        this.meansAndStandardDeviations = meansAndStandardDeviations;
        this.categories = categories;
    }
    
    private final Bag<String> categories;

    /**
     * <p>
     * A table holding the learned probabilities for each feature and class:<br>
     * Integer (feature index) | class1 (value1,value2,...,valueN), class2, ... , classN
     * </p>
     * 
     * <pre>
     * 1 | 0.3 (...), 0.6, ... , 0.1
     * x = featureIndex
     * y = classValue
     * z = featureValue
     * </pre>
     * */
    private final Tensor bayesProbabilityTensor;

    /**
     * <p>
     * The Bayes classifier is capable of classifying instances with numeric features too. For all learned numeric
     * features we need to store the mean and standard deviation. The probability tensor holds the index of this list in
     * the field for the numeric feature. We then need to lookup this list to find the mean in the first entry and the
     * standard deviation in the second entry of the array at the given index.
     * </p>
     */
    private final List<Double[]> meansAndStandardDeviations;
    
    public Tensor getBayesProbabilityTensor() {
        return bayesProbabilityTensor;
    }
    
    public List<Double[]> getMeansAndStandardDeviations() {
        return meansAndStandardDeviations;
    }

    public Bag<String> getCategories() {
        return categories;
    }

}
