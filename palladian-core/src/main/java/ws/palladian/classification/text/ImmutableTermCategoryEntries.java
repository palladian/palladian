package ws.palladian.classification.text;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.ImmutableCategoryEntries;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;

public class ImmutableTermCategoryEntries extends AbstractCategoryEntries implements TermCategoryEntries {

    private final String term;
    private final CategoryEntries categoryEntries;

    public ImmutableTermCategoryEntries(String term) {
        this(term, ImmutableCategoryEntries.EMPTY);
    }
    
    public ImmutableTermCategoryEntries(String term, CategoryEntries categoryEntries) {
        Validate.notNull(term, "term must not be null");
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        this.term = term;
        this.categoryEntries = categoryEntries;
    }

    @Override
    public Category getMostLikely() {
        return categoryEntries.getMostLikely();
    }

    @Override
    public Category getCategory(String categoryName) {
        return categoryEntries.getCategory(categoryName);
    }

    @Override
    public int size() {
        return categoryEntries.size();
    }

    @Override
    public int getTotalCount() {
        return categoryEntries.getTotalCount();
    }

    @Override
    public Iterator<Category> iterator() {
        return categoryEntries.iterator();
    }

    @Override
    public String getTerm() {
        return term;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + categoryEntries.hashCode();
        result = prime * result + term.hashCode();
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
        ImmutableTermCategoryEntries other = (ImmutableTermCategoryEntries)obj;
        if (!term.equals(other.term)) {
            return false;
        }
        return categoryEntries.equals(other.categoryEntries);
    }

    @Override
    public String toString() {
        return term + ":" + categoryEntries;
    }

}
