package tud.iir.extraction.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.helper.ConfigHolder;
import weka.core.Instance;

/**
 * @author Martin Wunderwald
 */
public class WhereClassifier extends Classifier {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(WhoClassifier.class);

    private final String MODEL;
    /**
     * the feature names
     */
    private final String[] featureNames;

    /**
     * @param type
     */
    public WhereClassifier(int type) {
        super(type);

        featureNames = new String[3];
        featureNames[0] = "titleEntityCount";
        featureNames[1] = "textEntityCount";
        // featureNames[2] = "type";
        featureNames[2] = "distribution";

        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL = config.getString("models.palladian.en.event.where");

        } else {
            MODEL = "";
        }

    }

    /**
     * @param fo
     * @return
     */
    public float classify(FeatureObject fo) {

        final Instance iUse = createInstance(getFvWekaAttributes(),
                discretize(fo.getFeatures()), getTrainingSet());

        try {
            final double[] fDistribution = getClassifier()
            .distributionForInstance(iUse);

            return (float) fDistribution[0];
        } catch (final Exception e) {
            LOGGER.error(e);
            return 0;
        }

    }

    /**
     * Train and save a classifier.
     * 
     * @param path
     */
    @Override
    public void trainClassifier(String filePath) {
        final ArrayList<FeatureObject> fo = readFeatureObjects(filePath);
        setTrainingObjects(fo);
        super.trainClassifier(filePath);

        try {
            weka.core.SerializationHelper.write(MODEL, getClassifier());
        } catch (final Exception e) {
            LOGGER.error(e);
        }

    }

    @Override
    public void testClassifier(String filePath) {
        final EventExtractor eventExtractor = EventExtractor.getInstance();
        // eventExtractor.setWhereClassifier(getChosenClassifier());
        final Event event = EventExtractor
        .createEventFromURL("http://www.bbc.co.uk/news/world-middle-east-10851692?print=true");
        eventExtractor.getFeatureExtractor().setFeatures(event);
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
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void collectTrainingData(String filePath) {

        final EventFeatureExtractor featureExtractor = new EventFeatureExtractor();

        final Map<Integer, String[]> events = featureExtractor
        .readCSV("data/news_articles.csv");

        for (final Entry<Integer, String[]> entry : events.entrySet()) {

            final HashMap<String, Event> eventMap = new HashMap<String, Event>();

            final String[] fields = entry.getValue();
            // int id = entry.getKey();

            final String url = fields[0];
            // String title = fields[1];
            // final String who = fields[2];
            final String where = fields[3];
            // String what = fields[4];
            // String why = fields[5];
            // String how = fields[6];

            eventMap.put(url, EventExtractor.extractEventFromURL(url));

            featureExtractor.setAnnotationFeatures(eventMap);

            List<String> wheres = new ArrayList<String>();

            if (where.contains("|")) {
                wheres = Arrays.asList(where.split("\\|"));
            } else {
                wheres.add(where);
            }
            // LOGGER.info(whos);

            featureExtractor.writeCSV(filePath, eventMap, wheres, true);

            // CollectionHelper.print(eventMap);

        }

    }

    public void collectOnlineTrainingData(String filePath) {

        final EventFeatureExtractor featureExtractor = new EventFeatureExtractor();

        final HashMap<String, String[]> data = new HashMap<String, String[]>();
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

        for (final Entry<String, String[]> entry : data.entrySet()) {
            final String query = entry.getKey();
            final ArrayList<String> wheres = new ArrayList<String>();
            wheres.addAll(Arrays.asList(entry.getValue()));

            final HashMap<String, Event> eventMap = (HashMap<String, Event>) EventFeatureExtractor
            .aggregateEvents(query);
            featureExtractor.setAnnotationFeatures(eventMap);
            featureExtractor.writeCSV(filePath, eventMap, wheres, true);

        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final WhereClassifier wc = new WhereClassifier(
                Classifier.NEURAL_NETWORK);
        // wc.collectTrainingData("data/features/where.csv");
        // wc.collectOnlineTrainingData("data/features/where_online.csv");
        // WhereClassifier wc = new WhereClassifier(Classifier.BAYES_NET);
        wc.testClassifier("");
        // ac.trainClassifier("data/benchmarkSelection/qa/training");
        // wc.useTrainedClassifier(wc.MODEL);
        // wc.testClassifier("");
    }
}
