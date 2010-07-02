package tud.iir.classification.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;

import tud.iir.helper.DateHelper;
import tud.iir.helper.MathHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Extractable;
import tud.iir.knowledge.Source;

/**
 * The Random Graph Walk assesses supervised information extraction. Algorithm similar to the one explained in
 * <ul>
 * <li>Language Independent Set Expansion of Named Entities Using the Web, 2007</li>
 * <li>Automatic Set Instance Extraction using the Web, 2009</li>
 * <li>Iterative Set Expansion of Named Entities using the Web, 2008</li>
 * </ul>
 * 
 * @author David Urbansky
 */

public class RandomGraphWalk extends EntityAssessor {

    // adjacency matrix Mxy
    private Array2DRowRealMatrix Mxy = null;

    // probability vector vt
    private ArrayRealVector vt = null;

    // probability vector at t = 0
    private ArrayRealVector v0 = null;

    // indices of test entities (use for evaluation of algorithm), [index,true|false]
    private HashMap<Integer, Boolean> testingEntities = new HashMap<Integer, Boolean>();

    // indices of training entities (use for threshold determination of algorithm), [index,true|false]
    private HashMap<Integer, Boolean> trainingEntities = new HashMap<Integer, Boolean>();

    // entities that are used as seeds
    private ArrayList<Entity> trainingEntitiesSeeds = new ArrayList<Entity>();

    // entities are loaded once for every concept and reused in subsequent iterations
    HashMap<String, ArrayList<Entity>> entityMap = new HashMap<String, ArrayList<Entity>>();

    // remember index and entity id in the adjacency matrix
    HashMap<Integer, Entity> entityMatrixMappingInverse = new HashMap<Integer, Entity>();

    // id map for calculating matrix (keep in memory because the same data is loaded several times from the database)
    // relation name,{id:[relatedID1,relatedIDN]}
    HashMap<String, HashMap<String, HashSet<Integer>>> idMap = new HashMap<String, HashMap<String, HashSet<Integer>>>();

    // µ, the probability to jump back to the seeds
    private double m = 0.5;

    public RandomGraphWalk() {
        // Concept concept = new Concept("University");
        // buildAdjacencyMatrix(concept);

        // fill idMap with possible relations
        idMap.put("extractionTypesForSource", new HashMap<String, HashSet<Integer>>());
        idMap.put("sourceEntities", new HashMap<String, HashSet<Integer>>());
        idMap.put("sourcesForExtractionType", new HashMap<String, HashSet<Integer>>());
        idMap.put("entitiesForExtractionType", new HashMap<String, HashSet<Integer>>());

    }

