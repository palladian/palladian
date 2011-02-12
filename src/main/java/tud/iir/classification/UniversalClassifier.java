package tud.iir.classification;

import org.apache.log4j.Logger;

import tud.iir.classification.numeric.NumericClassifier;
import tud.iir.classification.page.TextClassifier;


public class UniversalClassifier extends Classifier<UniversalInstance> {
    
    /** The serialize version ID. */
    private static final long serialVersionUID = 6434885229397022001L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(UniversalClassifier.class);
    
    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private TextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private NumericClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private BayesClassifier nominalClassifier;


    public void classify(Instance instance) {

        // classify numeric and nominal features with the KNN

        // classify text using the dictionary classifier

        // merge classification results

    }
    
}
