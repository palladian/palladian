package ws.palladian.extraction.date.rater;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance2;
import ws.palladian.classification.Predictor;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.helper.DateInstanceFactory;
import ws.palladian.helper.Cache;

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

    private static final String CLASSIFIER_MODEL_PUB = "/dates_pub_model.gz";
    private static final String CLASSIFIER_MODEL_MOD = "/dates_mod_model.gz";

    private Predictor<String> classifier;

    public ContentDateRater(PageDateType dateType) {
        super(dateType);
        if (dateType == PageDateType.publish) {
            loadClassifier(CLASSIFIER_MODEL_PUB);
        } else {
            loadClassifier(CLASSIFIER_MODEL_MOD);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadClassifier(String classifierModel) {
        classifier = (Predictor<String>)Cache.getInstance().getDataObject(classifierModel);
        if (classifier == null) {

            InputStream inputStream = this.getClass().getResourceAsStream(CLASSIFIER_MODEL_PUB);
            if (inputStream == null) {
                throw new IllegalStateException("Could not load model file \"" + classifierModel + "\"");
            }

            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(inputStream));
                classifier = (Predictor<String>)objectInputStream.readObject();
                Cache.getInstance().putDataObject(classifierModel, classifier);
            } catch (IOException e) {
                throw new IllegalStateException("Error loading the model file \"" + classifierModel + "\": "
                        + e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error loading the model file \"" + classifierModel + "\": "
                        + e.getMessage(), e);
            }
        }
    }

    @Override
    public Map<ContentDate, Double> rate(List<ContentDate> list) {

        Map<ContentDate, Double> returnDates = new HashMap<ContentDate, Double>();

        for (ContentDate date : list) {
            if (this.dateType.equals(PageDateType.publish) && date.isInUrl()) {
                returnDates.put(date, 1.0);
            } else {
                Instance2<String> instance = DateInstanceFactory.createInstance(date);
                try {
                    CategoryEntries dbl = classifier.predict(instance.featureVector);
                    returnDates.put(date, dbl.getMostLikelyCategoryEntry().getRelevance());
                } catch (Exception e) {
                    LOGGER.error("Exception " + date.getDateString() + " " + instance, e);
                }
            }

        }
        return returnDates;
    }
}
