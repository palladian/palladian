package tud.iir.classification.numeric;

import org.apache.log4j.Logger;

import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;

public abstract class Instance {

    /** Type of classification (tags or hierarchy). */
    private int classifiedAs = ClassificationTypeSetting.TAG;

    /** The category of the instance, null if not classified. */
    protected CategoryEntries assignedCategoryEntries;

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

    public void setClassifiedAs(int classifiedAs) {
        this.classifiedAs = classifiedAs;
    }

    public void sortCategoriesByRelevance() {
        assignedCategoryEntries.sortByRelevance();
    }

    public CategoryEntries getAssignedCategoryEntriesByRelevance(int classType) {
        if (classType == ClassificationTypeSetting.HIERARCHICAL) {
            return assignedCategoryEntries;
        }
        sortCategoriesByRelevance();
        return assignedCategoryEntries;
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

}
