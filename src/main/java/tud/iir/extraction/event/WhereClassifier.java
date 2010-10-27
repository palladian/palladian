package tud.iir.extraction.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import weka.core.Instance;

/**
 * @author Martin Wunderwald
 * 
 */
public class WhereClassifier extends Classifier {

    private String[] featureNames;

    public WhereClassifier(int type) {
        super(type);

        featureNames = new String[6];
        featureNames[0] = "titleEntityCount";
        featureNames[1] = "density";
        featureNames[2] = "textEntityCount";
        featureNames[3] = "avgStart";
        featureNames[4] = "type";
        featureNames[5] = "avgEnd";

    }

    /**
     * @param fo
     * @return
     */
    public float classify(FeatureObject fo) {

        Instance iUse = createInstance(getFvWekaAttributes(), discretize(fo
                .getFeatures()), getTrainingSet());

        try {
            double[] fDistribution = getClassifier().distributionForInstance(
                    iUse);

            return (float) fDistribution[0];
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    /**
     * Train and save a classifier.
     * 
     * @param path
     * 
     */
    @Override
    public void trainClassifier(String filePath) {
        ArrayList<FeatureObject> fo = readFeatureObjects(filePath);
        setTrainingObjects(fo);
        super.trainClassifier(filePath);

        try {
            weka.core.SerializationHelper.write(
                    "data/learnedClassifiers/where.model", getClassifier());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testClassifier(String filePath) {
        EventExtractor eventExtractor = EventExtractor.getInstance();
        eventExtractor.setWhereClassifier(getChosenClassifier());
        Event event = EventExtractor
                .extractEventFromURL("http://www.bbc.co.uk/news/world-middle-east-10851692?print=true");

        eventExtractor.extractWhere(event);

    }

    /**
     * Use an already trained classifier.
     */
    public void useTrainedClassifier(String filePath) {
        weka.classifiers.Classifier trainedAnswerClassifier;
        try {
            trainedAnswerClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper
                    .read(filePath);
            createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedAnswerClassifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void collectTrainingData(String filePath) {

        HashMap<String, String[]> data = new HashMap<String, String[]>();
        data.put("ghazni bomb", new String[] { "Afghanistan", "afghanistan",
                "Ghazni" });
        data.put("venezuela", new String[] { "venezuela", "Venezuela",
                "caracas" });
        data.put("indonesia train crash", new String[] { "indonesia",
                "Indonesia" });
        data.put("pakistan drones", new String[] { "pakistan", "Pakistan" });
        data.put("stuttgart 21", new String[] { "stuttgart", "Stuttgart",
                "germany" });
        data.put("ecuador correa",
                new String[] { "ecuador", "Ecuador", "Quito" });
        data.put("ramallah israel", new String[] { "ramallah", "Ramallah",
                "Israel", "israel", "palestina" });
        data.put("nigeria blast",
                new String[] { "nigeria", "Nigeria", "Abuja" });
        data.put("berlin anniversary", new String[] { "berlin", "Berlin",
                "germany" });

        for (Entry<String, String[]> entry : data.entrySet()) {
            String query = entry.getKey();
            ArrayList<String> wheres = new ArrayList<String>();
            wheres.addAll(Arrays.asList(entry.getValue()));

            HashMap<String, Event> eventMap = (HashMap<String, Event>) EventFeatureExtractor
                    .aggregateEvents(query);
            EventFeatureExtractor.setEntityFeatures(eventMap);
            EventFeatureExtractor.writeCSV(filePath, eventMap, wheres, true);

        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        WhereClassifier wc = new WhereClassifier(Classifier.LINEAR_REGRESSION);
        wc.collectTrainingData("data/features/where.csv");
        wc.trainClassifier("data/features/where.csv");
        // WhereClassifier wc = new WhereClassifier(Classifier.BAYES_NET);
        // wc.trainClassifier("data/features/where.csv");
        // ac.trainClassifier("data/benchmarkSelection/qa/training");
        // wc.testClassifier("data/benchmarkSelection/qa/testing");
    }

}
