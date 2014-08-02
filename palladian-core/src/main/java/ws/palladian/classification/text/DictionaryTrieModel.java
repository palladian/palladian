package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.ImmutableCategory;
import ws.palladian.classification.ImmutableCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Functions;

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
        private final TrieCategoryEntries entryTrie = new TrieCategoryEntries();
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
        private final List<PruningStrategy> pruningStrategies = CollectionHelper.newArrayList();

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
            Validate.notNull(terms, "terms must not be null");
            Validate.notNull(category, "category must not be null");
            for (String term : terms) {
                TrieCategoryEntries categoryEntries = entryTrie.getOrAdd(term, true);
                if (categoryEntries.totalCount == 0) { // term was not present before
                    numTerms++;
                }
                categoryEntries.increment(category, 1);
                termCountBuilder.add(category, 1);
            }
            documentCountBuilder.add(category, 1);
            return this;
        }

        @Override
        public DictionaryModel create() {
            if (pruningStrategies.size() > 0) {
                for (PruningStrategy pruningStrategy : pruningStrategies) {
                    Iterator<TrieCategoryEntries> iterator = new TrieIterator(entryTrie, false);
                    int numRemoved = 0;
                    while (iterator.hasNext()) {
                        TrieCategoryEntries categoryEntries = iterator.next();
                        if (pruningStrategy.remove(categoryEntries)) {
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
                TrieIterator iterator = new TrieIterator(entryTrie, true);
                while (iterator.hasNext()) {
                    termCountBuilder.add(iterator.next());
                }
            }
            return new DictionaryTrieModel(this);
        }

        @Override
        public DictionaryBuilder addPruningStrategy(PruningStrategy strategy) {
            Validate.notNull(strategy, "strategy must not be null");
            pruningStrategies.add(strategy);
            return this;
        }

        @Override
        public DictionaryBuilder addDictionary(DictionaryModel model) {
            for (TermCategoryEntries addEntry : model) {
                TrieCategoryEntries existingEntry = entryTrie.getOrAdd(addEntry.getTerm(), true);
                for (Category addCategory : addEntry) {
                    existingEntry.append(addCategory.getName(), addCategory.getCount());
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
    private transient TrieCategoryEntries entryTrie;

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

    /**
     * Create a new {@link DictionaryTrieModel}.
     * 
     * @param featureSetting The feature setting which was used for creating this model, may be <code>null</code>.
     * @deprecated Use a {@link Builder} instead.
     */
    @Deprecated
    public DictionaryTrieModel(FeatureSetting featureSetting) {
        this.entryTrie = new TrieCategoryEntries();
        this.numTerms = 0;
        this.featureSetting = featureSetting;
        this.name = NO_NAME;
        this.documentCounts = ImmutableCategoryEntries.EMPTY;
        this.termCounts = ImmutableCategoryEntries.EMPTY;
    }

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

    /**
     * @deprecated Use {@link #addDocument(Collection, String)} instead.
     */
    @Deprecated
    public void updateTerm(String term, String category) {
        Validate.notNull(term, "term must not be null");
        Validate.notNull(category, "category must not be null");
        TrieCategoryEntries categoryEntries = entryTrie.getOrAdd(term, true);
        if (categoryEntries.totalCount == 0) { // term was not present before
            numTerms++;
        }
        categoryEntries.increment(category, 1);
    }

    @Override
    public TermCategoryEntries getCategoryEntries(String term) {
        Validate.notNull(term, "term must not be null");
        TermCategoryEntries node = entryTrie.get(term);
        return node != null ? node : TrieCategoryEntries.EMPTY;
    }

    @Override
    public int getNumUniqTerms() {
        return numTerms;
    }

    @Override
    public Iterator<TermCategoryEntries> iterator() {
        return CollectionHelper.convert(new TrieIterator(entryTrie, true),
                Functions.adapt(TrieCategoryEntries.class, TermCategoryEntries.class));
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        if (documentCounts.size() > 0) {
            return documentCounts;
        } else {
            // workaround; if priors have not been set explicitly, by using the now deprecated #updateTerm method,
            // we need to collect the category names from the term entries
            CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
            for (TermCategoryEntries entries : this) {
                for (Category category : entries) {
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
        String dictName = name == null || name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
        ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask("Writing " + dictName, numTerms);
        for (TermCategoryEntries termEntry : this) {
            out.writeObject(termEntry.getTerm());
            out.writeInt(termEntry.size());
            for (Category category : termEntry) {
                int categoryIdx = categoryIndices.get(category.getName());
                out.writeInt(categoryIdx);
                out.writeInt(category.getCount());
            }
            monitor.increment();
        }
        // feature setting
        out.writeObject(featureSetting);
        // name
        out.writeObject(name);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // version
        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported version: " + version);
        }
        Map<Integer, String> categoryIndices = CollectionHelper.newHashMap();
        entryTrie = new TrieCategoryEntries();
        // header
        int numCategories = in.readInt();
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
        String dictName = name == null || name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
        ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask("Reading " + dictName, numTerms);
        CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numTerms; i++) {
            String term = (String)in.readObject();
            TrieCategoryEntries categoryEntries = entryTrie.getOrAdd(term, true);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                categoryEntries.append(categoryName, categoryCount);
                termCountBuilder.add(categoryName, categoryCount);
            }
            monitor.increment();
        }
        termCounts = termCountBuilder.create();
        // feature setting
        featureSetting = (FeatureSetting)in.readObject();
        // name
        name = (String)in.readObject();
    }

//    // iterator over all entries
//
//    private static final class TrieIterator extends AbstractIterator<TrieCategoryEntries> {
//        private final Deque<Iterator<TrieCategoryEntries>> stack;
//        private TrieCategoryEntries currentEntries;
//        private final boolean readOnly;
//
//        private TrieIterator(TrieCategoryEntries root, boolean readOnly) {
//            stack = new ArrayDeque<Iterator<TrieCategoryEntries>>();
//            stack.push(root.children());
//            this.readOnly = readOnly;
//        }
//
//        @Override
//        protected TrieCategoryEntries getNext() throws Finished {
//            for (;;) {
//                if (stack.isEmpty()) {
//                    throw FINISHED;
//                }
//                Iterator<TrieCategoryEntries> current = stack.peek();
//                if (!current.hasNext()) {
//                    throw FINISHED;
//                }
//                TrieCategoryEntries node = current.next();
//                if (!current.hasNext()) {
//                    stack.pop();
//                }
//                Iterator<TrieCategoryEntries> children = node.children();
//                if (children.hasNext()) {
//                    stack.push(children);
//                }
//                if (node.hasData()) {
//                    currentEntries = node;
//                    return node;
//                }
//            }
//        }
//
//        @Override
//        public void remove() {
//            if (readOnly) {
//                throw new UnsupportedOperationException("Modifications are not allowed.");
//            }
//            if (currentEntries == null) {
//                throw new NoSuchElementException();
//            }
//            currentEntries.firstCategory = null;
//        }
//
//    }

    // inner classes

//    private static class TrieCategoryEntries extends AbstractCategoryEntries implements TermCategoryEntries {
//
//        private static final char EMPTY_CHARACTER = '\u0000';
//
//        private static final TrieCategoryEntries EMPTY = new TrieCategoryEntries();
//
//        private static final TrieCategoryEntries[] EMPTY_ARRAY = new TrieCategoryEntries[0];
//
//        private final char character;
//
//        private final TrieCategoryEntries parent;
//
//        private TrieCategoryEntries[] children = EMPTY_ARRAY;
//
//        private LinkedCategoryCount firstCategory;
//
//        private int totalCount;
//
//        private TrieCategoryEntries() {
//            this(EMPTY_CHARACTER, null);
//        }
//
//        private TrieCategoryEntries(char character, TrieCategoryEntries parent) {
//            this.character = character;
//            this.parent = parent;
//        }
//
//        private TrieCategoryEntries get(CharSequence key) {
//            return getOrAdd(key, false);
//        }
//
//        private TrieCategoryEntries getOrAdd(CharSequence key, boolean create) {
//            if (key == null || key.length() == 0) {
//                return this;
//            }
//            char head = key.charAt(0);
//            CharSequence tail = tail(key);
//            for (TrieCategoryEntries node : children) {
//                if (head == node.character) {
//                    return node.getOrAdd(tail, create);
//                }
//            }
//            if (create) {
//                TrieCategoryEntries newNode = new TrieCategoryEntries(head, this);
//                if (children == EMPTY_ARRAY) {
//                    children = new TrieCategoryEntries[] {newNode};
//                } else {
//                    TrieCategoryEntries[] newArray = new TrieCategoryEntries[children.length + 1];
//                    System.arraycopy(children, 0, newArray, 0, children.length);
//                    newArray[children.length] = newNode;
//                    children = newArray;
//                }
//                return newNode.getOrAdd(tail, create);
//            } else {
//                return null;
//            }
//        }
//
//        private CharSequence tail(CharSequence seq) {
//            return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
//        }
//
//        /**
//         * Increments a category count by the given value.
//         * 
//         * @param category the category to increment, not <code>null</code>.
//         * @param count the number by which to increment, greater/equal zero.
//         */
//        private void increment(String category, int count) {
//            for (LinkedCategoryCount current = firstCategory; current != null; current = current.nextCategory) {
//                if (category.equals(current.categoryName)) {
//                    current.count += count;
//                    totalCount += count;
//                    return;
//                }
//            }
//            append(category, count);
//        }
//
//        /**
//         * Add a category with a given count (no duplicate checking takes place: only to be used, when one can make sure
//         * that it does not already exist).
//         * 
//         * @param category the category to add, not <code>null</code>.
//         * @param count the count to set for the category.
//         */
//        private void append(String category, int count) {
//            LinkedCategoryCount tmp = firstCategory;
//            firstCategory = new LinkedCategoryCount(category, count);
//            firstCategory.nextCategory = tmp;
//            totalCount += count;
//        }
//
//        @Override
//        public Iterator<Category> iterator() {
//            return new AbstractIterator<Category>() {
//                LinkedCategoryCount next = firstCategory;
//
//                @Override
//                protected Category getNext() throws Finished {
//                    if (next == null) {
//                        throw FINISHED;
//                    }
//                    String categoryName = next.categoryName;
//                    double probability = (double)next.count / totalCount;
//                    int count = next.count;
//                    next = next.nextCategory;
//                    return new ImmutableCategory(categoryName, probability, count);
//                }
//
//            };
//        }
//
//        private Iterator<TrieCategoryEntries> children() {
//            return new ArrayIterator<TrieCategoryEntries>(children);
//        }
//
//        private boolean hasData() {
//            return firstCategory != null;
//        }
//
//        @Override
//        public String getTerm() {
//            StringBuilder builder = new StringBuilder().append(character);
//            for (TrieCategoryEntries current = parent; current != null; current = current.parent) {
//                if (current.character != EMPTY_CHARACTER) {
//                    builder.append(current.character);
//                }
//            }
//            return builder.reverse().toString();
//        }
//
//        @Override
//        public int getTotalCount() {
//            return totalCount;
//        }
//
//        /**
//         * Remove all empty nodes which have no children (saves memory, in case terms have been removed from the trie).
//         * 
//         * @return <code>true</code> in case this node is empty and has no children.
//         */
//        private boolean clean() {
//            boolean clean = true;
//            List<TrieCategoryEntries> temp = CollectionHelper.newArrayList();
//            for (TrieCategoryEntries entries : children) {
//                boolean childClean = entries.clean();
//                if (!childClean) {
//                    temp.add(entries);
//                }
//                clean &= childClean;
//            }
//            int childCount = temp.size();
//            children = childCount > 0 ? temp.toArray(new TrieCategoryEntries[childCount]) : EMPTY_ARRAY;
//            clean &= !hasData();
//            return clean;
//        }
//
//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int result = 1;
//            for (Category category : this) {
//                result += category.hashCode();
//            }
//            result = prime * result + getTerm().hashCode();
//            return result;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) {
//                return true;
//            }
//            if (obj == null || getClass() != obj.getClass()) {
//                return false;
//            }
//            TermCategoryEntries other = (TermCategoryEntries)obj;
//            if (!getTerm().equals(other.getTerm())) {
//                return false;
//            }
//            if (size() != other.size()) {
//                return false;
//            }
//            for (Category thisCategory : this) {
//                int thisCount = thisCategory.getCount();
//                int otherCount = other.getCount(thisCategory.getName());
//                if (thisCount != otherCount) {
//                    return false;
//                }
//            }
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder builder = new StringBuilder();
//            builder.append(getTerm()).append(':');
//            boolean first = true;
//            for (Category category : this) {
//                if (first) {
//                    first = false;
//                } else {
//                    builder.append(',');
//                }
//                builder.append(category);
//            }
//            return builder.toString();
//        }
//
//    }

//    private static final class LinkedCategoryCount {
//        private final String categoryName;
//        private int count;
//        private LinkedCategoryCount nextCategory;
//
//        private LinkedCategoryCount(String name, int count) {
//            this.categoryName = name;
//            this.count = count;
//        }
//
//    }

}
