package ws.palladian.classification.text;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.Trie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * <p>
 * The model implementation for the {@link PalladianTextClassifier}. This class uses a <a
 * href="http://en.wikipedia.org/wiki/Trie">trie</a> for terms and associated probabilities for each term in different
 * categories. The internals of this class are optimized for low memory footprint, which means in particular, that no
 * standard <code>java.util.*</code> classes are used for storage, because they have a high memory overhead. Each trie
 * node links to its parent and to its children, further, it maintains a linked list for category probabilities, in case
 * the term belongs to the dictionary. In comparison to the former, "naive" implementation using nested hash maps, the
 * memory consumption is lowered to approximately 1/3, because the trie allows sharing common prefixes, which typically
 * occur when extracting high amounts of n-grams.
 * <p>
 * The following image gives an overview over the internal structure. The dictionary contains the terms "foo", "tea",
 * "the", and "theme", for each of those terms, LinkedCategories are maintained, which keep the occurrence counts of the
 * terms in the trained categories.
 * <p>
 * <img src="doc-files/DictionaryModel.png" />
 *
 * @author Philipp Katz
 */
public final class DictionaryTrieModel extends AbstractDictionaryModel {
    public static final class Builder implements DictionaryBuilder {

        /** The logger for this class. */
        private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryTrieModel.Builder.class);

        private static final int OPTIMIZE_AFTER_INSERTIONS = 10000;

        /** Trie with term-category combinations with their counts. */
        private final Trie<LinkedCategoryEntries> entryTrie = new Trie<>();
        /** Counter for categories based on documents. */
        private final CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();
        /** Counter for categories based on terms. */
        private final CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        /** Configuration for the feature extraction. */
        private FeatureSetting featureSetting;
        /** The name of this dictionary. */
        private String name;
        /** The number of terms stored in this dictionary. */
        private int numTerms;
        /** The pruning strategies to apply when creating the model. */
        private Predicate<? super CategoryEntries> pruningStrategy;

