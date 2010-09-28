/**
 * 
 * @author Martin Werner
 */
package tud.iir.classification.mio;

import java.io.File;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.extraction.mio.MIO;
import tud.iir.extraction.mio.MIOContextAnalyzer;
import tud.iir.extraction.mio.MIOPage;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;

/**
 * The MIOClassifier calculate scores for ranking of MIOs.
 * Attention: First train the Classifier, then save it.
 * For classifying the classifier must be loaded first.
 * 
 */
public class MIOClassifier extends Classifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOClassifier.class);

    /**
     * Instantiates a new mIO classifier.
     */
    public MIOClassifier() {
        super(Classifier.LINEAR_REGRESSION);
        // super(Classifier.NEURAL_NETWORK);
    }

    /**
     * Calculate the regression value for a given MIO.
     * 
     * @param mio the MIO
     * @return the float
     */
    public void classify(final MIO mio) {

        // use the MIOFeatures as FeatureObject
        final FeatureObject featObject = new FeatureObject(mio.getFeatures());

        int featureCount = featObject.getFeatureNames().length;

        // creating WekaAttributes is necessary because of loading the Classifier from File
        super.createWekaAttributes(featureCount, featObject.getFeatureNames());

        double[] probability = super.classifySoft(featObject);
        
        mio.setTrust(normalizeTrust(probability[0]));
    }
    
    private double normalizeTrust(double trust){
        double normalizedTrust=trust;
        if (trust>=1){
            normalizedTrust=1.;
            
        }
        if (trust<=0){
            normalizedTrust=0.;
        }
        normalizedTrust = Math.round( normalizedTrust * 1000. )/ 10.;
        return normalizedTrust;
    }

    /**
     * Train classifier.
     * 
     * @param filePath the file path
     * @return true, if successful
     */
    public void trainClassifier(String filePath) {
      
        super.trainClassifier(filePath);
    }

    /**
     * Load an already trained classifier.
     */
    public void loadTrainedClassifier() {
        weka.classifiers.Classifier trainedMIOClassifier;
        try {
            trainedMIOClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper.read("config/"
                    + "MIOClassifier" + getChosenClassifierName() + ".model");

            setClassifier(trainedMIOClassifier);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
    
    /**
     * Simply save the trained classifier.
     */
    public void saveTrainedClassifier() {
        weka.classifiers.Classifier trainedMIOClassifier = super.getClassifier();
        try {
            weka.core.SerializationHelper.write(
                    "config/" + "MIOClassifier" + getChosenClassifierName() + ".model", trainedMIOClassifier);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            }
    }

    /**
     * Check if an already trained MIOClassifier exists.
     */
    public boolean doesTrainedMIOClassifierExists() {
        boolean returnValue = false;
        File trainedClassifierModel = new File("config/MIOClassifierLinearRegression.model");
        if (trainedClassifierModel.exists()) {
            returnValue = true;
        }
        return returnValue;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(final String[] args) {
        MIOClassifier mioClass = new MIOClassifier();
         mioClass.trainClassifier("data/miofeatures_scored.txt");
//         mioClass.saveTrainedClassifier();
//        mioClass.loadTrainedClassifier();
//         mioClass.testClassifier("f:/features_bewertet_august.txt");
    }
}
