package tud.iir.classification.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * Implementation similar to the one described in the KnowItAll system:
 * <ul>
 * <li>Information Extraction from the Web: Techniques and Applications, Alexander Yates, 2007, page 43</li>
 * <li>The Use of Web-based Statistics to Validate Information Extraction, Stephen Soderland, Oren Etzioni, Tal Shaked, and Daniel S. Weld, 2004</li>
 * <li>WebScale Information Extraction in KnowItAll (Preliminary Results), Etzioni et al., 2004</li>
 * </ul>
 * 
 * Difference: No bootstrapping to find discriminators for each class, but use generic ones.
 * 
 * Workflow: 1. Find instances using discriminators. 2. Calculate prior probabilities P(I) and P(-I) by manually checking the extractions. I = correct instance.
 * 3. Take k (k ~ 10) positive and k negative instances (negative are positives from another class). 4. Calculate PMIs for all discriminators and all seeds of
 * the training set. 5. Find a threshold that splits positive and negative instances. 6. Create a tuning set of another k positive and k negative instances. 7.
 * Calculate for all discriminators and instances P(PMI > thresh | class), P(PMI > thresh | -class), P(PMI <= thresh | class), P(PMI <= thresh | -class) by
 * simply counting the correct/incorrect classifications. 8. Use trained probabilities P(PMI(I,D) > thresh | class) = P(fi|I) in NBC. 9. New instances can now
 * be classified using NBC.
 * 
 * @author David
 * 
 */
public class PMI extends EntityAssessor {

    private boolean benchmark = false;
    private HashMap<String, Double> benchmarkPMIs = null;
    private String[] discriminators = null;
    private HashMap<Concept, HashSet<Entity>> extractions = null;
    private HashMap<String, NBC> classifiers = null;

    public PMI() {

        // XP = concept name plural, XS = concept name singular
        discriminators = new String[10];
        discriminators[0] = "XP such as Y";
        discriminators[1] = "such XP as Y";
        discriminators[2] = "XP like Y";
        discriminators[3] = "XP especially Y";
        discriminators[4] = "XP including Y";
        discriminators[5] = "Y and other XP";
        discriminators[6] = "Y or other XP";
        discriminators[7] = "Y is a XS";
        discriminators[8] = "Y is the XS";
        discriminators[9] = "Y";

        extractions = new HashMap<Concept, HashSet<Entity>>();

        classifiers = new HashMap<String, NBC>();
    }

    public void addConcept(Concept c) {
        extractions.put(c, new HashSet<Entity>());
    }

    // private void extract() {
    //
    // /*
    // * SourceRetriever sr = new SourceRetriever(); // find instances for each concept for (Map.Entry<Concept,
    // HashSet<Entity>> entry :
    // * extractions.entrySet()) { Concept concept = entry.getKey(); String conceptNameSingular = concept.getName();
    // String conceptNamePlural =
    // * StringHelper.wordToPlural(conceptNameSingular); // query search engine with each discriminator for (String
    // discriminator : discriminators) { } }
    // */
    //
    // KnowledgeManager km = new KnowledgeManager();
    // km.addConcepts(extractions.keySet());
    //
    // EntityExtractor ee = EntityExtractor.getInstance();
    // ee.setAutoSave(false);
    // ee.setKnowledgeManager(km);
    //
    // SourceRetrieverManager.getInstance().setResultCount(10);
    // SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.GOOGLE);
    //
    // PhraseExtractor pe = new PhraseExtractor();
    // ee.extract(pe);
    //
    // ee.printExtractions();
    // }

