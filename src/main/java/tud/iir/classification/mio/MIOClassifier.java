/**
 * 
 * @author Martin Werner
 */
package tud.iir.classification.mio;

import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.extraction.mio.MIO;
import weka.core.Instance;

/**
 * The MIOClassifier calculate scores for ranking of MIOs.
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

    }

    /**
     * Calculate the regression value for a given MIO.
     * 
     * @param mio the mio
     * @return the float
     */

    public float classify(final MIO mio) {

        final FeatureObject featObject = new FeatureObject(mio.getFeatures());

        final Instance iUse = createInstance(getFvWekaAttributes(), discretize(featObject.getFeatures()),
                getTrainingSet());

        try {
            final double[] fDistribution = getClassifier().distributionForInstance(iUse);

            return (float) fDistribution[0];
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 0;
        }

    }

    /**
     * Train classifier.
     * 
     * @param conceptID the concept id
     * @param featureString the feature string
     * @param classificationString the classification string
     * @return true, if successful
     */
    public boolean trainClassifier(final int conceptID, final PreparedStatement featureString,
            final PreparedStatement classificationString) {
        setPsFeatureStatement(featureString);
        setPsClassificationStatementConcept(classificationString);

        // load training data
        trainingObjects = readFeatureObjects(conceptID, featureString);
        return trainClassifier();
    }

    /**
     * Use an already trained classifier.
     */
    public void useTrainedClassifier() {
        weka.classifiers.Classifier trainedMIOClassifier;
        try {
            trainedMIOClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper
                    .read("data/learnedClassifiers/" + this.getChosenClassifierName() + ".model");
            // createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedMIOClassifier);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {
        // MIOClassifier mioClass = new MIOClassifier();
        // Concept concept = new Concept("mobilePhone");
        // Entity entity = new Entity("Samsung S8500 Wave", concept);
        // MIO mio = new MIO("FLASH", "", "", entity);
        // float classification = mioClass.classify(mio);
        //
        // System.out.println(classification);
    }

}
