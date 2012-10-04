package ws.palladian.classification.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.Predictor;
import ws.palladian.classification.Term;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.date.DateHelper;
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
public class DictionaryClassifier implements Predictor<DictionaryModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DictionaryClassifier.class);

    /**
     * Configurations for the classification type ({@link ClassificationTypeSetting.SINGLE},
     * {@link ClassificationTypeSetting.HIERARCHICAL}, or {@link ClassificationTypeSetting.TAG}).
     */
    private ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

    /** The feature settings which should be used by the text classifier. */
    private FeatureSetting featureSetting = new FeatureSetting();

    @Override
    public DictionaryModel learn(List<NominalInstance> instances) {
        DictionaryModel dictionaryModel = new DictionaryModel();

        for (NominalInstance instance : instances) {
            addToDictionary(dictionaryModel, instance);
        }

        return dictionaryModel;
    }

    private void addToDictionary(DictionaryModel model, NominalInstance trainingInstance) {

        for (NominalFeature termFeature : trainingInstance.featureVector.getFeatures(NominalFeature.class, "term")) {

            // FIXME "Term" still necessary or simply string.intern()?
            model.updateWord(termFeature.getValue(), trainingInstance.targetClass, 1.0);

            // FIXME => trainingInstance.targetClass => there must be multiple classes allowed!!!
            // for (Category realCategory : trainingDocument.getRealCategories()) {
            // // System.out.println("update " + entry.getKey() + " " +
            // // realCategory.getName());
            // model.updateWord(entry.getKey(), realCategory.getName(), entry.getValue());
            // }
        }

    }

    private int getClassificationType() {
        return classificationTypeSetting.getClassificationType();
    }

    @Override
    public CategoryEntries predict(FeatureVector vector, DictionaryModel model) {
        return classify(vector, model, null);
    }

    private CategoryEntries classify(FeatureVector vector, DictionaryModel model, Set<String> possibleClasses) {

        int classType = getClassificationType();

        long t1 = System.currentTimeMillis();

        int matches = 0;

        // make a look up in the context map for every single term
        CategoryEntries bestFitList = new CategoryEntries();

        // create one category entry for every category with relevance 0
        for (Category category : model.getCategories()) {
            if (possibleClasses != null && !possibleClasses.contains(category.getName())) {
                continue;
            }
            CategoryEntry c = new CategoryEntry(bestFitList, category, 0);
            bestFitList.add(c);
        }

        // iterate through all weighted terms in the document
        for (NominalFeature termFeature : vector.getFeatures(NominalFeature.class, "term")) {

            CategoryEntries dictionaryCategoryEntries = model.get(termFeature.getValue());

            if (dictionaryCategoryEntries != null) {

                matches++;

                // iterate through all categories in the dictionary for the weighted term
                for (CategoryEntry categoryEntry : dictionaryCategoryEntries) {
                    String categoryName = categoryEntry.getCategory().getName();
                    CategoryEntry c = bestFitList.getCategoryEntry(categoryName);
                    if (c == null) {
                        continue;
                    }

                    // add the absolute weight of the term to the category
                    if (categoryEntry.getRelevance() > 0) {

                        // use relevance
                        c.addAbsoluteRelevance(categoryEntry.getRelevance() * categoryEntry.getRelevance());
                    }

                }

            } else {
                LOGGER.trace("the term \"" + termFeature.getValue()
                        + "\" is not in the learned dictionary and cannot be associated with any category");
            }
        }

        // calculate one regression value for the given documents
        if (classType == ClassificationTypeSetting.REGRESSION) {
            double regressionValue = 0;
            for (CategoryEntry ce : bestFitList) {
                if (ce.getRelevance() > 0) {
                    regressionValue += Double.valueOf(ce.getCategory().getName()) * ce.getRelevance();
                }
            }
            bestFitList.clear();
            bestFitList.add(new CategoryEntry(bestFitList, new Category(String.valueOf(regressionValue)), 1));
        }

        // if (bestFitList.isEmpty()) {
        // Category unassignedCategory = new Category(null);
        // categories.add(unassignedCategory);
        // CategoryEntry defaultCE = new CategoryEntry(bestFitList, unassignedCategory, 1);
        // bestFitList.add(defaultCE);
        // }

        System.out.println("matches: " + matches);

        LOGGER.debug("classified document (classType " + classType + ") in " + DateHelper.getRuntime(t1) + " " + " ("
                + bestFitList.getMostLikelyCategoryEntry() + ")");

        return bestFitList;
    }

    public ClassificationTypeSetting getClassificationTypeSetting() {
        return classificationTypeSetting;
    }

    public void setClassificationTypeSetting(ClassificationTypeSetting classificationTypeSetting) {
        this.classificationTypeSetting = classificationTypeSetting;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    public void setFeatureSetting(FeatureSetting featureSetting) {
        this.featureSetting = featureSetting;
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
    public DictionaryModel train(Dataset dataset) {
        List<NominalInstance> instances = createInstances(dataset);
        LOGGER.info("trained with " + instances.size() + " instances from " + dataset.getPath());
        return learn(instances);
    }

    /** FIXME in classifier utils **/
    public List<NominalInstance> createInstances(Dataset dataset) {

        List<NominalInstance> instances = new ArrayList<NominalInstance>();

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

            NominalInstance instance = new NominalInstance();
            instance.targetClass = instanceCategory;

            Preprocessor preprocessor = new Preprocessor(this);
            TextInstance preProcessDocument = preprocessor.preProcessDocument(learningText);
            for (Entry<Term, Double> entry : preProcessDocument.getWeightedTerms().entrySet()) {
                NominalFeature textFeature = new NominalFeature("term", entry.getKey().getText());
                instance.featureVector.add(textFeature);
            }

            instances.add(instance);

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