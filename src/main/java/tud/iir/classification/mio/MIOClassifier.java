/**
 * 
 * @author Martin Werner
 */
package tud.iir.classification.mio;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.extraction.mio.MIO;
import tud.iir.extraction.mio.MIOContextAnalyzer;
import tud.iir.extraction.mio.MIOPage;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import weka.classifiers.Evaluation;
import weka.core.Instance;

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
        normalizedTrust = Math.round( normalizedTrust * 100. ) / 100.;
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
            trainedMIOClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper.read("data/models/"
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
                    "data/models/" + "MIOClassifier" + getChosenClassifierName() + ".model", trainedMIOClassifier);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            }
    }

    /**
     * Check if an already trained MIOClassifier exists.
     */
    public boolean doesTrainedMIOClassifierExists() {
        boolean returnValue = false;
        File trainedClassifierModel = new File("data/models/MIOClassifierLinearRegression.model");
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
        // mioClass.saveTrainedClassifier();
//        mioClass.loadTrainedClassifier();
         mioClass.testClassifier("f:/features_bewertet_august_ohnexml_bkp.txt");
         System.out.println(mioClass.getEvaluation().rootMeanSquaredError());
        // System.out.println(mioClass.getClassifier().toString());

        // System.out.println(mioClass.getEvaluation().toString());
//         mioClass.getRMSE();
         System.exit(1);

        // mioClass.useTrainedClassifier();
        //
        Concept concept = new Concept("printer");
        Entity entity = new Entity("Canon MP990", concept);
        MIOPage mioPage = new MIOPage(
                "http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/main.html?WT.acCCI_PixmaTour_MP990_Europe");
        mioPage.setDedicatedPageTrust(0.9800000000000001);

        MIO mio = new MIO("FLASH", "http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/index.swf",
                "http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/main.html?WT.acCCI_PixmaTour_MP990_Europe",
                entity);

        mio.initializeFeatures();
        mio.setFeature("ALTTextRelevance", 0.31962481141090393);
        // MIOContextAnalyzer mioCA = new MIOContextAnalyzer(entity, mioPage);
        // mioCA.setFeatures(mio);
        // System.out.println(mio.getFeatures().entrySet().toString());
        // mio.setFeature(name, value)
        // float classification = mioClass.classify(mio);
        mioClass.classify(mio);
        System.out.println("LRTrust: " + mio.getMlTrust());
        System.exit(1);
        //
        MIOPage mioPage2 = new MIOPage("http://www.amazon.com/Canon-Wireless-Inkjet-Printer-3749B002/dp/B002M78HX6");
        MIO mio2 = new MIO("FLASH",
                "http://g-ecx.images-amazon.com/images/G/01/am3/20100615163706920/AMPlayer._V191513928_.swf",
                "http://www.amazon.com/Canon-Wireless-Inkjet-Printer-3749B002/dp/B002M78HX6", entity);

        MIOContextAnalyzer mioCA2 = new MIOContextAnalyzer(entity, mioPage2);
        mioCA2.setFeatures(mio2);
        // System.out.println(mio2.getFeatures().entrySet().toString());
        // System.out.println(classification);
        mioClass.classify(mio2);
    }

}