    /**
     * Train one classifier for each concept. Use manually labeled examples, calculate PMIs, find threshold, use tuning set to learn conditional probabilities
     * and create naive bayesian classifier.
     */
    private void trainClassifiers(Set<Entity> positiveEntities, Set<Entity> negativeEntities, Set<Entity> positiveEntitiesTuning,
            Set<Entity> negativeEntitiesTuning, double priorPositive, double priorNegative) {

        for (Map.Entry<Concept, HashSet<Entity>> entry : extractions.entrySet()) {
            Concept c = entry.getKey();

            Set<Feature> features = new HashSet<Feature>();
            double threshold = findThreshold(positiveEntities, negativeEntities, c);
            System.out.println("calculated threshold for concept " + c.getName() + ": " + threshold);

            for (int discriminatorNumber = 0; discriminatorNumber < discriminators.length; discriminatorNumber++) {

                String discriminator = discriminators[discriminatorNumber];

                // TODO train p(fi|C) for each discriminator

                // train P(PMI(I,D) > thresh | +) = P(fi|+) and P(PMI(I,D) < thresh | +) = P(-fi|+)
                int countPositive = 0;
                int countNegative = 0;
                for (Entity positiveEntity : positiveEntitiesTuning) {
                    double pmi = calculatePMI(positiveEntity, discriminator);
                    if (pmi > threshold) {
                        countPositive++;
                    } else {
                        countNegative++;
                    }
                }
                double pp = (double) countPositive / (double) positiveEntitiesTuning.size();
                double np = (double) countNegative / (double) positiveEntitiesTuning.size();

                // train P(PMI(I,D) > thresh | -) = P(fi|-) and P(PMI(I,D) < thresh | -) = P(-fi|-)
                countPositive = 0;
                countNegative = 0;
                for (Entity negativeEntity : negativeEntitiesTuning) {
                    double pmi = calculatePMI(negativeEntity, discriminator);
                    if (pmi > threshold) {
                        countPositive++;
                    } else {
                        countNegative++;
                    }
                }

                double pn = (double) countPositive / (double) negativeEntitiesTuning.size();
                double nn = (double) countNegative / (double) negativeEntitiesTuning.size();

                Feature feature = new Feature(discriminatorNumber, pp, pn, np, nn);
                features.add(feature);
            }

            // serialize classifier
            NBC nbc = new NBC(priorPositive, priorNegative, features, threshold);
            // FileHelper.serialize(nbc, "data/learnedClassifiers/nbc_"+StringHelper.makeSafeName(c.getName())+".model");
            // System.out.println("serialized classifier");
            classifiers.put(c.getName(), nbc);
        }
    }

    public Double[] classifySoft(Entity entity) {
        // get NBC for the concept of the entity
        NBC nbc = classifiers.get(entity.getConcept().getName());

        // if not trained, try to load it
        if (nbc == null) {
            nbc = (NBC) FileHelper.deserialize("data/learnedClassifiers/nbc_" + StringHelper.makeSafeName(entity.getConcept().getName()) + ".model");
        }

        double threshold = nbc.getThreshold();

        // calculate the PMIs for the given entity using all discriminators, use the outcomes as features for the naive bayesian classifier
        Map<Integer, Boolean> discriminatorPMIOutcomes = new HashMap<Integer, Boolean>();

        for (int discriminatorNumber = 0; discriminatorNumber < discriminators.length; discriminatorNumber++) {
            String discriminator = discriminators[discriminatorNumber];

            double pmi = calculatePMI(entity, discriminator);
            if (pmi > threshold) {
                discriminatorPMIOutcomes.put(discriminatorNumber, true);
            } else {
                discriminatorPMIOutcomes.put(discriminatorNumber, false);
            }
        }

        // use nbc to classify the given entity
        return nbc.classifySoft(discriminatorPMIOutcomes);
    }

    public boolean classify(Entity entity) {
        Double[] probabilities = classifySoft(entity);
        if (probabilities[0] > probabilities[1]) {
            return true;
        }
        return false;
    }

