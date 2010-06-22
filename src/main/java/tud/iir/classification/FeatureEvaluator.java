package tud.iir.classification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.classification.entity.EntityClassifier;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.knowledge.Concept;
import tud.iir.persistence.DatabaseManager;

/**
 * The FeatureEvaluator can be used to determine the value of features for different classifiers. Different combinations are tested with a training and a
 * testing set. All features must be available from the database and it must be possible to determine using SQL.
 * 
 * @author David Urbansky
 */
public class FeatureEvaluator {

    /** array of database attributes that can be used as features */
    private final String[] features;

    /** array of all SQL queries that can be generated using the features */
    private PreparedStatement[] featureQueries;

    /** array of all SQL queries that can be used to classify entities from a certain concept */
    private PreparedStatement[] classificationQueriesConcept;

    /** array of all SQL queries that can be used to classify a single entity from a certain concept */
    private PreparedStatement[] classificationQueriesEntity;

    /** set of concepts that should be evaluated */
    private Set<Concept> concepts = null;

    public FeatureEvaluator(Set<Concept> concepts, String[] features) {
        super();
        this.features = features;
        this.concepts = concepts;

        createQueries();
    }

    /**
     * Create all feature queries in advance, this speeds up later usage. Also create classification queries in advance.
     */
    private void createQueries() {

        // the training/testing query, replace _XXX_ with a feature string
        String featureQuery = "SELECT entities.id AS id, XXX `training_samples`.class FROM `entities_sources`,`sources`,`entities`,`training_samples` WHERE entities.conceptID = ? AND `entities_sources`.sourceID = `sources`.id AND `entities`.id = `entities_sources`.entityID AND `entities`.id = `training_samples`.entityID AND training_samples.test != ? GROUP BY `entities_sources`.entityID";

        // the classification query for all entities of a concept
        String classificationQueryConcept = "SELECT entities.id AS id, XXX 0 AS class FROM `entities_sources`,`sources`,`entities` WHERE conceptID = ? AND `entities_sources`.sourceID = `sources`.id AND `entities`.id = `entities_sources`.entityID GROUP BY entityID";

        // the classification query for all entities of a concept
        String classificationQueryEntity = "SELECT entities.id AS id, XXX 0 AS class FROM `entities_sources`,`sources`,`entities` WHERE `entities`.id = ? AND `entities_sources`.sourceID = `sources`.id AND `entities`.id = `entities_sources`.entityID GROUP BY entityID";

        Connection dbmc = DatabaseManager.getInstance().getConnection();
        featureQueries = new PreparedStatement[(int) (Math.pow(2, features.length)) - 1];
        classificationQueriesConcept = new PreparedStatement[featureQueries.length];
        classificationQueriesEntity = new PreparedStatement[featureQueries.length];

        Integer[] bits = new Integer[features.length];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = i - 1;
        }

        StringBuilder featureString = new StringBuilder();
        int counter = 0;
        for (int featureCount = 1; featureCount <= features.length; featureCount++) {

            // binomial coefficient of combinations
            int numberOfCombinations = 1;
            if (featureCount < features.length) {
                numberOfCombinations = MathHelper.faculty(features.length)
                        / (MathHelper.faculty(features.length - featureCount) * MathHelper.faculty(featureCount));
            }

            for (int i = 0; i < numberOfCombinations; i++) {

                // increase highest position if possible, else try to increase lower positions
                int c = featureCount - 1;
                int position = 1;
                while (c >= 0) {
                    if (bits[c] < features.length - position) {
                        bits[c]++;

                        // higher bits must be bigger
                        for (int b = 0; b < featureCount - c; b++) {
                            if ((bits.length > c + b + 1) && (bits[c + b + 1] <= bits[c + b])) {
                                bits[c + b + 1] = bits[c + b] + 1;
                            }
                        }
                        break;

                    } else {
                        bits[c] = c;
                    }
                    c--;
                    position++;
                }

                for (int j = 0; j < featureCount; j++) {
                    featureString.append(features[bits[j]]).append(",");
                }

                String featureQueryString = featureQuery.replace("XXX", featureString.toString());
                String classificationQueryStringConcept = classificationQueryConcept.replace("XXX", featureString.toString());
                String classificationQueryStringEntity = classificationQueryEntity.replace("XXX", featureString.toString());
                try {
                    featureQueries[counter] = dbmc.prepareStatement(featureQueryString);
                    classificationQueriesConcept[counter] = dbmc.prepareStatement(classificationQueryStringConcept);
                    classificationQueriesEntity[counter] = dbmc.prepareStatement(classificationQueryStringEntity);
                } catch (SQLException e) {
                    Logger.getRootLogger().error(e.getStackTrace());
                }

                counter++;
                featureString = new StringBuilder();
            }

            for (int i = 1; i <= features.length; i++) {
                bits[features.length - i] = features.length - i;
            }
            if (featureCount < features.length) {
                bits[featureCount]--;
            }
        }

