package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Trie;
import ws.palladian.helper.functional.Filter;

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

        /** Trie with term-category combinations with their counts. */
        private final Trie<LinkedCategoryEntries> entryTrie = new Trie<LinkedCategoryEntries>();
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
        private Filter<? super CategoryEntries> pruningStrategy;

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
                    if (!pruningStrategy.accept(entries)) {
                        iterator.remove();
                        numRemoved++;
                    }
                }
                LOGGER.info("Removed {} terms with {}", numRemoved, pruningStrategy);
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
        public DictionaryBuilder setPruningStrategy(Filter<? super CategoryEntries> strategy) {
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

    /**
     * Version number which is written/checked when serializing/deserializing, if you make incompatible changes, update
     * this constant and provide backwards compatibility, so that existing models do not break.
     */
    private static final int VERSION = 1;

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

//    /**
//     * Create a new {@link DictionaryTrieModel}.
//     * 
//     * @param featureSetting The feature setting which was used for creating this model, may be <code>null</code>.
//     * @deprecated Use a {@link Builder} instead.
//     */
//    @Deprecated
//    public DictionaryTrieModel(FeatureSetting featureSetting) {
//        this.entryTrie = new Trie<LinkedCategoryEntries>();
//        this.numTerms = 0;
//        this.featureSetting = featureSetting;
//        this.name = NO_NAME;
//        this.documentCounts = ImmutableCategoryEntries.EMPTY;
//        this.termCounts = ImmutableCategoryEntries.EMPTY;
//    }

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

//    /**
//     * @deprecated Use {@link #addDocument(Collection, String)} instead.
//     */
//    @Deprecated
//    public void updateTerm(String term, String category) {
//        Validate.notNull(term, "term must not be null");
//        Validate.notNull(category, "category must not be null");
//        LinkedCategoryEntries categoryEntries = entryTrie.getOrPut(term, LinkedCategoryEntries.FACTORY);
//        if (categoryEntries.getTotalCount() == 0) { // term was not present before
//            numTerms++;
//        }
//        categoryEntries.increment(category, 1);
//    }

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
        return new AbstractIterator<DictionaryEntry>() {
            final Iterator<Entry<String, LinkedCategoryEntries>> trieIterator = entryTrie.iterator();

            @Override
            protected DictionaryEntry getNext() throws Finished {
                if (trieIterator.hasNext()) {
                    Entry<String, LinkedCategoryEntries> nextNode = trieIterator.next();
                    return new ImmutableDictionaryEntry(nextNode.getKey(), nextNode.getValue());
                }
                throw FINISHED;
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
        // map the category names to numeric indices, so that we can use "1" instead of "aVeryLongCategoryName"
        List<Category> sortedCategories = CollectionHelper.newArrayList(getDocumentCounts());
        Collections.sort(sortedCategories, new Comparator<Category>() {
            @Override
            public int compare(Category c1, Category c2) {
                return c1.getName().compareTo(c2.getName());
            }
        });
        Map<String, Integer> categoryIndices = CollectionHelper.newHashMap();
        int idx = 0;
        for (Category category : sortedCategories) {
            categoryIndices.put(category.getName(), idx++);
        }
        // version (for being able to provide backwards compatibility from now on)
        out.writeInt(VERSION);
        // header; number of categories; [ (categoryName, count) , ...]
        out.writeInt(sortedCategories.size());
        for (Category category : sortedCategories) {
            out.writeObject(category.getName());
            out.writeInt(category.getCount());
        }
        // number of terms; list of terms: [ ( term, numProbabilityEntries, [ (categoryIdx, count), ... ] ), ... ]
        out.writeInt(numTerms);
//        String dictName = name == null || name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
//        ProgressMonitor monitor = new ProgressMonitor();
//        monitor.startTask("Writing " + dictName, numTerms);
        for (DictionaryEntry termEntry : this) {
            out.writeObject(termEntry.getTerm());
            CategoryEntries categoryEntries = termEntry.getCategoryEntries();
            out.writeInt(categoryEntries.size());
            for (Category category : categoryEntries) {
                int categoryIdx = categoryIndices.get(category.getName());
                out.writeInt(categoryIdx);
                out.writeInt(category.getCount());
            }
//            monitor.increment();
        }
        // feature setting
        out.writeObject(featureSetting);
        // name
        out.writeObject(name);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // version
        int version = in.readInt();
//    System.out.println("version="+version);
        if (version != VERSION) {
            throw new IOException("Unsupported version: " + version);
        }
        Map<Integer, String> categoryIndices = CollectionHelper.newHashMap();
        entryTrie = new Trie<LinkedCategoryEntries>();
        // header
        int numCategories = in.readInt();
//    System.out.println("numCategories="+numCategories);
        CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String)in.readObject();
            int categoryCount = in.readInt();
            documentCountBuilder.set(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        documentCounts = documentCountBuilder.create();
        // terms
        numTerms = in.readInt();
//    System.out.println("numTerms="+numTerms);
//        String dictName = name == null || name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
//        ProgressMonitor monitor = new ProgressMonitor();
//        monitor.startTask("Reading " + dictName, numTerms);
        CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numTerms; i++) {
            String term = (String)in.readObject();
//    System.out.println("term="+term);
            LinkedCategoryEntries entries = entryTrie.getOrPut(term, LinkedCategoryEntries.FACTORY);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                entries.append(categoryName, categoryCount);
                termCountBuilder.add(categoryName, categoryCount);
            }
//            monitor.increment();
        }
        termCounts = termCountBuilder.create();
        // feature setting
        featureSetting = (FeatureSetting)in.readObject();
        // name
        name = (String)in.readObject();
    }

}