    /**
     * Find the PMI threshold between a set of positive and negative entities. The threshold is found by averaging the PMI of all positive entities and
     * averaging the PMI of all negative entities separately. Then the average of both averages is taken as the threshold.
     * 
     * @param positiveEntities A set of positive entities.
     * @param negativeEntities A set of negative entities.
     * @return The determined threshold.
     */
    private double findThreshold(Set<Entity> positiveEntities, Set<Entity> negativeEntities, Concept concept) {

        ArrayList<ArrayList<Double[]>> chartData = new ArrayList<ArrayList<Double[]>>();
        ArrayList<Double[]> scatterPoints = new ArrayList<Double[]>();
        chartData.add(scatterPoints);

        // get average PMI of all positive extractions
        int counter = 0;
        double averagePMIPositive = 0.0;
        for (Entity positiveEntity : positiveEntities) {
            // query search engine with each discriminator
            for (String discriminator : discriminators) {
                double pmi = calculatePMI(positiveEntity, discriminator);
                averagePMIPositive += pmi;
                counter++;
                Double[] scatterPoint = new Double[2];
                scatterPoint[0] = 2.0;
                scatterPoint[1] = pmi;
                if (pmi < 1) {
                    scatterPoints.add(scatterPoint);
                }
            }
        }

        averagePMIPositive /= counter;

        // get average PMI of all negative extractions
        counter = 0;
        double averagePMINegative = 0.0;
        for (Entity negativeEntity : negativeEntities) {
            // query search engine with each discriminator
            for (String discriminator : discriminators) {
                double pmi = calculatePMI(negativeEntity, discriminator);
                averagePMINegative += pmi;
                counter++;
                Double[] scatterPoint = new Double[2];
                scatterPoint[0] = 1.0;
                scatterPoint[1] = pmi;
                if (pmi < 1) {
                    scatterPoints.add(scatterPoint);
                }
            }
        }

        averagePMINegative /= counter;

        double threshold = (averagePMIPositive + averagePMINegative) / 2.0;

        // create chart
        // ChartCreator.createXYChart("pmiScores"+concept.getName()+".png",chartData,"PMI scores","Class","PMI",true,ChartCreator.XY_SCATTER_CHART);
        // System.out.println("chart created, threshold: " + threshold);

        return threshold;
    }

    // private double calculatePMIForAllDiscriminators(Entity entity) {
    // double pmi = 0.0;
    // for (String discriminator : discriminators) {
    // pmi += calculatePMI(entity, discriminator);
    // }
    //
    // return pmi / (double) discriminators.length;
    // }

    /**
     * Calculate the PMI: Hits(E+D)/Hits(E)
     * 
     * @param entity The entity.
     * @param discriminator The discriminator
     * @param live If true, results are calculated online, otherwise they are fetched from the database (for benchmarking).
     */
    private double calculatePMI(Entity entity, String discriminator) {
        return calculatePMI(entity, discriminator, benchmark);
    }

    private double calculatePMI(Entity entity, String discriminator, boolean benchmark) {
        // System.out.println("entity:"+entity.getName()+","+discriminator+","+benchmark);

        int attributeID = getDiscriminatorID(discriminator);
        if (attributeID == -1) {
            System.out.println("ERROR at " + entity.getName() + " with discriminator" + discriminator);
            System.exit(1);
        }

        int eCount = 0;
        int edCount = 0;
        double pmi = 0.0;

        if (!benchmark) {
            SourceRetriever sr = new SourceRetriever();
            sr.setSource(SourceRetrieverManager.GOOGLE);

            // Source hitCountSource = new Source("http://www.google.com");

            String eQuery = entity.getName();
            String edQuery = "";
            try {
                edQuery = discriminator.replaceAll("Y", entity.getName());
            } catch (IndexOutOfBoundsException e) {
                edQuery = discriminator.replaceAll("Y", entity.getName().replaceAll("\\$", "\\\\\\$"));
                e.printStackTrace();
            }
            edQuery = edQuery.replaceAll("XP", StringHelper.wordToPlural(entity.getConcept().getName()));
            edQuery = edQuery.replaceAll("XS", entity.getConcept().getName());

            eCount = sr.getHitCount("\"" + eQuery + "\"");
            edCount = sr.getHitCount("\"" + edQuery + "\"");

            if (eCount == 0) {
                // dbm.addFact(new FactValue("0.0",hitCountSource,-1), entity.getID(), attributeID);
                return 0.0;
            }

            pmi = (double) edCount / (double) eCount;

            if (attributeID == 819) {
                pmi = eCount;
            }

            // dbm.addFact(new FactValue(String.valueOf(pmi),hitCountSource,-1), entity.getID(), attributeID);

        } else {
            // pmi = dbm.getPMI(entity.getID(),attributeID);
            pmi = benchmarkPMIs.get(String.valueOf(entity.getID()) + String.valueOf(attributeID));
        }

        // System.out.println(pmi);

        return pmi;
    }

