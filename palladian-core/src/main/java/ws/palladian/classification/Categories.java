package ws.palladian.classification;

import java.io.Serializable;
import java.util.Collection;

/**
 * An ArrayList of categories.
 * 
 * @author David Urbansky
 */
public class Categories extends java.util.ArrayList<Category> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean add(final Category category) {

        if (category == null) {
            return false;
        }

        if (contains(category)) {
            // this.getCategoryByName(category.getName()).addRelevance(category.getRelevance());
            return false;
        }

        return super.add(category);
    }

    @Override
    public boolean addAll(final Collection<? extends Category> c) {

        boolean allAdded = true;

        for (Category category : c) {
            if (!add(category)) {
                allAdded = false;
            }
        }

        return allAdded;
    }

    /**
     * The prior for a category is the probability of encountering a document of this category in a set of documents,
     * where each one is labeled with its true category. So in a set of labeled training documents, each category has a
     * frequency. The ratio of this frequency to total number of documents will be used to calculate the priors for each
     * category.
     * 
     * @param totalDocuments The total number of documents having a category assigned.
     */
    public void calculatePriors() {
        int totalDocuments = 0;
        for (Category category : this) {
            totalDocuments += category.getFrequency();
        }
        for (Category category : this) {
            category.calculatePrior(totalDocuments);
        }
    }

    /**
     * Check whether ArrayList contains obj.
     * 
     * @return True if the obj is contained, false otherwise.
     */
    @Override
    public boolean contains(final Object obj) {
        String categoryName = ((Category)obj).getName();

        for (Category c : this) {
            if (c.getName().equals(categoryName)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsCategoryName(final String categoryName) {

        for (Category c : this) {
            if (c.getName().equals(categoryName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get a certain category from the list.
     * 
     * @param categoryName
     * @return category
     */
    public Category getCategoryByName(final String categoryName) {
        for (Category c : this) {
            if (c.getName().equals(categoryName)) {
                return c;
            }
        }
        return null;
    }

    public void resetFrequencies() {
        for (Category category : this) {
            category.resetFrequency();
            ;
        }
    }

}