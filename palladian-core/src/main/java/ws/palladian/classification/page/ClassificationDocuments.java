package ws.palladian.classification.page;

import java.util.ArrayList;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;

/**
 * An ArrayList of documents.
 * 
 * @author David Urbansky
 * 
 */
public class ClassificationDocuments extends ArrayList<TextInstance> {

    private static final long serialVersionUID = 1L;

    /**
     * Get the number of documents that have been assigned to given category.
     * 
     * @param categoryName The name of the category.
     * @return number The number of documents classified in the given category.
     */
    public int getClassifiedNumberOfCategory(String categoryName) {
        return getClassifiedNumberOfCategory(new Category(categoryName));
    }

    /**
     * Get the number of documents that have been assigned to given category.
     * 
     * @param categoryName The category.
     * @return number The number of documents classified in the given category.
     */
    public int getClassifiedNumberOfCategory(Category category) {
        int number = 0;

        // skip categories that are not main categories because they are classified according to the main category
        if (category.getClassType() == ClassificationTypeSetting.HIERARCHICAL && !category.isMainCategory()) {

            return number;

        } else if (category.getClassType() == ClassificationTypeSetting.HIERARCHICAL && category.isMainCategory()
                || category.getClassType() == ClassificationTypeSetting.SINGLE) {

            for (TextInstance d : this) {
                if (d.getMainCategoryEntry().getCategory().getName().equals(category.getName())) {
                    ++number;
                }
            }

        } else {
            for (TextInstance d : this) {
                for (CategoryEntry c : d.getAssignedCategoryEntries()) {
                    if (c.getCategory().getName().equals(category.getName())) {
                        ++number;
                    }
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

        for (TextInstance d : this) {
            for (Category c : d.getRealCategories()) {
                if (c.getName().equals(category.getName())) {
                    ++number;
                }
            }
        }

        return number;
    }
}