    /**
     * 
     * @param entities A list of entities.
     * @param trainingPercentage Percentage of entities that should be used for training (control or seed).
     * @param seedPercentage Percentage of the trainingPercentage that should be used as seeds.
     */
    private void initSeedProbabilities(ArrayList<Entity> entities, double trainingPercentage, double seedPercentage) {
        // v0 = new ArrayRealVector(entities.size());

        // clean up training and testing sets
        trainingEntities.clear();
        trainingEntitiesSeeds.clear();
        testingEntities.clear();

        // initialize all nodes with trust of 0.5
        for (int i = 0; i < v0.getDimension(); i++) {
            v0.setEntry(i, 0.5);
        }

        double trainingSampleFrequency = 1.0 / trainingPercentage;
        double seedSampleFrequency = trainingSampleFrequency / seedPercentage;

        int vectorIndex = 0;
        for (Entity entity : entities) {

            if (vectorIndex % trainingSampleFrequency < 1) {
                entity.setType(Extractable.TRAINING);

                // System.out.println(entity.getTrust());

                // positive entities
                if (entity.getTrust() == 1) {

                    if (vectorIndex % seedSampleFrequency >= 1) {
                        trainingEntities.put(vectorIndex, true);
                    } else {
                        v0.setEntry(vectorIndex, 1.0);
                        trainingEntitiesSeeds.add(entity);
                    }

                    // negative entities
                } else {

                    if (vectorIndex % seedSampleFrequency >= 1) {
                        trainingEntities.put(vectorIndex, false);
                    } else {
                        v0.setEntry(vectorIndex, 0.0); // TODO ?
                        trainingEntitiesSeeds.add(entity);
                    }

                }

            } else {
                entity.setType(Extractable.TESTING);
                if (entity.getTrust() == 1) {
                    testingEntities.put(vectorIndex, true);
                    // testingEntities.put(entity,true);
                } else {
                    testingEntities.put(vectorIndex, false);
                    // testingEntities.put(entity,false);
                }
            }

            vectorIndex++;
        }

        System.out.println("Seed Entities actually used: " + trainingEntitiesSeeds.size() + ", " + seedSampleFrequency);
        System.out.println("Training Entities actually used: " + trainingEntities.size() + ", " + trainingSampleFrequency);
        System.out.println("Test Entities actually used: " + testingEntities.size() + "\n");

        /*
         * int vectorIndex = 0; for (Entity entity : entities) { if (entity.getType() == Extractable.TRAINING) { if (Math.random() < 0.5) { if
         * (entity.getTrust() == 1) { trainingEntities.put(vectorIndex,true); //trainingEntitiesControl.put(entity,true); } else {
         * trainingEntities.put(vectorIndex,false); //trainingEntitiesControl.put(entity,false); } } else if (entity.getTrust() == 1) { v0.setEntry(vectorIndex,
         * 1.0); trainingEntitiesSeeds.add(entity); } } else if (entity.getType() == Extractable.TESTING) { if (entity.getTrust() == 1) {
         * testEntities.put(vectorIndex,true); testingEntities.put(entity,true); } else { testEntities.put(vectorIndex,false);
         * testingEntities.put(entity,false); } } vectorIndex++; }
         */
    }

