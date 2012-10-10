package ws.palladian.classification;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * An ArrayList of categories.
 * 
 * @author David Urbansky
 */
public class Categories implements Serializable, Iterable<Category> {

    private List<Category> categories = CollectionHelper.newArrayList();

    private static final long serialVersionUID = 1L;

    public boolean add(Category category) {

        if (category == null) {
            return false;
        }

        // XXX it would make more sense to increment the frequency here.

        if (contains(category)) {
            // this.getCategoryByName(category.getName()).addRelevance(category.getRelevance());
            // Category existingCategory = this.getCategoryByName(category.getName());
            // existingCategory.increaseFrequency();
            return false;
        } else {
            // category.increaseFrequency();
            return categories.add(category);
        }
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
        for (Category category : categories) {
            totalDocuments += category.getFrequency();
        }
        for (Category category : categories) {
            category.calculatePrior(totalDocuments);
        }
    }

    private boolean contains(Category category) {
        String categoryName = category.getName();

        for (Category c : categories) {
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
    public Category getCategoryByName(String categoryName) {
        for (Category c : categories) {
            if (c.getName().equals(categoryName)) {
                return c;
            }
        }
        return null;
    }

    public void resetFrequencies() {
        for (Category category : categories) {
            category.resetFrequency();
        }
    }

    @Override
    public Iterator<Category> iterator() {
        return categories.iterator();
    }

}