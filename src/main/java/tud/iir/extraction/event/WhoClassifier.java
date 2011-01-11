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
 * The Who Classifier.
 * 
 * @author Martin Wunderwald
 */
public class WhoClassifier extends Classifier {

    /**
     * the feature names.
     */
    private final String[] featureNames;

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(WhoClassifier.class);

    /** The model file path. **/
    private final String MODEL;

    /**
     * Constructor.
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

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL = config.getString("models.palladian.en.event.who");

        } else {
            MODEL = "";
        }
    }

    /**
     * Classifies the feature object.
     * 
     * @param fo
     * @return distribution
     */
    public float classify(final FeatureObject fo) {

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
    public void trainClassifier(final String filePath) {
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
    public void testClassifier(final String filePath) {
        final EventExtractor eventExtractor = EventExtractor.getInstance();
        eventExtractor.setWhoClassifier(getChosenClassifier());
        Event event = EventExtractor
                .getInstance()
                .createEventFromURL(
                        "http://edition.cnn.com/2010/WORLD/europe/09/28/russia.moscow.mayor/?hpt=T1");

        eventExtractor.getFeatureExtractor().setFeatures(event);
        eventExtractor.extractWho(event);

    }

    /**
     * Use an already trained classifier.
     */
    public void useTrainedClassifier(final String filePath) {
        weka.classifiers.Classifier trainedClassifier;
        try {
            trainedClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper
                    .read(filePath);
            createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedClassifier);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Collect Trainingdata from different sources.
     * 
     * @param filePath
     */
    public void collectTrainingData(final String filePath) {

        EventExtractor eventExtractor = EventExtractor.getInstance();

        final Map<Integer, String[]> events = eventExtractor
                .getFeatureExtractor().readCSV("data/news_articles.csv");

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

            eventMap.put(url, eventExtractor.extractEventFromURL(url));

            eventExtractor.getFeatureExtractor()
                    .setAnnotationFeatures(eventMap);

            List<String> whos = new ArrayList<String>();

            if (who.contains("|")) {
                whos = Arrays.asList(who.split("\\|"));
            } else {
                whos.add(who);
            }

            eventExtractor.getFeatureExtractor().writeCSV(filePath, eventMap,
                    whos, true);

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
        final WhoClassifier wc = new WhoClassifier(Classifier.BAGGING);
        // wc.collectTrainingData("data/features/who.csv");

        wc.trainClassifier("data/features/who.csv");

        // wc.useTrainedClassifier(wc.MODEL);
        // wc.testClassifier(wc.MODEL);
    }

}