        try {
            dbmc.close();
        } catch (SQLException e) {
            Logger.getRootLogger().error("error closing connection");
        }

        System.out.println("created queries");
    }

    /**
     * Check all feature combinations for the given classifier and the given concept.
     * 
     * @deprecated Only for manual checking.
     * @param classifier The classifier to check all feature combinations with.
     * @param conceptID The id of the concept.
     * @return A list of RMSE scores for the classifier and the concept.
     */
    @Deprecated
    private ArrayList<Double> testFeatures(EntityClassifier classifier, int conceptID) {
        ArrayList<Double> resultArray = new ArrayList<Double>();

        for (int i = 0; i < featureQueries.length; i++) {

            PreparedStatement ps = featureQueries[i];

            try {
                ps.setInt(1, conceptID);

                ps.setInt(2, 1); // for training

                // train and test classifier
                classifier.trainClassifier(conceptID, ps, classificationQueriesConcept[i]);

                ps.setInt(2, 0); // for testing
                classifier.testClassifier(conceptID);

                // write result
                // System.out.println(classifier.getEvaluation().correct());
                // System.out.println(classifier.getEvaluation().toSummaryString());
                // double correct = MathHelper.round(classifier.getEvaluation().pctCorrect() / 100.0, 4);
                double rmse = MathHelper.round(classifier.getEvaluation().rootMeanSquaredError(), 4);
                resultArray.add(rmse);

            } catch (SQLException e) {
                Logger.getRootLogger().error(e.getStackTrace());
            }
        }

        return resultArray;
    }

    /**
     * Evaluate a classifier.
     * 
     * @deprecated Only for manual checking.
     * @param type
     * @return A List lists with RMSE scores for the classifier.
     */
    @Deprecated
    private ArrayList<ArrayList<Double>> evaluateClassifier(int type) {

        DatabaseManager dbm = DatabaseManager.getInstance();

        // this list holds a result list for every concept for the given classifier type
        ArrayList<ArrayList<Double>> conceptClassifierResults = new ArrayList<ArrayList<Double>>();

        EntityClassifier classifier = new EntityClassifier(type);

        for (Concept concept : concepts) {
            int conceptID = dbm.getConceptID(concept.getName());
            System.out.println("Evaluate concept " + conceptID);
            ArrayList<Double> resultList = testFeatures(classifier, conceptID);
            conceptClassifierResults.add(resultList);
        }

        return conceptClassifierResults;
    }

    /**
     * Find the best feature combination for the given concept with the given classifier.
     * 
     * @param type The type of the classifier.
     * @param conceptID The id of the concept for which the best feature combination should be found.
     * @return The classifier with a feature combination resulting in the lowest RMSE value for the given concept.
     */
    private Classifier getFeatureCombination(int type, int conceptID) {

        EntityClassifier bestCFC = new EntityClassifier(type);

        // iterate through all feature combinations, train and test the given classifier for the given concept
        for (int i = 0; i < featureQueries.length; i++) {

            PreparedStatement psTraining = featureQueries[i];
            PreparedStatement psClassificationConcept = classificationQueriesConcept[i];

            double lowestRMSE = 1.0; // binary classification, RMSE can only be in interval [0,1]
            if (bestCFC.getEvaluation() != null) {
                lowestRMSE = bestCFC.getRMSE();
            }

            try {
                psTraining.setInt(1, conceptID);

                psTraining.setInt(2, 0); // for training

                EntityClassifier classifier = new EntityClassifier(type);

                // train and test classifier
                boolean trained = classifier.trainClassifier(conceptID, psTraining, psClassificationConcept);

                // give the classifier the SQL query which can be used to classify a certain entity by ID
                classifier.setPsClassificationStatementEntity(classificationQueriesEntity[i]);

                if (!trained) {
                    continue;
                }

                psTraining.setInt(2, 1); // for testing
                classifier.testClassifier(conceptID);

                double rmse = MathHelper.round(classifier.getRMSE(), 4);

                // if tested feature combination resulted in lower RMSE, save feature combination
                if (rmse < lowestRMSE) {
                    bestCFC = classifier;
                }

                // System.out.println("current rmse " + rmse);

            } catch (SQLException e) {
                Logger.getRootLogger().error(e.getStackTrace());
            }
        }

        return bestCFC;
    }

    /**
     * Evaluate a single classifier for all concepts with all feature combinations. Replace other CFC if new CFC is better (lower RMSE).
     * 
     * @param type The type of the classifier.
     * @param cfc The map of the currently best CFC.
     * @return An updated map of CFC.
     */
    private Map<Integer, Classifier> evaluateClassifierForAllConcepts(int type, Map<Integer, Classifier> cfc) {

        for (Concept concept : concepts) {
            cfc = evaluateClassifier(type, concept, cfc);
        }

        return cfc;
    }

    /**
     * Evaluate a single classifier for one concept with all feature combinations.
     * 
     * @param type
     * @param concept
     * @param cfc
     * @return
     */
    private Map<Integer, Classifier> evaluateClassifier(int type, Concept concept, Map<Integer, Classifier> cfc) {
        DatabaseManager dbm = DatabaseManager.getInstance();

        int conceptID = dbm.getConceptID(concept.getName());
        Classifier currentlyBestCFC = cfc.get(conceptID);
        System.out.println("Evaluate concept " + concept.getName() + "(" + conceptID + ")");

        // get best feature combination for current concept with given classifier type
        Classifier bestCFC = getFeatureCombination(type, conceptID);

        // replace classifier in cfc map if new cfc is better than old one
        if (currentlyBestCFC == null || (bestCFC != null && currentlyBestCFC.getRMSE() > bestCFC.getRMSE())) {
            cfc.put(conceptID, bestCFC);
        }

        return cfc;
    }

    /**
     * CFL algorithm (Classifier Feature Learner) In this algorithm the best classifier with the best feature combination for a given concept is learned.
     * 
     * @param concept The concept for which the cfc should be generated. If null, a cfc for all concepts will be returned.
     * @return A map with the conceptID as key and the best classifier-feature combination for that concept as value.
     */
    public Map<Integer, Classifier> getClassifierFeatureCombination(Concept concept) {

        // the classifier feature combinations conceptID, Classifier (including the feature combination)
        Map<Integer, Classifier> cfc = new HashMap<Integer, Classifier>();

        // select a set of classifiers that should be considered
        Integer[] classifierTypes = { Classifier.BAYES_NET, Classifier.SVM, Classifier.NEURAL_NETWORK };

        // evaluate each of these classifiers on all concepts
        for (int i = 0; i < classifierTypes.length; i++) {
            System.out.println("Evaluate features with classifier " + classifierTypes[i]);
            if (concept == null) {
                cfc = evaluateClassifierForAllConcepts(classifierTypes[i], cfc);
            } else {
                cfc = evaluateClassifier(classifierTypes[i], concept, cfc);
            }
        }

        // output the classifiers that were found for each concept
        for (Map.Entry<Integer, Classifier> entry : cfc.entrySet()) {
            Classifier conceptClassifier = entry.getValue();
            System.out.println("For concept " + entry.getKey() + " classifier " + conceptClassifier.getChosenClassifier() + " with RMSE of "
                    + conceptClassifier.getRMSE() + " and feature combination " + conceptClassifier.getFeatureCombination() + " has been found");
        }

        return cfc;
    }

    public Map<Integer, Classifier> getClassifierFeatureCombination() {
        return getClassifierFeatureCombination(null);
    }

    /**
     * Create a spreadsheet file with all classifier, feature and concept combinations.
     * 
     * @deprecated Only for manual checking since the output is written to a file
     */
    @Deprecated
    @SuppressWarnings("unused")
    private void completeFeatureEvaluation() {
        // this list holds a result list for every concept for the given classifier type
        HashMap<Integer, ArrayList<ArrayList<Double>>> allConceptClassifierResults = new HashMap<Integer, ArrayList<ArrayList<Double>>>();

        Integer[] classifierTypes = { Classifier.BAYES_NET, Classifier.LINEAR_REGRESSION, Classifier.SVM, Classifier.NEURAL_NETWORK };

        for (int i = 0; i < classifierTypes.length; i++) {
            System.out.println("Evaluate features with classifier " + classifierTypes[i]);
            ArrayList<ArrayList<Double>> conceptClassifierResults = evaluateClassifier(classifierTypes[i]);
            allConceptClassifierResults.put(classifierTypes[i], conceptClassifierResults);
        }

        // serialize all results to a csv file
        StringBuilder resultString = new StringBuilder();

        Iterator<Map.Entry<Integer, ArrayList<ArrayList<Double>>>> classifierIterator = allConceptClassifierResults.entrySet().iterator();
        while (classifierIterator.hasNext()) {
            Map.Entry<Integer, ArrayList<ArrayList<Double>>> entry = classifierIterator.next();
            ArrayList<ArrayList<Double>> conceptClassifierResults = entry.getValue();

            for (int i = 0; i < Math.pow(2, features.length) - 1; i++) {
                for (int j = 0; j < conceptClassifierResults.size(); j++) {
                    ArrayList<Double> resultArray = conceptClassifierResults.get(j);
                    double correct = resultArray.get(i);
                    resultString.append(correct).append(";");
                }
                resultString.append("\n");
            }

        }

        FileHelper.writeToFile("data/reports/" + DateHelper.getCurrentDatetime() + "featureEvaluator.csv", resultString);
    }

    /**
     * For concept 17 classifier 1 with RMSE of 0.22196034583404162 and feature combination length sources entityTrust class has been found For concept 1
     * classifier 1 with RMSE of 0.21799234196876005 and feature combination sourceTrust class has been found For concept 18 classifier 3 with RMSE of 0.0 and
     * feature combination length class has been found For concept 3 classifier 3 with RMSE of 0.22360679774997896 and feature combination length wordCount
     * wordLength numericStart numericCount sources extractionTypes class has been found For concept 6 classifier 3 with RMSE of 0.31622776601683794 and feature
     * combination wordLength numericCount sourceTrust entityTrust class has been found For concept 8 classifier 3 with RMSE of 0.0 and feature combination
     * wordCount numericCount class has been found For concept 10 classifier 1 with RMSE of 0.2914830673506133 and feature combination wordCount class has been
     * found For concept 12 classifier 3 with RMSE of 0.0 and feature combination entityTrust class has been found For concept 13 classifier 3 with RMSE of 0.0
     * and feature combination wordCount wordLength class has been found For concept 15 classifier 1 with RMSE of 0.1835495977760554 and feature combination
     * wordCount wordLength numericStart numericEnd numericCount sourceTrust class has been found
     */
    public static void main(String[] args) {

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

        // fill array with features that can be queried from the database
        String[] features = new String[6];
        features[0] = "LENGTH(name) AS length";
        features[1] = "wordCount(name) AS wordCount";
        features[2] = "avgWordLength(name) AS wordLength";
        features[3] = "startsWithNumber(name) AS numericStart";
        features[4] = "endsWithNumber(name) AS numericEnd";
        features[5] = "countNumbers(name) AS numericCount";
        // features[6] = "COUNT(`entities_sources`.entityID) AS sources";
        // features[7] = "COUNT(DISTINCT extractionType) AS extractionTypes";
        // features[8] = "SUM(entityTrust) AS sourceTrust";
        // features[9] = "entities.trust AS entityTrust";

        // new FeatureEvaluator().test();
        // new FeatureEvaluator().testFeatures(null, 1);
        long t1 = System.currentTimeMillis();
        // new FeatureEvaluator().completeFeatureEvaluation();
        new FeatureEvaluator(concepts, features).getClassifierFeatureCombination();
        DateHelper.getRuntime(t1, System.currentTimeMillis(), true);

        // new FeatureEvaluator().testFeatures(new Classifier(Classifier.LINEAR_REGRESSION), 3);
    }

}