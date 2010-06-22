package tud.iir.classification;

import java.io.Serializable;
import java.util.Collection;

/**
 * An ArrayList of categories.
 * 
 * @author David Urbansky
 */
public class Categories extends java.util.ArrayList<Category> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Check whether ArrayList contains obj.
     * 
     * @return True if the obj is contained, false otherwise.
     */
    @Override
    public boolean contains(final Object obj) {
        String categoryName = ((Category) obj).getName();

        for (Category c : this) {
            if (c.equals(categoryName)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsCategoryName(final String categoryName) {

        for (Category c : this) {
            if (c.equals(categoryName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean add(final Category category) {

        if (category == null) {
            return false;
        }

        if (this.contains(category)) {
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
     * Get a certain category from the list.
     * 
     * @param categoryName
     * @return category
     */
    public Category getCategoryByName(final String categoryName) {
        for (Category c : this) {
            if (c.equals(categoryName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * After the learning phase, each category has a frequency. The ratio of frequency to total number of documents will be used to calculate the priors.
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

}