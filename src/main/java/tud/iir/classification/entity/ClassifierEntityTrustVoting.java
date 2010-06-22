package tud.iir.classification.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureEvaluator;
import tud.iir.classification.FeatureObject;
import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.KnowledgeManager;

/**
 * 
 */
public class ClassifierEntityTrustVoting extends EntityTrustVoting implements EntityTrustVotingInterface {

    private PreparedStatement psGetFeatures = null;
    private PreparedStatement psSetEntityClass = null;

    public ClassifierEntityTrustVoting() {
        try {
            psGetFeatures = dbm
                    .getConnection()
                    .prepareStatement(
                            "SELECT entities.id AS id, LENGTH(name) AS length, wordCount(name) AS wordCount,startsWithNumber(name) AS numericStart, endsWithNumber(name) AS numericEnd, COUNT(entityID) AS sources, COUNT(DISTINCT extractionType) AS extractionTypes, SUM(entityTrust) AS sourceTrust FROM `entities_sources`,`sources`,`entities` WHERE conceptID = ? AND `entities_sources`.sourceID = `sources`.id AND `entities`.id = `entities_sources`.entityID GROUP BY entityID");
            psSetEntityClass = dbm.getConnection().prepareStatement("UPDATE `entities` SET class = ? WHERE id = ?");
        } catch (SQLException e) {
            logger.error("create prepared statements", e);
        }
    }

    /**
     * Start classification using the best performing classifiers and feature combinations for each concept. :::: runtime: 27424.0 seconds, 04/04/2009
     */
    public void runVoting() {
        runVoting(-1);
    }

