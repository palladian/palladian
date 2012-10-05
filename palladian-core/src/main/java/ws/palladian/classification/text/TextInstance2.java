package ws.palladian.classification.text;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class TextInstance2 extends NominalInstance {

    /**
     * The real categories are given for training documents (and test documents that are used to determine the quality
     * of the classifier).
     */
    protected Categories realCategories;

    /** Each document has a unique URL. */
    private String content = "";

    /** The weighted terms with term,weight representation. */
    private Map<String, Double> weightedTerms;

    private CategoryEntries assignCategoryEntries;

    /**
     * The constructor.
     */
    public TextInstance2() {
        weightedTerms = new HashMap<String, Double>();
        assignCategoryEntries = new CategoryEntries();
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Double> getWeightedTerms() {
        return weightedTerms;
    }

    public void setWeightedTerms(Map<String, Double> weightedTerms) {
        this.weightedTerms = weightedTerms;
    }

    /** Type of classification (tags or hierarchy). */
    private int classifiedAs = ClassificationTypeSetting.TAG;

    /** The category of the instance, null if not classified. */
    protected CategoryEntries assignedCategoryEntries = new CategoryEntries();

    /** If the class is nominal we have an instance category. */
    private Category instanceCategory;

    public void addCategoryEntry(CategoryEntry categoryEntry) {
        this.assignedCategoryEntries.add(categoryEntry);
    }

    public void assignCategoryEntries(CategoryEntries categoryEntries) {
        this.assignedCategoryEntries = categoryEntries;
    }

    /**
     * Get all categories for the document.
     * 
     * @param relevancesInPercent If true then the relevance will be output in percent.
     * @return All categories.
     */
    public CategoryEntries getAssignedCategoryEntries() {
        return assignedCategoryEntries;
    }

    public CategoryEntries getAssignedCategoryEntriesByRelevance(int classType) {
        if (classType == ClassificationTypeSetting.HIERARCHICAL) {
            return assignedCategoryEntries;
        }
        sortCategoriesByRelevance();
        return assignedCategoryEntries;
    }

    public String getAssignedCategoryEntryNames() {
        StringBuilder nameList = new StringBuilder();

        for (CategoryEntry ce : assignedCategoryEntries) {
            nameList.append(ce.getCategory().getName()).append(",");
        }
        return nameList.substring(0, Math.max(0, nameList.length() - 1));
    }

    // public CategoryEntry getCategoryEntry(String categoryName) {
    // CategoryEntry ceMatch = null;
    //
    // for (CategoryEntry ce : this.assignedCategoryEntries) {
    //
    // if (ce == null) {
    // continue;
    // }
    //
    // if (ce.getCategory().getName().equalsIgnoreCase(categoryName)) {
    // ceMatch = ce;
    // break;
    // }
    // }
    //
    // return ceMatch;
    // }

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
            case ClassificationTypeSetting.REGRESSION:
                return "regression";
        }
        return "unknown";
    }

    public Category getInstanceCategory() {
        return instanceCategory;
    }

    public String getInstanceCategoryName() {
        return instanceCategory.getName();
    }

    // public Instances<Instance<T>> getInstances() {
    // return instances;
    // }

    /**
     * Get the category that is most relevant to this document.
     * 
     * @param relevanceInPercent If true then the relevance will be output in percent.
     * @return The most relevant category.
     */
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
            if (n < minCategories || n < maxCategories && c.getRelevance() >= relevanceThreshold) {
                // XXX added by Philipp, lower memory consumption.
                c.setCategoryEntries(limitedCategories);
                limitedCategories.add(c);
            }
            n++;
        }
        assignCategoryEntries(limitedCategories);
    }

    public void setClassifiedAs(int classifiedAs) {
        this.classifiedAs = classifiedAs;
    }

    public void setInstanceCategory(Category instanceCategory) {
        this.instanceCategory = instanceCategory;
    }

    public void sortCategoriesByRelevance() {
        assignedCategoryEntries.sortByRelevance();
    }

    @Override
    public String toString() {
        return getAssignedCategoryEntries().toString();
    }

}