package ws.palladian.extraction.location.scope;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.AbstractDictionaryModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.ImmutableDictionaryEntry;
import ws.palladian.core.AbstractCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.functional.Function;

/**
 * This decorator for a {@link DictionaryModel} only classifies into a given (sub)set of categories. This way, a little
 * speedup during classification can be achieved.
 * 
 * @author Philipp Katz
 * 
 */
final class FocusDictionaryDecorator extends AbstractDictionaryModel implements DictionaryModel {

    private static final class FocusedCategoryEntries extends AbstractCategoryEntries {

        private final CategoryEntries wrapped;
        private final Filter<String> categoryFilter;

        FocusedCategoryEntries(CategoryEntries wrapped, Filter<String> categoryFilter) {
            this.wrapped = wrapped;
            this.categoryFilter = categoryFilter;
        }

        @Override
        public Iterator<Category> iterator() {
            return CollectionHelper.filter(wrapped.iterator(), new Filter<Category>() {
                @Override
                public boolean accept(Category item) {
                    return categoryFilter.accept(item.getName());
                }
            });
        }

        @Override
        public int getTotalCount() {
            // this is not correct; but I guess actually calculating this here would take too much time
            return wrapped.getTotalCount();
        }

        @Override
        public Category getCategory(String categoryName) {
            return categoryFilter.accept(categoryName) ? wrapped.getCategory(categoryName) : null;
        }

    }

    private static final long serialVersionUID = 1L;

    private final DictionaryModel decorated;

    private final Filter<String> categoryFilter;
    
    public FocusDictionaryDecorator(DictionaryModel decorated, Set<String> categories) {
        this(decorated, Filters.equal(categories));
    }

    public FocusDictionaryDecorator(DictionaryModel decorated, Filter<String> categoryFilter) {
        Validate.notNull(decorated, "decorated must not be null");
        Validate.notNull(categoryFilter, "categoryFilter must not be null");
        this.decorated = decorated;
        this.categoryFilter = categoryFilter;
    }

    @Override
    public Set<String> getCategories() {
        return CollectionHelper.convertSet(getDocumentCounts(), new Function<Category, String>() {
            @Override
            public String compute(Category input) {
                return input.getName();
            }
        });
    }

//    @Override
//    public Iterator<TermCategoryEntries> iterator() {
//        throw new UnsupportedOperationException();
//    }

    @Override
    public String getName() {
        return decorated.getName();
    }

//    @Override
//    public void setName(String name) {
//        throw new UnsupportedOperationException();
//    }

    @Override
    public FeatureSetting getFeatureSetting() {
        return decorated.getFeatureSetting();
    }

//    @Override
//    public void addDocument(Collection<String> terms, String category) {
//        throw new UnsupportedOperationException();
//    }

    @Override
    public CategoryEntries getCategoryEntries(String term) {
        CategoryEntries categoryEntries = decorated.getCategoryEntries(term);
        return new FocusedCategoryEntries(categoryEntries, categoryFilter);
    }

    @Override
    public int getNumTerms() {
        return decorated.getNumTerms();
    }

    @Override
    public int getNumCategories() {
        return getCategories().size();
    }

    @Override
    public int getNumEntries() {
        return decorated.getNumEntries();
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        return new FocusedCategoryEntries(decorated.getDocumentCounts(), categoryFilter);
    }

    @Override
    public CategoryEntries getTermCounts() {
        return new FocusedCategoryEntries(decorated.getTermCounts(), categoryFilter);
    }

//    @Override
//    public void toCsv(PrintStream printStream) {
//        throw new UnsupportedOperationException();
//    }

    @Override
    public int getNumUniqTerms() {
        return decorated.getNumUniqTerms();
    }

    @Override
    public int getNumDocuments() {
        return decorated.getNumDocuments();
    }

    @Override
    public Iterator<DictionaryEntry> iterator() {
        return new Iterator<DictionaryEntry>() {
            final Iterator<DictionaryEntry> decoratedIterator = decorated.iterator();

            @Override
            public boolean hasNext() {
                return decoratedIterator.hasNext();
            }

            @Override
            public DictionaryEntry next() {
                // XXX could be made more efficient I guess, this way we have two lookups...
                String term = decoratedIterator.next().getTerm();
                CategoryEntries categoryEntries = getCategoryEntries(term);
                return new ImmutableDictionaryEntry(term, categoryEntries);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
