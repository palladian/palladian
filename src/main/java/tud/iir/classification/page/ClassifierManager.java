package tud.iir.classification.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Dictionary;
import tud.iir.classification.Term;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.TreeNode;
import tud.iir.temp.CSVRewriter;
import tud.iir.temp.TrainingDataSeparation;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;

/**
 * This class loads the training and test data, classifies and stores the results.
 * 
 * @author David Urbansky
 */
public class ClassifierManager {

    private static final Logger LOGGER = Logger.getLogger(ClassifierManager.class);

    /** The configuration must be located in config/classification.conf */
    private static PropertiesConfiguration config = null;

    /** the classifier used to categorize the web sites */
    private WebPageClassifier classifier = null;

    /** percentage of pages used as training data */
    private int trainingDataPercentage = 20;

    /** if true, a preprocessed document will be added to the dictionary right away, that saves memory */
    private boolean createDictionaryIteratively = true;

    /** decide whether to index the dictionary in lucene or database */
    private int dictionaryClassifierIndexType = Dictionary.DB_INDEX_FAST;

    /** decide whether to use mysql or h2 if index type is database */
    private int dictionaryDatabaseType = Dictionary.DB_MYSQL;

    /** if true, all n-grams will be searched once before inserting in the db, which saves look up time */
    private boolean createDictionaryNGramSearchMode = true;

    /** the list of training URLs */
    private URLs trainingUrls = null;

    /** the list of test URLs */
    private URLs testUrls = null;

    // // classification modes
    /** train model, serialize model and use serialized model for test */
    public static int CLASSIFICATION_TRAIN_TEST_SERIALIZE = 1;

    /** train model and used trained model for testing without serializing it */
    public static int CLASSIFICATION_TRAIN_TEST_VOLATILE = 2;

    /** test model without training it again (model has to exist) */
    public static int CLASSIFICATION_TEST_MODEL = 3;

    /** the character sequence that splits the training data and the class in the input file */
    private String separationString = " ";

    int classificationMode = CLASSIFICATION_TRAIN_TEST_SERIALIZE;

