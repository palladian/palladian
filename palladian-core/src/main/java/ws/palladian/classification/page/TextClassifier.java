package ws.palladian.classification.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instances;
import ws.palladian.classification.Term;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.ClassifierPerformance;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.PageAnalyzer;

/**
 * The classifier is an abstract class that provides basic methods used by concrete classifiers.
 * 
 * @author David Urbansky
 */
public abstract class TextClassifier extends Classifier<UniversalInstance> {

    /** The serialize version ID. */
    private static final long serialVersionUID = -2602257661494177552L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(TextClassifier.class);

    /** Start time of initialization of classifier. */
    protected long initTime = 0;

    /** A classifier has training documents. */
    private transient ClassificationDocuments trainingDocuments = new ClassificationDocuments();

    /** A classifier has test documents that can be used to calculate recall, precision, and F-score. */
    private transient ClassificationDocuments testDocuments = new ClassificationDocuments();

    private ClassifierPerformance performance = null;

    /** The document preprocessor. */
    protected Preprocessor preprocessor = new Preprocessor(this);

    /** whether or not the program runs in benchmark mode */
    protected boolean benchmark = false;

    public static final String UNASSIGNED = "UNASSIGNED";

    /**
     * The constructor, initiate members.
     */
    public TextClassifier() {
        reset();
    }

    /**
     * Reset the classifier.
     */
    public void reset() {
        categories = new Categories();
        trainingDocuments = new ClassificationDocuments();
        testDocuments = new ClassificationDocuments();
        setTrainingInstances(new Instances<UniversalInstance>());
        setTestInstances(new Instances<UniversalInstance>());
        preprocessor = new Preprocessor(this);
        performance = null;
        initTime = System.currentTimeMillis();
    }

    public TextClassifier copy() {

        TextClassifier copyClassifier = new DictionaryClassifier(getName(), "data/temp/");
        copyClassifier.setName(getName());

        Preprocessor preprocessorCopy = new Preprocessor(copyClassifier, getPreprocessor());
        copyClassifier.setPreprocessor(preprocessorCopy);

        FeatureSetting fs = new FeatureSetting(getFeatureSetting());
        copyClassifier.setFeatureSetting(fs);

        ClassificationTypeSetting cts = new ClassificationTypeSetting(getClassificationTypeSetting());
        copyClassifier.setClassificationTypeSetting(cts);

        return copyClassifier;
    }

    /**
     * @return All the categories the classifier orders documents to.
     */
    @Override
    public Categories getCategories() {
        return categories;
    }

    /**
     * @param categories All the categories the classifier orders documents to.
     */
    @Override
    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public ClassificationDocuments getTrainingDocuments() {
        return trainingDocuments;
    }

    public void setTrainingDocuments(ClassificationDocuments trainingDocuments) {
        this.trainingDocuments = trainingDocuments;
    }

    public ClassificationDocuments getTestDocuments() {
        return testDocuments;
    }