        private int insertions = 0;

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
            for (String term : terms) {
                if (term == null || term.isEmpty()) {
                    continue; // skip, because trie does not allow empty/null values
                }
                LinkedCategoryEntries entries = entryTrie.getOrPut(term, LinkedCategoryEntries.FACTORY);
                if (entries.getTotalCount() == 0) { // term was not present before
                    numTerms++;
                }
                entries.increment(category, weight);
                termCountBuilder.add(category, weight);
            }
            documentCountBuilder.add(category, weight);
            if (++insertions % OPTIMIZE_AFTER_INSERTIONS == 0) {
                LOGGER.debug("%s insertions -- optimizing category entries", insertions);
                Iterator<Entry<String, LinkedCategoryEntries>> iterator = entryTrie.iterator();
                while (iterator.hasNext()) {
                    LinkedCategoryEntries entries = iterator.next().getValue();
                    entries.sortByCount();
                }
            }
            return this;
        }

        @Override
        public DictionaryModel create() {
            if (pruningStrategy != null) {
                Iterator<Entry<String, LinkedCategoryEntries>> iterator = entryTrie.iterator();
                int numRemoved = 0;
                while (iterator.hasNext()) {
                    Entry<String, LinkedCategoryEntries> next = iterator.next();
                    LinkedCategoryEntries entries = next.getValue();
                    if (!pruningStrategy.test(entries)) {
                        iterator.remove();
                        numRemoved++;
                    }
                }
                double percentageRemoved = 100. * numRemoved / numTerms;
                NumberFormat format = NumberFormat.getInstance(Locale.US);
                LOGGER.info("Removed {} % terms ({}) with {}", format.format(percentageRemoved), numRemoved, pruningStrategy);
                numTerms -= numRemoved;
            }
            entryTrie.clean();
            // re-calculate term counts
            termCountBuilder.clear();
            for (Entry<String, LinkedCategoryEntries> entry : entryTrie) {
                termCountBuilder.add(entry.getValue());
            }
            return new DictionaryTrieModel(this);
        }

        @Override
        public DictionaryBuilder setPruningStrategy(Predicate<? super CategoryEntries> strategy) {
            Validate.notNull(strategy, "strategy must not be null");
            this.pruningStrategy = strategy;
            return this;
        }

        @Override
        public DictionaryBuilder addDictionary(DictionaryModel model) {
            Validate.notNull(model, "model must not be null");
            for (DictionaryEntry addEntry : model) {
                String term = addEntry.getTerm();
                LinkedCategoryEntries entries = entryTrie.getOrPut(term, LinkedCategoryEntries.FACTORY);
                for (Category addCategory : addEntry.getCategoryEntries()) {
                    // TODO really "append" (see JavaDoc), shouldn't this be "add"???
                    entries.append(addCategory);
                }
                numTerms++;
            }
            documentCountBuilder.add(model.getDocumentCounts());
            termCountBuilder.add(model.getTermCounts());
            return this;
        }

    }

    /**
     * Do not change this from now on, use the {@link #VERSION} instead, if you make incompatible changes, and ensure
     * backwards compatibility.
     */
    private static final long serialVersionUID = 4L;

    /** Trie with term-category combinations with their counts. */
    private transient Trie<LinkedCategoryEntries> entryTrie;

    /** The priors, determined from the documents. */
    private transient CategoryEntries documentCounts;

    /** The priors, determined from the individual terms. */
    private transient CategoryEntries termCounts;

    /** The number of terms in this dictionary. */
    private transient int numTerms;

    /** Configuration for the feature extraction. */
    private transient FeatureSetting featureSetting;

    /** The optional name of the model. */
    private transient String name;

    /** Constructor invoked from the builder only. */
    private DictionaryTrieModel(Builder builder) {
        this.entryTrie = builder.entryTrie;
        this.numTerms = builder.numTerms;
        this.featureSetting = builder.featureSetting;
        this.name = builder.name;
        this.documentCounts = builder.documentCountBuilder.create();
        this.termCounts = builder.termCountBuilder.create();
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
        LinkedCategoryEntries entries = entryTrie.get(term);
        return entries != null ? entries : CategoryEntries.EMPTY;
    }

    @Override
    public int getNumUniqTerms() {
        return numTerms;
    }

    @Override
    public Iterator<DictionaryEntry> iterator() {
        return new AbstractIterator2<DictionaryEntry>() {
            final Iterator<Entry<String, LinkedCategoryEntries>> trieIterator = entryTrie.iterator();

            @Override
            protected DictionaryEntry getNext() {
                if (trieIterator.hasNext()) {
                    Entry<String, LinkedCategoryEntries> nextNode = trieIterator.next();
                    return new ImmutableDictionaryEntry(nextNode.getKey(), nextNode.getValue());
                }
                return finished();
            }
        };
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        if (documentCounts.size() > 0) {
            return documentCounts;
        } else {
            // workaround; if priors have not been set explicitly, by using the now deprecated #updateTerm method,
            // we need to collect the category names from the term entries
            CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
            for (DictionaryEntry entry : this) {
                for (Category category : entry.getCategoryEntries()) {
                    builder.set(category.getName(), 1);
                }
            }
            return builder.create();
        }
    }

    @Override
    public CategoryEntries getTermCounts() {
        return termCounts;
    }

    // serialization code

    // Implementation note: in case you make any incompatible changes to the serialization protocol, provide backwards
    // compatibility by using the #VERSION constant. Add a test case for the new version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version from now on for the tests).

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
        entryTrie = new Trie<>();
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
        numTerms = in.readInt();
        CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numTerms; i++) {
            String term = (String) in.readObject();
            LinkedCategoryEntries entries = entryTrie.getOrPut(term, LinkedCategoryEntries.FACTORY);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                entries.append(categoryName, categoryCount);
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
