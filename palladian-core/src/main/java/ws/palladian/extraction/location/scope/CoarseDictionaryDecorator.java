package ws.palladian.extraction.location.scope;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.text.*;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Decorator for a fine grid dictionary to "simulate" a coarse grid dictionary.
 *
 * @author Philipp Katz
 */
final class CoarseDictionaryDecorator extends AbstractDictionaryModel {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoarseDictionaryDecorator.class);

    private static final long serialVersionUID = 1L;

    /** The lower size of the cache; each time the cache is cleaned, it is reduced to this size. */
    private static final int CACHE_LOWER_SIZE = 5000;

    /** The upper size of the cache; in case the cache exceeds this size, the cleaning is initiated. */
    private static final int CACHE_UPPER_SIZE = CACHE_LOWER_SIZE + 5000;

    private final DictionaryModel decorated;

    private final GridCreator coarseGrid;

    private final GridCreator fineGrid;

    /** Cache mapped values, because mapping is not very fast. */
    private final Map<String, String> fineToCoarseIdentifier = new HashMap<>();

    /** Cache often requested terms and their TermCategoryEntries. */
    private final Map<String, CategoryEntries> entriesCache = new HashMap<>();

    /**
     * Keep only those TermCategoryEntries cached, where the #size is greater than this value (this is adapted
     * dynamically with each cache cleanup).
     */
    private int minTermCategoriesSizeForCaching = 0;

    /** Cache the mapped priors. */
    private final CategoryEntries documentCounts;

    private final CategoryEntries termCounts;

    @Deprecated
    public CoarseDictionaryDecorator(DictionaryModel decorated, GridCreator coarseGrid, GridCreator fineGrid) {
        Validate.notNull(decorated, "decorated must not be null");
        Validate.notNull(coarseGrid, "coarseGrid must not be null");
        Validate.notNull(fineGrid, "fineGrid must not be null");
        this.decorated = decorated;
        this.coarseGrid = coarseGrid;
        this.fineGrid = fineGrid;
        this.documentCounts = mapToCoarse(decorated.getDocumentCounts());
        this.termCounts = mapToCoarse(decorated.getTermCounts());
    }

    public CoarseDictionaryDecorator(DictionaryScopeModel model, double coarseGridSize) {
        Validate.notNull(model, "model must not be null");
        this.decorated = model.dictionaryModel;
        this.coarseGrid = new GridCreator(coarseGridSize);
        this.fineGrid = new GridCreator(model.gridSize);
        this.documentCounts = mapToCoarse(decorated.getDocumentCounts());
        this.termCounts = mapToCoarse(decorated.getTermCounts());
    }

    @Override
    public Set<String> getCategories() {
        Set<String> fineCategories = decorated.getCategories();
        return CollectionHelper.convertSet(fineCategories, new Function<String, String>() {
            @Override
            public String apply(String input) {
                return mapToCoarse(input);
            }
        });
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
                // XXX could be made more efficient  I guess, this way we have two lookups...
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
        CategoryEntries coarseEntries = entriesCache.get(term);
        if (coarseEntries != null) { // entries were cached
            return coarseEntries;
        }
        CategoryEntries fineEntries = decorated.getCategoryEntries(term);
        if (fineEntries.getTotalCount() == 0) { // entries are empty, don't bother any further
            return CategoryEntries.EMPTY;
        }
        coarseEntries = mapToCoarse(fineEntries);
        putToCache(term, coarseEntries);
        return coarseEntries;
    }

    // w/o caching: 15m:1s:1ms
    // w/ caching: 8m:3s:964ms
    // speedup ~ 50 %

    private void putToCache(String term, CategoryEntries entries) {
        if (entriesCache.size() >= CACHE_UPPER_SIZE) {
            // do a cache cleanup, reduce the size to CACHE_LOWER_SIZE,
            // remove TermCategoryEntries with small size and from now
            // on only add TermCategoryEntries to the cache, where the
            // size is above the now removed threshold ...
            // StopWatch stopWatch = new StopWatch();
            // LOGGER.debug("Size of cache {}, cleaning up", entriesCache.size());
            List<Entry<String, CategoryEntries>> temp = new ArrayList<>(entriesCache.entrySet());
            Collections.sort(temp, new Comparator<Entry<String, CategoryEntries>>() {
                @Override
                public int compare(Entry<String, CategoryEntries> e1, Entry<String, CategoryEntries> e2) {
                    return e2.getValue().size() - e1.getValue().size();
                }
            });
            entriesCache.clear();
            int newMinSizeForCaching = Integer.MAX_VALUE;
            int maxSize = Integer.MIN_VALUE;
            for (Entry<String, CategoryEntries> current : temp) {
                CategoryEntries categoryEntries = current.getValue();
                entriesCache.put(current.getKey(), categoryEntries);
                newMinSizeForCaching = Math.min(newMinSizeForCaching, categoryEntries.size());
                maxSize = Math.max(maxSize, categoryEntries.size());
                if (entriesCache.size() >= CACHE_LOWER_SIZE) {
                    break;
                }
            }
            // LOGGER.debug("Size of cache after clean up {}", entriesCache.size());
            LOGGER.debug("New min. TermCategoryEntries#size in {}° decorator for caching {} (max. size={})", coarseGrid.getGridSize(), newMinSizeForCaching, maxSize);
            // LOGGER.debug("Cleanup took {}", stopWatch);
            minTermCategoriesSizeForCaching = newMinSizeForCaching;
        }
        if (entries.size() >= minTermCategoriesSizeForCaching) {
            entriesCache.put(term, entries);
        }
    }

    private CategoryEntries mapToCoarse(CategoryEntries fineCategoryEntries) {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Category category : fineCategoryEntries) {
            builder.add(mapToCoarse(category.getName()), category.getCount());
        }
        return builder.create();
    }

    private String mapToCoarse(String fineCellIdentifier) {
        String coarseCellIdentifier = fineToCoarseIdentifier.get(fineCellIdentifier);
        if (coarseCellIdentifier == null) {
            GridCell coarseCell = fineGrid.getCell(fineCellIdentifier);
            coarseCellIdentifier = coarseGrid.getCell(coarseCell.getCenter()).getIdentifier();
            fineToCoarseIdentifier.put(fineCellIdentifier, coarseCellIdentifier);
        }
        return coarseCellIdentifier;
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
        return documentCounts;
    }

    @Override
    public CategoryEntries getTermCounts() {
        return termCounts;
    }

    @Override
    public int getNumUniqTerms() {
        return decorated.getNumUniqTerms();
    }

    @Override
    public int getNumDocuments() {
        return decorated.getNumDocuments();
    }

}
