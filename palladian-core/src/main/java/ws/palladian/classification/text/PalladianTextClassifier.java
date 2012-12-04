package ws.palladian.classification.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.log4j.Logger;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.classification.universal.UniversalClassifier;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

/**
 * This classifier builds a weighed term look up table for the categories to
 * classify new documents. XXX add second dictionary: p(at|the), p(the|at) to
 * counter the problem of sparse categories
 * 
 * @author David Urbansky
 */
public class PalladianTextClassifier implements Classifier<DictionaryModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianTextClassifier.class);

    @Override
    public DictionaryModel train(List<Instance> instances) {
        return train(instances, new FeatureSetting());
    }

    public DictionaryModel train(List<Instance> instances, FeatureSetting featureSetting) {
        Validate.notNull(featureSetting, "fs must not be null");

        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Instance instance : instances) {
            List<NominalFeature> terms = instance.getFeatureVector().getAll(NominalFeature.class,
                    UniversalClassifier.FEATURE_TERM);
            for (NominalFeature term : terms) {
                model.updateTerm(term.getValue(), instance.getTargetClass());
            }
            model.addCategory(instance.getTargetClass());
        }
        return model;
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        FeatureVector fv = Preprocessor.preProcessDocument(text, model.getFeatureSetting());
        return classify(fv, model);
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, DictionaryModel model) {

        // initialize probability Map with mutable double objects, so we can add relevance values to them
        Map<String, MutableDouble> probabilities = CollectionHelper.newHashMap();
        for (String category : model.getCategories()) {
            probabilities.put(category, new MutableDouble());
        }

        // sum up the probabilities for normalization
        double probabilitySum = 0.;

        // iterate through all terms in the document
        for (NominalFeature termFeature : vector.getAll(NominalFeature.class, UniversalClassifier.FEATURE_TERM)) {
            CategoryEntries categoryFrequencies = model.getCategoryEntries(termFeature.getValue());
            for (CategoryEntry category : categoryFrequencies) {
                double categoryFrequency = category.getProbability();
                if (categoryFrequency > 0) {
                    double weight = categoryFrequency * categoryFrequency;
                    probabilities.get(category.getName()).add(weight);
                    probabilitySum += weight;
                }
            }
        }

        CategoryEntries categories = new CategoryEntries();

        // If we have a category weight by matching terms from the document, use them to create the probability
        // distribution. Else wise return the prior probability distribution of the categories.
        if (probabilitySum > 0) {
            for (String category : model.getCategories()) {
                categories.add(new CategoryEntry(category, probabilities.get(category).doubleValue() / probabilitySum));
            }
        } else {
            for (String category : model.getCategories()) {
                categories.add(new CategoryEntry(category, model.getPrior(category)));
            }
        }
        return categories;
    }

    // /**
    // * FIXME somewhere else
    // * This method calls the classify function that is implemented by each concrete classifier all test documents are
    // * classified.
    // */
    // public void classifyTestDocuments() {
    //
    // int c = 1;
    // for (TextInstance testDocument : testDocuments) {
    // classify(testDocument);
    // if (c % 100 == 0) {
    // LOGGER.info("classified " + MathHelper.round(100 * c / (double)testDocuments.size(), 2)
    // + "% of the test documents");
    // }
    // c++;
    // }
    // }
    //
    // /**
    // * FIXME somwhere else
    // * @return
    // */
    // public ClassifierPerformance getPerformance() {
    //
    // if (performance == null) {
    // performance = new ClassifierPerformance(this);
    // }
    //
    // return performance;
    // }
    //
    //
    // /**
    // * FIXME somewhere else
    // */
    // public final ClassifierPerformance evaluate(Dataset dataset) {
    //
    // StopWatch sw = new StopWatch();
    //
    // // read the testing URLs from the given dataset
    // readTestData(dataset);
    //
    // // classify
    // classifyTestDocuments();
    //
    // LOGGER.info("classified " + getTestDocuments().size() + " documents in " + sw.getTotalElapsedTimeString());
    //
    // return getPerformance();
    // }
    //
    // /**
    // * FIXME somwehere else
    // * @param testInstances
    // * @return
    // */
    // public final ClassifierPerformance evaluate(Instances<UniversalInstance> testInstances) {
    //
    // StopWatch sw = new StopWatch();
    //
    // // instances to classification documents
    // setTestDocuments(new ClassificationDocuments());
    //
    // TextInstance preprocessedDocument = null;
    //
    // for (UniversalInstance universalInstance : testInstances) {
    //
    // preprocessedDocument = new TestDocument();
    //
    // String documentContent = universalInstance.getTextFeature();
    //
    // preprocessedDocument = preprocessDocument(documentContent, preprocessedDocument);
    // preprocessedDocument.setContent(documentContent);
    //
    // Categories categories = new Categories();
    // categories.add(new Category(universalInstance.getInstanceCategoryName()));
    //
    // preprocessedDocument.setDocumentType(TextInstance.TEST);
    // preprocessedDocument.setRealCategories(categories);
    // getTestDocuments().add(preprocessedDocument);
    // }
    //
    // // classify
    // classifyTestDocuments();
    //
    // LOGGER.info("classified " + getTestDocuments().size() + " documents in " + sw.getTotalElapsedTimeString());
    //
    // return getPerformance();
    // }
    //
    // /**
    // * FIXME somewhere else
    // * @param dataset
    // */
    // private void readTestData(final Dataset dataset) {
    //
    // // reset training and testing documents as well as learned categories
    // setTestDocuments(new ClassificationDocuments());
    //
    // final List<String[]> documentInformationList = new ArrayList<String[]>();
    //
    // LineAction la = new LineAction() {
    //
    // @Override
    // public void performAction(String line, int lineNumber) {
    //
    // // split the line using the separation string
    // String[] siteInformation = line.split(dataset.getSeparationString());
    //
    // // store the content (or link to the content) and all categories subsequently
    // String[] documentInformation = new String[siteInformation.length];
    // documentInformation[0] = siteInformation[0];
    //
    // // iterate over all parts of the line (in SINGLE mode this would be two iterations)
    // for (int i = 1; i < siteInformation.length; ++i) {
    //
    // String[] categorieNames = siteInformation[i].split("/");
    // if (categorieNames.length == 0) {
    // LOGGER.warn("no category names found for " + line);
    // return;
    // }
    // String categoryName = categorieNames[0];
    //
    // documentInformation[i] = categoryName;
    //
    // // add category if it does not exist yet
    // if (!getCategories().containsCategoryName(categoryName)) {
    // Category cat = new Category(categoryName);
    // cat.setClassType(getClassificationType());
    // cat.increaseFrequency();
    // getCategories().add(cat);
    // } else {
    // getCategories().getCategoryByName(categoryName).setClassType(getClassificationType());
    // getCategories().getCategoryByName(categoryName).increaseFrequency();
    // }
    //
    // // only take first category in "first" mode
    // if (getClassificationType() == ClassificationTypeSetting.SINGLE) {
    // break;
    // }
    // }
    //
    // // add to test urls
    // documentInformationList.add(documentInformation);
    //
    // if (lineNumber % 1000 == 0) {
    // LOGGER.info("read another 1000 lines from test file, total: " + lineNumber);
    // }
    //
    // }
    // };
    //
    // FileHelper.performActionOnEveryLine(dataset.getPath(), la);
    //
    // int c = 0;
    // for (String[] documentInformation : documentInformationList) {
    //
    // TextInstance preprocessedDocument = null;
    //
    // preprocessedDocument = new TestDocument();
    //
    // String firstField = documentInformation[0];
    //
    // String documentContent = firstField;
    //
    // // if the first field should be interpreted as a link to the actual document, get it and preprocess it
    // if (dataset.isFirstFieldLink()) {
    // documentContent = FileHelper.readFileToString(dataset.getRootPath() + firstField);
    // }
    //
    // preprocessedDocument = preprocessDocument(documentContent, preprocessedDocument);
    // preprocessedDocument.setContent(firstField);
    //
    // Categories categories = new Categories();
    // for (int j = 1; j < documentInformation.length; j++) {
    // categories.add(new Category(documentInformation[j]));
    // }
    //
    // preprocessedDocument.setDocumentType(TextInstance.TEST);
    // preprocessedDocument.setRealCategories(categories);
    // getTestDocuments().add(preprocessedDocument);
    //
    // if (c++ % (documentInformationList.size() / 100 + 1) == 0) {
    // LOGGER.info(Math.floor(100.0 * (c + 1) / documentInformationList.size()) + "% preprocessed (= " + c
    // + " documents)");
    // }
    // }
    //
    // // calculate the prior for all categories
    // getCategories().calculatePriors();
    // }

    /**
     * FIXME put this somewhere else
     * <p>
     * Train the text classifier with the given dataset.
     * </p>
     * 
     * @param dataset The dataset to train from.
     */
    @Override
    public DictionaryModel train(Dataset dataset) {
        return train(dataset, null);
    }

    public DictionaryModel train(Dataset dataset, FeatureSetting fs) {
        List<Instance> instances = createInstances(dataset, fs);
        LOGGER.info("trained with " + instances.size() + " instances from " + dataset.getPath());
        return train(instances, fs);
    }

    /** FIXME in classifier utils **/
    public List<Instance> createInstances(Dataset dataset, FeatureSetting featureSettings) {

        List<Instance> instances = new ArrayList<Instance>();

        int added = 1;
        List<String> trainingArray = FileHelper.readFileToArray(dataset.getPath());
        for (String string : trainingArray) {

            String[] parts = string.split(dataset.getSeparationString());
            if (parts.length != 2) {
                continue;
            }

            String learningText = "";
            if (!dataset.isFirstFieldLink()) {
                learningText = parts[0];
            } else {
                learningText = FileHelper.readFileToString(dataset.getRootPath() + parts[0]);
            }

            String instanceCategory = parts[1];

            FeatureVector featureVector = Preprocessor.preProcessDocument(learningText, featureSettings);
            instances.add(new Instance(instanceCategory, featureVector));

            ProgressHelper.showProgress(added++, trainingArray.size(), 1);
        }

        return instances;
    }

    /**
     * FIXME make this work again
     * <p>
     * For quick thread-safe classification use this. The DictionaryClassifier is thread safe by itself but this is
     * faster since copies of classifiers are created which all use the same dictionary (read-only).
     * </p>
     * 
     * @param classifier
     *            The classifier to use (will be copied).
     * @param text
     *            The text to classify.
     * @return The classified text instance.
     */
    // public static CategoryEntries predict(FeatureVector vector, DictionaryClassifier classifier) {
    //
    // // TODO: DictionaryClassifier copy = (DictionaryClassifier)
    // // classifier.copy();
    // DictionaryClassifier copy = new DictionaryClassifier();
    // copy.setClassificationTypeSetting(classifier.getClassificationTypeSetting());
    // return copy.classify(vector, model);
    //
    // }

}