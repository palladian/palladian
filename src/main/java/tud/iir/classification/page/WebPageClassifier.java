package tud.iir.classification.page;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Term;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.FileHelper;
import tud.iir.web.Crawler;

/**
 * The classifier is an abstract class that provides basic methods used by concrete classifiers.
 * 
 * @author David Urbansky
 */
public abstract class WebPageClassifier {

    protected final Logger logger = Logger.getLogger(WebPageClassifier.class);

    /** start time of initialization of classifier */
    protected long initTime = 0;

    // // classifier types
    /** classify a page solely by its URL */
    public static final int URL = 1;

    /** classify a page by its contents */
    public static final int FULL_PAGE = 2;

    /** use a k-nearest neighbor classifier */
    public static final int KNN = 3;

    /** use a combination of URL and full page classification */
    public static final int COMBINED = 4;

    // // classification types
    /** take only the first category specified in the txt file */
    public static final int FIRST = 1;

    /** take all categories and treat them as a hierarchy */
    public static final int HIERARCHICAL = 2;

    /** take all categories ant treat them as tags */
    public static final int TAG = 3;

    /** a classifier has a name */
    private String name = "";

    /** a classifier classifies to certain categories */
    public Categories categories = null;

    /** a classifier has training documents */
    public ClassificationDocuments trainingDocuments = null;

    /** a classifier has test documents that can be used to calculate recall, precision, and F-score */
    public ClassificationDocuments testDocuments = null;

    protected Preprocessor preprocessor = null;

    /** whether or not the program runs in benchmark mode */
    protected boolean benchmark = false;

	/** only tags that are classified with a confidence above the threshold are assigned */ 
	public double tagConfidenceThreshold = 0.0;
	
	/**
     * the constructor, initiate members
     */
    public WebPageClassifier() {
        categories = new Categories();
        trainingDocuments = new ClassificationDocuments();
        testDocuments = new ClassificationDocuments();
        preprocessor = new Preprocessor();
        initTime = System.currentTimeMillis();
    }

    public Categories getCategories() {
        return categories;
    }

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

    public static int getUrl() {
        return URL;
    }

    public static int getFullPage() {
        return FULL_PAGE;
    }

    public static int getCombined() {
        return COMBINED;
    }

    /**
     * Check whether a given web page is a forum/board page. Make use of heuristics.
     * 
     * @param url The url of the web page.
     * @return True if it is considered a forum, false otherwise.
     */
    public static boolean isForum(String url) {
        Crawler crawler = new Crawler();
        return isForum(crawler.getWebDocument(url));
    }

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

        if (indicatorCount >= 3)
            return true;