    private void buildAdjacencyMatrix(Concept concept, double trainingPercentage, double seedPercentage) {

        // the index in the matrix (i or j)
        int matrixIndex = 0;

        // TODO load entities
        // ArrayList<Entity> entities = DatabaseManager.getInstance().loadEvaluationEntities(concept);

        // load the entities for each concept only once
        ArrayList<Entity> entities = entityMap.get(concept.getName());
        if (entities == null) {
            entities = dbm.loadEvaluationEntities(concept);
            entityMap.put(concept.getName(), entities);
        }

        // save the mapping of entityID to the index in the matrix
        HashMap<Integer, Integer> entityMatrixMapping = new HashMap<Integer, Integer>();

        for (Entity entity : entities) {
            entityMatrixMapping.put(entity.getID(), matrixIndex);
            entityMatrixMappingInverse.put(matrixIndex, entity);
            matrixIndex++;
        }

        // save the mapping of sourceID to the index in the matrix
        HashMap<Integer, Integer> sourceMatrixMapping = new HashMap<Integer, Integer>();
        HashSet<Source> sources = new HashSet<Source>();

        for (Entity entity : entities) {
            for (Source source : entity.getSources()) {
                if (!sourceMatrixMapping.containsKey(source.getID())) {
                    sourceMatrixMapping.put(source.getID(), matrixIndex++);
                    sources.add(source);
                }
            }
        }

        // int numberOfSources = sourceMatrixMapping.size();

        // save the mapping of entityExtractionID to the index in the matrix
        HashMap<Integer, Integer> extractionTypeMatrixMapping = new HashMap<Integer, Integer>();

        for (Entity entity : entities) {
            for (Integer extractionType : entity.getExtractionTypes()) {
                if (!extractionTypeMatrixMapping.containsKey(extractionType)) {
                    extractionTypeMatrixMapping.put(extractionType, matrixIndex++);
                }
            }
        }

        // int numberOfExtractionTypes = extractionTypeMatrixMapping.size();

        // initialize the matrix and vectors
        int n = matrixIndex;// entities.size() + numberOfSources + numberOfExtractionTypes;

        Mxy = new Array2DRowRealMatrix(n, n);
        v0 = new ArrayRealVector(n);
        vt = new ArrayRealVector(n);

        initSeedProbabilities(entities, trainingPercentage, seedPercentage);

        // // fill matrix entries which are not 0, entities can be connected to sources and extraction types and vice versa
        int c = 0;

        // remember all sources that have been mapped to entities already
        HashSet<Integer> sourcesMapped = new HashSet<Integer>();

        // remember all extraction types that have been mapped to entities already
        HashSet<Integer> extractionTypesMapped = new HashSet<Integer>();

        // connect entities, sources and extraction types
        // entity line: number of sources, number of extraction types
        // source line: number of entities, number of extraction types
        // extraction type line: number of entities, number of sources
        for (Entity entity : entities) {

            int matrixIndexEntity = entityMatrixMapping.get(entity.getID());

            HashSet<Source> sources2 = entity.getSources();
            HashSet<Integer> extractionTypes = entity.getExtractionTypes();

            // get connections to sources TODO use extraction types of source (hibernate manyToMany?)
            for (Source source : sources2) {

                // entity line
                int matrixIndexSource = sourceMatrixMapping.get(source.getID());

                // i = entityIndex, j = sourceIndex
                Mxy.setEntry(matrixIndexEntity, matrixIndexSource, 1.0 / ((double) sources2.size() + (double) extractionTypes.size()));

                if (!sourcesMapped.add(source.getID())) {
                    continue;
                }

                // source line
                HashSet<Integer> extractionTypesForSource = idMap.get("extractionTypesForSource").get(source.getID() + concept.getName());
                if (extractionTypesForSource == null) {
                    extractionTypesForSource = dbm.getExtractionTypesForSource(source.getID(), concept);
                    idMap.get("extractionTypesForSource").put(source.getID() + concept.getName(), extractionTypesForSource);
                    System.out.println("had to load from database " + source.getID() + concept.getName());
                }

                HashSet<Integer> sourceEntities = idMap.get("sourceEntities").get(source.getID());
                if (sourceEntities == null) {
                    sourceEntities = dbm.getEntitiesForSource(source.getID());
                    idMap.get("sourceEntities").put(String.valueOf(source.getID()), sourceEntities);
                    System.out.println("had to load from database " + source.getID());
                }

                // i = sourceIndex, j = entityIndex
                // TODO do it this way: HashSet<Entity> sourceEntities = sources2.getEntities();
                for (Integer sourceEntityID : sourceEntities) {
                    int matrixIndexSourceEntity = entityMatrixMapping.get(sourceEntityID);
                    Mxy.setEntry(matrixIndexSource, matrixIndexSourceEntity, 1.0 / ((double) sourceEntities.size() + (double) extractionTypesForSource.size()));
                }

                // i = sourceIndex, j = extractionTypeIndex
                for (Integer extractionType : extractionTypesForSource) {
                    int matrixIndexExtractionType = extractionTypeMatrixMapping.get(extractionType);
                    Mxy.setEntry(matrixIndexSource, matrixIndexExtractionType,
                            1.0 / ((double) sourceEntities.size() + (double) extractionTypesForSource.size()));

                }

                // TODO do it this way: HashSet<Integer> extractionTypes = source.getExtractionTypes();
            }

            // get connections to extraction types
            for (Integer extractionType : extractionTypes) {

                // extraction type line
                int matrixIndexExtractionType = extractionTypeMatrixMapping.get(extractionType);

                // i = entityIndex, j = extractionTypeIndex
                Mxy.setEntry(matrixIndexEntity, matrixIndexExtractionType, 1.0 / ((double) sources2.size() + (double) extractionTypes.size()));

                if (!extractionTypesMapped.add(extractionType)) {
                    continue;
                }

                HashSet<Integer> sourcesForExtractionType = idMap.get("sourcesForExtractionType").get(extractionType + concept.getName());
                if (sourcesForExtractionType == null) {
                    sourcesForExtractionType = dbm.getSourcesForExtractionType(extractionType, concept);
                    idMap.get("sourcesForExtractionType").put(extractionType + concept.getName(), sourcesForExtractionType);
                    System.out.println("had to load from database " + extractionType + concept.getName());
                }

                HashSet<Integer> entitiesForExtractionType = idMap.get("entitiesForExtractionType").get(extractionType + concept.getName());
                if (entitiesForExtractionType == null) {
                    entitiesForExtractionType = dbm.getEntitiesForExtractionType(extractionType, concept);
                    idMap.get("entitiesForExtractionType").put(extractionType + concept.getName(), entitiesForExtractionType);
                    System.out.println("had to load from database " + extractionType + concept.getName());
                }

                // i = extractionTypeIndex, j = entityIndex
                for (Integer extractionTypeEntityID : entitiesForExtractionType) {
                    int matrixIndexExtractionTypeEntity = entityMatrixMapping.get(extractionTypeEntityID);
                    Mxy.setEntry(matrixIndexExtractionType, matrixIndexExtractionTypeEntity,
                            1.0 / ((double) entitiesForExtractionType.size() + (double) sourcesForExtractionType.size()));
                }

                // i = extractionTypeIndex, j = sourceIndex
                for (Integer extractionTypeSourceID : sourcesForExtractionType) {
                    int matrixIndexExtractionTypeSource = sourceMatrixMapping.get(extractionTypeSourceID);
                    Mxy.setEntry(matrixIndexExtractionType, matrixIndexExtractionTypeSource,
                            1.0 / ((double) entitiesForExtractionType.size() + (double) sourcesForExtractionType.size()));
                }
            }
            System.out.println("loaded object " + c++);
        }

        System.out.println("matrix built completed");

        // check if each line sums up to 1
        /*
         * for (int i = 0; i < Mxy.getRowDimension(); i++) { double sum = 0.0; for (int j = 0; j < Mxy.getColumnDimension(); j++) { sum += Mxy.getEntry(i, j); }
         * System.out.println(i+":"+sum); } System.exit(0);
         */
    }

