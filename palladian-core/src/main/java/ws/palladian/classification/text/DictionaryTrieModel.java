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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.ImmutableCategory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.Adapter;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;

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
        
        /** Trie with term-category combinations with their counts. */
        private final TrieCategoryEntries entryTrie = new TrieCategoryEntries();
        /** Configuration for the feature extraction. */
        private FeatureSetting featureSetting;
        /** The name of this dictionary. */
        private String name;
        /** The number of terms stored in this dictionary. */
        private int numTerms;
        private final Bag<String> priors = Bag.create();
        private final Bag<String> termPriors = Bag.create();

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
                termPriors.add(category);
            }
            priors.add(category);
            return this;
        }
        

        @Override
        public DictionaryModel create() {
            return new DictionaryTrieModel(this);
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
    private transient CategoryEntries documentPriors;
    
    /** The priors, determined from the individual terms. */
    private transient CategoryEntries termPriors;

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
    }

    /** Constructor invoked from the builder only. */
    private DictionaryTrieModel(Builder builder) {
        this.entryTrie = builder.entryTrie;
        this.numTerms = builder.numTerms;
        this.featureSetting = builder.featureSetting;
        this.name = builder.name;
        this.documentPriors = new MapTermCategoryEntries(StringUtils.EMPTY,builder.priors.toMap());
        this.termPriors = new MapTermCategoryEntries(StringUtils.EMPTY,builder.termPriors.toMap());
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
        return CollectionHelper.convert(new TrieIterator(entryTrie),
                Adapter.create(TrieCategoryEntries.class, TermCategoryEntries.class));
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        if (documentPriors.size() > 0) {
            return documentPriors;
        } else {
            // workaround; if priors have not been set explicitly, by using the now deprecated #updateTerm method,
            // we need to collect the category names from the term entries
            Map<String, Double> categories = CollectionHelper.newHashMap();
            for (TermCategoryEntries entries : this) {
                for (Category category : entries) {
                    categories.put(category.getName(), 1.);
                }
            }
            return new CategoryEntriesBuilder(categories).create();
        }
    }
    
    @Override
    public CategoryEntries getTermCounts() {
        return termPriors;
    }

    @Override
    public int prune(PruningStrategy strategy) {
        Validate.notNull(strategy, "strategy must not be null");
        int removedTerms = 0;
        int removedCategories = 0;
        Iterator<TermCategoryEntries> iterator = iterator();
        while (iterator.hasNext()) {
            TermCategoryEntries categoryEntries = iterator.next();
            if (strategy.remove(categoryEntries)) {
                removedTerms++;
                iterator.remove();
            } else {
                Iterator<Category> categoryIterator = categoryEntries.iterator();
                while (categoryIterator.hasNext()) {
                    if (strategy.remove(categoryIterator.next())) {
                        categoryIterator.remove();
                        removedCategories++;
                    }
                }
            }
        }
        numTerms -= removedTerms;
        entryTrie.clean();
        return removedTerms + removedCategories;
    }

    // serialization code

    // Implementation note: in case you make any incompatible changes to the serialization protocol, provide backwards
    // compatibility by using the #VERSION constant. Add a test case for the new version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version from now on for the tests).

    private void writeObject(ObjectOutputStream out) throws IOException {
        // map the category names to numeric indices, so that we can use "1" instead of "aVeryLongCategoryName"
        List<Category> sortedCategories = CollectionHelper.newArrayList(getDocumentCounts());
        Collections.sort(sortedCategories,new Comparator<Category>(){
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
        ProgressMonitor monitor = new ProgressMonitor(numTerms, 1, "Writing " + dictName);
        for (TermCategoryEntries termEntry : this) {
            out.writeObject(termEntry.getTerm());
            out.writeInt(termEntry.size());
            for (Category category : termEntry) {
                int categoryIdx = categoryIndices.get(category.getName());
                out.writeInt(categoryIdx);
                out.writeInt(category.getCount());
            }
            monitor.incrementAndPrintProgress();
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
        Bag<String> priorEntriesBag = Bag.create();
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String)in.readObject();
            int categoryCount = in.readInt();
            priorEntriesBag.set(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        documentPriors = new MapTermCategoryEntries("", priorEntriesBag.toMap());
        // terms
        numTerms = in.readInt();
        String dictName = name == null || name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
        ProgressMonitor monitor = new ProgressMonitor(numTerms, 1, "Reading " + dictName);
        Bag<String> termPriorEntriesBuilder = Bag.create();
        for (int i = 0; i < numTerms; i++) {
            String term = (String)in.readObject();
            TrieCategoryEntries categoryEntries = entryTrie.getOrAdd(term, true);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                categoryEntries.append(categoryName, categoryCount);
                termPriorEntriesBuilder.add(categoryName, categoryCount);
            }
            monitor.incrementAndPrintProgress();
        }
        termPriors = new MapTermCategoryEntries("", termPriorEntriesBuilder.toMap());
        // feature setting
        featureSetting = (FeatureSetting)in.readObject();
        // name
        name = (String)in.readObject();
    }

    // iterator over all entries

    private static final class TrieIterator extends AbstractIterator<TrieCategoryEntries> {
        private final Deque<Iterator<TrieCategoryEntries>> stack;
        private TrieCategoryEntries currentEntries;

        private TrieIterator(TrieCategoryEntries root) {
            stack = new ArrayDeque<Iterator<TrieCategoryEntries>>();
            stack.push(root.children());
        }

        @Override
        protected TrieCategoryEntries getNext() throws Finished {
            for (;;) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<TrieCategoryEntries> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                TrieCategoryEntries node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<TrieCategoryEntries> children = node.children();
                if (children.hasNext()) {
                    stack.push(children);
                }
                if (node.hasData()) {
                    currentEntries = node;
                    return node;
                }
            }
        }

        @Override
        public void remove() {
            if (currentEntries == null) {
                throw new NoSuchElementException();
            }
            currentEntries.firstCategory = null;
        }

    }

    // inner classes

    private static class TrieCategoryEntries extends AbstractCategoryEntries implements TermCategoryEntries {

        private static final char EMPTY_CHARACTER = '\u0000';

        private static final TrieCategoryEntries EMPTY = new TrieCategoryEntries();

        private static final TrieCategoryEntries[] EMPTY_ARRAY = new TrieCategoryEntries[0];

        private final char character;

        private final TrieCategoryEntries parent;

        private TrieCategoryEntries[] children = EMPTY_ARRAY;

        private LinkedCategoryCount firstCategory;

        private int totalCount;

        private TrieCategoryEntries() {
            this(EMPTY_CHARACTER, null);
        }

        private TrieCategoryEntries(char character, TrieCategoryEntries parent) {
            this.character = character;
            this.parent = parent;
        }

        private TrieCategoryEntries get(CharSequence key) {
            return getOrAdd(key, false);
        }

        private TrieCategoryEntries getOrAdd(CharSequence key, boolean create) {
            if (key == null || key.length() == 0) {
                return this;
            }
            char head = key.charAt(0);
            CharSequence tail = tail(key);
            for (TrieCategoryEntries node : children) {
                if (head == node.character) {
                    return node.getOrAdd(tail, create);
                }
            }
            if (create) {
                TrieCategoryEntries newNode = new TrieCategoryEntries(head, this);
                if (children == EMPTY_ARRAY) {
                    children = new TrieCategoryEntries[] {newNode};
                } else {
                    TrieCategoryEntries[] newArray = new TrieCategoryEntries[children.length + 1];
                    System.arraycopy(children, 0, newArray, 0, children.length);
                    newArray[children.length] = newNode;
                    children = newArray;
                }
                return newNode.getOrAdd(tail, create);
            } else {
                return null;
            }
        }

        private CharSequence tail(CharSequence seq) {
            return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
        }

        /**
         * Increments a category count by the given value.
         * 
         * @param category the category to increment, not <code>null</code>.
         * @param count the number by which to increment, greater/equal zero.
         */
        private void increment(String category, int count) {
            for (LinkedCategoryCount current = firstCategory; current != null; current = current.nextCategory) {
                if (category.equals(current.categoryName)) {
                    current.count += count;
                    totalCount += count;
                    return;
                }
            }
            append(category, count);
        }

        /**
         * Add a category with a given count (no duplicate checking takes place: only to be used, when one can make sure
         * that it does not already exist).
         * 
         * @param category the category to add, not <code>null</code>.
         * @param count the count to set for the category.
         */
        private void append(String category, int count) {
            LinkedCategoryCount tmp = firstCategory;
            firstCategory = new LinkedCategoryCount(category, count);
            firstCategory.nextCategory = tmp;
            totalCount += count;
        }

        @Override
        public Iterator<Category> iterator() {
            return new AbstractIterator<Category>() {
                LinkedCategoryCount next = firstCategory;
                LinkedCategoryCount current = null;

                @Override
                protected Category getNext() throws Finished {
                    if (next == null) {
                        throw FINISHED;
                    }
                    String categoryName = next.categoryName;
                    double probability = (double)next.count / totalCount;
                    int count = next.count;
                    current = next;
                    next = next.nextCategory;
                    return new ImmutableCategory(categoryName, probability, count);
                }

                @Override
                public void remove() {
                    if (current == null) {
                        throw new NoSuchElementException();
                    }
                    if (firstCategory == current) { // removing first element, update pointer
                        firstCategory = current.nextCategory;
                    } else { // removing element within tail, iterate to the predecessor and update next pointer
                        for (LinkedCategoryCount temp = firstCategory; temp != null; temp = temp.nextCategory) {
                            if (temp.nextCategory == current) {
                                temp.nextCategory = current.nextCategory;
                                break;
                            }
                        }
                    }
                    // update the total count
                    totalCount -= current.count;
                }
            };
        }

        private Iterator<TrieCategoryEntries> children() {
            return new ArrayIterator<TrieCategoryEntries>(children);
        }

        private boolean hasData() {
            return firstCategory != null;
        }

        @Override
        public String getTerm() {
            StringBuilder builder = new StringBuilder().append(character);
            for (TrieCategoryEntries current = parent; current != null; current = current.parent) {
                if (current.character != EMPTY_CHARACTER) {
                    builder.append(current.character);
                }
            }
            return builder.reverse().toString();
        }

        @Override
        public int getTotalCount() {
            return totalCount;
        }

        /**
         * Remove all empty nodes which have no children (saves memory, in case terms have been removed from the trie).
         * 
         * @return <code>true</code> in case this node is empty and has no children.
         */
        private boolean clean() {
            boolean clean = true;
            List<TrieCategoryEntries> temp = CollectionHelper.newArrayList();
            for (TrieCategoryEntries entries : children) {
                boolean childClean = entries.clean();
                if (!childClean) {
                    temp.add(entries);
                }
                clean &= childClean;
            }
            int childCount = temp.size();
            children = childCount > 0 ? temp.toArray(new TrieCategoryEntries[childCount]) : EMPTY_ARRAY;
            clean &= !hasData();
            return clean;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            for (Category category : this) {
                result += category.hashCode();
            }
            result = prime * result + getTerm().hashCode();
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
            TermCategoryEntries other = (TermCategoryEntries)obj;
            if (!getTerm().equals(other.getTerm())) {
                return false;
            }
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

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getTerm()).append(':');
            boolean first = true;
            for (Category category : this) {
                if (first) {
                    first = false;
                } else {
                    builder.append(',');
                }
                builder.append(category);
            }
            return builder.toString();
        }

    }

    private static final class LinkedCategoryCount {
        private final String categoryName;
        private int count;
        private LinkedCategoryCount nextCategory;

        private LinkedCategoryCount(String name, int count) {
            this.categoryName = name;
            this.count = count;
        }

    }

}