    public void setTestDocuments(ClassificationDocuments testDocuments) {
        this.testDocuments = testDocuments;
    }

    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    public void setPreprocessor(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public boolean isBenchmark() {
        return benchmark;
    }

    public void setBenchmark(boolean benchmark) {
        this.benchmark = benchmark;
    }

    // TODO develop Web Page classifier subclass
    /**
     * Check whether a given web page is a forum/board page. Make use of heuristics.
     * 
     * @param url The url of the web page.
     * @return True if it is considered a forum, false otherwise.
     */
    public static boolean isForum(String url) {
        DocumentRetriever crawler = new DocumentRetriever();
        return isForum(crawler.getWebDocument(url));
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param document
     * @return
     */
    public static boolean isForum(org.w3c.dom.Document document) {

        int indicatorCount = 0;

        PageAnalyzer pa = new PageAnalyzer();
        pa.setDocument(document);

        String url = document.getDocumentURI();
        String title = pa.getTitle();

        // check url and page title for keywords
        if (url.toLowerCase().indexOf("forum") > -1) {
            indicatorCount += 2;
        }
        if (url.toLowerCase().indexOf("board") > -1) {
            indicatorCount += 2;
        }
        if (url.toLowerCase().indexOf("thread") > -1) {
            indicatorCount++;
        }
        if (title.toLowerCase().indexOf("forum") > -1) {
            indicatorCount++;
        }
        if (title.toLowerCase().indexOf("board") > -1) {
            indicatorCount++;
        }
        if (title.toLowerCase().indexOf("thread") > -1) {
            indicatorCount++;
        }

        if (indicatorCount >= 3) {
            return true;
        }

        return false;
    }

    /**
     * Check whether given url is a FAQ web page. Make use of heuristics.
     * 
     * @param url The url of the page.
     * @return True if the page has FAQ, false otherwise.
     */
    public static boolean isFAQ(String url) {
        DocumentRetriever crawler = new DocumentRetriever();
        return isFAQ(crawler.getWebDocument(url));
    }

    public static boolean isFAQ(org.w3c.dom.Document document) {

        int indicatorCount = 0;

        PageAnalyzer pa = new PageAnalyzer();
        pa.setDocument(document);

        String url = document.getDocumentURI();
        String title = pa.getTitle();

        // check url and page title for keywords
        if (url.toLowerCase().indexOf("faq") > -1) {
            indicatorCount += 2;
        }
        if (url.toLowerCase().indexOf("frequently asked questions") > -1) {
            indicatorCount += 2;
        }
        if (title.toLowerCase().indexOf("faq") > -1) {
            indicatorCount += 2;
        }
        if (title.toLowerCase().indexOf("f.a.q.") > -1) {
            indicatorCount += 2;
        }
        if (title.toLowerCase().indexOf("frequently asked questions") > -1) {
            indicatorCount += 2;
        }

        if (indicatorCount >= 2) {
            return true;
        }

        return false;
    }

    /**
     * This method calls the classify function that is implemented by each concrete classifier all test documents are
     * classified.
     */
    public void classifyTestDocuments() {

        int c = 1;
        for (TextInstance testDocument : testDocuments) {
            classify(testDocument);
            if (c % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * c / (double)testDocuments.size(), 2)
                        + "% of the test documents");
            }
            c++;
        }
    }

    /**
     * This method turns a web document into a document that can be classified. The subclasses implement this method
     * according to the information they need for
     * a classification document.
     * 
     * @param document The web document that should be prepared for classification.
     * @return A document that can be classified.
     */
    public abstract TextInstance preprocessDocument(String text);

    public abstract TextInstance preprocessDocument(String text, TextInstance classificationDocument);

    /**
     * Classify a document that is given with an URL. This method is implemented in concrete classifiers.
     * 
     * @param url The URL of the document that has to be classified.
     * @return A classified document.
     */
    public synchronized TextInstance classify(String text) {
        TextInstance processedDocument;
        processedDocument = preprocessDocument(text);
        processedDocument.setContent(text);
        return classify(processedDocument);
    }

    public synchronized TextInstance classify(String text, Set<String> possibleClasses) {
        TextInstance processedDocument;
        processedDocument = preprocessDocument(text);
        processedDocument.setContent(text);
        return classify(processedDocument, possibleClasses);
    }

    /**
     * This method is implemented in concrete classifiers.
     * 
     * @param document The document that has to be classified.
     * @return A classified document.
     */
    public abstract TextInstance classify(TextInstance document);

    public abstract TextInstance classify(TextInstance document, Set<String> possibleClasses);

    /**
     * Get the parameters used for the classifier.
     * 
     * @return A string with information about the parameters that have been set for the classifier.
     */
    public String getParameters() {
        return "no paramters used";
    }

    public boolean isSerialize() {
        return getClassificationTypeSetting().isSerializeClassifier();
    }

    public void setPerformance(ClassifierPerformance performance) {
        this.performance = performance;
    }

    public ClassifierPerformance getPerformance() {

        if (performance == null) {
            performance = new ClassifierPerformance(this);
        }

        return performance;
    }

    /**
     * Get a copy of the classifier performance.
     * Delete weighted terms in documents to lower memory consumption.
     * 
     * @return A new instance of classifier performance.
     */
    public ClassifierPerformance getPerformanceCopy() {
        ClassifierPerformance cfpc = new ClassifierPerformance(this);

        for (TextInstance d : cfpc.getTestDocuments()) {
            d.setWeightedTerms(null);
        }
        for (TextInstance d : cfpc.getTrainingDocuments()) {
            d.setWeightedTerms(null);
        }

        return cfpc;
    }

    // //////////////////////////////////////// only DEBUG purposes /////////////////////////////////
    public String showTrainingDocuments() {
        StringBuilder show = new StringBuilder();

        for (TextInstance document : trainingDocuments) {

            Iterator<Term> j = document.getWeightedTerms().keySet().iterator();

            show.append("Document: (" + document.getWeightedTerms().keySet().size() + ")\n\t");
            while (j.hasNext()) {
                show.append(j.next().getText() + " | ");
            }
            show.append("\n\n");
        }
        return show.toString();
    }

