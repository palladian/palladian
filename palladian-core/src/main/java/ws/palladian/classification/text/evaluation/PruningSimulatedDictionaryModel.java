package ws.palladian.classification.text.evaluation;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.AbstractDictionaryModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.DictionaryModel.DictionaryEntry;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;

public final class PruningSimulatedDictionaryModel extends AbstractDictionaryModel {

    private static final long serialVersionUID = 1L;

    private final DictionaryModel delegate;

    private final Filter<? super CategoryEntries> strategy;

    /** Cached value, lazy initialized. */
    private Integer numUniqTerms;

    public PruningSimulatedDictionaryModel(DictionaryModel delegate, Filter<? super CategoryEntries> strategy) {
        Validate.notNull(delegate, "delegate must not be null");
        Validate.notNull(strategy, "strategy must not be null");
        this.delegate = delegate;
        this.strategy = strategy;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public FeatureSetting getFeatureSetting() {
        return delegate.getFeatureSetting();
    }

    @Override
    public CategoryEntries getCategoryEntries(String term) {
        CategoryEntries entries = delegate.getCategoryEntries(term);
        return strategy.accept(entries) ? entries : CategoryEntries.EMPTY;
    }

    @Override
    public int getNumUniqTerms() {
        if (numUniqTerms == null) {
            numUniqTerms = CollectionHelper.count(this.iterator());
        }
        return numUniqTerms;
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        return delegate.getDocumentCounts();
    }

    @Override
    public CategoryEntries getTermCounts() {
        return delegate.getTermCounts();
    }

    @Override
    public Iterator<DictionaryEntry> iterator() {
        return CollectionHelper.filter(delegate.iterator(), new Filter<DictionaryEntry>() {
            @Override
            public boolean accept(DictionaryEntry item) {
                return strategy.accept(item.getCategoryEntries());
            }
        });
    }

}
