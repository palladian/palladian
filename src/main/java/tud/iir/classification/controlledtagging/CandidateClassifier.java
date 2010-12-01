package tud.iir.classification.controlledtagging;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import weka.core.SerializationHelper;

public class CandidateClassifier extends Classifier {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(CandidateClassifier.class);

    public CandidateClassifier() {
        super(Classifier.NEURAL_NETWORK);
    }

    /**
     * Classify a candidate object.
     * @param candidate
     */
    public void classify(Candidate candidate) {
        FeatureObject featureObject = new FeatureObject(candidate.getFeatures());
        double result = classifySoft(featureObject)[0];
        candidate.setRegressionValue(result);
    }
    
    /**
     * Classify a list of candidate objects.
     * @param candidates
     */
    public void classify(DocumentModel candidates) {
        for (Candidate candidate : candidates) {
            classify(candidate);
        }
    }

    // overridden in order to create the necessary weka attributes before classifying.
    // this is necessary, when the classifier is deserialized, as we dont have the feature names
    // in the serialized model.
    // TODO pull up?
    @Override
    public double[] classifySoft(FeatureObject fo) {

        // if the classifier was loaded from file, we need to create those Weka attributes.
        // to be honest, I did not fully get, whats going on here, but it works and I am tired now :(
        if (trainingObjects == null) {
            createWekaAttributes(fo.getFeatureNames().length, fo.getFeatureNames());
        }

        return super.classifySoft(fo);

    }

    /**
     * Use an already trained classifier.
     * 
     * TODO pull this method up? I have copied this to NewsRankingClassifier for now. We should have the possibility to
     * set file names for the serialized model to avoid conflicts between different Classifier subclasses -- Philipp.
     */
    public void useTrainedClassifier() {
        try {
            weka.classifiers.Classifier trainedClassifier = (weka.classifiers.Classifier) SerializationHelper
                    .read(getFileName());
            setClassifier(trainedClassifier);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public void loadTrainedClassifier(String filePath) {
        try {
            weka.classifiers.Classifier trainedClassifier = (weka.classifiers.Classifier) SerializationHelper
                    .read(filePath);
            setClassifier(trainedClassifier);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * Simply save the trained classifier.
     * 
     * TODO pull up?
     */
    public void saveTrainedClassifier() {
        try {
            SerializationHelper.write(getFileName(), getClassifier());
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * Get the file name for serializing/deserializing the trained classifier.
     * 
     * TODO pull up?
     * 
     * @return
     */
    public String getFileName() {
        return "data/models/CandidateClassifier_" + getChosenClassifierName() + ".model";
    }

    public static void main(String[] args) {
        CandidateClassifier c = new CandidateClassifier();
        c.trainClassifier("train_1000_new.csv");
        c.saveTrainedClassifier();
    }

}
