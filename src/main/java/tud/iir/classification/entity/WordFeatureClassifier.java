package tud.iir.classification.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureEvaluator;
import tud.iir.classification.FeatureObject;
import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Extractable;
import tud.iir.persistence.DatabaseManager;

/**
 * The WordFeatureClassifier uses only string features from the entity name to assess or classify it.
 * 
 * @author David Urbansky
 */
public class WordFeatureClassifier extends EntityAssessor {

    private FeatureEvaluator featureEvaluator = null;
    // private PreparedStatement psGetFeatures = null;

    // the features that should be used for training and classification (database fields)
    private String[] features = null;

    // training entities
    private HashSet<Entity> trainingEntities = new HashSet<Entity>();

    // testing entities
    private HashSet<Entity> testingEntities = new HashSet<Entity>();

    public WordFeatureClassifier() {

        features = new String[6];
        features[0] = "LENGTH(name) AS length";
        features[1] = "wordCount(name) AS wordCount";
        features[2] = "avgWordLength(name) AS wordLength";
        features[3] = "startsWithNumber(name) AS numericStart";
        features[4] = "endsWithNumber(name) AS numericEnd";
        features[5] = "countNumbers(name) AS numericCount";

        // try {
        StringBuilder featureQuery = new StringBuilder();
        for (String feature : features) {
            featureQuery.append(feature).append(",");
        }
        featureQuery.deleteCharAt(featureQuery.length() - 1);
        System.out.println(featureQuery.toString());
        // psGetFeatures = dbm.getConnection().prepareStatement("SELECT entities.id AS id, " + featureQuery.toString() +
        // " FROM `training_samples` WHERE entityID = ?");
        // } catch (SQLException e) {
        // logger.error(e.getMessage());
        // }
    }