    /**
     * The the id of the attribute conntecte to the discriminator from the database.
     * 
     * @return
     */
    private int getDiscriminatorID(String discriminator) {
        if (discriminator.equalsIgnoreCase("XP such as Y")) {
            return 810;
        } else if (discriminator.equalsIgnoreCase("such XP as Y")) {
            return 811;
        } else if (discriminator.equalsIgnoreCase("XP like Y")) {
            return 812;
        } else if (discriminator.equalsIgnoreCase("XP especially Y")) {
            return 813;
        } else if (discriminator.equalsIgnoreCase("XP including Y")) {
            return 814;
        } else if (discriminator.equalsIgnoreCase("Y and other XP")) {
            return 815;
        } else if (discriminator.equalsIgnoreCase("Y or other XP")) {
            return 816;
        } else if (discriminator.equalsIgnoreCase("Y is a XS")) {
            return 817;
        } else if (discriminator.equalsIgnoreCase("Y is the XS")) {
            return 818;
        } else if (discriminator.equalsIgnoreCase("Y")) {
            return 819;
        }

        return -1;
    }

    /**
     * Evaluate the algorithm by classifying entities with a score above the threshold as true and calculating precision and recall using the test entities.
     */
    @Override
    public void evaluate() {

        benchmark = true;
        if (benchmark) {
            benchmarkPMIs = dbm.getBenchmarkPMIs();
            System.out.println("loaded PMI scores from database");
        }

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

        // entities are loaded once for every concept and reused in subsequent iterations
        HashMap<String, ArrayList<Entity>> entityMap = new HashMap<String, ArrayList<Entity>>();

        // data for the graph, double (percentage of training set,[precision,recall,f1])
        TreeMap<Double, ArrayList<Double>> graphData = new TreeMap<Double, ArrayList<Double>>();

        // what percentage of the entities should be used for training
        // for (double trainingPercentage = trainingPercentageStepSize; trainingPercentage <= 0.901; trainingPercentage += trainingPercentageStepSize) {
        double trainingPercentage = 0.2;
        // for (trustThreshold = trustThresholdStepSize; trustThreshold <= 0.91; trustThreshold += trustThresholdStepSize) {
        for (int step = 0; step <= 11; step++) {
            trustThreshold = 0.05 * Math.pow(1.2589, step);

            logger.info("################################## start new iteration ##################################");

            classifiers.clear();

            // what percentage of the training entities should be used for tuning
            double tuningPercentage = 0.5;

            double trainingSampleFrequency = 1.0 / trainingPercentage;
            double tuningSampleFrequency = trainingSampleFrequency / tuningPercentage;

            // save the evaluation metrics: concept name : [precision,recall,f1,correctAssigned,realCorrect,totalAssignedCorrect]
            HashMap<String, ArrayList<Double>> evaluationMetrics = new HashMap<String, ArrayList<Double>>();

            logger.info("use " + concepts.size() + " concepts for evaluation");
            logger.info("training percentage: " + trainingPercentage);
            logger.info("tuning percentage: " + tuningPercentage + "\n");
            logger.info("trust threshold: " + trustThreshold + "\n");

            for (Concept concept : concepts) {

                logger.info("start evaluating concept " + concept.getName());

                Set<Entity> positiveEntities = new HashSet<Entity>();
                Set<Entity> positiveEntitiesTuning = new HashSet<Entity>();
                Set<Entity> negativeEntities = new HashSet<Entity>();
                Set<Entity> negativeEntitiesTuning = new HashSet<Entity>();
                Set<Entity> testingEntities = new HashSet<Entity>();
                double priorPositive = 0.5;
                double priorNegative = 0.5;

                ArrayList<Entity> entities = entityMap.get(concept.getName());
                if (entities == null) {
                    entities = dbm.loadEvaluationEntities(concept);
                    entityMap.put(concept.getName(), entities);
                }

                logger.info("loaded evaluation entities (" + entities.size() + ")");

                int count = 0;
                for (Entity entity : entities) {

                    if (count % trainingSampleFrequency < 1) {
                        entity.setType(Entity.TRAINING);

                        // System.out.println(entity.getTrust());

                        // positive entities
                        if (entity.getTrust() == 1) {

                            if (count % tuningSampleFrequency >= 1) {
                                positiveEntities.add(entity);
                            } else {
                                positiveEntitiesTuning.add(entity);
                            }

                            // negative entities
                        } else {

                            if (count % tuningSampleFrequency >= 1) {
                                negativeEntities.add(entity);
                            } else {
                                negativeEntitiesTuning.add(entity);
                            }

                        }

                    } else {
                        entity.setType(Entity.TESTING);
                        testingEntities.add(entity);
                    }

                    count++;
                }

                logger.info("start training the classifier with " + positiveEntities.size() + " positive, " + negativeEntities.size() + " negative , "
                        + positiveEntitiesTuning.size() + " pos. tuning and " + negativeEntitiesTuning.size() + " neg. tuning entities");

                // PMI pmi = new PMI();
                // pmi.addConcept(concept);
                // pmi.trainClassifiers(positiveEntities, negativeEntities, positiveEntitiesTuning, negativeEntitiesTuning, priorPositive, priorNegative);

                extractions.clear();
                addConcept(concept);
                trainClassifiers(positiveEntities, negativeEntities, positiveEntitiesTuning, negativeEntitiesTuning, priorPositive, priorNegative);

                logger.info("training finished, start calculating evaluation metrics for concept " + concept.getName());

                // calculate evaluation metrics
                int totalRealCorrect = 0;
                int totalAssigned = 0;
                int totalCorrect = 0;

                for (Entity testEntity : testingEntities) {
                    // logger.log("test entity: \""+testEntity.getName()+"\"");

                    // Double[] p = classifySoft(testEntity);
                    // logger.log("p+: " + p[0]);
                    // logger.log("p-: " + p[1]);

                    if (classifySoft(testEntity)[0] > trustThreshold) {
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

    public static void main(String[] args) {

        PMI pmi = new PMI();

        pmi.evaluate();

        System.exit(0);

        Concept c = new Concept("City");
        pmi.addConcept(c);
        Concept c2 = new Concept("Movie");

        // System.out.println(pmi.classify(new Entity("Thundertown", c)));

        // pmi.extract();
        // System.exit(0);

        /*
         * pmi.calculatePMI(new Entity("Los Angeles",c), "Y is a city"); pmi.calculatePMI(new Entity("Dresden",c), "Y is a city"); pmi.calculatePMI(new
         * Entity("Melbourne",c), "Y is a city");
         */

        Set<Entity> positiveEntitiesCity = new HashSet<Entity>();
        Set<Entity> positiveEntitiesCityTuning = new HashSet<Entity>();
        Set<Entity> negativeEntitiesCity = new HashSet<Entity>();
        Set<Entity> negativeEntitiesCityTuning = new HashSet<Entity>();
        double priorPositive = 0.918;
        double priorNegative = 0.082;

        // city positive training
        positiveEntitiesCity.add(new Entity("London", c));
        positiveEntitiesCity.add(new Entity("Birmingham", c));
        positiveEntitiesCity.add(new Entity("Manchester", c));
        positiveEntitiesCity.add(new Entity("Glasgow", c));
        positiveEntitiesCity.add(new Entity("Athens", c));
        positiveEntitiesCity.add(new Entity("Beirut", c));
        positiveEntitiesCity.add(new Entity("Antwerp", c));
        positiveEntitiesCity.add(new Entity("Ceuta", c));
        positiveEntitiesCity.add(new Entity("Chicago", c));
        positiveEntitiesCity.add(new Entity("Los Angeles", c));
        positiveEntitiesCity.add(new Entity("San Francisco", c));
        positiveEntitiesCity.add(new Entity("Paris", c));
        positiveEntitiesCity.add(new Entity("Tokyo", c));
        positiveEntitiesCity.add(new Entity("Montgomery", c));
        positiveEntitiesCity.add(new Entity("Mobile", c));
        positiveEntitiesCity.add(new Entity("Huntsville", c));
        positiveEntitiesCity.add(new Entity("Anchorage", c));
        positiveEntitiesCity.add(new Entity("Juneau", c));
        positiveEntitiesCity.add(new Entity("Fairbanks", c));
        positiveEntitiesCity.add(new Entity("Phoenix", c));

        // city positive tuning
        positiveEntitiesCityTuning.add(new Entity("Tucson", c));
        positiveEntitiesCityTuning.add(new Entity("Mesa", c));
        positiveEntitiesCityTuning.add(new Entity("Glendale", c));
        positiveEntitiesCityTuning.add(new Entity("Little Rock", c));
        positiveEntitiesCityTuning.add(new Entity("Fort Smith", c));
        positiveEntitiesCityTuning.add(new Entity("North Little Rock", c));
        positiveEntitiesCityTuning.add(new Entity("Fayetteville", c));
        positiveEntitiesCityTuning.add(new Entity("San Diego", c));
        positiveEntitiesCityTuning.add(new Entity("Denver", c));
        positiveEntitiesCityTuning.add(new Entity("Colorado Springs", c));
        positiveEntitiesCityTuning.add(new Entity("Aurora", c));
        positiveEntitiesCityTuning.add(new Entity("Lakewood", c));
        positiveEntitiesCityTuning.add(new Entity("Bridgeport", c));
        positiveEntitiesCityTuning.add(new Entity("New Haven", c));
        positiveEntitiesCityTuning.add(new Entity("Hartford", c));
        positiveEntitiesCityTuning.add(new Entity("Stamford", c));
        positiveEntitiesCityTuning.add(new Entity("Wilmington", c));
        positiveEntitiesCityTuning.add(new Entity("Newark", c));
        positiveEntitiesCityTuning.add(new Entity("New Castle", c));
        positiveEntitiesCityTuning.add(new Entity("Miami", c));

        // city negative training (positive movie)
        negativeEntitiesCity.add(new Entity("Rain Man", c2));
        negativeEntitiesCity.add(new Entity("Blade", c2));
        negativeEntitiesCity.add(new Entity("Hollow Man", c2));
        negativeEntitiesCity.add(new Entity("Bride", c2));
        negativeEntitiesCity.add(new Entity("Thirteen Moons", c2));
        negativeEntitiesCity.add(new Entity("Wishcraft", c2));
        negativeEntitiesCity.add(new Entity("Made", c2));
        negativeEntitiesCity.add(new Entity("The Crew", c2));
        negativeEntitiesCity.add(new Entity("The Lesser Evil", c2));
        negativeEntitiesCity.add(new Entity("TV", c2));
        negativeEntitiesCity.add(new Entity("A Beautiful Mind", c2));
        negativeEntitiesCity.add(new Entity("Oscar", c2));
        negativeEntitiesCity.add(new Entity("The Forsaken", c2));
        negativeEntitiesCity.add(new Entity("Sonic Mirror", c2));
        negativeEntitiesCity.add(new Entity("16 Blocks", c2));
        negativeEntitiesCity.add(new Entity("New York Minute", c2));
        negativeEntitiesCity.add(new Entity("Jungle Fever", c2));
        negativeEntitiesCity.add(new Entity("Glory", c2));
        negativeEntitiesCity.add(new Entity("Shawshank Redemption", c2));
        negativeEntitiesCity.add(new Entity("Star Wars", c2));

        // city negative tuning (positive movie)
        negativeEntitiesCityTuning.add(new Entity("Spider-Man", c2));
        negativeEntitiesCityTuning.add(new Entity("Up", c2));
        negativeEntitiesCityTuning.add(new Entity("X-Men 2", c2));
        negativeEntitiesCityTuning.add(new Entity("Star Trek", c2));
        negativeEntitiesCityTuning.add(new Entity("Harry Potter", c2));
        negativeEntitiesCityTuning.add(new Entity("Jumanji", c2));
        negativeEntitiesCityTuning.add(new Entity("Taken", c2));
        negativeEntitiesCityTuning.add(new Entity("Superbad", c2));
        negativeEntitiesCityTuning.add(new Entity("Transformers 2", c2));
        negativeEntitiesCityTuning.add(new Entity("X-Men Origins", c2));
        negativeEntitiesCityTuning.add(new Entity("Fight Club", c2));
        negativeEntitiesCityTuning.add(new Entity("Memento", c2));
        negativeEntitiesCityTuning.add(new Entity("Monsters", c2));
        negativeEntitiesCityTuning.add(new Entity("Die Hard", c2));
        negativeEntitiesCityTuning.add(new Entity("Planet 51", c2));
        negativeEntitiesCityTuning.add(new Entity("Watchmen", c2));
        negativeEntitiesCityTuning.add(new Entity("The Uninvited", c2));
        negativeEntitiesCityTuning.add(new Entity("Spiderman 3", c2));
        negativeEntitiesCityTuning.add(new Entity("The Bourne Supremacy", c2));
        negativeEntitiesCityTuning.add(new Entity("Casino Royale", c2));

        pmi.trainClassifiers(positiveEntitiesCity, negativeEntitiesCity, positiveEntitiesCityTuning, negativeEntitiesCityTuning, priorPositive, priorNegative);

        // pmi.classify("Los Angeles");
    }
}

/**
 * Store information for one feature. 1. the number of the discriminator used 2. probability that PMI is above threshold for positive instance p(PMI > threshold
 * | +) 3. probability that PMI is above threshold for negative instance p(PMI > threshold | -) 4. probability that PMI is above threshold for positive instance
 * p(PMI < threshold | +) 5. probability that PMI is above threshold for negative instance p(PMI < threshold | -)
 * 
 * @author David
 * 
 */
class Feature {
    private double discriminatorNumber = 0;
    private double pp = 0.0;
    private double pn = 0.0;
    private double np = 0.0;
    private double nn = 0.0;

    public Feature(double discriminatorNumber, double pp, double pn, double np, double nn) {
        setDiscriminatorNumber(discriminatorNumber);
        setPP(pp);
        setPN(pn);
        setNP(np);
        setNN(nn);
    }

    public Double[] getProbablities() {
        Double[] probablities = new Double[4];
        probablities[0] = pp;
        probablities[1] = pn;
        probablities[2] = np;
        probablities[3] = nn;

        return probablities;
    }

    public double getDiscriminatorNumber() {
        return discriminatorNumber;
    }

    public void setDiscriminatorNumber(double discriminatorNumber) {
        this.discriminatorNumber = discriminatorNumber;
    }

    public double getPP() {
        return pp;
    }

    public void setPP(double pp) {
        this.pp = pp;
    }

    public double getPN() {
        return pn;
    }

    public void setPN(double pn) {
        this.pn = pn;
    }

    public double getNP() {
        return np;
    }

    public void setNP(double np) {
        this.np = np;
    }

    public double getNN() {
        return nn;
    }

    public void setNN(double nn) {
        this.nn = nn;
    }
}

/**
 * Naive Bayesian Classifier / Probability Update
 * 
 * Example from another domain: P(+| sunny, cool, high, strong) vs. P(-| sunny, cool, high, strong) = P(sunny|+)P(cool|+)P(high|+)P(strong|+)P(+) vs.
 * P(sunny|-)P(cool|-)P(high|-)P(strong|-)P(-)
 * 
 * One such classifier must be created for each concept. The classifier holds information about the learned probabilities of the features (PMI >/< thresh | +/-
 * for the discriminators). The classifier can be used to determine whether an unseen entity belongs to the concept or not by looking at the PMI scores of the
 * discriminators.
 * 
 * @author David Urbansky
 */
class NBC implements Serializable {

    private static final long serialVersionUID = -7636382810518699049L;

    // the priors (manually determined) for the concept
    private double positivePrior = 0.0;
    private double negativePrior = 0.0;

    // the threshold for the PMI scores of the concept
    private double threshold = 0.0;

    // probabilities, discriminator id, set of 2 probabilities: p(PMI > thresh | +), p(PMI < thresh | +), p(PMI > thresh | -), p(PMI < thresh | -)
    private HashMap<Integer, Double[]> probabilities = null;

    public NBC(double positivePrior, double negativePrior, Set<Feature> features, double threshold) {
        this.positivePrior = positivePrior;
        this.negativePrior = negativePrior;

        probabilities = new HashMap<Integer, Double[]>();
        for (Feature feature : features) {
            probabilities.put((int) feature.getDiscriminatorNumber(), feature.getProbablities());
            System.out.println("set probabilities for discriminator (duplicates are eliminated) " + feature.getDiscriminatorNumber() + ": "
                    + CollectionHelper.getPrint(feature.getProbablities()));
        }

        setThreshold(threshold);
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Calculate the probability for a new instance.
     * 
     * @param discriminatorPMIOutcomes The feature values for the discriminators, 1 if PMI > thresh, 0 if PMI < thresh.
     * @return True if it is more probable that the entity is member of the positive than the negative class.
     */
    public Double[] classifySoft(Map<Integer, Boolean> discriminatorPMIOutcomes) {
        /*
         * double probability = positivePrior * dependendProbabilityPositive; probability /= (positivePrior * dependendProbabilityPositive) + (negativePrior *
         * dependendProbabilityNegative); if (probability > threshold) return true; return false;
         */

        // p(+|discriminatorPMIOutcomes)
        double pp = 1.0;
        for (Map.Entry<Integer, Boolean> entry : discriminatorPMIOutcomes.entrySet()) {
            Double[] discriminatorProbabilities = probabilities.get(entry.getKey());

            // if PMI > thresh for the given discriminator, multiply with p(PMI > thresh | +)
            if (entry.getValue()) {
                pp *= discriminatorProbabilities[0];
            } else {
                pp *= discriminatorProbabilities[2];
            }
        }
        pp *= positivePrior;

        // p(-|discriminatorPMIOutcomes)
        double pn = 1.0;
        for (Map.Entry<Integer, Boolean> entry : discriminatorPMIOutcomes.entrySet()) {
            Double[] discriminatorProbabilities = probabilities.get(entry.getKey());

            // if PMI > thresh for the given discriminator, multiply with p(PMI > thresh | -)
            if (entry.getValue()) {
                pn *= discriminatorProbabilities[1];
            } else {
                pn *= discriminatorProbabilities[3];
            }
        }
        pn *= negativePrior;

        Double[] probabilities = new Double[2];
        probabilities[0] = pp;
        probabilities[1] = pn;

        return probabilities;

        /*
         * double probability = positivePrior; double ppProduct = 1.0; double pnProduct = 1.0; for (Feature feature : features) { probability *=
         * feature.getPP(); ppProduct *= feature.getPP(); pnProduct *= feature.getPN(); } probability /= (positivePrior * ppProduct + negativePrior *
         * pnProduct); return probability;
         */
    }

    public boolean classify(Map<Integer, Boolean> discriminatorPMIOutcomes) {
        Double[] probabilities = classifySoft(discriminatorPMIOutcomes);
        if (probabilities[0] > probabilities[1]) {
            return true;
        }
        return false;
    }
}