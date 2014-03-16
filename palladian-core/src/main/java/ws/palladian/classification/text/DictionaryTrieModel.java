package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.ImmutableCategory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.Adapter;
import ws.palladian.helper.collection.ArrayIterator;
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
public final class DictionaryTrieModel implements DictionaryModel {

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

    /**
     * The key under which to store the prior probabilities; empty string so that the priors are stored in the root node
     * of the trie.
     */
    private static final String PRIOR_KEY = StringUtils.EMPTY;

    /** Hash table with term-category combinations with their counts. */
    private transient TrieCategoryEntries entryTrie;

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
     */
    public DictionaryTrieModel(FeatureSetting featureSetting) {
        this.entryTrie = new TrieCategoryEntries();
        this.numTerms = 0;
        this.featureSetting = featureSetting;
        this.name = NO_NAME;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Validate.notNull(name, "name must not be null");
        this.name = name;
    }

    @Override
    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    @Override
    public void addDocument(Collection<String> terms, String category) {
        Validate.notNull(terms, "terms must not be null");
        Validate.notNull(category, "category must not be null");
        for (String term : terms) {
            updateTerm(term, category);
        }
        entryTrie.getOrAdd(PRIOR_KEY, true).increment(category, 1);
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
    public int getNumTerms() {
        return numTerms;
    }

    @Override
    public int getNumCategories() {
        return getCategories().size();
    }

    @Override
    public Iterator<TermCategoryEntries> iterator() {
        return CollectionHelper.convert(new TrieIterator(entryTrie),
                Adapter.create(TrieCategoryEntries.class, TermCategoryEntries.class));
    }

    @Override
    public Set<String> getCategories() {
        Set<String> categories = CollectionHelper.newHashSet();
        CategoryEntries priors = getPriors();
        if (priors.size() > 0) {
            for (Category category : priors) {
                categories.add(category.getName());
            }
        } else {
            // workaround; if priors have not been set explicitly, by using the now deprecated #updateTerm method,
            // we need to collect the category names from the term entries
            for (TermCategoryEntries entries : this) {
                for (Category category : entries) {
                    categories.add(category.getName());
                }
            }
        }
        return categories;
    }

    @Override
    public CategoryEntries getPriors() {
        return entryTrie.get(PRIOR_KEY);
    }

    @Override
    public void toCsv(PrintStream printStream) {
        Validate.notNull(printStream, "printStream must not be null");
        printStream.print("Term,");
        printStream.print(StringUtils.join(getPriors(), ","));
        printStream.print('\n');
        Set<String> categories = getCategories();
        for (TermCategoryEntries entries : this) {
            printStream.print(entries.getTerm());
            printStream.print(',');
            boolean first = true;
            for (String category : categories) {
                double probability = entries.getProbability(category);
                if (!first) {
                    printStream.print(',');
                } else {
                    first = false;
                }
                printStream.print(probability);
            }
            printStream.print('\n');
        }
        printStream.flush();
    }

    @Override
    public int prune(PruningStrategy strategy) {
        Validate.notNull(strategy, "strategy must not be null");
        int oldCount = numTerms;
        Iterator<TermCategoryEntries> iterator = iterator();
        while (iterator.hasNext()) {
            if (strategy.remove(iterator.next())) {
                numTerms--;
                iterator.remove();
            }
        }
        entryTrie.clean();
        return oldCount - numTerms;
    }

    // toString

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DictionaryTrieModel [featureSetting=").append(featureSetting);
        builder.append(", #terms=").append(getNumTerms());
        builder.append(", #categories=").append(getNumCategories()).append("]");
        return builder.toString();
    }

    // hashCode + equals

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (TermCategoryEntries entries : this) {
            result += entries.hashCode();
        }
        result = prime * result + (featureSetting == null ? 0 : featureSetting.hashCode());
        result = prime * result + numTerms;
        result = prime * result + getPriors().hashCode();
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
        DictionaryTrieModel other = (DictionaryTrieModel)obj;
        if (featureSetting == null) {
            if (other.featureSetting != null) {
                return false;
            }
        } else if (!featureSetting.equals(other.featureSetting)) {
            return false;
        }
        if (numTerms != other.numTerms) {
            return false;
        }
        if (!getPriors().equals(other.getPriors())) {
            return false;
        }
        for (TermCategoryEntries thisEntries : this) {
            TermCategoryEntries otherEntries = other.getCategoryEntries(thisEntries.getTerm());
            if (!thisEntries.equals(otherEntries)) {
                return false;
            }
        }
        return true;
    }

