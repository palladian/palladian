/**
 * 
 * @author Martin Werner
 */
package tud.iir.classification.mio;

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
 * 
 */
public class MIOClassifier extends Classifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOClassifier.class);

    /**
     * Instantiates a new mIO classifier.
     */
    public MIOClassifier() {
//         super(Classifier.LINEAR_REGRESSION);
        super(Classifier.NEURAL_NETWORK);

    }

    /**
     * Calculate the regression value for a given MIO.
     * 
     * @param mio the MIO
     * @return the float
     */
    public void classify(final MIO mio) {

        // System.out.println(mio.getFeatures().keySet().toString());
        final FeatureObject featObject = new FeatureObject(mio.getFeatures());
        double[] probability = super.classifySoft(featObject);
//        System.out.println("Probability of beeing positive: " +probability[0]);
//        System.out.println("Probability of beeing negative: " +probability[1]);
//        final boolean check = super.classifyBinary(featObject, true);
        if (probability[0]>0) {
//             System.out.println(mio.getDirectURL() + " is classified as positive");
//            LOGGER.info("object is classified as positive");
            mio.setMlTrust(probability[1]);
        } else {
//             System.out.println(mio.getDirectURL() + "object is classified as negative");
//            LOGGER.info("object is classified as negative");
            mio.setMlTrust(probability[0]);
        }

        // final Instance iUse = createInstance(getFvWekaAttributes(), discretize(featObject.getFeatures()),
        // getTrainingSet());
        //
        // try {
        // final double[] fDistribution = getClassifier().distributionForInstance(iUse);
        //
        // return (float) fDistribution[0];
        // } catch (Exception e) {
        // LOGGER.error(e.getMessage());
        // return 0;
        // }

    }

    /**
     * Train classifier.
     *
     * @param filePath the file path
     * @return true, if successful
     */
    public void trainClassifier(String filePath) {
        // setPsFeatureStatement(featureString);
        // setPsClassificationStatementConcept(classificationString);
        //
        // // load training data
        // trainingObjects = readFeatureObjects(conceptID, featureString);
        super.trainClassifier(filePath);
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
    public static void main(final String[] args) {
         MIOClassifier mioClass = new MIOClassifier();
         mioClass.trainClassifier("f:/features - printer - allcontextfeat.txt");
//         mioClass.useTrainedClassifier();
//        
         Concept concept = new Concept("printer");
         Entity entity = new Entity("Canon MP990", concept);
         MIOPage mioPage = new MIOPage(
         "http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/main.html?WT.acCCI_PixmaTour_MP990_Europe");
         mioPage.setDedicatedPageTrust(0.9800000000000001);
        
         MIO mio = new MIO("FLASH", "http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/index.swf",
         "http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/main.html?WT.acCCI_PixmaTour_MP990_Europe",
         entity);
        
         mio.setFeature("ALTTextRelevance", 0.31962481141090393);
         MIOContextAnalyzer mioCA = new MIOContextAnalyzer(entity, mioPage);
         mioCA.setFeatures(mio);
//         System.out.println(mio.getFeatures().entrySet().toString());
         // mio.setFeature(name, value)
         // float classification = mioClass.classify(mio);
         mioClass.classify(mio);
//        
         MIOPage mioPage2 = new MIOPage("http://www.amazon.com/Canon-Wireless-Inkjet-Printer-3749B002/dp/B002M78HX6");
         MIO mio2 = new MIO("FLASH",
         "http://g-ecx.images-amazon.com/images/G/01/am3/20100615163706920/AMPlayer._V191513928_.swf",
         "http://www.amazon.com/Canon-Wireless-Inkjet-Printer-3749B002/dp/B002M78HX6", entity);
        
         MIOContextAnalyzer mioCA2 = new MIOContextAnalyzer(entity, mioPage2);
         mioCA2.setFeatures(mio2);
//         System.out.println(mio2.getFeatures().entrySet().toString());
         // System.out.println(classification);
         mioClass.classify(mio2);
    }

}
