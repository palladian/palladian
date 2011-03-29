package ws.palladian.classification.page;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instances;
import ws.palladian.classification.Term;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.ClassifierPerformance;
import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.web.DocumentRetriever;

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
        initTime = System.currentTimeMillis();
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
        for (TextInstance testDocument : testDocuments) {
            classify(testDocument);
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
    public TextInstance classify(String text) {
        TextInstance processedDocument;
        processedDocument = preprocessDocument(text);
        processedDocument.setUrl(text);
        return classify(processedDocument);
    }

    public TextInstance classify(String text, Set<String> possibleClasses) {
        TextInstance processedDocument;
        processedDocument = preprocessDocument(text);
        processedDocument.setUrl(text);
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

            show.append(document.getUrl() + "\n\treal (" + document.getClassifiedAsReadable() + "): ")
            .append(document.getRealCategoriesString()).append("\n\tclassified:");
            while (j.hasNext()) {
                CategoryEntry categoryEntry = j.next();
                show.append(categoryEntry.getCategory().getName()).append("(")
                .append(Math.round(100 * categoryEntry.getRelevance())).append("%) ");
            }

            if (getClassificationType() == ClassificationTypeSetting.TAG) {

                int correctlyAssigned = ((TestDocument) document).getCorrectlyAssignedCategoryEntries().size();
                int totalAssigned = document.getAssignedCategoryEntries().size();
                int real = document.getRealCategories().size();

                if (totalAssigned == 0 || real == 0) {
                    Logger.getRootLogger().warn("no category has been assigned to document " + document.getUrl());
                    continue;
                }

                for (int i = 1; i <= precisionAtRank; i++) {
                    totalPrecisionAts[i - 1] += ((TestDocument) document).getPrecisionAt(i);
                }

                double precision = (double) correctlyAssigned / (double) totalAssigned;
                double recall = (double) correctlyAssigned / (double) real;

                totalPrecision += precision;
                totalRecall += recall;

                String result = "pr: " + precision + ", rc: " + recall;

                show.append("=> ").append(" ").append(result).append("\n");

            } else {
                String result = "WRONG";
                if (((TestDocument) document).isCorrectClassified()) {
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
                show.append("@").append(i).append(": ").append((int) Math.floor(100 * averagePrecisionAtX))
                .append("% ");
            }
            show.append("\n");
            show.append("Average Precision: ").append((int) Math.floor(100 * averagePrecision)).append("%\n");
            show.append("Average Recall: ").append((int) Math.floor(100 * averageRecall)).append("%\n");
            show.append("Average F1: ").append((int) Math.floor(100 * averageF1)).append("%\n");
        }

        // FileHelper.writeToFile("data/temp/dataRewrittenCombinedClassified.csv", structuredOutput);

        return show.toString();
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