    /**
     * XXX TextClassifier line 380, calculation must be the same, CrossValidator && console output, see mail Philipp to
     * David <a href="https://mail.google.com/mail/#inbox/129b348034382d62">mail</a>
     */
    public String showTestDocuments() {

        // for tagging evaluation we calculate average precision, recall and F1 by averaging over all classifications
        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        int precisionAtRank = 5;
        double[] totalPrecisionAts = new double[precisionAtRank];

        StringBuilder show = new StringBuilder("");
        // StringBuilder structuredOutput = new StringBuilder();

        for (TextInstance document : testDocuments) {

            Iterator<CategoryEntry> j = document.getAssignedCategoryEntries(true).iterator();

            document.sortCategoriesByRelevance();

            show.append(document.getContent() + "\n\treal (" + document.getClassifiedAsReadable() + "): ")
            .append(document.getRealCategoriesString()).append("\n\tclassified:");
            while (j.hasNext()) {
                CategoryEntry categoryEntry = j.next();
                show.append(categoryEntry.getCategory().getName()).append("(")
                .append(Math.round(100 * categoryEntry.getRelevance())).append("%) ");
            }

            if (getClassificationType() == ClassificationTypeSetting.TAG) {

                int correctlyAssigned = ((TestDocument)document).getCorrectlyAssignedCategoryEntries().size();
                int totalAssigned = document.getAssignedCategoryEntries().size();
                int real = document.getRealCategories().size();

                if (totalAssigned == 0 || real == 0) {
                    Logger.getRootLogger().warn("no category has been assigned to document " + document.getContent());
                    continue;
                }

                for (int i = 1; i <= precisionAtRank; i++) {
                    totalPrecisionAts[i - 1] += ((TestDocument)document).getPrecisionAt(i);
                }

                double precision = (double)correctlyAssigned / (double)totalAssigned;
                double recall = (double)correctlyAssigned / (double)real;

                totalPrecision += precision;
                totalRecall += recall;

                String result = "pr: " + precision + ", rc: " + recall;

                show.append("=> ").append(" ").append(result).append("\n");

            } else {
                String result = "WRONG";
                if (((TestDocument)document).isCorrectClassified()) {
                    result = "CORRECT";
                }
                show.append("=> ").append(document.getMainCategoryEntry().getCategory().getName()).append(" ")
                .append(result).append("\n");
                // structuredOutput.append(" #").append(document.getMainCategoryEntry().getCategory().getName()).append("\n");
            }
        }

        if (getClassificationType() == ClassificationTypeSetting.TAG) {
            double averagePrecision = totalPrecision / testDocuments.size();
            double averageRecall = totalRecall / testDocuments.size();
            double averageF1 = 2 * averagePrecision * averageRecall / (averagePrecision + averageRecall);

            show.append("\n");
            show.append("Average Precision@: ");
            for (int i = 1; i <= precisionAtRank; i++) {
                double averagePrecisionAtX = totalPrecisionAts[i - 1] / testDocuments.size();
                show.append("@").append(i).append(": ").append((int)Math.floor(100 * averagePrecisionAtX)).append("% ");
            }
            show.append("\n");
            show.append("Average Precision: ").append((int)Math.floor(100 * averagePrecision)).append("%\n");
            show.append("Average Recall: ").append((int)Math.floor(100 * averageRecall)).append("%\n");
            show.append("Average F1: ").append((int)Math.floor(100 * averageF1)).append("%\n");
        }

        // FileHelper.writeToFile("data/temp/dataRewrittenCombinedClassified.csv", structuredOutput);

        return show.toString();
    }

    /**
     * <p>
     * Train the text classifier with the given dataset.
     * </p>
     * 
     * @param dataset The dataset to train from.
     */
    public void train(Dataset dataset) {

        Instances<UniversalInstance> instances = getTrainingInstances();

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

            UniversalInstance instance = new UniversalInstance(instances);
            instance.setInstanceCategory(instanceCategory);
            instance.setTextFeature(learningText);

            train(instance);
            ProgressHelper.showProgress(added++, trainingArray.size(), 1);
        }

