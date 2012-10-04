package ws.palladian.classification.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.Term;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.ClassifierPerformance;
import ws.palladian.classification.page.evaluation.CrossValidationResult;
import ws.palladian.classification.page.evaluation.CrossValidator;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.EvaluationSetting;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.TreeNode;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * This class loads the training and test data, classifies and stores the results.
 * 
 * @author David Urbansky
 * @deprecated To be removed. Soon!
 */
@Deprecated
public class ClassifierManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClassifierManager.class);

    /**
     * This is an example of how to use a classifier.
     */
    public static void evaluateLanguageModel() {
        // take the time for the learning
        StopWatch stopWatch = new StopWatch();

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // the path to the classifier we want to use
        String classifierPath = "data/models/palladianLanguageClassifier/LanguageClassifier.gz";

        // specify the dataset that should be used as testing data
        Dataset dataset = new Dataset();

        // set the path to the dataset (should NOT overlap with the training set)
        dataset.setPath("data/datasets/classification/language/languageDocumentIndex2.txt");

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // load the language classifier
        TextClassifier classifier = DictionaryClassifier.load(classifierPath);

        // now we can test the classifier using the given dataset (output is written to the console)
        ClassifierPerformance classifierPerformance = null;
        classifierPerformance = classifierManager.testClassifier(dataset, classifier);

        LOGGER.info(classifierPerformance.getCorrectlyClassified());

        LOGGER.info("finished testing classifier in " + stopWatch.getElapsedTimeString());
    }

    public static void log(String message) {
        System.out.println(message);
    }

    /**
     * If arguments are given, they must be in the following order: trainingPercentage inputFilePath classifierType
     * classificationType training For example:
     * java -jar classifierManager.jar 80 data/benchmarkSelection/page/deliciouspages_cleansed_400.txt 1 3 true
     * 
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        trainLanguageModel();
        evaluateLanguageModel();
        // useLanguageModel();
        System.exit(0);

        // args = new String[4];
        // // args[0] = "--trainingFile";
        // args[0] = "--testingFile";
        // args[1] = "data/temp/opendirectory_urls_noregional_small.txt";
        // args[2] = "--name";
        // args[3] = "test";

        CommandLineParser parser = new BasicParser();

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("trainingFile").withDescription(
        "train a classifier on the data in the given file").hasArg().withArgName("filename").withType(
                Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("testingFile").withDescription(
        "test a classifier on the data in the given file").hasArg().withArgName("filename").withType(
                Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("name").withDescription(
        "the name under which the classifier is saved").hasArg().withArgName("string").withType(Number.class)
        .create());
        // options.addOption(OptionBuilder.withLongOpt("save").withDescription("save the trained classifier"))

        try {

            if (args.length < 1) {
                // no options supplied, go to catch clause, print help.
                throw new ParseException(null);
            }

            CommandLine cmd = parser.parse(options, args);

            ClassifierManager classifierManager = new ClassifierManager();

            Dataset dataset = new Dataset();
            TextClassifier classifier = null;

            if (cmd.hasOption("name")) {
                if (cmd.hasOption("trainingFile")) {
                    classifier = new DictionaryClassifier(cmd.getOptionValue("name"), "");// new KNNClassifier();
                } else if (cmd.hasOption("testingFile")) {
                    classifier = DictionaryClassifier.load(cmd.getOptionValue("name"));
                }
            } else {
                classifier = new DictionaryClassifier();// new KNNClassifier();
                ((DictionaryClassifier) classifier).getDictionary().setIndexType(Dictionary.DB_H2);
            }

            if (classifier == null) {
                System.out.println("classifier could not be loaded");
                return;
            }

            ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
            FeatureSetting featureSetting = new FeatureSetting();
            classifier.setClassificationTypeSetting(classificationTypeSetting);
            classifier.setFeatureSetting(featureSetting);

            // train and test a classifier
            StopWatch stopWatch = new StopWatch();

            if (cmd.hasOption("trainingFile")) {
                // train
                dataset.setPath(cmd.getOptionValue("trainingFile"));
                classifierManager.trainClassifier(dataset, classifier);

                if (cmd.hasOption("name")) {
                    classifier.save("");
                }

            }

            if (cmd.hasOption("testingFile")) {
                // test
                dataset.setPath(cmd.getOptionValue("testingFile"));
                classifierManager.testClassifier(dataset, classifier);
            }

            System.out.println("All actions performed in " + stopWatch.getElapsedTimeString());

            // done, exit.
            return;

        } catch (ParseException e) {
            // print usage help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ClassifierManager [options]", options);
        }

        // ///////////////////////// test reading from file index // /////////////////////////////
        // DictionaryIndex dictionaryIndex = new
        // DictionaryIndex("data/models/dictionary_URLClassifier_1");
        // dictionaryIndex.openReader();
        // CategoryEntries ces = dictionaryIndex.read(".com/a");
        // System.out.println(ces);
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////////////////////////

        // //////////////////////////// test classification ////////////////////////////////
        //
        // ///////////////////////////////////////////////////////////////////////////////

        // /////////////////////////// learn best classifiers ///////////////////////////////
        ClassifierManager classifierManager = new ClassifierManager();

        // build a set of classification type settings to evaluate
        List<ClassificationTypeSetting> classificationTypeSettings = new ArrayList<ClassificationTypeSetting>();
        ClassificationTypeSetting cts = new ClassificationTypeSetting();
        cts.setClassificationType(ClassificationTypeSetting.SINGLE);
        cts.setSerializeClassifier(false);
        classificationTypeSettings.add(cts);

        // build a set of classifiers to evaluate
        List<TextClassifier> classifiers = new ArrayList<TextClassifier>();
        TextClassifier classifier = null;
        classifier = new DictionaryClassifier();
        classifiers.add(classifier);
        classifier = new KNNClassifier();
        classifiers.add(classifier);

        // build a set of feature settings for evaluation
        List<FeatureSetting> featureSettings = new ArrayList<FeatureSetting>();
        FeatureSetting fs = null;
        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(4);
        fs.setMaxNGramLength(7);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(3);
        fs.setMaxNGramLength(8);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(3);
        featureSettings.add(fs);

        // build a set of datasets that should be used for evaluation
        Set<Dataset> datasets = new HashSet<Dataset>();
        Dataset dataset = new Dataset();
        dataset.setPath("data/temp/opendirectory_urls_noregional_small.txt");
        datasets.add(dataset);
        dataset = new Dataset();
        dataset.setPath("data/temp/productStrings.csv");
        dataset.setSeparationString("#");
        datasets.add(dataset);

        // set evaluation settings
        EvaluationSetting evaluationSetting = new EvaluationSetting();
        evaluationSetting.setTrainingPercentageMin(20);
        evaluationSetting.setTrainingPercentageMax(80);
        evaluationSetting.setkFolds(5);
        evaluationSetting.addDataset(dataset);

        // train and test all classifiers in all combinations
        StopWatch stopWatch = new StopWatch();

        // train + test
        classifierManager.learnBestClassifier(classificationTypeSettings, classifiers, featureSettings,
                evaluationSetting);

        System.out.println("finished training and testing classifier in " + stopWatch.getElapsedTimeString());
        System.exit(0);

        // /////////////////////////////////////////////////////////////////////////////////

        // ///////////////////////////// learn classifiers /////////////////////////////////
        classifierManager = new ClassifierManager();
        dataset = new Dataset();
        classifier = new DictionaryClassifier();// new KNNClassifier();
        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        FeatureSetting featureSetting = new FeatureSetting();
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // train and test all classifiers
        stopWatch = new StopWatch();

        // train
        // dataset.setPath("data/temp/opendirectory_urls_noregional_small_train.txt");
        // classifierManager.trainClassifier(dataset, classifier);

        // test
        // dataset.setPath("data/temp/opendirectory_urls_noregional_small_test.txt");
        // classifierManager.testClassifier(dataset, classifier);

        // train + test
        evaluationSetting = new EvaluationSetting();
        evaluationSetting.setTrainingPercentageMin(50);
        evaluationSetting.setTrainingPercentageMax(50);
        evaluationSetting.setkFolds(1);
        evaluationSetting.addDataset(dataset);
        dataset.setPath("data/temp/opendirectory_urls_noregional_small.txt");
        classifierManager.trainAndTestClassifier(classifier, evaluationSetting);

        System.out.println("finished training and testing classifier in " + stopWatch.getElapsedTimeString());
        System.exit(0);

    }

    /**
     * This is an example of how to train a classifier.
     */
    public static void trainLanguageModel() {

        // take the time for the learning
        StopWatch stopWatch = new StopWatch();

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset
        dataset.setPath("data/datasets/classification/language/languageDocumentIndex.txt");

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // create a text classifier by giving a name and a path where it should be saved to
        TextClassifier classifier = new DictionaryClassifier("LanguageClassifier",
        "data/models/palladianLanguageClassifier/");

        // specify the settings for the classification
        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

        // we use only a single category per document
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.SINGLE);

        // we want the classifier to be serialized in the end
        classificationTypeSetting.setSerializeClassifier(true);

        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = new FeatureSetting();

        // we want to create character-level n-grams
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);

        // the minimum length of our n-grams should be 4
        featureSetting.setMinNGramLength(4);

        // the maximum length of our n-grams should be 7
        featureSetting.setMaxNGramLength(7);

        // we assign the settings to our classifier
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // now we can train the classifier using the given dataset
        classifierManager.trainClassifier(dataset, classifier);

        // // train + test
        // EvaluationSetting evaluationSetting = new EvaluationSetting();
        // evaluationSetting.setTrainingPercentageMin(50);
        // evaluationSetting.setTrainingPercentageMax(50);
        // evaluationSetting.setkFolds(1);
        // evaluationSetting.addDataset(dataset);
        // classifierManager.trainAndTestClassifier(classifier, evaluationSetting);

        System.out.println("finished training classifier in " + stopWatch.getElapsedTimeString());
    }

    /**
     * This is an example of how to use a classifier.
     */
    public static void useLanguageModel() {
        // the path to the classifier we want to use
        String classifierPath = "data/models/languageClassifier/LanguageClassifier.ser";

        // load the language classifier
        TextClassifier classifier = DictionaryClassifier.load(classifierPath);

        // create a classification document that holds the result
        TextInstance classifiedDocument = null;

        // classify the little text (if classifier works it would say Spanish)
        classifiedDocument = classifier.classify("Yo solo sé que no sé nada.");

        // print the classified document
        System.out.println(classifiedDocument);
    }

    /** The classifier used to categorize the web sites. */
    private TextClassifier classifier = null;

    /** Percentage of pages used as training data. */
    private int trainingDataPercentage = 20;

    /** If true, a preprocessed document will be added to the dictionary right away, that saves memory. */
    private boolean createDictionaryIteratively = false;

    // // classification modes
    // /** train model, serialize model and use serialized model for test */
    // public static int CLASSIFICATION_TRAIN_TEST_SERIALIZE = 1;
    //
    // /** train model and used trained model for testing without serializing it */
    // public static int CLASSIFICATION_TRAIN_TEST_VOLATILE = 2;
    //
    // /** test model without training it again (model has to exist) */
    // public static int CLASSIFICATION_TEST_MODEL = 3;

    // int classificationMode = CLASSIFICATION_TRAIN_TEST_SERIALIZE;

    /** Decide whether to index the dictionary in lucene or database. */
    private int dictionaryClassifierIndexType = Dictionary.DB_INDEX_NORMALIZED;

    /** Decide whether to use mysql or h2 if index type is database. */
    private int dictionaryDatabaseType = Dictionary.DB_H2;

    /** If true, all n-grams will be searched once before inserting in the db, which saves look up time. */
    private boolean createDictionaryNGramSearchMode = true;

    /** The list of training URLs. */
    private List<String[]> trainingUrls = CollectionHelper.newArrayList();

    /** The list of test URLs. */
    private List<String[]> testUrls = CollectionHelper.newArrayList();

    /** A simple stop watch to measure performance. */
    private StopWatch stopWatch;

    public ClassifierManager() {

        stopWatch = new StopWatch();

        // try to find the classification configuration, if it is not present use default values

        final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        if (config.containsKey("classification.page.trainingPercentage")
                && config.getInt("classification.page.trainingPercentage") > -1) {
            setTrainingDataPercentage(config.getInt("classification.page.trainingPercentage"));
        }

        try {
            createDictionaryIteratively = config.getBoolean("classification.page.createDictionaryIteratively");
            dictionaryClassifierIndexType = config.getInt("classification.page.dictionaryClassifierIndexType");
            dictionaryDatabaseType = config.getInt("classification.page.databaseType");
            createDictionaryNGramSearchMode = config.getBoolean("classification.page.createDictionaryNGramSearchMode");
        } catch (Exception e) {
            LOGGER.debug("use default settings for ClassifierManager because not all the fields have been set in the palladian.properites file");
        }

    }

    public final int getTrainingDataPercentage() {
        return trainingDataPercentage;
    }

    public boolean isCreateDictionaryIteratively() {
        return createDictionaryIteratively;
    }

    /**
     * Retrieve web pages for a set of categories implying their category.
     */
    public final void learnAndTestClassifierOnline() {

        // category: ["keyword1 keyword2", "keyword4"...]
        HashMap<String, HashSet<String>> dictionary = new HashMap<String, HashSet<String>>();

        // category literature
        HashSet<String> literatureKeywords = new HashSet<String>();
        literatureKeywords.add("\"stephen king\"+buchempfehlung");
        literatureKeywords.add("\"val mcdermid\"+buchempfehlung");
        literatureKeywords.add("\"barbara wood\"+buchempfehlung");
        literatureKeywords.add("buchrezension");
        literatureKeywords.add("buchbesprechung buch");
        literatureKeywords.add("literaturblog");
        literatureKeywords.add("literatur buch");
        dictionary.put("literature", literatureKeywords);

        // category literature
        HashSet<String> filmKeywords = new HashSet<String>();
        filmKeywords.add("\"The Hangover\"+filmkritik");
        filmKeywords.add("\"Cobra\"+filmkritik");
        filmKeywords.add("\"Herz\"+filmkritik");
        filmKeywords.add("\"Liebe\"+filmrezension");
        filmKeywords.add("\"Terminator\"+filmkritik");
        filmKeywords.add("\"Braveheart\"+filmkritik");
        filmKeywords.add("film neuigkeiten");
        filmKeywords.add("film forum guter schauspieler");
        dictionary.put("film", filmKeywords);

        HashSet<String> musicKeywords = new HashSet<String>();
        musicKeywords.add("\"The Beatles\"+album+rezension");
        musicKeywords.add("\"Jimmy Eat World\"+album+kritik");
        musicKeywords.add("\"John Hiatt\"+cd-kritik");
        musicKeywords.add("\"Johnny Cash\"+album+rezension");
        musicKeywords.add("\"Mark Knopfler\"+album+kritik");
        musicKeywords.add("\"Blink-182\"+cd-kritik");
        musicKeywords.add("konzert+live+bericht");
        musicKeywords.add("album+empfehlung+musik");
        dictionary.put("music", musicKeywords);

        // category literature
        HashSet<String> travelKeywords = new HashSet<String>();
        travelKeywords.add("reisebericht");
        travelKeywords.add("reisebericht+urlaub");
        travelKeywords.add("urlaubsempfehlung");
        travelKeywords.add("reise+ratgeber");
        travelKeywords.add("buchbesprechung buch");
        travelKeywords.add("reiseforum+aussicht+urlaub");
        travelKeywords.add("urlaub+blog");
        dictionary.put("travel", travelKeywords);

        // retrieve web pages matching the keywords, download pages and build
        // index
        WebSearcher<WebResult> sr = new GoogleSearcher();

        HttpRetriever retriever = new HttpRetriever();

        StringBuilder fileIndex = new StringBuilder();
        StringBuilder urlIndex = new StringBuilder();
        int fileCounter = 1;

        LOGGER.info("Start retrieving web pages...");
        for (Map.Entry<String, HashSet<String>> category : dictionary.entrySet()) {

            for (String keyword : category.getValue()) {
                List<String> urls = Collections.emptyList();
                try {
                    urls = sr.searchUrls(keyword, 50, Language.GERMAN);
                } catch (SearcherException e) {
                    LOGGER.error(e);
                }

                for (String url : urls) {
                    String shortURLName = StringHelper.makeSafeName(UrlHelper.getCleanUrl(url));
                    String cleanURLName = "webpage" + fileCounter++ + "_"
                    + shortURLName.substring(0, Math.min(25, shortURLName.length())) + ".html";

                    // download file
                    if (retriever.downloadAndSave(url, "data/benchmarkSelection/page/automatic/" + cleanURLName)) {
                        fileIndex.append(cleanURLName).append(" ").append(category.getKey()).append("\n");
                        urlIndex.append(UrlHelper.getCleanUrl(url)).append(" ").append(category.getKey()).append("\n");
                        System.out.println("Saved and indexed " + url + " to " + cleanURLName);
                    } else {
                        System.out.println("Failed to save from page from " + url);
                    }

                }

            }

        }

        System.out.print("Saving index files...");
        FileHelper.writeToFile("data/benchmarkSelection/page/automatic/4categories_index.txt", fileIndex);
        FileHelper.writeToFile("data/benchmarkSelection/page/url/4categories_urls.txt", urlIndex);
        System.out.println("done");
    }

    /**
     * This method simplifies the search for the best combination of classifier and feature settings.
     * It automatically learns and evaluates all given combinations.
     * The result will be a ranked list (by F1 score) of the combinations that perform best on the given training/test
     * data.
     * 
     * @param classificationTypeSettings
     * @param featureSettings
     * @param classifiers
     * @param evaluationSetting
     */
    public final void learnBestClassifier(List<ClassificationTypeSetting> classificationTypeSettings,
            List<TextClassifier> classifiers, List<FeatureSetting> featureSettings, EvaluationSetting evaluationSetting) {

        Set<CrossValidationResult> cvResults = new HashSet<CrossValidationResult>();

        CrossValidator crossValidator = new CrossValidator();
        crossValidator.setEvaluationSetting(evaluationSetting);

        // loop through all classifiers
        for (TextClassifier classifier : classifiers) {

            // loop through all classification types
            for (ClassificationTypeSetting cts : classificationTypeSettings) {

                // loop through all features
                for (FeatureSetting featureSetting : featureSettings) {

                    classifier.setClassificationTypeSetting(cts);
                    classifier.setFeatureSetting(featureSetting);

                    // cross validation
                    CrossValidationResult cvResult = crossValidator.crossValidate(classifier);

                    cvResults.add(cvResult);
                }

            }
        }

        crossValidator.printEvaluationFiles(cvResults, "data/temp");

    }

    private TextInstance preprocessDocument(String[] data, int type) {
        TextInstance preprocessedDocument = null;

        if (type == TextInstance.TEST) {
            preprocessedDocument = new TestDocument();
        } else {
            preprocessedDocument = new TextInstance();
        }
        preprocessedDocument = classifier.preprocessDocument(data[0], preprocessedDocument);

        preprocessedDocument.setContent(data[0]);

        Categories categories = new Categories();
        for (int j = 1; j < data.length; j++) {
            categories.add(new Category(data[j]));
        }

        if (categories.isEmpty()) {
            LOGGER.warn("no real categories assigned to document with content: "  +data[0]);
        }
        
        preprocessedDocument.setRealCategories(categories);
        preprocessedDocument.setDocumentType(type);

        return preprocessedDocument;
    }

    /**
     * Create a document representation of the data read.
     */
    private void preprocessDocuments(int classType, boolean addToDictionary, boolean forTraining, Dataset dataset) {

        int size = 0;
        if (forTraining) {
            size = trainingUrls.size();
        } else {
            size = testUrls.size();
        }

        for (int i = 0; i < size; ++i) {

            String[] tData;
            TextInstance preprocessedDocument = null;

            if (forTraining) {

                tData = trainingUrls.get(i);
                preprocessedDocument = new TextInstance();

            } else {

                tData = testUrls.get(i);
                preprocessedDocument = new TestDocument();
            }

            String firstField = tData[0];

            String documentContent = firstField;

            // if the first field should be interpreted as a link to the actual document, get it and preprocess it
            if (dataset.isFirstFieldLink()) {
                documentContent = FileHelper.readFileToString(dataset.getRootPath() + firstField);
            }

            preprocessedDocument = classifier.preprocessDocument(documentContent, preprocessedDocument);

            preprocessedDocument.setContent(firstField);

            Categories categories = new Categories();
            for (int j = 1; j < tData.length; j++) {
                categories.add(new Category(tData[j]));
            }
            if (categories.isEmpty()) {
                LOGGER.warn("no real categories assigned to document with content: "  +tData[0]);
            }
            if (forTraining) {
                preprocessedDocument.setDocumentType(TextInstance.TRAINING);
                preprocessedDocument.setRealCategories(categories);
                classifier.getTrainingDocuments().add(preprocessedDocument);

                if (addToDictionary && classifier instanceof DictionaryClassifier) {
                    ((DictionaryClassifier) classifier).addToDictionary(preprocessedDocument, classType);
                }

            } else {
                preprocessedDocument.setDocumentType(TextInstance.TEST);
                preprocessedDocument.setRealCategories(categories);
                classifier.getTestDocuments().add(preprocessedDocument);
            }
            log(Math.floor(100.0 * (i + 1) / size) + "% preprocessed: " + tData[0] + ", i:" + (i + 1) + ", size:"
                    + size);

        }

        // ThreadHelper.sleep(2 * DateHelper.MINUTE_MS);
    }

    private void preprocessDocumentsFast(int classType) {

        // keep all nGrams that have been found already in memory so we don't
        // have to look them up in the db
        HashSet<Integer> nGramFound = new HashSet<Integer>();

        // iterate through all training and test URLs and preprocess them
        int size = trainingUrls.size();

        for (int i = 0; i < size; ++i) {

            LOGGER.info("processed " + MathHelper.round(100 * i / (double) size, 2) + "% of the documents, ngrams: "
                    + nGramFound.size() + ", time: " + DateHelper.getRuntime(classifier.initTime));

            TextInstance preprocessedDocument = null;

            String[] tData = trainingUrls.get(i);
            preprocessedDocument = preprocessDocument(tData, TextInstance.TRAINING);
            classifier.getTrainingDocuments().add(preprocessedDocument);

            // all nGrams of the current URL are saved in the map with their
            // categories and relevances as ngram => [category => relevance]
            HashMap<String, HashMap<String, Double>> temporaryNGramMap = new HashMap<String, HashMap<String, Double>>();

            for (Map.Entry<Term, Double> nGram : preprocessedDocument.getWeightedTerms().entrySet()) {
                if (nGramFound.add(nGram.hashCode())) {
                    HashMap<String, Double> categoryMap = new HashMap<String, Double>();
                    for (Category c : preprocessedDocument.getRealCategories()) {
                        categoryMap.put(c.getName().toLowerCase(), 1.0);
                    }
                    temporaryNGramMap.put(nGram.getKey().getText().toLowerCase(), categoryMap);
                }
            }

            LOGGER.info(temporaryNGramMap.size() + " new ngrams found...look through all following documents");

            if (temporaryNGramMap.isEmpty()) {
                continue;
            }

            // find same nGrams in all following documents
            for (int j = i + 1; j < size; ++j) {
                String[] tData2 = trainingUrls.get(j);
                TextInstance preprocessedDocument2 = preprocessDocument(tData2, TextInstance.TRAINING);

                // add categories and relevances to temporaryNGramMap
                HashMap<String, Double> categoryMap2 = new HashMap<String, Double>();
                for (Category c : preprocessedDocument2.getRealCategories()) {
                    categoryMap2.put(c.getName().toLowerCase(), 1.0);
                }
                for (Map.Entry<Term, Double> nGram : preprocessedDocument2.getWeightedTerms().entrySet()) {

                    // check if nGram also appears in first document (we do not
                    // want to take all possible nGrams just yet because the
                    // memory will
                    // overflow)
                    if (temporaryNGramMap.containsKey(nGram.getKey().getText().toLowerCase())) {

                        // get list of categories that are currently assigned to
                        // the nGram
                        HashMap<String, Double> categoryMapEntry = temporaryNGramMap.get(nGram.getKey().getText()
                                .toLowerCase());

                        // add categories from second document to nGram or
                        // update relevance if category existed already
                        for (Entry<String, Double> categoryMap2Entry : categoryMap2.entrySet()) {

                            if (categoryMapEntry.containsKey(categoryMap2Entry.getKey())) {
                                // update relevance
                                Double relevance = categoryMapEntry.get(categoryMap2Entry.getKey());
                                relevance += 1.0;
                                categoryMapEntry.put(categoryMap2Entry.getKey(), relevance);
                            } else {
                                // add category
                                categoryMapEntry.put(categoryMap2Entry.getKey(), 1.0);
                            }

                        }

                    }

                }

                // give memory free for jth document
                preprocessedDocument2.getWeightedTerms().clear();
            }

            // write temporary nGram map to database, all nGrams in that map
            // should not appear again in the training set and are final entries
            // (no
            // update or check in db necessary)
            for (Entry<String, HashMap<String, Double>> entry : temporaryNGramMap.entrySet()) {
                CategoryEntries ces = new CategoryEntries();
                for (Entry<String, Double> categoryMapEntry : entry.getValue().entrySet()) {
                    CategoryEntry ce = new CategoryEntry(ces, new Category(categoryMapEntry.getKey()), categoryMapEntry
                            .getValue());
                    ces.add(ce);
                }
            }
            ((DictionaryClassifier) classifier).addToDictionary(preprocessedDocument, classType);

            // give memory free for ith document
            preprocessedDocument.getWeightedTerms().clear();
        }

        // preprocess test URLs
        size = trainingUrls.size() + testUrls.size();

        LOGGER.info("start preprocessing test documents");

        for (int i = trainingUrls.size(); i < size; ++i) {
            TestDocument preprocessedDocument = null;
            String[] tData = testUrls.get(i - trainingUrls.size());
            preprocessedDocument = (TestDocument) preprocessDocument(tData, TextInstance.TEST);
            classifier.getTestDocuments().add(preprocessedDocument);
        }

    }

    // TODO similar code in TextClassifier (where it belongs, but hierarchy and tagging is only in here)
    private void readTrainingTestingData(Dataset dataset, boolean forTraining, int classType) {

        // reset training and testing urls as well as learned categories
        if (forTraining) {
            classifier.setTrainingDocuments(new ClassificationDocuments());
            classifier.setCategories(new Categories());
            trainingUrls.clear();
        } else {
            classifier.setTestDocuments(new ClassificationDocuments());
            testUrls.clear();
        }

        // determine last line when we want to break in case we don't want to use all data from the dataset
        int lastLine = (int) (FileHelper.getNumberOfLines(dataset.getPath()) * dataset.getUsePercentTraining() / 100.0);

        final Object[] obj = new Object[4];
        obj[0] = forTraining;
        obj[1] = classType;
        obj[2] = dataset;
        obj[3] = lastLine;

        LineAction la = new LineAction(obj) {

            @Override
            public void performAction(String line, int lineNumber) {

                if (lineNumber > (Integer) obj[3]) {
                    looping = false;
                    return;
                }

                String[] siteInformation = line.split(((Dataset) obj[2]).getSeparationString());

                int l = siteInformation.length;
                if ((Integer) obj[1] == ClassificationTypeSetting.SINGLE) {
                    l = 2;
                }

                String[] urlInformation = new String[l];
                urlInformation[0] = siteInformation[0];

                String lastCategoryName = "";
                String lastCategoryPrefix = "";
                for (int i = 1; i < siteInformation.length; ++i) {
                    String[] categorieNames = siteInformation[i].split("/");
                    if (categorieNames.length == 0) {
                        LOGGER.warn("no category names found for " + line);
                        return;
                    }
                    String categoryName = categorieNames[0];

                    // update hierarchy
                    if ((Integer) obj[1] == ClassificationTypeSetting.HIERARCHICAL) {
                        // category names must be saved with the information
                        // about the preceding node
                        if (lastCategoryName.length() > 0) {
                            categoryName = lastCategoryPrefix + "_" + categoryName;
                        }

                        TreeNode newNode = new TreeNode(categoryName);
                        if (i == 1) {
                            ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode.addNode(newNode);
                        } else {
                            ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode.getNode(
                                    lastCategoryName).addNode(newNode);
                        }
                    }
                    urlInformation[i] = categoryName;

                    // add category if it does not exist yet
                    if (!classifier.getCategories().containsCategoryName(categoryName)) {
                        Category cat = new Category(categoryName);
                        if ((Integer) obj[1] == ClassificationTypeSetting.HIERARCHICAL
                                && ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode.getNode(
                                        categoryName).getParent() == ((DictionaryClassifier) classifier)
                                        .getDictionary().hierarchyRootNode
                                        || (Integer) obj[1] == ClassificationTypeSetting.SINGLE) {
                            cat.setMainCategory(true);
                        }
                        cat.setClassType((Integer) obj[1]);
                        cat.increaseFrequency();
                        classifier.getCategories().add(cat);
                    } else {
                        classifier.getCategories().getCategoryByName(categoryName).setClassType((Integer) obj[1]);
                        classifier.getCategories().getCategoryByName(categoryName).increaseFrequency();
                    }

                    // only take first category in "first" mode
                    if ((Integer) obj[1] == ClassificationTypeSetting.SINGLE) {
                        break;
                    }

                    lastCategoryName = categoryName;
                    lastCategoryPrefix = categorieNames[0];
                }

                // add to training urls
                if ((Boolean) obj[0]) {
                    trainingUrls.add(urlInformation);

                } else {
                    // add to test urls
                    testUrls.add(urlInformation);
                }

                if (lineNumber % 1000 == 0) {
                    log("read another 1000 lines from training/testing file, total: " + lineNumber);
                }

            }
        };

        FileHelper.performActionOnEveryLine(dataset.getPath(), la);

        // calculate the prior for all categories classifier.categories.calculatePriors(trainingUrls.size() +
        // testUrls.size());
        classifier.getCategories().calculatePriors();
    }

    public void setCreateDictionaryIteratively(boolean createDictionaryIteratively) {
        this.createDictionaryIteratively = createDictionaryIteratively;
    }

    public final void setTrainingDataPercentage(int trainingDataPercentage) {
        this.trainingDataPercentage = trainingDataPercentage;
    }

    /**
     * consider using evaluateClassifier() in TextClassifier
     */
    @Deprecated
    public final ClassifierPerformance testClassifier(Dataset dataset, TextClassifier classifier) {

        this.classifier = classifier;

        StopWatch sw = new StopWatch();

        // read the testing URLs from the given dataset
        readTrainingTestingData(dataset, false, classifier.getClassificationType());

        LOGGER.info("start classifying " + testUrls.size() + " documents");

        preprocessDocuments(classifier.getClassificationType(), false, false, dataset);

        LOGGER.info("preprocessed documents in " + sw.getElapsedTimeString());

        if (classifier instanceof DictionaryClassifier) {
            classifier.setCategories(((DictionaryClassifier) classifier).getCategories());
        }

        if (!classifier.isSerialize() && classifier instanceof DictionaryClassifier) {
            ((DictionaryClassifier) classifier).classifyTestDocuments(false);
        } else {
            classifier.classifyTestDocuments();
        }

        writeLog(classifier);

        LOGGER.info("classified " + testUrls.size() + " documents in " + sw.getElapsedTimeString());

        return classifier.getPerformance();
    }

    public final void trainAndTestClassifier(TextClassifier classifier, EvaluationSetting evaluationSetting) {

        CrossValidator cv = new CrossValidator();
        cv.setEvaluationSetting(evaluationSetting);
        cv.crossValidate(classifier);

    }

    public final void trainClassifier(Dataset dataset, TextClassifier classifier) {

        this.classifier = classifier;

        stopWatch = new StopWatch();

        if (!(classifier instanceof KNNClassifier)) {

            // set index location (lucene or database)
            ((DictionaryClassifier) classifier).dictionary.setIndexType(dictionaryClassifierIndexType);

            // set database type
            ((DictionaryClassifier) classifier).dictionary.setDatabaseType(dictionaryDatabaseType);

            // set class type
            ((DictionaryClassifier) classifier).dictionary.setClassType(classifier.getClassificationType());

            // if index should be created iteratively, we do not keep it in memory
            // but write it to disk right away
            if (isCreateDictionaryIteratively()) {
                ((DictionaryClassifier) classifier).dictionary.useIndex();

                // in training mode, the dictionary will be deleted first
                if (classifier.isSerialize()) {
                    ((DictionaryClassifier) classifier).dictionary.emptyIndex();
                }
            }
        }

        // read the training URLs from the given dataset
        readTrainingTestingData(dataset, true, classifier.getClassificationType());

        // load the text data from the gathered URLs, preprocess the data and create document representations
        if (classifier.isSerialize()) {

            ((DictionaryClassifier) classifier).dictionary.setReadFromIndexForUpdate(!createDictionaryNGramSearchMode);
            if (createDictionaryNGramSearchMode && isCreateDictionaryIteratively()) {
                preprocessDocumentsFast(classifier.getClassificationType());
            } else {
                preprocessDocuments(classifier.getClassificationType(), isCreateDictionaryIteratively(), true, dataset);
            }

        } else {
            preprocessDocuments(classifier.getClassificationType(), true, true, dataset);
        }

        LOGGER.info("loaded and preprocessed successfully");

        if (classifier instanceof DictionaryClassifier) {

            // create the dictionary in one single step
            if (!isCreateDictionaryIteratively() && classifier.isSerialize()) {
                ((DictionaryClassifier) classifier).buildDictionary(classifier.getClassificationType());
            } else {
                // close the dictionary index writer
                ((DictionaryClassifier) classifier).dictionary.closeIndexWriter();
            }

        }

        // in hierarchy mode we have to tell the dictionary which categories are main categories
        if (classifier.getClassificationType() == ClassificationTypeSetting.HIERARCHICAL) {
            ((DictionaryClassifier) classifier).getDictionary().setMainCategories(classifier.getCategories());
        }

        // save the dictionary (serialize, in-memory dictionary will be deleted at this point)
        if (classifier instanceof DictionaryClassifier && classifier.isSerialize()) {
            // ((DictionaryClassifier) classifier).saveDictionary(((DictionaryClassifier)
            // classifier).getDictionaryPath(),!isCreateDictionaryIteratively(), true);
            ((DictionaryClassifier) classifier).save(((DictionaryClassifier) classifier).getDictionaryPath(),
                    !isCreateDictionaryIteratively(), true);

        }

    }

    private void writeLog(TextClassifier classifier) {

        // create log document
        String timeNeeded = stopWatch.getElapsedTimeString();

        ClassifierPerformance performance = classifier.getPerformance();

        LOGGER.info("Classifier: " + classifier.getName() + " (with parameters: " + classifier.getParameters() + ")");
        LOGGER.info("Classification type: " + classifier.getClassificationType());
        LOGGER.info("Document Representation Settings: " + classifier.getFeatureSetting() + " Weights: Domain "
                + Preprocessor.WEIGHT_DOMAIN_TERM + " Title " + Preprocessor.WEIGHT_TITLE_TERM + " Keyword "
                + Preprocessor.WEIGHT_KEYWORD_TERM + " Meta " + Preprocessor.WEIGHT_META_TERM + " Body "
                + Preprocessor.WEIGHT_BODY_TERM);
        LOGGER.info("Use " + trainingDataPercentage + "% as training data. Loaded " + trainingUrls.size()
                + " training urls, " + testUrls.size() + " test urls in " + classifier.getCategories().size()
                + " categories");
        LOGGER.info("Runtime: " + timeNeeded);

        if (classifier.getClassificationType() != ClassificationTypeSetting.TAG) {

            LOGGER
            .info("Category                      Training  Test  Classified  Correct  Precision        Recall           F1               Sensitivity      Specificity      Accuracy         Weight/Prior");

            int totalCorrect = 0;
            for (Category category : classifier.getCategories()) {

                // skip categories that are not main categories because they are
                // classified according to the main category
                if (classifier.getClassificationType() == ClassificationTypeSetting.HIERARCHICAL
                        && !category.isMainCategory()) {
                    continue;
                }

                StringBuilder logLine = new StringBuilder(category.getName());
                for (int i = 0, l = logLine.length(); i < Math.max(0, 30 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(classifier.getTrainingDocuments().getRealNumberOfCategory(category));
                for (int i = 0, l = logLine.length(); i < Math.max(0, 40 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(classifier.getTestDocuments().getRealNumberOfCategory(category));
                for (int i = 0, l = logLine.length(); i < Math.max(0, 46 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(classifier.getTestDocuments().getClassifiedNumberOfCategory(category));
                for (int i = 0, l = logLine.length(); i < Math.max(0, 58 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(performance.getNumberOfCorrectClassifiedDocumentsInCategory(category));
                totalCorrect += performance.getNumberOfCorrectClassifiedDocumentsInCategory(category);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 67 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append((int) Math.floor(100 * performance.getPrecisionForCategory(category)) + "%");
                for (int i = 0, l = logLine.length(); i < Math.max(0, 84 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append((int) Math.floor(100 * performance.getRecallForCategory(category)) + "%");
                for (int i = 0, l = logLine.length(); i < Math.max(0, 101 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(Math.floor(100 * performance.getFForCategory(category, 0.5)) / 100);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 118 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append((int) Math.floor(100 * performance.getSensitivityForCategory(category)) + "%");
                for (int i = 0, l = logLine.length(); i < Math.max(0, 135 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append((int) Math.floor(100 * performance.getSpecificityForCategory(category)) + "%");
                for (int i = 0, l = logLine.length(); i < Math.max(0, 152 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(MathHelper.round(performance.getAccuracyForCategory(category), 2));
                for (int i = 0, l = logLine.length(); i < Math.max(0, 169 - l); i++) {
                    logLine.append(" ");
                }
                logLine.append(MathHelper.round(performance.getWeightForCategory(category), 2));
                LOGGER.info(logLine.toString());
            }
            LOGGER.info("Average Precision: " + (int) Math.floor(100 * performance.getAveragePrecision(false))
                    + "%, weighted: " + (int) Math.floor(100 * performance.getAveragePrecision(true)) + "%");
            LOGGER.info("Average Recall: " + (int) Math.floor(100 * performance.getAverageRecall(false))
                    + "%, weighted: " + (int) Math.floor(100 * performance.getAverageRecall(true)) + "%");
            LOGGER.info("Average F1: " + Math.floor(1000 * performance.getAverageF(0.5, false)) / 1000 + ", weighted: "
                    + Math.floor(1000 * performance.getAverageF(0.5, true)) / 1000);
            LOGGER.info("Average Sensitivity: " + (int) Math.floor(100 * performance.getAverageSensitivity(false))
                    + "%, weighted: " + (int) Math.floor(100 * performance.getAverageSensitivity(true)) + "%");
            LOGGER.info("Average Specificity: " + (int) Math.floor(100 * performance.getAverageSpecificity(false))
                    + "%, weighted: " + (int) Math.floor(100 * performance.getAverageSpecificity(true)) + "%");
            LOGGER.info("Average Accuracy: " + Math.floor(1000 * performance.getAverageAccuracy(false)) / 1000
                    + ", weighted: " + Math.floor(1000 * performance.getAverageAccuracy(true)) / 1000);

            if (classifier.getClassificationType() == ClassificationTypeSetting.SINGLE) {
                double correctClassified = (double) totalCorrect / (double) testUrls.size();
                LOGGER.info("Correctly Classified: " + MathHelper.round(100 * correctClassified, 2) + "%");
            }

        }

        LOGGER.info("\nClassified Documents in Detail:");
        LOGGER.info(classifier.showTestDocuments());

        LOGGER.info("FINISH, classified and logged successfully in " + stopWatch.getElapsedTimeString());
    }
}