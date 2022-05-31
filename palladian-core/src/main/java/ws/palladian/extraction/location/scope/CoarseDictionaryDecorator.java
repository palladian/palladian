package ws.palladian.extraction.location.scope;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import ws.palladian.classification.text.AbstractDictionaryModel;
import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.ImmutableDictionaryEntry;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.helper.collection.CollectionHelper;

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
    private final Map<String, String> fineToCoarseIdentifier = new Object2ObjectOpenHashMap<>();

    /** Cache often requested terms and their TermCategoryEntries. */
    private final Map<String, CategoryEntries> entriesCache = new Object2ObjectOpenHashMap<>();

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
        return CollectionHelper.convertSet(fineCategories, input -> mapToCoarse(input));
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
            List<Pair<String, CategoryEntries>> temp = entriesCache //
                    .entrySet() //
                    .stream() //
                    .map(e -> Pair.of(e.getKey(), e.getValue())) // nb: need to copy due to clear()
                    .sorted((e1, e2) -> e2.second().size() - e1.second().size()) //
                    .collect(Collectors.toList()); //
            entriesCache.clear();
            int newMinSizeForCaching = Integer.MAX_VALUE;
            int maxSize = Integer.MIN_VALUE;
            for (Pair<String, CategoryEntries> current : temp) {
                CategoryEntries categoryEntries = current.second();
                entriesCache.put(current.first(), categoryEntries);
                newMinSizeForCaching = Math.min(newMinSizeForCaching, categoryEntries.size());
                maxSize = Math.max(maxSize, categoryEntries.size());
                if (entriesCache.size() >= CACHE_LOWER_SIZE) {
                    break;
                }
            }
            // LOGGER.debug("Size of cache after clean up {}", entriesCache.size());
            LOGGER.debug("New min. TermCategoryEntries#size in {}° decorator for caching {} (max. size={})",
                    coarseGrid.getGridSize(), newMinSizeForCaching, maxSize);
            // LOGGER.debug("Cleanup took {}", stopWatch);
            minTermCategoriesSizeForCaching = newMinSizeForCaching;
        }
        if (entries.size() >= minTermCategoriesSizeForCaching) {
            entriesCache.put(term, entries);
        }
    }

    private CategoryEntries mapToCoarse(CategoryEntries fineCategoryEntries) {
        // TODO use LinkedCategoryEntries instead? -- too slow to build due to `increment`,
        // but maybe we can use them just for storage?
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Category category : fineCategoryEntries) {
            builder.add(mapToCoarse(category.getName()), category.getCount());
        }
        return builder.create();
//      LinkedCategoryEntries categoryEntries = new LinkedCategoryEntries();
//      for (Category category : fineCategoryEntries) {
//          categoryEntries.increment(mapToCoarse(category.getName()), category.getCount());
//      }
//      return categoryEntries;
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