    /**
     * Evaluate the algorithm by classifying entities with a score above the threshold as true and calculating precision and recall using the test entities.
     */
    @Override
    protected void evaluate() {

        long t1 = System.currentTimeMillis();

        HashSet<Concept> concepts = new HashSet<Concept>();
        concepts.add(new Concept("University"));
        concepts.add(new Concept("Actor"));
        concepts.add(new Concept("Airplane"));
        concepts.add(new Concept("Animal"));
        concepts.add(new Concept("Band"));
        concepts.add(new Concept("Fish"));
        concepts.add(new Concept("Board Game"));
        concepts.add(new Concept("Mineral"));
        concepts.add(new Concept("Plant"));
        concepts.add(new Concept("Song"));
        concepts.add(new Concept("City"));
        concepts.add(new Concept("Video Game"));
        concepts.add(new Concept("Movie"));
        concepts.add(new Concept("Guitar"));
        concepts.add(new Concept("Insect"));
        concepts.add(new Concept("Perfume"));
        concepts.add(new Concept("Mobile Phone"));
        concepts.add(new Concept("Island"));
        concepts.add(new Concept("TV Show"));
        concepts.add(new Concept("Mountain"));
        concepts.add(new Concept("Car"));
        concepts.add(new Concept("Company"));
        concepts.add(new Concept("Sport"));
        concepts.add(new Concept("Printer"));
        concepts.add(new Concept("Drug"));

        // if classification probability is above the threshold the entity is considered to be correct
        double trustThreshold = 0.5;

        featureEvaluator = new FeatureEvaluator(concepts, features);

        // data for the graph, double (percentage of training set,[precision,recall,f1])
        TreeMap<Double, ArrayList<Double>> graphData = new TreeMap<Double, ArrayList<Double>>();

        // entities are loaded once for every concept and reused in subsequent iterations
        HashMap<String, ArrayList<Entity>> entityMap = new HashMap<String, ArrayList<Entity>>();

        // for (double trainingPercentage = trainingPercentageStepSize; trainingPercentage <= 0.901; trainingPercentage += trainingPercentageStepSize) {
        double trainingPercentage = 0.2;
        for (trustThreshold = 0; trustThreshold <= 1.01; trustThreshold += trustThresholdStepSize) {

            logger.info("################################## start new iteration ##################################");

            double trainingSampleFrequency = 1.0 / trainingPercentage;

            logger.info("use " + concepts.size() + " concepts for evaluation");
            logger.info("training percentage: " + trainingPercentage);
            logger.info("trust threshold: " + trustThreshold + "\n");

            // save the evaluation metrics: concept name : [precision,recall,f1,correctAssigned,realCorrect,totalAssignedCorrect]
            HashMap<String, ArrayList<Double>> evaluationMetrics = new HashMap<String, ArrayList<Double>>();

            for (Concept concept : concepts) {

                concept.setID(dbm.getConceptID(concept.getName()));

                ArrayList<Entity> entities = entityMap.get(concept.getName());
                if (entities == null) {
                    entities = DatabaseManager.getInstance().loadEvaluationEntities(concept);
                    entityMap.put(concept.getName(), entities);
                }

                logger.info("loaded evaluation entities (" + entities.size() + ")");

                // set training and testing field in database table training_samples.test (feature evaluator depends on that)
                trainingEntities.clear();
                testingEntities.clear();
                int count = 0;
                for (Entity entity : entities) {

                    if ((count % trainingSampleFrequency) < 1) {

                        entity.setType(Extractable.TRAINING);
                        dbm.setTestField(entity.getID(), false);
                        trainingEntities.add(entity);

                    } else {

                        entity.setType(Extractable.TESTING);
                        dbm.setTestField(entity.getID(), true);
                        testingEntities.add(entity);

                    }

                    count++;
                }

                // get cfc for the current concept, the call must happen here because the cfc depends on the test field in training_samples
                Map<Integer, Classifier> cfc = featureEvaluator.getClassifierFeatureCombination(concept);
                logger.info("For concept " + concept.getName() + " classifier " + cfc.get(concept.getID()).getChosenClassifier() + " with RMSE of "
                        + cfc.get(concept.getID()).getRMSE() + " and feature combination " + cfc.get(concept.getID()).getFeatureCombination()
                        + " has been found");

                // calculate evaluation metrics
                int totalRealCorrect = 0;
                int totalAssigned = 0;
                int totalCorrect = 0;

                for (Entity testEntity : testingEntities) {
                    // logger.log("test entity: \""+testEntity.getName()+"\"");

                    try {
                        FeatureObject fo = null;

                        PreparedStatement bestFeatureCombination = cfc.get(concept.getID()).getPsClassificationStatementEntity();
                        bestFeatureCombination.setInt(1, testEntity.getID());

                        ResultSet rs = dbm.runQuery(bestFeatureCombination);

                        if (rs.next()) {

                            int columnCount = rs.getMetaData().getColumnCount();

                            Double[] featureValues = new Double[columnCount - 1];
                            String[] featureNames = new String[columnCount - 1];

                            for (int i = 2; i <= columnCount; i++) {
                                featureValues[i - 2] = rs.getDouble(i);
                                featureNames[i - 2] = rs.getMetaData().getColumnLabel(i);
                            }

                            fo = new FeatureObject(featureValues, featureNames);
                        }

                        // if (fo == null) continue;

                        double[] probabilities = cfc.get(concept.getID()).classifySoft(fo);
                        // logger.log("p+: " + probabilities[0]);
                        // logger.log("p-: " + probabilities[1]);

                        if (probabilities[0] > trustThreshold) {
                            totalAssigned++;
                            // logger.logInline(" classify as true");
                            if (testEntity.getTrust() > 0) {
                                totalCorrect++;
                            }
                        } else {
                            // logger.logInline(" classify as false");
                        }
                        if (testEntity.getTrust() > 0) {
                            // logger.log(" and is true");
                            totalRealCorrect++;
                        } else {
                            // logger.log(" and is false");
                        }

                    } catch (SQLException e) {
                        logger.error(e.getMessage());
                    }
                }

                evaluationMetrics.put(concept.getName(), calculateMetrics(totalRealCorrect, totalAssigned, totalCorrect, testingEntities.size()));
            }

            // log all metrics
            // graphData.put(trainingPercentage, logMetrics(concepts,evaluationMetrics));
            graphData.put(trustThreshold, logMetrics(concepts, evaluationMetrics));
        }

        createFlashChartLog(graphData);

        logger.info(DateHelper.getRuntime(t1, System.currentTimeMillis(), true));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        WordFeatureClassifier wfc = new WordFeatureClassifier();
        wfc.evaluate();
    }

}