        LOGGER.info("trained with " + added + " documents from " + dataset.getPath());
    }

    public abstract void train(UniversalInstance instance);

    public final ClassifierPerformance evaluate(Dataset dataset) {

        StopWatch sw = new StopWatch();

        // read the testing URLs from the given dataset
        readTestData(dataset);

        // classify
        classifyTestDocuments();

        LOGGER.info("classified " + getTestDocuments().size() + " documents in " + sw.getTotalElapsedTimeString());

        return getPerformance();
    }

    public final ClassifierPerformance evaluate(Instances<UniversalInstance> testInstances) {

        StopWatch sw = new StopWatch();

        // instances to classification documents
        setTestDocuments(new ClassificationDocuments());

        TextInstance preprocessedDocument = null;

        for (UniversalInstance universalInstance : testInstances) {

            preprocessedDocument = new TestDocument();

            String documentContent = universalInstance.getTextFeature();

            preprocessedDocument = preprocessDocument(documentContent, preprocessedDocument);
            preprocessedDocument.setContent(documentContent);

            Categories categories = new Categories();
            categories.add(new Category(universalInstance.getInstanceCategoryName()));

            preprocessedDocument.setDocumentType(TextInstance.TEST);
            preprocessedDocument.setRealCategories(categories);
            getTestDocuments().add(preprocessedDocument);
        }

        // classify
        classifyTestDocuments();

        LOGGER.info("classified " + getTestDocuments().size() + " documents in " + sw.getTotalElapsedTimeString());

        return getPerformance();
    }

    public ClassificationDocuments readTestData(final Dataset dataset) {

        // reset training and testing documents as well as learned categories
        setTestDocuments(new ClassificationDocuments());

        final List<String[]> documentInformationList = new ArrayList<String[]>();

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                // split the line using the separation string
                String[] siteInformation = line.split(dataset.getSeparationString());

                // store the content (or link to the content) and all categories subsequently
                String[] documentInformation = new String[siteInformation.length];
                documentInformation[0] = siteInformation[0];

                // iterate over all parts of the line (in SINGLE mode this would be two iterations)
                for (int i = 1; i < siteInformation.length; ++i) {

                    String[] categorieNames = siteInformation[i].split("/");
                    if (categorieNames.length == 0) {
                        LOGGER.warn("no category names found for " + line);
                        return;
                    }
                    String categoryName = categorieNames[0];

                    documentInformation[i] = categoryName;

                    // add category if it does not exist yet
                    if (!getCategories().containsCategoryName(categoryName)) {
                        Category cat = new Category(categoryName);
                        cat.setClassType(getClassificationType());
                        cat.increaseFrequency();
                        getCategories().add(cat);
                    } else {
                        getCategories().getCategoryByName(categoryName).setClassType(getClassificationType());
                        getCategories().getCategoryByName(categoryName).increaseFrequency();
                    }

                    // only take first category in "first" mode
                    if (getClassificationType() == ClassificationTypeSetting.SINGLE) {
                        break;
                    }
                }

                // add to test urls
                documentInformationList.add(documentInformation);

                if (lineNumber % 1000 == 0) {
                    LOGGER.info("read another 1000 lines from test file, total: " + lineNumber);
                }

            }
        };

        FileHelper.performActionOnEveryLine(dataset.getPath(), la);

        int c = 0;
        for (String[] documentInformation : documentInformationList) {

            TextInstance preprocessedDocument = null;

            preprocessedDocument = new TestDocument();

            String firstField = documentInformation[0];

            String documentContent = firstField;

            // if the first field should be interpreted as a link to the actual document, get it and preprocess it
            if (dataset.isFirstFieldLink()) {
                documentContent = FileHelper.readFileToString(dataset.getRootPath() + firstField);
            }

            preprocessedDocument = preprocessDocument(documentContent, preprocessedDocument);
            preprocessedDocument.setContent(firstField);

            Categories categories = new Categories();
            for (int j = 1; j < documentInformation.length; j++) {
                categories.add(new Category(documentInformation[j]));
            }

            preprocessedDocument.setDocumentType(TextInstance.TEST);
            preprocessedDocument.setRealCategories(categories);
            getTestDocuments().add(preprocessedDocument);

            if (c++ % (documentInformationList.size() / 100 + 1) == 0) {
                LOGGER.info(Math.floor(100.0 * (c + 1) / documentInformationList.size()) + "% preprocessed (= " + c
                        + " documents)");
            }
        }

        // calculate the prior for all categories
        getCategories().calculatePriors();

        return getTestDocuments();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextClassifier [name=");
        builder.append(getName());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public abstract void save(String path);

}