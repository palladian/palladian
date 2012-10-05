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
    public double bayesRelevance = 1.0;
    //    private double trust = -1.0;

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

        if (!this.categoryEntries.isRelevancesUpToDate()) {
            this.categoryEntries.calculateRelativeRelevances();
        }

        // return getCategory().getPrior() * this.relativeRelevance; // yields lower performance (qualitative)
        // double r = 1000000 * this.categoryEntries.getTermWeight(category) * this.relativeRelevance; // yields lower
        // performance (qualitative)
        // double r = category.getPrior() * this.categoryEntries.getTermWeight(category) * this.relativeRelevance;
        // return r;
        return this.relativeRelevance;
    }

    public double getTrust() {
        // double trust = -1.0;

        // CountMap avgPos = (CountMap)Cache.getInstance().getDataObject("posAvg");
        // if (avgPos == null) {
        // avgPos = FileHelper.deserialize("data/posAvg.gz");
        // Cache.getInstance().putDataObject("posAvg", avgPos);
        // }
        // CountMap avgNeg = (CountMap)Cache.getInstance().getDataObject("negAvg");
        // if (avgNeg == null) {
        // avgNeg = FileHelper.deserialize("data/negAvg.gz");
        // Cache.getInstance().putDataObject("negAvg", avgNeg);
        // }
        //
        // // double avgPositive = 62.45;
        // // double avgNegative = 31;
        //
        // double avgPositive = avgPos.get(getCategory().getName()) / 10000.0;
        // double avgNegative = avgNeg.get(getCategory().getName()) / 10000.0;
        //
        // double max = avgPositive - avgNegative;
        //
        // trust = (getRelevance() - avgNegative) / max;

        // distance first and second entry
        // double ownRelevance = getRelevance();
        //
        // double secondRelevance = 0.0;
        // for (CategoryEntry ce : categoryEntries) {
        // if (!ce.getCategory().getName().equals(getCategory().getName()) && ce.getRelevance() > secondRelevance) {
        // secondRelevance = ce.getRelevance();
        // }
        // }
        //
        // trust = ownRelevance - secondRelevance;

        return getRelevance();
    }

    public void multAbsRel(double factor) {
        if (absoluteRelevance <= 0) {
            absoluteRelevance = 1;
        }
        this.absoluteRelevance *= factor;
        this.categoryEntries.setRelevancesUpToDate(false);
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
        this.categoryEntries.setRelevancesUpToDate(false);
    }

    //    public CategoryEntries getCategoryEntries() {
    //        return categoryEntries;
    //    }

    public void setCategoryEntries(CategoryEntries categoryEntries) {
        this.categoryEntries = categoryEntries;
    }

    @Override
    public String toString() {
        return "CategoryEntry [category=" + category + ", abs. relevance=" + absoluteRelevance + ", rel. relevance="
                + getRelevance() + "]";
    }

    // // TODO only for debugging, remove this later.
    // @Override
    // protected void finalize() throws Throwable {
    // System.out.println("finalizing " + this);
    // super.finalize();
    // }
}