    public void runVoting(int classifierType) {

        logger.info("run voting with classifier type " + classifierType);

        String[] featureString = new String[6];
        featureString[0] = "LENGTH(name) AS length";
        featureString[1] = "wordCount(name) AS wordCount";
        featureString[2] = "avgWordLength(name) AS wordLength";
        featureString[3] = "startsWithNumber(name) AS numericStart";
        featureString[4] = "endsWithNumber(name) AS numericEnd";
        featureString[5] = "countNumbers(name) AS numericCount";
        featureString[6] = "COUNT(`entities_sources`.entityID) AS sources";
        featureString[7] = "COUNT(DISTINCT extractionType) AS extractionTypes";
        featureString[8] = "SUM(entityTrust) AS sourceTrust";
        featureString[9] = "entities.trust AS entityTrust";

        KnowledgeManager km = dbm.loadOntology();
        ArrayList<Concept> concepts = km.getConcepts();

        HashSet<Concept> conceptSet = new HashSet<Concept>();
        for (Concept concept : concepts) {
            conceptSet.add(concept);
        }

        Map<Integer, Classifier> cfc = null;
        if (classifierType == -1) {
            cfc = new FeatureEvaluator(conceptSet, featureString).getClassifierFeatureCombination();
            logger.info("cfc found");
        }

        for (Concept concept : concepts) {
            int conceptID = dbm.getConceptID(concept.getName());

            logger.info("start classifying entities of concept " + concept.getName() + " (" + conceptID + ")");

            // train classifier
            EntityClassifier classifier = null;
            if (classifierType == -1) {

                // get best matching cfc (trained already)
                classifier = (EntityClassifier) cfc.get(conceptID);

                try {
                    PreparedStatement classificationStatement = classifier.getPsClassificationStatementConcept();
                    classificationStatement.setInt(1, conceptID);

                    // retrain classifier also with training and testing set
                    logger.info("retrain classifier " + classifier.getChosenClassifier());
                    PreparedStatement trainingStatement = classifier.getPsFeatureStatement();
                    trainingStatement.setInt(1, conceptID);
                    trainingStatement.setInt(2, 2);
                    classifier.trainClassifier(conceptID, trainingStatement, classificationStatement);

                    logger.info("classifier " + classifier.getChosenClassifier() + " is going to be used. RMSE: " + classifier.getRMSE());

                    logger.info(classificationStatement.toString());

                    ResultSet rs = dbm.runQuery(classificationStatement);

                    dbm.getConnection().setAutoCommit(false);

                    int counter = 0;
                    while (rs.next()) {

                        int columnCount = rs.getMetaData().getColumnCount();

                        Double[] features = new Double[columnCount - 1];
                        String[] featureNames = new String[columnCount - 1];

                        // start with 2 to skip "id" field
                        for (int i = 2; i <= columnCount; i++) {
                            features[i - 2] = rs.getDouble(i);
                            featureNames[i - 2] = rs.getMetaData().getColumnLabel(i);
                        }

                        FeatureObject fo = new FeatureObject(features, featureNames);
                        boolean positive = classifier.classifyBinary(fo, false);
                        if (positive) {
                            psSetEntityClass.setInt(1, 1);
                        } else {
                            psSetEntityClass.setInt(1, 0);
                        }

                        if (counter % 1000 == 0) {
                            logger.info("classified next 1000 entities, " + rs.getInt("id"));
                        }

                        psSetEntityClass.setInt(2, rs.getInt("id"));
                        dbm.runUpdate(psSetEntityClass);
                        ++counter;
                    }

                    if (!dbm.getConnection().getAutoCommit()) {
                        dbm.getConnection().commit();
                        dbm.getConnection().setAutoCommit(true);
                    }

                    rs.close();
                    rs = null;
                    logger.info("all entities of concept " + conceptID + " have been classified");

                } catch (SQLException e) {
                    logger.error("Error: ", e);
                }

            } else {
                classifier = new EntityClassifier(classifierType);

                try {
                    PreparedStatement psTraining = dbm
                            .getConnection()
                            .prepareStatement(
                                    "SELECT entities.id AS id, LENGTH(name) AS length, wordCount(name) AS wordCount,startsWithNumber(name) AS numericStart, endsWithNumber(name) AS numericEnd, COUNT(entityID) AS sources, COUNT(DISTINCT extractionType) AS extractionTypes, SUM(entityTrust) AS sourceTrust, `training_samples`.class FROM `entities_sources`,`sources`,`entities`,`training_samples` WHERE conceptID = ? AND `entities_sources`.sourceID = `sources`.id AND `entities`.id = `entities_sources`.entityID AND `entities`.id = `training_samples`.entityID AND training_samples.test = ? GROUP BY `entities_sources`.entityID");
                    psTraining.setInt(1, conceptID);
                    psTraining.setInt(2, 2);
                    classifier.trainClassifier(conceptID, psTraining, psGetFeatures);
                    logger.info("classifier trained for concept " + conceptID);

                    psGetFeatures.setInt(1, conceptID);
                    ResultSet rs = dbm.runQuery(psGetFeatures);

                    dbm.getConnection().setAutoCommit(false);

                    while (rs.next()) {
                        Double[] features = new Double[8];
                        features[0] = rs.getDouble("length");
                        features[1] = rs.getDouble("wordCount");
                        features[2] = rs.getDouble("numericStart");
                        features[3] = rs.getDouble("numericEnd");
                        features[4] = rs.getDouble("sources");
                        features[5] = rs.getDouble("extractionTypes");
                        features[6] = rs.getDouble("sourceTrust");
                        features[7] = 0.0;

                        FeatureObject fo = new FeatureObject(features, new String[0]);
                        boolean positive = classifier.classifyBinary(fo, false);
                        if (positive) {
                            psSetEntityClass.setDouble(1, 1);
                        } else {
                            psSetEntityClass.setDouble(1, 0);
                        }

                        psSetEntityClass.setInt(2, rs.getInt("id"));
                        dbm.runUpdate(psSetEntityClass);
                    }

                    if (!dbm.getConnection().getAutoCommit()) {
                        dbm.getConnection().commit();
                        dbm.getConnection().setAutoCommit(true);
                    }

                    rs.close();
                    rs = null;
                    logger.info("all entities of concept " + conceptID + " classified");

                } catch (SQLException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("start classification of entities...");
        long t1 = System.currentTimeMillis();
        ClassifierEntityTrustVoting fetv = new ClassifierEntityTrustVoting();
        fetv.runVoting();
        DateHelper.getRuntime(t1);

        System.exit(0);
        int conceptID = 10;
        double tt = fetv.findTrustThreshold(conceptID);
        System.out.println("The trust threshold for concept " + conceptID + " is: " + tt);
    }
}