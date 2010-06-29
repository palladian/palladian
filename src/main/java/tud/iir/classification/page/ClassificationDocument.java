package tud.iir.classification.page;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.log4j.Logger;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Term;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.helper.MathHelper;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class ClassificationDocument {

    // a document can be a test or a training document
    public static final int TEST = 1;
    public static final int TRAINING = 2;
    public static final int UNCLASSIFIED = 3;

    /** the real categories are given for training documents (and test documents that are used to determine the quality of the classifier) */
    protected Categories realCategories;

    /** each document has a unique url */
    private String url = "";

    /** the category of the document, null if not classified */
    protected CategoryEntries assignedCategoryEntries = null;

    /** the weighted terms with term,weight representation */
    private HashMap<Term, Double> weightedTerms;

    /** the type of the document (TEST, TRAINING or unknown) */
    private int documentType = UNCLASSIFIED;

    /** type of classification (tags or hierarchy) */
    private int classifiedAs = ClassificationTypeSetting.TAG;

    /** comparator to sort categories by relevance */
    Comparator<CategoryEntry> comparator = new Comparator<CategoryEntry>() {
        public int compare(CategoryEntry o1, CategoryEntry o2) {
            return ((Comparable<Double>) o2.getRelevance()).compareTo(o1.getRelevance());
        }
        /*
         * public int compare(CategoryEntry o1, CategoryEntry o2) { return ((Comparable<Double>) ((CategoryEntry) (o2)).bayesRelevance)
         * .compareTo(((CategoryEntry) (o1)).bayesRelevance); }
         */
    };

    /**
     * The constructor.
     */
    public ClassificationDocument() {
        weightedTerms = new HashMap<Term, Double>();
        assignedCategoryEntries = new CategoryEntries();
    }

    /**
     * Set the real categories (mainly for training documents).
     * 
     * @param categories The real categories.
     */
    public void setRealCategories(Categories categories) {
        this.realCategories = categories;
    }

    /**
     * Get the real categories of the document.
     * 
     * @return The real categories.
     */
    public Categories getRealCategories() {
        return realCategories;
    }

    public String getRealCategoriesString() {
        StringBuilder sb = new StringBuilder();
        for (Category c : realCategories) {
            sb.append(c.getName()).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public Category getFirstRealCategory() {
        if (realCategories != null && !realCategories.isEmpty()) {
            return realCategories.get(0);
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the category that is most relevant to this document.
     * 
     * @param relevanceInPercent If true then the relevance will be output in percent.
     * @return The most relevant category.
     */
    public CategoryEntry getMainCategoryEntry(boolean relevanceInPercent) {
        if (relevanceInPercent) {
            assignedCategoryEntries.transformRelevancesInPercent(true);
        }
        return getMainCategoryEntry();
    }

    public CategoryEntry getMainCategoryEntry() {
        CategoryEntry highestMatch = null;

        for (CategoryEntry ce : this.assignedCategoryEntries) {

            if (ce == null) {
                // Logger.getRootLogger().warn("an assigned category entry has been null for document " + getUrl());
                continue;
            }

            if (highestMatch == null) {
                highestMatch = ce;
                continue;
            }

            if (ce.getRelevance() > highestMatch.getRelevance()) {
                highestMatch = ce;
            }
            // System.out.println(c.getName()+" with "+c.getRelevance()+" highest so far "+highestMatch.getName()+" with "+highestMatch.getRelevance());
        }

        if (highestMatch == null) {
            Logger.getRootLogger().warn("no assigned category found");
            return new CategoryEntry(this.assignedCategoryEntries, new Category(null), 0.0);
        }

        return highestMatch;
    }

    public void sortCategoriesByRelevance() {
        Collections.sort(assignedCategoryEntries, comparator);
    }

    public CategoryEntries getAssignedCategoryEntriesByRelevance(int classType) {
        if (classType == ClassificationTypeSetting.HIERARCHICAL) {
            return assignedCategoryEntries;
        }
        Collections.sort(assignedCategoryEntries, comparator);
        return assignedCategoryEntries;
    }

    /**
     * Get all categories for the document.
     * 
     * @param relevancesInPercent If true then the relevance will be output in percent.
     * @return All categories.
     */
    public CategoryEntries getAssignedCategoryEntries(boolean relevancesInPercent) {
        if (relevancesInPercent) {
            assignedCategoryEntries.transformRelevancesInPercent(true);
        }
        return assignedCategoryEntries;
    }

    public CategoryEntries getAssignedCategoryEntries() {
        return assignedCategoryEntries;
    }

    public String getAssignedCategoryEntryNames() {
        StringBuilder nameList = new StringBuilder();

        for (CategoryEntry ce : assignedCategoryEntries) {
            nameList.append(ce.getCategory().getName()).append(",");
        }
        return nameList.substring(0, Math.max(0, nameList.length() - 1));
    }

    public void assignCategoryEntries(CategoryEntries categoryEntries) {
        this.assignedCategoryEntries = categoryEntries;
        categoryEntries.transformRelevancesInPercent(true);
    }

    public void addCategoryEntry(CategoryEntry categoryEntry) {
        this.assignedCategoryEntries.add(categoryEntry);
    }

    /**
     * Limit number of assigned categories.
     * 
     * @param number Number of categories to keep.
     * @param relevanceThreshold Categories must have at least this much relevance to be kept.
     */
    public void limitCategories(int minCategories, int maxCategories, double relevanceThreshold) {
        CategoryEntries limitedCategories = new CategoryEntries();
        int n = 0;
        for (CategoryEntry c : getAssignedCategoryEntriesByRelevance(getClassifiedAs())) {
            if (n < minCategories || (n < maxCategories && c.getRelevance() >= relevanceThreshold)) {
                limitedCategories.add(c);
            }
            n++;
        }
        assignCategoryEntries(limitedCategories);
    }

    public HashMap<Term, Double> getWeightedTerms() {
        return weightedTerms;
    }

    public void setWeightedTerms(HashMap<Term, Double> weightedTerms) {
        this.weightedTerms = weightedTerms;
    }

    public int getDocumentType() {
        return documentType;
    }

    public void setDocumentType(int documentType) {
        this.documentType = documentType;
    }

    public int getClassifiedAs() {
        return classifiedAs;
    }

    public String getClassifiedAsReadable() {
        switch (classifiedAs) {
            case ClassificationTypeSetting.SINGLE:
                return "single";
            case ClassificationTypeSetting.TAG:
                return "tag";
            case ClassificationTypeSetting.HIERARCHICAL:
                return "hierarchical";
        }
        return "unknown";
    }

    public void setClassifiedAs(int classifiedAs) {
        this.classifiedAs = classifiedAs;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        //string.append(getUrl());
        CategoryEntries sortedCategories = getAssignedCategoryEntriesByRelevance(getClassifiedAs());
        for (CategoryEntry categoryEntry : sortedCategories) {
            string.append(categoryEntry.getCategory().getName()).append(" (").append(MathHelper.round(100 * categoryEntry.getRelevance(), 2)).append("%)\n");
        }
        return string.toString();
    }
}