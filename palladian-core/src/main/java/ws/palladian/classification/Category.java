package ws.palladian.classification;

import java.io.Serializable;

import org.apache.log4j.Logger;

import ws.palladian.classification.text.PalladianTextClassifier;

/**
 * A category has a name and a relevance for certain resource.
 * 
 * @author David Urbansky
 * 
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 8831509827509452692L;

    /** The name of the category. */
    private final String name;

    /**
     * The frequency of documents belonging to this category, it will be used to
     * calculate the prior.
     */
    private int frequency = 0;

    /** the total number of weights for all terms in this category */
    private double totalTermWeight = 0.0;

    /** the prior probability of this category */
    private double prior = 0.0;

    public Category(String name) {
        if (name == null) {
            this.name = PalladianTextClassifier.UNASSIGNED;
            Logger.getRootLogger().warn("category with NULL as name was created");
        } else {
            this.name = name;
        }
    }

    public String getName() {
        return name;
    }

    int getFrequency() {
        return frequency;
    }

    public void increaseFrequency() {
        frequency++;
    }

    /**
     * The prior probability of this category. Set after learning.
     * 
     * @return The prior probability of this category.
     */
    public double getPrior() {
        if (prior == 0.0) {
            Logger.getRootLogger().debug("prior was set to 0.0 for category " + getName());
        }
        return prior;
    }

    /**
     * <p>
     * Calculates the prior for this category, which is the ratio between this category's frequency to all documents in
     * the corpus.
     * </p>
     * 
     * @param totalDocuments
     *            The count of total documents on this corpus.
     */
    void calculatePrior(int totalDocuments) {
        prior = (double)frequency / totalDocuments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Category cat = (Category)obj;
        // Equality is checked by category name.
        return cat.getName().equals(getName());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName() + "(prior:" + getPrior() + ")";
    }

    public void increaseTotalTermWeight(double totalTermWeight) {
        this.totalTermWeight += totalTermWeight;
    }

    double getTotalTermWeight() {
        return totalTermWeight;
    }

    void resetFrequency() {
        this.frequency = 0;
    }
}