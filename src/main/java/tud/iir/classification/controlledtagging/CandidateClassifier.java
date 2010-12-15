package tud.iir.classification.controlledtagging;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import weka.core.SerializationHelper;

/**
 * TODO all relevant functionality has been pulled to {@link Classifier}, so this subclass is basically obsolete now.
 * 
 * 
 * @author Philipp Katz
 */
public class CandidateClassifier extends Classifier {

    /** The class logger. */
    // private static final Logger LOGGER = Logger.getLogger(CandidateClassifier.class);

    public CandidateClassifier() {
        // super(Classifier.NEURAL_NETWORK);
        super(Classifier.BAGGING);
    }

    /**
     * Classify a candidate object.
     * 
     * @param candidate
     */
    public void classify(Candidate candidate) {
        FeatureObject featureObject = new FeatureObject(candidate.getFeatures());
        double result = classifySoft(featureObject)[0];
        candidate.setRegressionValue(result);
    }

    /**
     * Classify a list of candidate objects.
     * 
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
//    @Override
//    public double[] classifySoft(FeatureObject fo) {
//
//        // if the classifier was loaded from file, we need to create those Weka attributes.
//        // to be honest, I did not fully get, whats going on here, but it works and I am tired now :(
//        if (trainingObjects == null) {
//            createWekaAttributes(fo.getFeatureNames().length, fo.getFeatureNames());
//        }
//
//        return super.classifySoft(fo);
//
//    }

    /**
     * Use an already trained classifier.
     * 
     * TODO pull this method up? I have copied this to NewsRankingClassifier for now. We should have the possibility to
     * set file names for the serialized model to avoid conflicts between different Classifier subclasses -- Philipp.
     */
    // public void useTrainedClassifier() {
    // try {
    // weka.classifiers.Classifier trainedClassifier = (weka.classifiers.Classifier) SerializationHelper
    // .read(getFileName());
    // setClassifier(trainedClassifier);
    // } catch (Exception e) {
    // LOGGER.error(e);
    // }
    // }

    // public void loadTrainedClassifier(String filePath) {
    // try {
    // weka.classifiers.Classifier trainedClassifier = (weka.classifiers.Classifier) SerializationHelper
    // .read(filePath);
    // setClassifier(trainedClassifier);
    // } catch (Exception e) {
    // LOGGER.error(e);
    // }
    // }

    /**
     * Simply save the trained classifier.
     * 
     * TODO pull up?
     */
    // public void saveTrainedClassifier() {
    // try {
    // SerializationHelper.write(getFileName(), getClassifier());
    // } catch (Exception e) {
    // LOGGER.error(e);
    // }
    // }

//    public void load(String filePath) {
//        try {
//            weka.classifiers.Classifier trainedClassifier = (weka.classifiers.Classifier) SerializationHelper
//                    .read(filePath);
//            setClassifier(trainedClassifier);
//        } catch (Exception e) {
//            LOGGER.error(e);
//        }
//    }
//
//    public void save(String filePath) {
//        try {
//            SerializationHelper.write(filePath, getClassifier());
//        } catch (Exception e) {
//            LOGGER.error(e);
//        }
//    }

//    /**
//     * Get the file name for serializing/deserializing the trained classifier.
//     * 
//     * TODO pull up?
//     * 
//     * @return
//     */
//    public String getFileName() {
//        return "data/models/CandidateClassifier_" + getChosenClassifierName() + ".model";
//    }

//    @Override
//    public void trainClassifier(String filePath) {
//        // trainingObjects = readFeatureObjects(filePath, true);
//        boolean hasHeaderRow = true;
//        boolean hasIdColumn = false;
//        trainingObjects = readFeatureObjects(filePath, hasHeaderRow, hasIdColumn);
//        trainClassifier();
//    }

//    /**
//     * Load feature objects from a file. The first column is ingored, as it contains IDs.
//     * TODO pull this up to Classifier?
//     * 
//     * @param filePath The file with the training data.
//     * @param readFeatureNames Read the names of the features from the first line in the file.
//     * @return A list with the feature objects.
//     */
//    public ArrayList<FeatureObject> readFeatureObjects(String filePath, boolean readFeatureNames) {
//        ArrayList<FeatureObject> featureObjects = new ArrayList<FeatureObject>();
//
//        try {
//            FileReader in = new FileReader(filePath);
//            BufferedReader br = new BufferedReader(in);
//
//            String line = "";
//            String[] featureNames = null;
//            do {
//                line = br.readLine();
//                if (line == null) {
//                    break;
//                }
//
//                // skip comment lines
//                if (line.isEmpty() || line.startsWith("#")) {
//                    continue;
//                }
//
//                String[] featureStrings = line.split(";");
//                Double[] features = new Double[featureStrings.length];
//
//                if (featureNames == null) {
//                    // assume, that first line contains feature names
//                    if (readFeatureNames) {
//                        featureNames = new String[featureStrings.length];
//                        for (int i = 0; i < featureStrings.length; i++) {
//                            featureNames[i] = featureStrings[i];
//                        }
//                        continue;
//                    } else {
//                        featureNames = new String[featureStrings.length];
//                    }
//                }
//
//                for (int i = 0; i < featureStrings.length; i++) {
//                    features[i] = Double.valueOf(featureStrings[i]);
//                }
//                FeatureObject fo = new FeatureObject(features, featureNames);
//                featureObjects.add(fo);
//
//            } while (line != null);
//
//            in.close();
//            br.close();
//
//        } catch (FileNotFoundException e) {
//            LOGGER.error(filePath, e);
//        } catch (IOException e) {
//            LOGGER.error(filePath, e);
//        } catch (OutOfMemoryError e) {
//            LOGGER.error(filePath, e);
//        }
//
//        return featureObjects;
//    }

    public static void main(String[] args) {

        String filePath = "data/temp/KeyphraseExtractorTraining.csv";

        CandidateClassifier c = new CandidateClassifier();
        c.trainClassifier(filePath);
        c.saveTrainedClassifier("neuralnet_classifier.ser");

        System.out.println(c.getClassifier());

    }

}
