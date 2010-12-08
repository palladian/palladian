package tud.iir.extraction.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import weka.core.Instance;

/**
 * The Who Classifier
 * 
 * @author Martin Wunderwald
 */
public class WhoClassifier extends Classifier {

    /**
     * the feature names
     */
    private final String[] featureNames;

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(WhoClassifier.class);

    private final String MODEL;

    /**
     * constructor.
     * 
     * @param type
     */
    public WhoClassifier(int type) {
        super(type);

        featureNames = new String[4];
        featureNames[0] = "titleEntityCount";
        featureNames[1] = "textEntityCount";
        featureNames[2] = "type";
        featureNames[3] = "distribution";

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (final ConfigurationException e) {
            LOGGER.error("could not get model path from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL = config.getString("models.palladian.en.event.who");

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

    /*
     * (non-Javadoc)
     * @see tud.iir.classification.Classifier#testClassifier(java.lang.String)
     */
    @Override
    public void testClassifier(String filePath) {
        final EventExtractor eventExtractor = EventExtractor.getInstance();
        eventExtractor.setWhoClassifier(getChosenClassifier());
        final Event event = EventExtractor
                .extractEventFromURL("http://edition.cnn.com/2010/WORLD/europe/09/28/russia.moscow.mayor/?hpt=T1");

        eventExtractor.getFeatureExtractor().setFeatures(event);
        eventExtractor.extractWho(event);

    }

    /**
     * Use an already trained classifier.
     */
    public void useTrainedClassifier(String filePath) {
        weka.classifiers.Classifier trainedClassifier;
        try {
            trainedClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper
                    .read(filePath);
            createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedClassifier);
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * Collect Trainingdata from different sources.
     * 
     * @param filePath
     */
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
            final String who = fields[2];
            // String where = fields[3];
            // String what = fields[4];
            // String why = fields[5];
            // String how = fields[6];

            eventMap.put(url, EventExtractor.extractEventFromURL(url));

            featureExtractor.setAnnotationFeatures(eventMap);

            List<String> whos = new ArrayList<String>();

            if (who.contains("|")) {
                whos = Arrays.asList(who.split("\\|"));
            } else {
                whos.add(who);
            }

            featureExtractor.writeCSV(filePath, eventMap, whos, true);

            // CollectionHelper.print(eventMap);

        }

        // EventFeatureExtractor.writeCSV(filePath, eventMap1, whos, true);
        // EventFeatureExtractor.writeCSV(filePath, eventMap2, whos, true);

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        LOGGER.info("classifier runs");
        final WhoClassifier wc = new WhoClassifier(Classifier.LINEAR_REGRESSION);
        // wc.collectTrainingData("data/features/who.csv");

        wc.trainClassifier("data/features/who.csv");

        wc.useTrainedClassifier(wc.MODEL);
        wc.testClassifier(wc.MODEL);
    }

}
