package ws.palladian.classification;

import java.util.ArrayList;

import ws.palladian.classification.text.TextInstance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;

public class Instances<NominalInstance> extends ArrayList<NominalInstance> {

    private static final long serialVersionUID = 9062002858891518522L;

    private Categories categories = new Categories();

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

            for (TextInstance d : (Instances<TextInstance>)this) {
                if (d.getMainCategoryEntry().getCategory().getName().equals(category.getName())) {
                    ++number;
                }
            }

        } else {
            for (TextInstance d : (Instances<TextInstance>)this) {
                for (CategoryEntry c : d.getAssignedCategoryEntries()) {
                    if (c.getCategory().getName().equals(category.getName())) {
                        ++number;
                    }
                }
            }
        }

        return number;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public Categories getCategories() {
        return categories;
    }
}
