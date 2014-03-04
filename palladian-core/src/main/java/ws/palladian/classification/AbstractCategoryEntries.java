package ws.palladian.classification;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.helper.math.MathHelper;

public abstract class AbstractCategoryEntries implements CategoryEntries {

    @Override
    public double getProbability(String categoryName) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Category category = getCategory(categoryName);
        return category != null ? category.getProbability() : 0;
    }

    @Override
    public int getCount(String categoryName) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Category category = getCategory(categoryName);
        return category != null ? category.getCount() : 0;
    }

    @Override
    public String getMostLikelyCategory() {
        Category mostLikely = getMostLikely();
        return mostLikely != null ? mostLikely.getName() : null;
    }

    @Override
    public Category getMostLikely() {
        Category mostLikely = null;
        for (Category category : this) {
            if (mostLikely == null || mostLikely.getProbability() < category.getProbability()) {
                mostLikely = category;
            }
        }
        return mostLikely;
    }

    @Override
    public boolean contains(String category) {
        Validate.notNull(category, "category must not be null");
        return getCategory(category) != null;
    }

    @Override
    public Category getCategory(String categoryName) {
        Validate.notNull(categoryName, "categoryName must not be null");
        for (Category category : this) {
            if (category.getName().equals(categoryName)) {
                return category;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append(this.getClass().getSimpleName()).append(" [");
        boolean first = true;
        for (Category category : this) {
            if (first) {
                first = false;
            } else {
                toStringBuilder.append(", ");
            }
            toStringBuilder.append(category.getName());
            toStringBuilder.append("=");
            toStringBuilder.append(MathHelper.round(category.getProbability(), 4));
        }
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

}
