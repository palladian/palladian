package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.IntTrie;

/**
 * See {@link DictionaryMapModel} -- difference: Keep one Trie for each category
 * which is trained which has the count for each word at its leaf.
 *
 * @author Philipp Katz
 */
public final class DictionaryTrieModel2 extends AbstractDictionaryModel {

    public static final class Builder implements DictionaryBuilder {
        /** Trie with term-category combinations with their counts. */
        private final Map<String, IntTrie> categoryTries = new Object2ObjectOpenHashMap<>();

        /** Counter for categories based on documents. */
        private final CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();

        /** Counter for categories based on terms. */
        private final CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();

        /** Configuration for the feature extraction. */
        private FeatureSetting featureSetting;

        /** The name of this dictionary. */
        private String name;

        /** The terms stored in this dictionary. */
        private final Set<String> uniqueTerms = new ObjectOpenHashSet<>();

        @Override
        public DictionaryBuilder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public DictionaryBuilder setFeatureSetting(FeatureSetting featureSetting) {
            this.featureSetting = featureSetting;
            return this;
        }

        @Override
        public DictionaryBuilder addDocument(Collection<String> terms, String category) {
            return addDocument(terms, category, 1);
        }

        @Override
        public DictionaryBuilder addDocument(Collection<String> terms, String category, int weight) {
            Validate.notNull(terms, "terms must not be null");
            Validate.notNull(category, "category must not be null");
            Validate.isTrue(weight >= 1, "weight must be equal/greater one");
            IntTrie trie = categoryTries.compute(category,
                    (categoryValue, trieValue) -> trieValue != null ? trieValue : new IntTrie());
            for (String term : terms) {
                Integer count = trie.get(term);
                int newCount = (count != null ? count : 0) + weight;
                trie.put(term, newCount);
                termCountBuilder.add(category, weight);
                uniqueTerms.add(term);
            }
            documentCountBuilder.add(category, weight);
            return this;
        }

        @Override
        public DictionaryModel create() {
            return new DictionaryTrieModel2(this);
        }

        @Override
        public DictionaryBuilder setPruningStrategy(Predicate<? super CategoryEntries> strategy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DictionaryBuilder addDictionary(DictionaryModel model) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Do not change this from now on, use the {@link #VERSION} instead, if you make
     * incompatible changes, and ensure backwards compatibility.
     */
    private static final long serialVersionUID = 1L;

    /** Trie with term-category combinations with their counts. */
    private transient Map<String, IntTrie> categoryTries;

    /** The priors, determined from the documents. */
    private transient CategoryEntries documentCounts;

    /** The priors, determined from the individual terms. */
    private transient CategoryEntries termCounts;

    /** Configuration for the feature extraction. */
    private transient FeatureSetting featureSetting;

    /** The optional name of the model. */
    private transient String name;

    private transient Set<String> uniqueTerms;

    /** Constructor invoked from the builder only. */
    private DictionaryTrieModel2(Builder builder) {
        categoryTries = builder.categoryTries;
        featureSetting = builder.featureSetting;
        name = builder.name;
        documentCounts = builder.documentCountBuilder.create();
        termCounts = builder.termCountBuilder.create();
        uniqueTerms = builder.uniqueTerms;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    @Override
    public CategoryEntries getCategoryEntries(String term) {
        Validate.notNull(term, "term must not be null");
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Entry<String, IntTrie> entry : categoryTries.entrySet()) {
            Integer count = entry.getValue().get(term);
            if (count != null) {
                builder.add(entry.getKey(), count);
            }
        }
        return builder.create();
    }

    @Override
    public int getNumUniqTerms() {
        return uniqueTerms.size();
    }

    @Override
    public Iterator<DictionaryEntry> iterator() {
        return new AbstractIterator2<DictionaryEntry>() {
            final Iterator<String> trieIterator = uniqueTerms.iterator();

            @Override
            protected DictionaryEntry getNext() {
                if (trieIterator.hasNext()) {
                    String term = trieIterator.next();
                    CategoryEntries categoryEntries = getCategoryEntries(term);
                    return new ImmutableDictionaryEntry(term, categoryEntries);
                }
                return finished();
            }
        };
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        return documentCounts;
    }

    @Override
    public CategoryEntries getTermCounts() {
        return termCounts;
    }

    // serialization code

    // Implementation note: in case you make any incompatible changes to the
    // serialization protocol, provide backwards
    // compatibility by using the #VERSION constant. Add a test case for the new
    // version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version
    // from now on for the tests).

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeObject_(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // version
        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported version: " + version);
        }
        Map<Integer, String> categoryIndices = new HashMap<>();
        categoryTries = new Object2ObjectOpenHashMap<>();
        // header
        int numCategories = in.readInt();
        CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String) in.readObject();
            int categoryCount = in.readInt();
            documentCountBuilder.set(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        documentCounts = documentCountBuilder.create();
        // terms
        uniqueTerms = new ObjectOpenHashSet<>();
        int numTerms = in.readInt();
        CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numTerms; i++) {
            String term = (String) in.readObject();
            uniqueTerms.add(term);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                IntTrie trie = categoryTries.compute(categoryName,
                        (categoryValue, trieValue) -> trieValue != null ? trieValue : new IntTrie());
                int categoryCount = in.readInt();
                trie.put(term, categoryCount);
                termCountBuilder.add(categoryName, categoryCount);
            }
        }
        termCounts = termCountBuilder.create();
        // feature setting
        featureSetting = (FeatureSetting) in.readObject();
        // name
        name = (String) in.readObject();
    }
}
