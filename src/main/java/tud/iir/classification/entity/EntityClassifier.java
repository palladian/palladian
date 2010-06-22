package tud.iir.classification.entity;

import java.sql.PreparedStatement;

import tud.iir.classification.Classifier;

public class EntityClassifier extends Classifier {

    public EntityClassifier(int type) {
        super(type);
    }

    /**
     * Train a classifier with the samples save in the database. The classifier is trained on a concept level.
     * 
     * @param conceptID The id of the concept for which the classifier should be trained.
     * @param featureString The SQL query string with the desired features to train the classifier.
     */
    public boolean trainClassifier(int conceptID, PreparedStatement featureString) {
        setPsFeatureStatement(featureString);

        // load training data
        trainingObjects = readFeatureObjects(conceptID, featureString);
        return trainClassifier();
    }

    public boolean trainClassifier(int conceptID, PreparedStatement featureString, PreparedStatement classificationString) {
        setPsFeatureStatement(featureString);
        setPsClassificationStatementConcept(classificationString);

        // load training data
        trainingObjects = readFeatureObjects(conceptID, featureString);
        return trainClassifier();
    }

}
