package ws.palladian.classification;

import java.io.Serializable;

import org.apache.log4j.Logger;

import ws.palladian.classification.text.PalladianTextClassifier;

/**
 * <p>Hold information about how relevant a category is.</p>
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntry implements Serializable {

    private static final long serialVersionUID = 2420029642880591759L;

    private CategoryEntries categoryEntries;
    private Category category;
    private double absoluteRelevance;
    private double relativeRelevance = -1.0;

    public CategoryEntry(CategoryEntries categoryEntries, Category category, double absoluteRelevance) {
        super();
        this.categoryEntries = categoryEntries;
        if (category != null) {
            setCategory(category);
        } else {
            setCategory(new Category(PalladianTextClassifier.UNASSIGNED));
            Logger.getRootLogger().warn("A category entry was created with NULL as category");
        }
        this.absoluteRelevance = absoluteRelevance;
    }

    public Category getCategory() {
        return category;
    }

    private void setCategory(Category category) {
        this.category = category;
    }

    public double getRelevance() {
        if (!categoryEntries.isRelevancesUpToDate()) {
            categoryEntries.calculateRelativeRelevances();
        }
        return relativeRelevance;
    }

    public double getTrust() {
        return getRelevance();
    }

//    public void multAbsRel(double factor) {
//        if (absoluteRelevance <= 0) {
//            absoluteRelevance = 1;
//        }
//        this.absoluteRelevance *= factor;
//        this.categoryEntries.setRelevancesUpToDate(false);
//    }

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

    //    public CategoryEntries getCategoryEntries() {
    //        return categoryEntries;
    //    }

//    public void setCategoryEntries(CategoryEntries categoryEntries) {
//        this.categoryEntries = categoryEntries;
//    }

    @Override
    public String toString() {
        return "CategoryEntry [category=" + category + ", abs. relevance=" + absoluteRelevance + ", rel. relevance="
                + getRelevance() + "]";
    }

}