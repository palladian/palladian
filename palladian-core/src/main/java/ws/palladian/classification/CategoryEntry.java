package ws.palladian.classification;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * <p>Hold information about how relevant a category is.</p>
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntry implements Serializable {

    private static final long serialVersionUID = 2420029642880591759L;

    private CategoryEntries categoryEntries;
    private String category;
    private double absoluteRelevance;
    private double relativeRelevance = -1.0;

    public CategoryEntry(CategoryEntries categoryEntries, String category, double absoluteRelevance) {
        this.categoryEntries = categoryEntries;
        this.category = category;
        if (category == null) {
            Logger.getRootLogger().warn("A category entry was created with NULL as category");
        }
        this.absoluteRelevance = absoluteRelevance;
    }

    public String getCategory() {
        return category;
    }

    public double getRelevance() {
        if (!categoryEntries.isRelevancesUpToDate()) {
            categoryEntries.calculateRelativeRelevances();
        }
        return relativeRelevance;
    }

    void setRelativeRelevance(double relevance) {
        this.relativeRelevance = relevance;
    }

    public double getAbsoluteRelevance() {
        return absoluteRelevance;
    }

    public void addAbsoluteRelevance(double value) {
        this.absoluteRelevance += value;
        // If a CategoryEntry is entered, the relative relevances are not up to date anymore.
        categoryEntries.setRelevancesUpToDate(false);
    }

    @Override
    public String toString() {
        return "CategoryEntry [category=" + category + ", abs. relevance=" + absoluteRelevance + ", rel. relevance="
                + getRelevance() + "]";
    }

}