    /**
     * Calculate the vector that holds probabilities for all nodes in the graph. The probabilities indicate how similar the node is to the seed nodes. The
     * vector updates t times and should converge. v0 is initialized with probabilities uniformly distributed over seed nodes. The adjacency matrix is build
     * using this formula for every node x and y:
     * 
     * Mxy = SUM(P(r|x)*P(y|r,x)) with: P(r|x) = 1 / number of outgoing edges from x of type r P(y|r,x) = 1 / number of nodes y that can be reached from x using
     * relation r
     * 
     * vt+1 = µ*v0 + (1-µ) * Mxy*vt
     * 
     */
    private void buildProbabilityVector(int timeSteps) {

        System.out.println(v0.toString());
        System.out.println("build probability vector\n" + vt.toString());

        // ! transposing the matrix means that scores of neighbor nodes are distributed by their number of outgoing connections, the result is a score and not a
        // probability
        // Mxy = (Array2DRowRealMatrix) Mxy.transpose();

        for (int i = 0; i < timeSteps; i++) {
            vt = (ArrayRealVector) v0.mapMultiply(m).add(Mxy.operate(vt).mapMultiply(1 - m));
            if (i < 10) {
                System.out.println(i + ":" + vt.toString());
            }
        }

        System.out.println("probability vector built completed\ndimension: " + vt.getDimension() + "," + vt.toString());
    }