    // serialization code

    // Implementation note: in case you make any incompatible changes to the serialization protocol, provide backwards
    // compatibility by using the #VERSION constant. Add a test case for the new version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version from now on for the tests).

    private void writeObject(ObjectOutputStream out) throws IOException {
        // map the category names to numeric indices, so that we can use "1" instead of "aVeryLongCategoryName"
        SortedSet<String> sortedCategoryNames = new TreeSet<String>(getCategories());
        Map<String, Integer> categoryIndices = CollectionHelper.newHashMap();
        int idx = 0;
        for (String categoryName : sortedCategoryNames) {
            categoryIndices.put(categoryName, idx++);
        }
        // version (for being able to provide backwards compatibility from now on)
        out.writeInt(VERSION);
        // header; number of categories; [ (categoryName, count) , ...]
        out.writeInt(sortedCategoryNames.size());
        CategoryEntries priors = getPriors();
        for (String categoryName : sortedCategoryNames) {
            out.writeObject(categoryName);
            out.writeInt(priors.getCount(categoryName));
        }
        // number of terms; list of terms: [ ( term, numProbabilityEntries, [ (categoryIdx, count), ... ] ), ... ]
        out.writeInt(numTerms);
        String dictName = name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
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
        TrieCategoryEntries priorEntries = entryTrie.getOrAdd(PRIOR_KEY, true);
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String)in.readObject();
            int categoryCount = in.readInt();
            priorEntries.increment(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        // terms
        numTerms = in.readInt();
        String dictName = name.equals(NO_NAME) ? DictionaryTrieModel.class.getSimpleName() : name;
        ProgressMonitor monitor = new ProgressMonitor(numTerms, 1, "Reading " + dictName);
        for (int i = 0; i < numTerms; i++) {
            String term = (String)in.readObject();
            TrieCategoryEntries categoryEntries = entryTrie.getOrAdd(term, true);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                categoryEntries.increment(categoryName, categoryCount);
            }
            monitor.incrementAndPrintProgress();
        }
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
            totalCount += count;
            for (LinkedCategoryCount current = firstCategory; current != null; current = current.nextCategory) {
                if (category.equals(current.categoryName)) {
                    current.count += count;
                    return;
                }
            }
            append(category, count);
        }

        private void append(String category, int count) {
            LinkedCategoryCount tmp = firstCategory;
            firstCategory = new LinkedCategoryCount(category, count);
            firstCategory.nextCategory = tmp;
        }

        @Override
        public Iterator<Category> iterator() {
            return new AbstractIterator<Category>() {
                LinkedCategoryCount current = firstCategory;

                @Override
                protected Category getNext() throws Finished {
                    if (current == null) {
                        throw FINISHED;
                    }
                    String categoryName = current.categoryName;
                    double probability = (double)current.count / totalCount;
                    int count = current.count;
                    current = current.nextCategory;
                    return new ImmutableCategory(categoryName, probability, count);
                }
            };
        }

        private Iterator<TrieCategoryEntries> children() {
            return new ArrayIterator<TrieCategoryEntries>(children);
        }

        private boolean hasData() {
            return firstCategory != null;
        }

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
            for (int i = 0; i < children.length; i++) {
                boolean childClean = children[i].clean();
                if (!childClean) {
                    temp.add(children[i]);
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
