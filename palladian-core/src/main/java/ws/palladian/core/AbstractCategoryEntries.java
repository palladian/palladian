package ws.palladian.core;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

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
    public int size() {
        int count = 0;
        for (Iterator<Category> iterator = this.iterator(); iterator.hasNext();) {
            Category category = iterator.next();
            if (category.getProbability() > 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTotalCount() {
        int totalCount = 0;
        for (Category category : this) {
            if (category.getCount() == -1) {
                return -1; // in case, at least one item has a count of -1, we can determine no total count
            }
            totalCount += category.getCount();
        }
        return totalCount;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append('[');
        boolean first = true;
        for (Category category : this) {
            if (first) {
                first = false;
            } else {
                toStringBuilder.append(", ");
            }
            toStringBuilder.append(category.getName());
            toStringBuilder.append('=');
            toStringBuilder.append(MathHelper.round(category.getProbability(), 4));
        }
        toStringBuilder.append(']');
        return toStringBuilder.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (Category category : this) {
            result += category.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CategoryEntries other = (CategoryEntries)obj;
        if (size() != other.size()) {
            return false;
        }
        for (Category thisCategory : this) {
            int thisCount = thisCategory.getCount();
            int otherCount = other.getCount(thisCategory.getName());
            if (thisCount != otherCount) {
                return false;
            }
        }
        return true;
    }

}