    public ClassifierManager() {

        // try to find the classification configuration, if it is not present
        // use default values
        try {
            config = new PropertiesConfiguration("config/classification.conf");
            if (config.getInt("page.trainingPercentage") > -1) {
                setTrainingDataPercentage(config.getInt("page.trainingPercentage"));
            }
            createDictionaryIteratively = config.getBoolean("page.createDictionaryIteratively");
            dictionaryClassifierIndexType = config.getInt("page.dictionaryClassifierIndexType");
            dictionaryDatabaseType = config.getInt("page.databaseType");
            createDictionaryNGramSearchMode = config.getBoolean("page.createDictionaryNGramSearchMode");

        } catch (ConfigurationException e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * Retrieve web pages for a set of categories implying their category.
     */
    public void learnAndTestClassifierOnline() {

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
        SourceRetriever sr = new SourceRetriever();
        sr.setSource(SourceRetrieverManager.GOOGLE);
        sr.setResultCount(50);
        sr.setLanguage(SourceRetriever.LANGUAGE_GERMAN);

        Crawler crawler = new Crawler();

        StringBuilder fileIndex = new StringBuilder();
        StringBuilder urlIndex = new StringBuilder();
        int fileCounter = 1;

        System.out.println("Start retrieving web pages...");
        for (Map.Entry<String, HashSet<String>> category : dictionary.entrySet()) {

            for (String keyword : category.getValue()) {
                ArrayList<String> urls = sr.getURLs(keyword);

                for (String url : urls) {
                    String shortURLName = StringHelper.makeSafeName(Crawler.getCleanURL(url));
                    String cleanURLName = "webpage" + (fileCounter++) + "_" + shortURLName.substring(0, Math.min(25, shortURLName.length())) + ".html";

                    // download file
                    if (crawler.downloadAndSave(url, "data/benchmarkSelection/page/automatic/" + cleanURLName)) {
                        fileIndex.append(cleanURLName).append(" ").append(category.getKey()).append("\n");
                        urlIndex.append(Crawler.getCleanURL(url)).append(" ").append(category.getKey()).append("\n");
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
     * Start training a classifier.
     * 
     * @filePath The path of the text file with the URLs and categories.
     * @classifierType The type of the classifier that should be trained.
     */
    public void trainAndTestClassifier(String filePath, int classifierType, int classType, int classificationMode) {

        long startTime = System.currentTimeMillis();
        // if (classificationMode != CLASSIFICATION_TEST_MODEL) {
        // create classifier
        if (classifierType == WebPageClassifier.URL) {
            classifier = new URLClassifier();
        } else if (classifierType == WebPageClassifier.FULL_PAGE) {
            classifier = new FullPageClassifier();
        } else if (classifierType == WebPageClassifier.COMBINED) {
            classifier = new CombinedClassifier();
        } else if (classifierType == WebPageClassifier.KNN) {
            classifier = new KNNClassifier();
        }
        // XXX temp. added by Philipp,
        // to allow classification of local text contents,
        // see JavaDoc comments of the class for more information
        else {
            classifier = new TextClassifier();
        }
        // }

        if (classifierType != WebPageClassifier.KNN) {

            // set index location (lucene or database)
            ((DictionaryClassifier) classifier).dictionary.setIndexType(dictionaryClassifierIndexType);

            // set database type
            ((DictionaryClassifier) classifier).dictionary.setDatabaseType(dictionaryDatabaseType);

            // set class type
            ((DictionaryClassifier) classifier).dictionary.setClassType(classType);

            // if index should be created iteratively, we do not keep it in memory
            // but write it to disk right away
            if (createDictionaryIteratively) {
                ((DictionaryClassifier) classifier).dictionary.useIndex(classType);

                // in training mode, the dictionary will be deleted first
                if (classificationMode == CLASSIFICATION_TRAIN_TEST_SERIALIZE) {
                    ((DictionaryClassifier) classifier).dictionary.emptyIndex();
                }
            }

        }

        // classifier.setBenchmark(true);

        // read the urls (training and test data) from urls.txt file
        trainingUrls = new URLs();
        testUrls = new URLs();
        readTrainingTestingData(trainingDataPercentage, filePath, classType);

        // load the text data from the gathered urls, preprocess the data and
        // create document representations
        if (classificationMode == CLASSIFICATION_TRAIN_TEST_SERIALIZE) {

            ((DictionaryClassifier) classifier).dictionary.setReadFromIndexForUpdate(!createDictionaryNGramSearchMode);
            if (createDictionaryNGramSearchMode && createDictionaryIteratively) {
                preprocessDocumentsFast(classType);
            } else {
                preprocessDocuments(classType, createDictionaryIteratively);
            }

        } else if (classificationMode == CLASSIFICATION_TEST_MODEL) {
            preprocessDocuments(classType, false);
        } else if (classificationMode == CLASSIFICATION_TRAIN_TEST_VOLATILE) {
            if (classifierType != WebPageClassifier.KNN) {
                preprocessDocuments(classType, true);
            } else {
                preprocessDocuments(classType, false);
            }
        }

        LOGGER.info("loaded and preprocessed successfully");

        // create the dictionary in one single step
        if (!createDictionaryIteratively && classificationMode == CLASSIFICATION_TRAIN_TEST_SERIALIZE) {
            ((DictionaryClassifier) classifier).buildDictionary(classType);
        }
        // close the dictionary index writer
        else if (classifierType != WebPageClassifier.KNN) {
            ((DictionaryClassifier) classifier).dictionary.closeIndexWriter();
        }

        // in hierarchy mode we have to tell the dictionary which categories are
        // main categories
        if (classType == WebPageClassifier.HIERARCHICAL) {
            ((DictionaryClassifier) classifier).getDictionary().setMainCategories(classifier.categories);
        }

        // save the dictionary (serialize, in-memory dictionary will be deleted
        // at this point)
        if (classificationMode == CLASSIFICATION_TRAIN_TEST_SERIALIZE) {
            ((DictionaryClassifier) classifier).saveDictionary(classType, !createDictionaryIteratively, true);
        }

        LOGGER.info("start classifying " + testUrls.size() + " documents");

        if (classifier instanceof DictionaryClassifier) {
            classifier.setCategories(((DictionaryClassifier) classifier).getCategories());
        }

        if (classificationMode == CLASSIFICATION_TRAIN_TEST_VOLATILE) {
            if (classifier instanceof DictionaryClassifier) {
                ((DictionaryClassifier) classifier).classifyTestDocuments(classType, false);
            } else {
                ((WebPageClassifier) classifier).classifyTestDocuments(classType);
            }
        } else {
            classifier.classifyTestDocuments(classType);
        }

        // create log document
        String timeNeeded = DateHelper.getRuntime(startTime);

        LOGGER.info("Classifier: " + classifier.getName() + " (with parameters: " + classifier.getParameters() + ")");
        LOGGER.info("Classification type: " + classType);
        LOGGER.info("Document Representation Settings: maximum terms: " + Preprocessor.MAXIMUM_TERMS + " url n-grams min/max: " + Preprocessor.MIN_NGRAM_SIZE
                + "/" + Preprocessor.MAX_NGRAM_SIZE + " Weights: Domain " + Preprocessor.WEIGHT_DOMAIN_TERM + " Title " + Preprocessor.WEIGHT_TITLE_TERM
                + " Keyword " + Preprocessor.WEIGHT_KEYWORD_TERM + " Meta " + Preprocessor.WEIGHT_META_TERM + " Body " + Preprocessor.WEIGHT_BODY_TERM);
        LOGGER.info("Use " + trainingDataPercentage + "% as training data. Loaded " + trainingUrls.size() + " training urls, " + testUrls.size()
                + " test urls in " + classifier.categories.size() + " categories");
        LOGGER.info("Runtime: " + timeNeeded);

        if (classType != WebPageClassifier.TAG) {

            LOGGER
                    .info("Category                      Training  Test  Classified  Correct  Precision        Recall           F1               Sensitivity      Specificity      Accuracy         Weight/Prior");

            int totalCorrect = 0;
            for (Category category : classifier.categories) {

                // skip categories that are not main categories because they are
                // classified according to the main category
                if (classType == WebPageClassifier.HIERARCHICAL && !category.isMainCategory()) {
                    continue;
                }

                String logLine = category.getName();
                for (int i = 0, l = logLine.length(); i < Math.max(0, 30 - l); i++) {
                    logLine += " ";
                }
                logLine += classifier.trainingDocuments.getRealNumberOfCategory(category);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 40 - l); i++) {
                    logLine += " ";
                }
                logLine += classifier.testDocuments.getRealNumberOfCategory(category);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 46 - l); i++) {
                    logLine += " ";
                }
                logLine += classifier.testDocuments.getClassifiedNumberOfCategory(category);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 58 - l); i++) {
                    logLine += " ";
                }
                logLine += classifier.getNumberOfCorrectClassifiedDocumentsInCategory(category);
                totalCorrect += classifier.getNumberOfCorrectClassifiedDocumentsInCategory(category);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 67 - l); i++) {
                    logLine += " ";
                }
                logLine += (int) Math.floor(100 * classifier.getPrecisionForCategory(category)) + "%";
                for (int i = 0, l = logLine.length(); i < Math.max(0, 84 - l); i++) {
                    logLine += " ";
                }
                logLine += (int) Math.floor(100 * classifier.getRecallForCategory(category)) + "%";
                for (int i = 0, l = logLine.length(); i < Math.max(0, 101 - l); i++) {
                    logLine += " ";
                }
                logLine += Math.floor(100 * classifier.getFForCategory(category, 0.5)) / 100;
                for (int i = 0, l = logLine.length(); i < Math.max(0, 118 - l); i++) {
                    logLine += " ";
                }
                logLine += (int) Math.floor(100 * classifier.getSensitivityForCategory(category)) + "%";
                for (int i = 0, l = logLine.length(); i < Math.max(0, 135 - l); i++) {
                    logLine += " ";
                }
                logLine += (int) Math.floor(100 * classifier.getSpecificityForCategory(category)) + "%";
                for (int i = 0, l = logLine.length(); i < Math.max(0, 152 - l); i++) {
                    logLine += " ";
                }
                logLine += MathHelper.round(classifier.getAccuracyForCategory(category), 2);
                for (int i = 0, l = logLine.length(); i < Math.max(0, 169 - l); i++) {
                    logLine += " ";
                }
                logLine += MathHelper.round(classifier.getWeightForCategory(category), 2);
                LOGGER.info(logLine);
            }
            LOGGER.info("Average Precision: " + (int) Math.floor(100 * classifier.getAveragePrecision(false)) + "%, weighted: "
                    + (int) Math.floor(100 * classifier.getAveragePrecision(true)) + "%");
            LOGGER.info("Average Recall: " + (int) Math.floor(100 * classifier.getAverageRecall(false)) + "%, weighted: "
                    + (int) Math.floor(100 * classifier.getAverageRecall(true)) + "%");
            LOGGER.info("Average F1: " + Math.floor(1000 * classifier.getAverageF(0.5, false)) / 1000 + ", weighted: "
                    + Math.floor(1000 * classifier.getAverageF(0.5, true)) / 1000);
            LOGGER.info("Average Sensitivity: " + (int) Math.floor(100 * classifier.getAverageSensitivity(false)) + "%, weighted: "
                    + (int) Math.floor(100 * classifier.getAverageSensitivity(true)) + "%");
            LOGGER.info("Average Specificity: " + (int) Math.floor(100 * classifier.getAverageSpecificity(false)) + "%, weighted: "
                    + (int) Math.floor(100 * classifier.getAverageSpecificity(true)) + "%");
            LOGGER.info("Average Accuracy: " + Math.floor(1000 * classifier.getAverageAccuracy(false)) / 1000 + ", weighted: "
                    + Math.floor(1000 * classifier.getAverageAccuracy(true)) / 1000);

            if (classType == WebPageClassifier.FIRST) {
                double correctClassified = (double) totalCorrect / (double) testUrls.size();
                LOGGER.info("Correctly Classified: " + MathHelper.round(100 * correctClassified, 2) + "%");
            }

        }