    /**
     * Evaluate the algorithm by classifying entities with a score above the threshold as true and calculating precision and recall using the test entities.
     */
    @Override
    public void evaluate() {

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

        // the classification threshold is learned using the training entities, the bias can be used to shift the threhold towards the lower bound (0) or the
        // upper bound (1), 0.5 means the learned threshold is exactly the middle between average positive and average negative trust
        double trustThresholdBias = 0.5;

        // data for the graph, double (percentage of training set,[precision,recall,f1])
        TreeMap<Double, ArrayList<Double>> graphData = new TreeMap<Double, ArrayList<Double>>();

        // what percentage of the training entities should be used for tuning
        double seedPercentage = 0.2;

        // what percentage of the entities should be used for training
        // for (double trainingPercentage = trainingPercentageStepSize; trainingPercentage <= 0.901; trainingPercentage += trainingPercentageStepSize) {
        double trainingPercentage = 0.2;
        for (trustThresholdBias = 0.0; trustThresholdBias <= 1; trustThresholdBias += trustThresholdStepSize) {

            logger.info("################################## start new iteration ##################################");

            // save the evaluation metrics: concept name : [precision,recall,f1,correctAssigned,realCorrect,totalAssignedCorrect]
            HashMap<String, ArrayList<Double>> evaluationMetrics = new HashMap<String, ArrayList<Double>>();

            logger.info("use " + concepts.size() + " concepts for evaluation");
            logger.info("training percentage: " + MathHelper.round(trainingPercentage, 2) + " (should be " + 400 * trainingPercentage + ")");
            logger.info("seed percentage: " + seedPercentage + " (should be " + 400 * trainingPercentage * seedPercentage + ")");
            logger.info("testing percentage: " + (1.0 - MathHelper.round(trainingPercentage, 2)) + " (" + 400 * (1.0 - trainingPercentage) + ")\n");
            logger.info("trust threshold bias: " + trustThresholdBias + "\n");

            for (Concept concept : concepts) {

                // build the matrix for the concept (
                buildAdjacencyMatrix(concept, trainingPercentage, seedPercentage);

                // learn the probability vector
                buildProbabilityVector(1000);

                // automatically determine the threshold by taking the average of the positives, the average of the negatives and then the average of the
                // averages
                int countPositive = 0;
                int countNegative = 0;
                double sumPositiveScores = 0;
                double sumNegativeScores = 0;
                for (Map.Entry<Integer, Boolean> trainingEntry : trainingEntities.entrySet()) {
                    double score = vt.getEntry(trainingEntry.getKey());
                    if (trainingEntry.getValue() == true) {
                        sumPositiveScores += score;
                        countPositive++;
                    } else {
                        sumNegativeScores += score;
                        countNegative++;
                    }
                }
                double positiveAverage = sumPositiveScores / countPositive;
                double negativeAverage = sumNegativeScores / countNegative;
                double threshold = negativeAverage + trustThresholdBias * Math.abs((positiveAverage - negativeAverage));

                logger.info("positive " + positiveAverage + "," + sumPositiveScores);
                logger.info("negative " + negativeAverage + "," + sumNegativeScores);
                logger.info("threshold " + threshold);

                int totalRealCorrect = 0;
                int totalAssigned = 0;
                int totalCorrect = 0;

                for (Map.Entry<Integer, Boolean> testEntry : testingEntities.entrySet()) {
                    Entity e = entityMatrixMappingInverse.get(testEntry.getKey());
                    System.out.print("test entity: \"" + e.getName() + "\"");
                    double score = vt.getEntry(testEntry.getKey());
                    if (score >= threshold) {
                        totalAssigned++;
                        System.out.print(" classify as true");
                        if (testEntry.getValue() == true) {
                            totalCorrect++;
                        }
                    } else {
                        System.out.print(" classify as false");
                    }
                    if (testEntry.getValue() == true) {
                        System.out.println(" and is true");
                        totalRealCorrect++;
                    } else {
                        System.out.println(" and is false");
                    }
                }

                evaluationMetrics.put(concept.getName(), calculateMetrics(totalRealCorrect, totalAssigned, totalCorrect, testingEntities.size()));

                System.out.println("------------------");

                logger.info("Seed Entities actually used: " + trainingEntitiesSeeds.size());
                /*
                 * for (Entity entity : trainingEntitiesSeeds) { System.out.println(entity.getName()); } System.out.println(" ");
                 */

                // System.out.println("Control Entities ("+trainingEntitiesControl.size()+")");
                logger.info("Training Entities actually used: " + trainingEntities.size());
                /*
                 * for (Map.Entry<Entity,Boolean> entry: trainingEntitiesControl.entrySet()) {
                 * System.out.println(entry.getKey().getName()+" | "+entry.getValue()); } System.out.println(" ");
                 */

                logger.info("Test Entities actually used: " + testingEntities.size());
                /*
                 * for (Map.Entry<Entity,Boolean> entry: testingEntities.entrySet()) { System.out.println(entry.getKey().getName()+" | "+entry.getValue()); }
                 * System.out.println(" ");
                 */
            }

            // log all metrics
            // graphData.put(trainingPercentage, logMetrics(concepts,evaluationMetrics));
            graphData.put(trustThresholdBias, logMetrics(concepts, evaluationMetrics));
        }

        createFlashChartLog(graphData);

        logger.info(DateHelper.getRuntime(t1, System.currentTimeMillis(), true));
    }

