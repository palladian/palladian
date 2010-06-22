package tud.iir.classification.page;

import java.util.ArrayList;

import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntry;

/**
 * An ArrayList of documents.
 * 
 * @author David Urbansky
 * 
 */
class ClassificationDocuments extends ArrayList<ClassificationDocument> {

    private static final long serialVersionUID = 1L;

    /**
     * get the number of documents that have been assigned to given category
     * 
     * @param categoryName
     * @return number
     */
    public int getClassifiedNumberOfCategory(String categoryName) {
        return getClassifiedNumberOfCategory(new Category(categoryName));
    }

    /**
     * Get the number of documents that have been assigned to given category.
     * 
     * @param category
     * @return number
     */
    public int getClassifiedNumberOfCategory(Category category) {
        int number = 0;

        // skip categories that are not main categories because they are classified according to the main category
        if (category.getClassType() == WebPageClassifier.HIERARCHICAL && !category.isMainCategory()) {

            return number;

        } else if ((category.getClassType() == WebPageClassifier.HIERARCHICAL && category.isMainCategory())
                || category.getClassType() == WebPageClassifier.FIRST) {

            for (ClassificationDocument d : this) {
                if (d.getMainCategoryEntry().getCategory().getName().equals(category.getName()))
                    ++number;
            }

        } else {
            for (ClassificationDocument d : this) {
                for (CategoryEntry c : d.getAssignedCategoryEntries()) {
                    if (c.getCategory().getName().equals(category.getName()))
                        ++number;
                }
            }
        }

        return number;
    }

    /**
     * Get the number of documents that actually ARE in the given category.
     * 
     * @param categoryName
     * @return number
     */
    public int getRealNumberOfCategory(String categoryName) {
        return getRealNumberOfCategory(new Category(categoryName));
    }

    /**
     * Get the number of documents that actually ARE in the given category.
     * 
     * @param category
     * @return number
     */
    public int getRealNumberOfCategory(Category category) {
        int number = 0;

        for (ClassificationDocument d : this) {
            for (Category c : d.getRealCategories()) {
                if (c.getName().equals(category.getName()))
                    ++number;
            }
        }

        return number;
    }
}