        LOGGER.info("\nClassified Documents in Detail:");
        LOGGER.info(classifier.showTestDocuments(classType));

        LOGGER.info("FINISH, classified and logged successfully " + DateHelper.getRuntime(startTime));
    }

    /**
     * Load, read and build the training data. URLs from the text file are separated into test and training URLs and we create the categories.
     * 
     * @param trainingPercentage Number in percent of how many documents should be used as training data.
     */
    private void readTrainingTestingData(double trainingPercentage, String filePath, int classType) {

        final Object[] obj = new Object[2];
        obj[0] = trainingPercentage;
        obj[1] = classType;

        LineAction la = new LineAction(obj) {

            @Override
            public void performAction(String line, int lineNumber) {

                double trainingStep = 100.0 / (Double) obj[0];

                String[] siteInformation = line.split(getSeparationString());

                int l = siteInformation.length;
                if (((Integer) obj[1]) == WebPageClassifier.FIRST) {
                    l = 2;
                }

                String[] urlInformation = new String[l];
                urlInformation[0] = siteInformation[0];

                String lastCategoryName = "";
                String lastCategoryPrefix = "";
                for (int i = 1; i < siteInformation.length; ++i) {
                    String[] categorieNames = siteInformation[i].split("/");
                    if (categorieNames.length == 0) {
                        LOGGER.debug("no category names found for " + line);
                        return;
                    }
                    String categoryName = categorieNames[0];

                    // update hierarchy
                    if (((Integer) obj[1]) == WebPageClassifier.HIERARCHICAL) {
                        // category names must be saved with the information
                        // about the preceding node
                        if (lastCategoryName.length() > 0)
                            categoryName = lastCategoryPrefix + "_" + categoryName;

                        TreeNode newNode = new TreeNode(categoryName);
                        if (i == 1) {
                            ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode.addNode(newNode);
                        } else {
                            ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode.getNode(lastCategoryName).addNode(newNode);
                        }
                    }
                    urlInformation[i] = categoryName;

                    // add category if it does not exist yet
                    if (!classifier.categories.containsCategoryName(categoryName)) {
                        Category cat = new Category(categoryName);
                        if ((((Integer) obj[1]) == WebPageClassifier.HIERARCHICAL && ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode
                                .getNode(categoryName).getParent() == ((DictionaryClassifier) classifier).getDictionary().hierarchyRootNode)
                                || ((Integer) obj[1]) == WebPageClassifier.FIRST) {
                            cat.setMainCategory(true);
                        }
                        cat.setClassType((Integer) obj[1]);
                        cat.increaseFrequency();
                        classifier.categories.add(cat);
                    } else {
                        classifier.categories.getCategoryByName(categoryName).setClassType((Integer) obj[1]);
                        classifier.categories.getCategoryByName(categoryName).increaseFrequency();
                    }

                    // only take first category in "first" mode
                    if (((Integer) obj[1]) == WebPageClassifier.FIRST) {
                        break;
                    }

                    lastCategoryName = categoryName;
                    lastCategoryPrefix = categorieNames[0];
                }

                // add to training urls
                if (lineNumber % trainingStep < 1) {
                    trainingUrls.add(urlInformation);

                    // add to test urls
                } else {
                    testUrls.add(urlInformation);
                }

                if (lineNumber % 1000 == 0) {
                    log("read another 1000 lines from training/testing file, total: " + lineNumber);
                }

            }
        };

        FileHelper.performActionOnEveryLine(filePath, la);

        // calculate the prior for all categories
        // classifier.categories.calculatePriors(trainingUrls.size() +
        // testUrls.size());
        classifier.categories.calculatePriors();
    }

    /**
     * Create a document representation of the data read.
     */
    private void preprocessDocuments(int classType, boolean addToDictionary) {

        int size = trainingUrls.size() + testUrls.size();

        for (int i = 0; i < size; ++i) {

            String[] tData;
            ClassificationDocument preprocessedDocument = null;

            boolean isTrainingDocument;
            if (i < trainingUrls.size()) {
                isTrainingDocument = true;
                tData = trainingUrls.get(i);

                // free memory, delete training URL
                // trainingUrls.set(i, empty);

                preprocessedDocument = new ClassificationDocument();
            } else {
                isTrainingDocument = false;
                tData = testUrls.get(i - trainingUrls.size());

                // free memory, delete testing URL
                // testUrls.set(i - trainingUrls.size(), empty);

                preprocessedDocument = new TestDocument();
            }

            String url = tData[0];

            if (!isTrainingDocument) {
                preprocessedDocument = (TestDocument) classifier.preprocessDocument(url, preprocessedDocument);
            } else {
                preprocessedDocument = classifier.preprocessDocument(url, preprocessedDocument);
            }

            preprocessedDocument.setUrl(tData[0]);

            Categories categories = new Categories();
            for (int j = 1; j < tData.length; j++) {
                categories.add(new Category(tData[j]));
            }
            if (isTrainingDocument) {
                preprocessedDocument.setDocumentType(ClassificationDocument.TRAINING);
                preprocessedDocument.setRealCategories(categories);
                classifier.trainingDocuments.add(preprocessedDocument);

                if (addToDictionary) {
                    ((DictionaryClassifier) classifier).addToDictionary(preprocessedDocument, classType);
                }

            } else {
                preprocessedDocument.setDocumentType(ClassificationDocument.TEST);
                preprocessedDocument.setRealCategories(categories);
                classifier.testDocuments.add(preprocessedDocument);
            }
            log(Math.floor(100.0 * (double) i / (double) size) + "% preprocessed: " + tData[0] + ", i:" + i + ", size:" + size);

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

            LOGGER.info("processed " + MathHelper.round(100 * i / size, 2) + "% of the documents, ngrams: " + nGramFound.size() + ", time: "
                    + DateHelper.getRuntime(classifier.initTime));

            ClassificationDocument preprocessedDocument = null;

            String[] tData = trainingUrls.get(i);
            preprocessedDocument = preprocessDocument(tData, ClassificationDocument.TRAINING);
            classifier.trainingDocuments.add(preprocessedDocument);

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

            if (temporaryNGramMap.size() == 0)
                continue;

            // find same nGrams in all following documents
            for (int j = i + 1; j < size; ++j) {
                String[] tData2 = trainingUrls.get(j);
                ClassificationDocument preprocessedDocument2 = preprocessDocument(tData2, ClassificationDocument.TRAINING);

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
                        HashMap<String, Double> categoryMapEntry = temporaryNGramMap.get(nGram.getKey().getText().toLowerCase());

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
                    CategoryEntry ce = new CategoryEntry(ces, new Category(categoryMapEntry.getKey()), categoryMapEntry.getValue());
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
            preprocessedDocument = (TestDocument) preprocessDocument(tData, ClassificationDocument.TEST);
            classifier.testDocuments.add(preprocessedDocument);
        }

    }

    private ClassificationDocument preprocessDocument(String[] data, int type) {
        ClassificationDocument preprocessedDocument = null;

        if (type == ClassificationDocument.TEST) {
            preprocessedDocument = new TestDocument();
        } else {
            preprocessedDocument = new ClassificationDocument();
        }
        preprocessedDocument = classifier.preprocessDocument(data[0], preprocessedDocument);

        preprocessedDocument.setUrl(data[0]);

        Categories categories = new Categories();
        for (int j = 1; j < data.length; j++) {
            categories.add(new Category(data[j]));
        }

        preprocessedDocument.setRealCategories(categories);
        preprocessedDocument.setDocumentType(type);

        return preprocessedDocument;
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public int getTrainingDataPercentage() {
        return trainingDataPercentage;
    }

    public void setTrainingDataPercentage(int trainingDataPercentage) {
        this.trainingDataPercentage = trainingDataPercentage;
    }

    public void setSeparationString(String separationString) {
        this.separationString = separationString;
    }

    public String getSeparationString() {
        return separationString;
    }
    
    private void setThreshold(double threshold) {
		classifier.setTagConfidenceThreshold(threshold);
	}

	/**
	 * Method to compare the open analytix performance for classification depending on
	 * values trainingPercentage, threshold for assigning a second category and number 
	 * of loops to average the performance with fixed trainingPercentage and threshold
	 * but random select of lines to be assigned to training and testing set
	 * 
	 * @param trainingPercentageMin The percentage of the data set to be used for 
	 * training - minimum value of loop, range [0,100].
	 * @param trainingPercentageMax The percentage of the data set to be used for 
	 * training - maximum value of loop, range [0,100].
	 * @param trainingPercentageStep The percentage of the data set to be used for 
	 * training - step between loops, range [0,100].
	 * @param randomSplitTrainingDataSet If true, initial data set is split randomly 
	 * into training and test set (fixed percentage but randomly chosen lines). If false,
	 * the first lines are training set and the remainder is the test set.
	 * @param numberLoopsToAverage Number of loops to average the performance with 
	 * fixed trainingPercentage and threshold but random select of lines to be 
	 * assigned to training and testing set. Ignored if randomSplitTrainingDataSet=false,
	 * e.g. only one loop is executed per trainingPercentage and threshold.
	 * @param thMin Minimum value for the threshold used to assign a second category.
	 * @param thMax Maximum value for the threshold used to assign a second category.
	 * @param thStep Value to add to the threshold per loop. 
	 * @param classType The type of WebPageClassifier to be used, e.g. WebPageClassifier.FIRST.
	 */
	private void openAnalytix(int trainingPercentageMin, 
			int trainingPercentageMax, 
			int trainingPercentageStep,
			boolean randomSplitTrainingDataSet,
			int numberLoopsToAverage,
			int thMin, 
			int thMax, 
			int thStep,
			int classType) {
		
		if(!randomSplitTrainingDataSet) numberLoopsToAverage = 1;
		
		ClassifierManager classifierManager = new ClassifierManager();
		

		// helper
		int numberTrainingPercentageLoops = ((trainingPercentageMax - trainingPercentageMin) / trainingPercentageStep)+1;
		int numberThresholdLoops = (int)Math.abs(((double)(thMax-thMin)/(double)thStep))+1;

		// results[trainingPercentage][threshold][iteration]
		// TODO change to numberLoopsToAverage to add average value in last line
		double[][][] openAnalytixPerformances = new double[numberTrainingPercentageLoops][numberThresholdLoops][numberLoopsToAverage];
		double[][][] numberOf2Categories = new double[numberTrainingPercentageLoops][numberThresholdLoops][numberLoopsToAverage];
		int[] trainingPercentageUsed = new int[numberTrainingPercentageLoops];
		int[] thresholdsUsed = new int[numberThresholdLoops];
		int trainingPercentageLoop = 0;
		
		
		
		// e.g. test from 40:60 to 90:10 
		for (int trainingPercentage = trainingPercentageMin; 
			trainingPercentage <= trainingPercentageMax ; 
			trainingPercentage += trainingPercentageStep) {
			
			trainingPercentageUsed[trainingPercentageLoop] = trainingPercentage;
			logger.info("\n start trainingPercentage classification loop on dataRewrittenCombined_Testing.csv with trainingPercentage " + trainingPercentage + "%, random = " + randomSplitTrainingDataSet + "\n");			
			
			// test with different thresholds
			int thresholdLoop=0;
			for (int th = thMin; th <= thMax; th += thStep) {
				
				logger.info("\n start thresholdLoop classification loop on dataRewrittenCombined_Testing.csv with trainingPercentage " + trainingPercentage + "%, random = " + randomSplitTrainingDataSet + ", threshold = " + (double)th/100 + "\n");
				thresholdsUsed[thresholdLoop] = th;
				// e.g. 10 loops to average over random selection training and test data 
				for (int k = 0; k < numberLoopsToAverage; k++){
					
					logger.info("\n start inner (cross-validation) classification loop on dataRewrittenCombined_Testing.csv with trainingPercentage " + trainingPercentage + "%, random = " + randomSplitTrainingDataSet + ", threshold = " + (double)th/100 + ", iteration = " + (k+1) + "\n");
										
					String currentTime = DateHelper.getCurrentDatetime();
					new TrainingDataSeparation().seperateFile(trainingPercentage, randomSplitTrainingDataSet);
					new CSVRewriter().rewriteOutputGoldstandard();
//					createDictionaryIteratively = false;
					// train with two categories 
					classifierManager.setSeparationString("###");
					classifierManager.setTrainingDataPercentage(100);
//					classifierManager.trainAndTestClassifier("data/temp/dataRewrittenCombined_Training.csv", WebPageClassifier.URL, WebPageClassifier.TAG, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
//					classifierManager.trainAndTestClassifier("data/temp/dataRewrittenCombined_Training.csv", WebPageClassifier.URL, WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE, true); // 95%
					classifierManager.trainAndTestClassifier("data/temp/dataRewrittenCombined_Training.csv", WebPageClassifier.URL, classType, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE, true);
					
					// classify 
//					classifierManager = new ClassifierManager();
//					classifierManager.setSeparationString("###");
					classifierManager.setTrainingDataPercentage(0);
					classifierManager.setThreshold((double)th/100);
//					classifierManager.trainAndTestClassifier("data/temp/dataRewrittenCombined_Testing.csv", WebPageClassifier.URL, WebPageClassifier.TAG, ClassifierManager.CLASSIFICATION_TEST_MODEL);
//					classifierManager.trainAndTestClassifier("data/temp/dataRewrittenCombined_Testing.csv", WebPageClassifier.URL, WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TEST_MODEL, false); // 95% ??
					// wenn createNewClassifier=true, dann ist Dictionary.dictionaryIndex wieder null , d.h. die jdbc Verbindung verloren
					classifierManager.trainAndTestClassifier("data/temp/dataRewrittenCombined_Testing.csv", WebPageClassifier.URL, classType, ClassifierManager.CLASSIFICATION_TEST_MODEL, false);
					
					HashMap<String,Double> statistics = classifierManager.getClassifier().showTestDocuments(WebPageClassifier.FIRST);
					
					if(statistics.containsKey("Number of two categories")){
						numberOf2Categories[trainingPercentageLoop][thresholdLoop][k] = statistics.get("Number of two categories");
					}
					else //e.g. WebPageClassifier.FIRST - modus
					numberOf2Categories[trainingPercentageLoop][thresholdLoop][k] = 0;
					
					//rewrite output
					CSVRewriter csvRewriter = new CSVRewriter();
					csvRewriter.rewriteOutput();
					csvRewriter.combineGoldstandardAndPredictedCategories();			
					double performance = csvRewriter.evaluate("data/temp/" + currentTime + "_dataTestAndClassifiedResults_trainingPercentage_" + trainingPercentage + "_threshold_"+ (double)th/100 +"_co-occurrenceBoost_"+DictionaryClassifier.COOCCURRENCE_BOOST+".csv");
					
					StringBuilder trainingsetPercentSB = new StringBuilder();
					trainingsetPercentSB.append(currentTime);
					trainingsetPercentSB.append("random trainingPercentage: ").append(trainingPercentage).append("\n");
					FileHelper.appendToFile("data/temp/thresholds.txt", trainingsetPercentSB, false);
					logger.info("\n finished inner (cross-validation) classification loop on dataRewrittenCombined_Testing.csv with trainingPercentage " + trainingPercentage + "%, random = " + randomSplitTrainingDataSet + ", threshold = " + (double)th/100 + ", iteration = " + (k+1) + "\n");
					
					openAnalytixPerformances[trainingPercentageLoop][thresholdLoop][k] = performance;
				}
				thresholdLoop++;
				logger.info("\n finished thresholdLoop classification loop on dataRewrittenCombined_Testing.csv with trainingPercentage " + trainingPercentage + "%, random = " + randomSplitTrainingDataSet + ", threshold = " + (double)th/100 + "\n");
			}
			logger.info("\n finished trainingPercentage classification loop on dataRewrittenCombined_Testing.csv with trainingPercentage " + trainingPercentage + "%, random = " + randomSplitTrainingDataSet + "\n");
			trainingPercentageLoop++;
		}	
		
//		//print results
		String resultFilePath = "data/temp/" + DateHelper.getCurrentDatetime() + "_results.csv";
		System.out.println("Writing final results to " + resultFilePath);
		
		String useRandom = (randomSplitTrainingDataSet)? "random":"static";
		
		StringBuilder finalResultSB = new StringBuilder();
		finalResultSB.append("ave perf @ ").append(numberLoopsToAverage).append(";");
				
		for (int i = 0; i < numberThresholdLoops; i++) {
			finalResultSB.append("thres ").append((double)thresholdsUsed[i]/100).append(";std deviation;# 2 cat;");
		}
		finalResultSB.append("\n");
		
		for (int i = 0; i < numberTrainingPercentageLoops; i++) {
			finalResultSB.append("train ").append(trainingPercentageUsed[i]).append("% ").append(useRandom).append(";");
			for (int j = 0; j < numberThresholdLoops; j++) {
				double culmulatedPrecision = 0;
				double culmulatedNumber2Categories = 0;
				for (int k = 0; k < numberLoopsToAverage; k++){					
					culmulatedPrecision += openAnalytixPerformances[i][j][k];
					culmulatedNumber2Categories += numberOf2Categories[i][j][k];					
				}
				finalResultSB.append(MathHelper.round(culmulatedPrecision/numberLoopsToAverage, 4)).append(";");
				StandardDeviation std = new StandardDeviation();
				double stdDev = std.evaluate(openAnalytixPerformances[i][j]);
				finalResultSB.append(MathHelper.round(stdDev, 4)).append(";");
				finalResultSB.append(MathHelper.round(culmulatedNumber2Categories/numberLoopsToAverage, 1)).append(";");
			}
			finalResultSB.append("\n");
			System.out.print("\n");
		}
		
		FileHelper.writeToFile(resultFilePath, finalResultSB);
	}

	private WebPageClassifier getClassifier() {
		return this.classifier;
	}
        
    /**
     * If arguments are given, they must be in the following order: trainingPercentage inputFilePath classifierType classificationType training For example:
     * java -jar classifierManager.jar 80 data/benchmarkSelection/page/deliciouspages_cleansed_400.txt 1 3 true
     * 
     * @param args
     */
    public static void main(String[] args) {

        if (args.length > 0) {
            System.out.println("arguments found");
            ClassifierManager classifierManager = new ClassifierManager();
            classifierManager.setTrainingDataPercentage(Integer.parseInt(args[0]));
            classifierManager.trainAndTestClassifier(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            System.out.println("finished");
            System.exit(0);
        }

        // URLClassifier classifier = new URLClassifier();
        // ((DictionaryClassifier) classifier).dictionary.setIndexType(2);
        // ((DictionaryClassifier) classifier).saveDictionary(3);
        //		
        // Dictionary d = (Dictionary)
        // FileHelper.deserialize("data/models/dictionary_URLClassifier_3.ser");
        //		
        // System.exit(0);

        // String[] arguments = { "knn", "3" };
        // new ClassifierManager().start_(arguments);
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/index.txt");
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls.txt");
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/url/list_german_sample20000.txt");
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/url/4categories_urls.txt",
        // false);
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/url/4categories_urls.txt",
        // WebPageClassifier.URL);
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/automatic/4categories_index.txt",
        // WebPageClassifier.FULL_PAGE);
        // new
        // ClassifierManager().trainAndTestClassifier("data/benchmarkSelection/page/automatic/4categories_index.txt",
        // WebPageClassifier.COMBINED);
        // new ClassifierManager()._learnAndTestClassifierOnline();
        // if (true) return;

        // ///////////////////////// test reading from file index // /////////////////////////////
        // DictionaryIndex dictionaryIndex = new
        // DictionaryIndex("data/models/dictionary_URLClassifier_1");
        // dictionaryIndex.openReader();
        // CategoryEntries ces = dictionaryIndex.read(".com/a");
        // System.out.println(ces);
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////////////////////////

        // //////////////////////////// test classification ////////////////////////////////
        // WebPageClassifier classifier = new URLClassifier();
        // ClassificationDocument classifiedDocument = null;
        // classifiedDocument =
        // classifier.classify("http://www.hinternet.de/buch/g/george.php",
        // WebPageClassifier.HIERARCHICAL);
        // System.out.println(classifiedDocument);
        // classifiedDocument =
        // classifier.classify("http://www.cinefreaks.com/movies/film.php",
        // WebPageClassifier.FIRST);
        // System.out.println(classifiedDocument);
        // classifiedDocument =
        // classifier.classify("http://www.cinefreaks.com/movies/film.php",
        // WebPageClassifier.TAG);
        // System.out.println(classifiedDocument);
        // //classifiedDocument =
        // classifier.classify("http://www.hinternet.de/buch/g/george.php",
        // WebPageClassifier.HIERARCHICAL);
        // //System.out.println(classifiedDocument);
        // classifiedDocument =
        // classifier.classify("http://www.newsroom.com/news/international",
        // WebPageClassifier.TAG);
        // //System.out.println(classifiedDocument);
        // System.exit(0);
        // ///////////////////////////////////////////////////////////////////////////////

        // ///////////////////////////// learn classifiers /////////////////////////////////
        ClassifierManager classifierManager = new ClassifierManager();
        // classifierManager.setTrainingDataPercentage(10);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls.txt",
        // WebPageClassifier.URL,
        // WebPageClassifier.FIRST);
        // System.exit(0);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small.txt",
        // WebPageClassifier.URL, WebPageClassifier.HIERARCHICAL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small.txt",
        // WebPageClassifier.URL, WebPageClassifier.FIRST);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small.txt",
        // WebPageClassifier.URL, WebPageClassifier.HIERARCHICAL);

        // train and test all classifiers
        long t1 = System.currentTimeMillis();
        System.out.println("start training all classifiers");
        classifierManager.setTrainingDataPercentage(80);
        //classifierManager.setSeparationString("#");
        //classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small.txt", WebPageClassifier.KNN, WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        //classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt", WebPageClassifier.KNN, WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        //classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/productStrings.csv", WebPageClassifier.KNN, WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        
        classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt", WebPageClassifier.URL, WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        //classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/productStrings.csv", WebPageClassifier.URL,WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);

        // classifierManager.setTrainingDataPercentage(100);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small_training.txt", WebPageClassifier.URL,
        // WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.setTrainingDataPercentage(0);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small_test.txt", WebPageClassifier.URL,
        // WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TEST_MODEL);
        System.exit(0);

        // FIRST : 67%
        // classifierManager.setSeparationString("###");
        // classifierManager.setTrainingDataPercentage(100);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/researchGarden_Training.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.setTrainingDataPercentage(0);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/researchGarden_Testing.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TEST_MODEL);

        // FIRST
        classifierManager.setSeparationString("###");
        classifierManager.setTrainingDataPercentage(100);
        classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/researchGarden_Training.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
                ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        classifierManager.setTrainingDataPercentage(0);
        classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/researchGarden_Testing.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
                ClassifierManager.CLASSIFICATION_TEST_MODEL);

        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.FULL_PAGE,
        // WebPageClassifier.FIRST);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.FULL_PAGE,
        // WebPageClassifier.HIERARCHICAL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages.txt",
        // WebPageClassifier.FULL_PAGE,
        // WebPageClassifier.TAG);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt", WebPageClassifier.URL,
        // WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.URL,
        // WebPageClassifier.HIERARCHICAL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages.txt",
        // WebPageClassifier.URL, WebPageClassifier.TAG);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_02k_200.txt",
        // WebPageClassifier.URL, WebPageClassifier.TAG, true);

        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt", WebPageClassifier.KNN,
        // WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt", WebPageClassifier.URL,
        // WebPageClassifier.TAG, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_200.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_200.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TEST_MODEL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_200.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_02k_200.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TEST_MODEL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_02k_200.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_10k_500_n.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_500_n.txt", WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        // classifierManager.setTrainingDataPercentage(0);
        // classifierManager.setSeparationString("#");
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/productStrings_training.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/productStrings_testing.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TEST_MODEL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/productStrings_training.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TEST_MODEL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/productStrings.csv", WebPageClassifier.URL, WebPageClassifier.FIRST,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);

        // language classification
        // classifierManager.setTrainingDataPercentage(20);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/language/languageDocumentIndex.txt", WebPageClassifier.FULL_PAGE,
        // WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);

        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_10k_200.txt",
        // WebPageClassifier.URL,WebPageClassifier.TAG, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_cleansed_400.txt",
        // WebPageClassifier.URL,
        // WebPageClassifier.TAG, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_100k_300.txt",
        // WebPageClassifier.URL,
        // WebPageClassifier.TAG, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_130k_300.txt",
        // WebPageClassifier.URL,
        // WebPageClassifier.TAG, true);

        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_200.txt",
        // WebPageClassifier.URL, WebPageClassifier.TAG, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_200.txt",
        // WebPageClassifier.URL, WebPageClassifier.TAG, false);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_10k_200.txt",
        // WebPageClassifier.URL, WebPageClassifier.TAG, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages_1k_200.txt",
        // WebPageClassifier.FULL_PAGE, WebPageClassifier.TAG, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small.txt", WebPageClassifier.URL,
        // WebPageClassifier.FIRST, ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.URL, WebPageClassifier.HIERARCHICAL, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.FULL_PAGE, WebPageClassifier.HIERARCHICAL, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.FULL_PAGE,WebPageClassifier.FIRST, true);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional_small.txt",
        // WebPageClassifier.URL, WebPageClassifier.HIERARCHICAL, true);

        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.COMBINED, WebPageClassifier.FIRST);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/opendirectory_urls_noregional.txt",
        // WebPageClassifier.COMBINED, WebPageClassifier.HIERARCHICAL);
        // classifierManager.trainAndTestClassifier("data/benchmarkSelection/page/deliciouspages.txt",
        // WebPageClassifier.COMBINED, WebPageClassifier.TAG);

        
        classifierManager.openAnalytix(50,90,10,true,10,99,99,1,WebPageClassifier.FIRST);
        
        System.out.println("finished training all classifiers in " + DateHelper.getRuntime(t1));
        System.exit(0);
        // ///////////////////////////////////////////////////////////////////////////////

        // WebPageClassifier classifier = new URLClassifier();
        // ClassificationDocument classifiedDocument =
        // classifier.classify("http://www.hinternet.de/buch/g/george.php",
        // WebPageClassifier.HIERARCHICAL);
        // classifiedDocument =
        // classifier.classify("http://www.cinefreaks.com/movies/793",
        // WebPageClassifier.HIERARCHICAL);
        // classifiedDocument =
        // classifier.classify("http://www.cinefreaks.com/movies/793",
        // WebPageClassifier.FIRST);
        // System.out.println(classifiedDocument);
    }

}