    /**
     * Example graph walk: E:\Projects\Programming\Java\WebKnox\documentationImages\graphWalkExample.png
     */
    public void matrixTest() {
        // small matrix 2x2
        /*
         * Mxy = new Array2DRowRealMatrix(2, 2); Mxy.setEntry(0, 0, 1); Mxy.setEntry(0, 1, 2); Mxy.setEntry(1, 0, 3); Mxy.setEntry(1, 1, 4); vt = new
         * ArrayRealVector(2); vt.setEntry(0, 10); vt.setEntry(1, 20); RealVector vt2 = Mxy.operate(vt); System.out.println(Mxy.toString());
         * System.out.println(vt.toString()); System.out.println(vt2.toString());
         */

        // big matrix 6x6
        Mxy = new Array2DRowRealMatrix(6, 6);
        Mxy.setEntry(0, 0, 0.0);
        Mxy.setEntry(0, 1, 0.0);
        Mxy.setEntry(0, 2, 0.0);
        Mxy.setEntry(0, 3, 0.0);
        Mxy.setEntry(0, 4, 0.0);
        Mxy.setEntry(0, 5, 1.0);
        Mxy.setEntry(1, 0, 0.0);
        Mxy.setEntry(1, 1, 0.0);
        Mxy.setEntry(1, 2, 0.5);
        Mxy.setEntry(1, 3, 0.0);
        Mxy.setEntry(1, 4, 0.0);
        Mxy.setEntry(1, 5, 0.5);
        Mxy.setEntry(2, 0, 0.0);
        Mxy.setEntry(2, 1, 0.5);
        Mxy.setEntry(2, 2, 0.0);
        Mxy.setEntry(2, 3, 0.5);
        Mxy.setEntry(2, 4, 0.0);
        Mxy.setEntry(2, 5, 0.0);
        Mxy.setEntry(3, 0, 0.0);
        Mxy.setEntry(3, 1, 0.0);
        Mxy.setEntry(3, 2, 0.5);
        Mxy.setEntry(3, 3, 0.0);
        Mxy.setEntry(3, 4, 0.0);
        Mxy.setEntry(3, 5, 0.5);
        Mxy.setEntry(4, 0, 0.0);
        Mxy.setEntry(4, 1, 0.0);
        Mxy.setEntry(4, 2, 0.0);
        Mxy.setEntry(4, 3, 0.0);
        Mxy.setEntry(4, 4, 0.0);
        Mxy.setEntry(4, 5, 1.0);
        Mxy.setEntry(5, 0, 0.25);
        Mxy.setEntry(5, 1, 0.25);
        Mxy.setEntry(5, 2, 0.0);
        Mxy.setEntry(5, 3, 0.25);
        Mxy.setEntry(5, 4, 0.25);
        Mxy.setEntry(5, 5, 0.0);

        vt = new ArrayRealVector(6);
        v0 = new ArrayRealVector(6);
        v0.setEntry(0, 0.0);
        v0.setEntry(1, 0.0);
        v0.setEntry(2, 0.0);
        v0.setEntry(3, 1.0);
        v0.setEntry(4, 1.0);
        v0.setEntry(5, 0.0);

        buildProbabilityVector(100);

        testingEntities.put(1, true);
        testingEntities.put(0, false);
    }

    public boolean classify(Entity entity) {
        // if (calculateProbability(e, c, p, k, n) > 0.5) return true;
        return false;
    }

    public static void main(String[] args) {

        RandomGraphWalk rgw = new RandomGraphWalk();
        /*
         * ArrayList<Entity> entities = DatabaseManager.getInstance().loadEvaluationEntities(new Concept("University")); double seedPercentage = 0.2; for
         * (double trainingPercentage = 0.05; trainingPercentage <= 0.951; trainingPercentage += 0.05) {
         * rgw.initSeedProbabilities(entities,trainingPercentage,seedPercentage); } System.exit(0);
         */

        // rgw.buildProbabilityVector(1000);
        // rgw.matrixTest();
        rgw.evaluate();
        // new RandomGraphWalk().buildProbabilityVector(1000);
        // new RandomGraphWalk().matrixTest();
    }
}