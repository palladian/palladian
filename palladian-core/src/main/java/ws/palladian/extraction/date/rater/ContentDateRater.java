package ws.palladian.extraction.date.rater;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.SerializationHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.helper.DateWekaInstanceFactory;
import ws.palladian.helper.Cache;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.dates.ContentDate;

/**
 * <p>
 * This class evaluates content-dates. Doing this by dividing dates in three parts: Keyword in attribute, in text and no
 * keyword; each part will be rated different. Part one by keyword classes, see
 * {@link KeyWords#getKeywordPriority(String)} and age. Part two by distance of keyword an date, keyword classes and
 * age. Part three by age.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class ContentDateRater extends TechniqueDateRater<ContentDate> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ContentDateRater.class);

    public static final String DATE_CLASSIFIER_IDENTIFIER = "wekaRandomCommitteeObjectModel";

    private Classifier classifier = null;

    public ContentDateRater(PageDateType dateType) {
        super(dateType);
        loadClassifier();
    }

    private void loadClassifier() {
        Configuration config = ConfigHolder.getInstance().getConfig();

        String classifierFile;
        String modelsRoot = config.getString("models.root");
        String modelPublished = config.getString("models.palladian.date.published");
        String modelModified = config.getString("models.palladian.date.modified");
        if (modelsRoot == null || modelPublished == null || modelModified == null) {
            throw new IllegalStateException("Path to the models has not been set.");
        }
        
        if (this.dateType.equals(PageDateType.publish)) {
            classifierFile = modelsRoot + modelPublished;
        } else {
            classifierFile = modelsRoot + modelModified;
        }
        
        // FIXME there are two different models, but they are cached as one item with one identifier?
        try {
            this.classifier = (Classifier)Cache.getInstance().getDataObject(DATE_CLASSIFIER_IDENTIFIER);
            if (this.classifier == null) {
                LOGGER.debug("load classifier from " + classifierFile);
                InputStream stream = ContentDateRater.class.getResourceAsStream(classifierFile);
                this.classifier = (Classifier)SerializationHelper.read(stream);
                Cache.getInstance().putDataObject(DATE_CLASSIFIER_IDENTIFIER, this.classifier);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load the classifier from " + classifierFile, e);
        }
    }

    @Override
    public Map<ContentDate, Double> rate(List<ContentDate> list) {

        int pubModCLassifierIndex;
        Map<ContentDate, Double> returnDates = new HashMap<ContentDate, Double>();
        DateWekaInstanceFactory dwif = new DateWekaInstanceFactory(this.dateType);

        if (this.dateType.equals(PageDateType.publish)) {
            pubModCLassifierIndex = 0;
        } else {
            pubModCLassifierIndex = 0;
        }

        for (ContentDate date : list) {
            if (this.dateType.equals(PageDateType.publish) && date.isInUrl()) {
                returnDates.put(date, 1.0);
            } else {
                Instance instance = dwif.getDateInstanceByArffTemplate(date);
                try {
                    double[] dbl = this.classifier.distributionForInstance(instance);
                    returnDates.put(date, dbl[pubModCLassifierIndex]);
                } catch (Exception e) {
                    LOGGER.error("Exception " + date.getDateString() + " " + instance, e);
                }
            }

        }
        return returnDates;
    }
}
