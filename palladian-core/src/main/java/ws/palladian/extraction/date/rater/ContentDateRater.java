package ws.palladian.extraction.date.rater;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.helper.Cache;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDateRater.class);

    private static final String CLASSIFIER_MODEL_PUB = "/dates_pub_model.gz";
    private static final String CLASSIFIER_MODEL_MOD = "/dates_mod_model.gz";

    private final BaggedDecisionTreeModel model;
    private final BaggedDecisionTreeClassifier predictor;

    public ContentDateRater(PageDateType dateType) {
        super(dateType);
        if (dateType == PageDateType.PUBLISH) {
            model = loadModel(CLASSIFIER_MODEL_PUB);
        } else {
            model = loadModel(CLASSIFIER_MODEL_MOD);
        }
        this.predictor = new BaggedDecisionTreeClassifier();
    }

    private BaggedDecisionTreeModel loadModel(String classifierModel) {
        BaggedDecisionTreeModel model = (BaggedDecisionTreeModel)Cache.getInstance().getDataObject(classifierModel);
        if (model == null) {
            InputStream inputStream = this.getClass().getResourceAsStream(CLASSIFIER_MODEL_PUB);
            if (inputStream == null) {
                throw new IllegalStateException("Could not load model file \"" + classifierModel + "\"");
            }

            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(inputStream));
                model = (BaggedDecisionTreeModel)objectInputStream.readObject();
                Cache.getInstance().putDataObject(classifierModel, model);
            } catch (IOException e) {
                throw new IllegalStateException("Error loading the model file \"" + classifierModel + "\": "
                        + e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error loading the model file \"" + classifierModel + "\": "
                        + e.getMessage(), e);
            }
        }
        return model;
    }

    @Override
    public List<RatedDate<ContentDate>> rate(List<ContentDate> list) {
        List<RatedDate<ContentDate>> result = CollectionHelper.newArrayList();

        for (ContentDate date : list) {
            if (dateType.equals(PageDateType.PUBLISH) && date.isInUrl()) {
                result.add(RatedDate.create(date, 1.0));
            } else {
                FeatureVector featureVector = DateInstanceFactory.createFeatureVector(date);
                try {
                    CategoryEntries dbl = predictor.classify(featureVector, model);
                    result.add(RatedDate.create(date, dbl.getProbability(dbl.getMostLikelyCategory())));
                } catch (Exception e) {
                    LOGGER.error("Exception " + date.getDateString() + " " + featureVector, e);
                }
            }

        }
        return result;
    }

    /**
     * <p>
     * Build the model files for the classifier from the training CSV.
     * </p>
     * 
     * @param inputCsv The path to the CSV file.
     * @param outputPath The path and filename for the model file.
     */
    private static void buildModel(String inputCsv, String outputPath) {
        List<Trainable> instances = ClassificationUtils.createInstances(inputCsv, true);
        BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();
        BaggedDecisionTreeModel model = classifier.train(instances);
        FileHelper.serialize(model, outputPath);
    }

    public static void main(String[] args) {
        buildModel("/Users/pk/Dropbox/Uni/Datasets/DateDatasetMartinGregor/dates_mod.csv",
                "src/main/resources/dates_mod_model.gz");
        buildModel("/Users/pk/Dropbox/Uni/Datasets/DateDatasetMartinGregor/dates_pub.csv",
                "src/main/resources/dates_pub_model.gz");
    }

}