        return false;
    }

    /**
     * Check whether given url is a FAQ web page. Make use of heuristics.
     * 
     * @param url The url of the page.
     * @return True if the page has FAQ, false otherwise.
     */
    public static boolean isFAQ(String url) {
        Crawler crawler = new Crawler();
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

        if (indicatorCount >= 2)
            return true;

        return false;
    }

    /**
     * this method calls the classify function that is implemented by each concrete classifier all test documents are classified
     */
    public void classifyTestDocuments(int classType) {
        for (ClassificationDocument testDocument : testDocuments) {
            classify(testDocument, classType);
        }
    }

    /**
     * This method turns a web document into a document that can be classified. The subclasses implement this method according to the information they need for
     * a classification document.
     * 
     * @param document The web document that should be prepared for classification.
     * @return A document that can be classified.
     */
    public abstract ClassificationDocument preprocessDocument(String url);

    public abstract ClassificationDocument preprocessDocument(String url, ClassificationDocument classificationDocument);

    /**
     * Classify a document that is given with an URL. This method is implemented in concrete classifiers.
     * 
     * @param url The URL of the document that has to be classified.
     * @return A classified document.
     */
    public ClassificationDocument classify(String url, int classType) {
        ClassificationDocument processedDocument;
        processedDocument = preprocessDocument(url);
        processedDocument.setUrl(url);
        return classify(processedDocument, classType);
    }

    /**
     * This method is implemented in concrete classifiers.
     * 
     * @param document The document that has to be classified.
     * @return A classified document.
     */
    public abstract ClassificationDocument classify(ClassificationDocument document, int classType);

    /**
     * Get the parameters used for the classifier.
     * 
     * @return A string with information about the parameters that have been set for the classifier.
     */
    public String getParameters() {
        return "no paramters used";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public double getTagConfidenceThreshold() {
		return tagConfidenceThreshold;
	}

	public void setTagConfidenceThreshold(double tagConfidenceThreshold) {
		this.tagConfidenceThreshold = tagConfidenceThreshold;
	}

    /**
     * Get the number of correct classified documents in a given category.
     * 
     * @param category The category.
     * @return Number of correct classified documents in a given category.
     */
    public int getNumberOfCorrectClassifiedDocumentsInCategory(Category category) {
        int number = 0;

        for (ClassificationDocument document : this.testDocuments) {
            TestDocument d = (TestDocument) document;

            if (category.getClassType() == WebPageClassifier.FIRST) {
                if (document.getMainCategoryEntry().getCategory().getName().equals(category.getName()) && d.isCorrectClassified()) {
                    ++number;
                }
            } else {
                for (CategoryEntry c : d.getAssignedCategoryEntries()) {
                    if (c.getCategory().getName().equals(category.getName()) && d.isCorrectClassified()) {
                        ++number;
                        break;
                    }
                }
            }

        }

        return number;
    }

    /**
     * calculate and return the precision for a given category
     * 
     * @param category
     * @return the precision for a given category
     */
    public double getPrecisionForCategory(Category category) {
        try {
            double correct = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double classified = testDocuments.getClassifiedNumberOfCategory(category);

            if (classified < 1.0)
                return -1.0;

            return (correct / classified);
        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Precision in Category " + category.getName());
            return -1.0;
        }
    }

    /**
     * calculate and return the recall for a given category
     * 
     * @param category
     * @return the recall for a given category
     */
    public double getRecallForCategory(Category category) {
        try {
            double correct = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double real = testDocuments.getRealNumberOfCategory(category);
            if (real < 1.0)
                return -1.0;
            return (correct / real);
        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Recall in Category " + category.getName());
            return -1.0;
        }
    }

    /**
     * Calculate and return the F for a given category.
     * 
     * @param category The category.
     * @param alpha A value between 0 and 1 to weight precision and recall (0.5 for F1).
     * @return F for a given category.
     */
    public double getFForCategory(Category category, double alpha) {
        try {
            double pfc = getPrecisionForCategory(category);
            double rfc = getRecallForCategory(category);

            if (pfc < 0 || rfc < 0)
                return -1.0;

            return 1.0 / (alpha * (1.0 / pfc) + (1.0 - alpha) * (1.0 / rfc));
            // double alphaSquared = alpha * alpha;
            // return ((1+alphaSquared)*getPrecisionForCategory(category) * getRecallForCategory(category)) /
            // (alphaSquared*getPrecisionForCategory(category)+getRecallForCategory(category));
        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for F in Category " + category.getName());
            return -1.0;
        }
    }

    /**
     * Calculate the sensitivity for a given category. Sensitivity = TP / (TP + FN). Sensitivity specifies what percentage of actual category members were
     * found. 100% sensitivity means that all actual documents belonging to the category were classified correctly.
     * 
     * @param category
     * @return
     */
    public double getSensitivityForCategory(Category category) {
        double sensitivity = 0.0;
        try {
            double truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double realPositives = testDocuments.getRealNumberOfCategory(category);

            double falseNegatives = realPositives - truePositives;

            if ((truePositives + falseNegatives) == 0)
                return -1.0;

            sensitivity = (truePositives) / (truePositives + falseNegatives);

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Sensitivity in Category " + category.getName());
            return -1.0;
        }

        return sensitivity;
    }

    /**
     * Calculate the specificity for a given category. Specificity = (TN) / (TN + FP). Specificity specifies what percentage of not-category members were
     * recognized as such. 100% specificity means that there were no documents classified as category member when they were actually not.
     * 
     * @param category The category.
     * @return The specificity.
     */
    public double getSpecificityForCategory(Category category) {
        double specificity = 0.0;
        try {
            double truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double realPositives = testDocuments.getRealNumberOfCategory(category);
            double classifiedPositives = testDocuments.getClassifiedNumberOfCategory(category);

            double falsePositives = classifiedPositives - truePositives;
            double falseNegatives = realPositives - truePositives;
            double trueNegatives = testDocuments.size() - classifiedPositives - falseNegatives;

            if ((trueNegatives + falsePositives) == 0)
                return -1.0;

            specificity = (trueNegatives) / (trueNegatives + falsePositives);

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Specificity in Category " + category.getName());
            return -1.0;
        }

        return specificity;
    }

    /**
     * Calculate the accuracy for a given category. Accuracy = (TP + TN) / (TP + TN + FP + FN).
     * 
     * @param category The category.
     * @return The accuracy.
     */
    public double getAccuracyForCategory(Category category) {
        double accuracy = 0.0;
        try {
            double truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double realPositives = testDocuments.getRealNumberOfCategory(category);
            double classifiedPositives = testDocuments.getClassifiedNumberOfCategory(category);

            double falsePositives = classifiedPositives - truePositives;
            double falseNegatives = realPositives - truePositives;
            double trueNegatives = testDocuments.size() - classifiedPositives - falseNegatives;

            if ((truePositives + trueNegatives + falsePositives + falseNegatives) == 0)
                return -1.0;

            accuracy = (truePositives + trueNegatives) / (truePositives + trueNegatives + falsePositives + falseNegatives);

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Accuracy in Category " + category.getName());
            return -1.0;
        }

        return accuracy;
    }

    /**
     * Calculate the prior for the given category. The prior is determined by calculating the frequency of the category in the training and test set and
     * dividing it by the total number of documents. XXX use only test documents to determine prior?
     * 
     * @param category The category for which the prior should be determined.
     * @return The prior for the category.
     */
    public double getWeightForCategory(Category category) {
        if (category.getTestSetWeight() > -1) {
            return category.getTestSetWeight();
        }

        try {
            // the number of documents that belong to the given category
            int documentCount = testDocuments.getRealNumberOfCategory(category) + trainingDocuments.getRealNumberOfCategory(category);

            // the total number of documents assigned to categories, one document can be assigned to multiple categories!
            int totalAssigned = 0;
            for (Category c : categories) {

                // skip categories that are not main categories because they are classified according to the main category
                if (category.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                    continue;
                }

                totalAssigned += testDocuments.getRealNumberOfCategory(c) + trainingDocuments.getRealNumberOfCategory(c);
            }

            // double ratio = (double) documentCount / (double) (testDocuments.size() + trainingDocuments.size());
            double weight = ((double) documentCount / (double) totalAssigned);
            category.setTestSetWeight(weight);
            return weight;

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Recall in Category " + category.getName());
            return 0.0;
        }
    }

    /**
     * Get the average precision of all categories.
     * 
     * @return The average precision of all categories.
     */
    public double getAveragePrecision(boolean weighted) {
        double precision = 0.0;

        double count = 0.0;
        for (Category c : this.categories) {

            // skip categories that are not main categories because they are classified according to the main category
            if (c.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                continue;
            }

            double pfc = getPrecisionForCategory(c);
            if (pfc < 0)
                continue;

            if (weighted) {
                precision += getWeightForCategory(c) * pfc;
            } else {
                precision += pfc;
            }
            ++count;
        }

        if (weighted)
            return precision;

        if (count == 0)
            return -1.0;

        return precision / count;
    }

    /**
     * Get the average recall of all categories.
     * 
     * @return The average recall of all categories.
     */
    public double getAverageRecall(boolean weighted) {
        double recall = 0.0;

        double count = 0.0;
        for (Category c : this.categories) {

            // skip categories that are not main categories because they are classified according to the main category
            if (c.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                continue;
            }

            double rfc = getRecallForCategory(c);
            if (rfc < 0.0)
                continue;

            if (weighted) {
                recall += getWeightForCategory(c) * rfc;
            } else {
                recall += rfc;
            }
            ++count;
        }

        if (weighted)
            return recall;

        if (count == 0)
            return -1.0;

        return recall / count;
    }

    /**
     * Get the average F of all categories.
     * 
     * @param alpha to weight precision and recall (0.5 for F1)
     * @return The average F of all categories.
     */
    public double getAverageF(double alpha, boolean weighted) {
        double f = 0.0;

        double count = 0.0;
        for (Category c : this.categories) {

            // skip categories that are not main categories because they are classified according to the main category
            if (c.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                continue;
            }

            double ffc = getFForCategory(c, alpha);

            if (ffc < 0.0)
                continue;

            if (weighted) {
                f += getWeightForCategory(c) * ffc;
            } else {
                f += ffc;
            }

            ++count;
        }

        if (weighted)
            return f;

        if (count == 0)
            return -1.0;

        return f / count;
    }

    /**
     * Calculate the average sensitivity.
     * 
     * @param weighted If true, the average sensitivity is weighted using the priors of the categories.
     * @return The (weighted) average sensitivity.
     */
    public double getAverageSensitivity(boolean weighted) {
        double sensitivity = 0.0;

        double count = 0.0;
        for (Category c : this.categories) {

            // skip categories that are not main categories because they are classified according to the main category
            if (c.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                continue;
            }

            double sfc = getSensitivityForCategory(c);

            if (sfc < 0.0)
                continue;

            if (weighted) {
                sensitivity += getWeightForCategory(c) * sfc;
            } else {
                sensitivity += sfc;
            }

            ++count;
        }

        if (weighted)
            return sensitivity;

        if (count == 0)
            return -1.0;

        return sensitivity / count;
    }

    /**
     * Calculate the average specificity.
     * 
     * @param weighted If true, the average accuracy is weighted using the priors of the categories.
     * @return The (weighted) average accuracy.
     */
    public double getAverageSpecificity(boolean weighted) {
        double specificity = 0.0;

        double count = 0.0;
        for (Category c : this.categories) {

            // skip categories that are not main categories because they are classified according to the main category
            if (c.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                continue;
            }

            double sfc = getSpecificityForCategory(c);

            if (sfc < 0)
                return -1.0;

            if (weighted) {
                specificity += getWeightForCategory(c) * sfc;
            } else {
                specificity += sfc;
            }

            ++count;
        }

        if (weighted)
            return specificity;

        if (count == 0)
            return -1.0;

        return specificity / count;
    }

    /**
     * Calculate the average accuracy.
     * 
     * @param weighted If true, the average accuracy is weighted using the priors of the categories.
     * @return The (weighted) average accuracy.
     */
    public double getAverageAccuracy(boolean weighted) {
        double accuracy = 0.0;

        double count = 0.0;
        for (Category c : this.categories) {

            // skip categories that are not main categories because they are classified according to the main category
            if (c.getClassType() == WebPageClassifier.HIERARCHICAL && !c.isMainCategory()) {
                continue;
            }

            double afc = getAccuracyForCategory(c);

            if (afc < 0.0)
                return -1.0;

            if (weighted) {
                accuracy += getWeightForCategory(c) * afc;
            } else {
                accuracy += afc;
            }

            ++count;
        }

        if (weighted)
            return accuracy;

        if (count == 0)
            return -1.0;

        return accuracy / count;
    }

    // //////////////////////////////////////// only DEBUG purposes /////////////////////////////////
    public String showTrainingDocuments() {
        String show = "";

        for (ClassificationDocument document : trainingDocuments) {

            Iterator<Term> j = document.getWeightedTerms().keySet().iterator();

            show += "Document: (" + document.getWeightedTerms().keySet().size() + ")\n\t";
            while (j.hasNext()) {
                show += j.next().getText() + " | ";
            }
            show += "\n\n";
        }
        return show;
    }

    public String showTestDocuments(int classType) {

        // for tagging evaluation we calculate average precision, recall and F1 by averaging over all classifications
        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        int precisionAtRank = 5;
        double[] totalPrecisionAts = new double[precisionAtRank];

        StringBuilder show = new StringBuilder("");
        // StringBuilder structuredOutput = new StringBuilder();

        for (ClassificationDocument document : testDocuments) {

            Iterator<CategoryEntry> j = document.getAssignedCategoryEntries(true).iterator();

            document.sortCategoriesByRelevance();

            show.append(document.getUrl() + "\n\treal (" + document.getClassifiedAsReadable() + "): ").append(document.getRealCategoriesString()).append(
                    "\n\tclassified:");
            while (j.hasNext()) {
                CategoryEntry categoryEntry = j.next();
                show.append(categoryEntry.getCategory().getName()).append("(").append(Math.round(100 * categoryEntry.getRelevance())).append("%) ");
            }

            if (classType == WebPageClassifier.TAG) {

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
                show.append("=> ").append(document.getMainCategoryEntry().getCategory().getName()).append(" ").append(result).append("\n");
                // structuredOutput.append(" #").append(document.getMainCategoryEntry().getCategory().getName()).append("\n");
            }
        }

        if (classType == WebPageClassifier.TAG) {
            double averagePrecision = totalPrecision / testDocuments.size();
            double averageRecall = totalRecall / testDocuments.size();
            double averageF1 = (2 * averagePrecision * averageRecall) / (averagePrecision + averageRecall);

            show.append("\n");
            show.append("Average Precision@: ");
            for (int i = 1; i <= precisionAtRank; i++) {
                double averagePrecisionAtX = totalPrecisionAts[i - 1] / testDocuments.size();
                show.append("@").append(i).append(": ").append((int) Math.floor(100 * averagePrecisionAtX)).append("% ");
            }
            show.append("\n");
            show.append("Average Precision: ").append((int) Math.floor(100 * averagePrecision)).append("%\n");
            show.append("Average Recall: ").append((int) Math.floor(100 * averageRecall)).append("%\n");
            show.append("Average F1: ").append((int) Math.floor(100 * averageF1)).append("%\n");
        }

        // FileHelper.writeToFile("data/temp/dataRewrittenCombinedClassified.csv", structuredOutput);

        return show.toString();
    }
    
    /**
     * 
     * @param classType
     * @return HashMap<Name of value, value> with statistical values 
     */
    // TODO: rename/refactor, get more statistical infos
    public HashMap<String,Double> showTestDocuments2(int classType) {
        
        HashMap<String,Double> statistics = new HashMap<String,Double>();
        
        // for tagging evaluation we calculate average precision, recall and F1 by averaging over all classifications
        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        int precisionAtRank = 5;
        double[] totalPrecisionAts = new double[precisionAtRank];

        StringBuilder show = new StringBuilder("");
        StringBuilder structuredOutput = new StringBuilder();
        StringBuilder thresholdOutput = new StringBuilder();

        int numberOf2Categories = 0;
        for (ClassificationDocument document : testDocuments) {

            Iterator<CategoryEntry> j = document.getAssignedCategoryEntries(true).iterator();           
            
            document.sortCategoriesByRelevance();
            
            show.append(document.getUrl()+"\n\treal ("+document.getClassifiedAsReadable()+"): ").append(document.getRealCategoriesString()).append("\n\tclassified:");
            while (j.hasNext()) {
                CategoryEntry categoryEntry = j.next();
                show.append(categoryEntry.getCategory().getName()).append("(").append(Math.round(100*categoryEntry.getRelevance())).append("%) "); 
            }
            
            if (classType == WebPageClassifier.TAG) {
                
                int correctlyAssigned = ((TestDocument)document).getCorrectlyAssignedCategoryEntries().size();
                int totalAssigned = document.getAssignedCategoryEntries().size();
                int real = document.getRealCategories().size();
                
                if (totalAssigned == 0 || real == 0) {
                    Logger.getRootLogger().warn("no category has been assigned to document " + document.getUrl());
                    continue;
                }
                
                for (int i = 1; i <= precisionAtRank; i++) {
                    totalPrecisionAts[i-1] += ((TestDocument)document).getPrecisionAt(i);
                }
                
                double precision = (double)correctlyAssigned / (double)totalAssigned;
                double recall = (double)correctlyAssigned / (double)real;
                
                totalPrecision += precision;
                totalRecall += recall;
                
                String result = "pr: " + precision + ", rc: " + recall;
                
                show.append("=> ").append(" ").append(result).append("\n");
                boolean isFirst = true;
                for (CategoryEntry ce : document.getAssignedCategoryEntries()) {
                    structuredOutput.append(" ###").append(ce.getCategory().getName());
                    if (!isFirst) {
                        numberOf2Categories++;
                    } else {
                        isFirst = false;
                    }
                }
                structuredOutput.append("\n");
            } else {
                String result = "WRONG";
                if (((TestDocument)document).isCorrectClassified()) result = "CORRECT";
                show.append("=> ").append(document.getMainCategoryEntry().getCategory().getName()).append(" ").append(result).append("\n");
                structuredOutput.append(" ###").append(document.getMainCategoryEntry().getCategory().getName()).append("\n");
            }
        }
        
        if (classType == WebPageClassifier.TAG) {
            double averagePrecision = totalPrecision / (double)testDocuments.size();
            double averageRecall = totalRecall / (double)testDocuments.size();
            double averageF1 = (2 * averagePrecision * averageRecall) / (averagePrecision + averageRecall);
            
            show.append("\n");
            show.append("Average Precision@: ");
            for (int i = 1; i <= precisionAtRank; i++) {
                double averagePrecisionAtX = totalPrecisionAts[i-1] / (double)testDocuments.size();
                show.append("@").append(i).append(": ").append((int) Math.floor(100 * averagePrecisionAtX)).append("% ");
            }
            show.append("\n");
            thresholdOutput.append("----\n");
            thresholdOutput.append("Threshold used: ").append(getTagConfidenceThreshold()).append("\n");
            thresholdOutput.append("Number of two categories: ").append(numberOf2Categories).append("\n");
            thresholdOutput.append("Average Precision: ").append((int) Math.floor(100 * averagePrecision)).append("%\n");
            thresholdOutput.append("Average Recall: ").append((int) Math.floor(100 * averageRecall)).append("%\n");
            thresholdOutput.append("Average F1: ").append((int) Math.floor(100 * averageF1)).append("%\n");
            statistics.put("Number of two categories", (double)numberOf2Categories);
        }
        
        FileHelper.appendToFile("data/temp/thresholds.txt", thresholdOutput, false);
        show.append(thresholdOutput);
        FileHelper.writeToFile("data/temp/dataRewrittenCombinedClassified.csv", structuredOutput);
        
        ClassifierManager.log(show.toString());
        
        return statistics;
    }   
}