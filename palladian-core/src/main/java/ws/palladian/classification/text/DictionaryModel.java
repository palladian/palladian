package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Model;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * The model implementation for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DictionaryModel implements Model, Iterable<CountingCategoryEntries> {

    private static final long serialVersionUID = 4L;

    /** The initial size of the hashtable. */
    private static final int INITIAL_SIZE = 1024;

    /** The maximum load factor, until we do a re-hashing. */
    private static final float MAX_LOAD_FACTOR = 0.75f;

    /** The optional name of the model. */
    private String name = "NONAME";

    /** Hash table with term-category combinations with their counts. */
    private transient CountingCategoryEntries[] entries;

    /** The number of terms in this dictionary. */
    private transient int numEntries;

    /** Categories with their counts. */
    private transient CountingCategoryEntries priors;

    /** Configuration for the feature extraction. */
    private transient FeatureSetting featureSetting;

    /**
     * @param featureSetting The feature setting which was used for creating this model.
     */
    public DictionaryModel(FeatureSetting featureSetting) {
        this.entries = new CountingCategoryEntries[INITIAL_SIZE];
        this.numEntries = 0;
        this.priors = new CountingCategoryEntries(null);
        this.featureSetting = featureSetting;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    /**
     * <p>
     * Add a document (represented by a {@link Collection} of terms) to this model.
     * </p>
     * 
     * @param terms The terms from the document.
     * @param category The category of the document.
     */
    public void addDocument(Collection<String> terms, String category) {
        for (String term : terms) {
            updateTerm(term, category);
        }
        priors.increment(category);
    }

    /**
     * @deprecated Use {@link #addDocument(Collection, String)} instead.
     */
    @Deprecated
    public void updateTerm(String term, String category) {
        CountingCategoryEntries counts = get(term);
        if (counts == null) {
            put(new CountingCategoryEntries(term, category));
        } else {
            counts.increment(category);
        }
    }

    public CategoryEntries getCategoryEntries(String term) {
        CountingCategoryEntries result = get(term);
        return result != null ? result : CountingCategoryEntries.EMPTY;
    }

    private CountingCategoryEntries get(String term) {
        for (CountingCategoryEntries entry = entries[index(term.hashCode())]; entry != null; entry = entry.next) {
            if (entry.getTerm().equals(term)) {
                return entry;
            }
        }
        return null;
    }

    private void put(CountingCategoryEntries entries) {
        numEntries++;
        if ((float)numEntries / this.entries.length > MAX_LOAD_FACTOR) {
            rehash();
        }
        internalAdd(index(entries.getTerm().hashCode()), entries);
    }

    private void internalAdd(int idx, CountingCategoryEntries entries) {
        CountingCategoryEntries current = this.entries[idx];
        if (current == null) {
            this.entries[idx] = entries;
        } else {
            for (;; current = current.next) {
                if (current.next == null) {
                    current.next = entries;
                    break;
                }
            }
        }
    }

    private void rehash() {
        CountingCategoryEntries[] oldArray = entries;
        entries = new CountingCategoryEntries[oldArray.length * 2];
        for (CountingCategoryEntries entry : oldArray) {
            if (entry != null) {
                internalAdd(index(entry.getTerm().hashCode()), entry);
            }
        }
    }

    private int index(int hash) {
        return Math.abs(hash % entries.length);
    }

    public int getNumTerms() {
        return numEntries;
    }

    public int getNumCategories() {
        return getCategories().size();
    }
    
    @Override
    public Iterator<CountingCategoryEntries> iterator() {
        return new DictionaryIterator();
    }

    @Override
    public Set<String> getCategories() {
        Set<String> categories = CollectionHelper.newHashSet();
        for (Category category : priors) {
            categories.add(category.getName());
        }
        return categories;
    }

    public CategoryEntries getPriors() {
        return priors;
    }

    /**
     * <p>
     * Dump the {@link DictionaryModel} as CSV format to a {@link PrintStream}. This is more memory efficient than
     * invoking {@link #toString()} as the dictionary can be written directly to a file or console.
     * </p>
     * 
     * @param printStream The print stream to which to write the model.
     */
    public void toCsv(PrintStream printStream) {
        Validate.notNull(printStream, "printStream must not be null");
        printStream.print("Term,");
        printStream.print(StringUtils.join(priors, ","));
        printStream.print('\n');
        Set<String> categories = getCategories();
        for (CountingCategoryEntries entries : this) {
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
    
    // toString

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DictionaryModel [featureSetting=");
        builder.append(featureSetting);
        builder.append(", numTerms=");
        builder.append(getNumTerms());
        builder.append(", numCategories=");
        builder.append(getNumCategories());
        builder.append("]");
        return builder.toString();
    }
    
    // hashCode + equals
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CollectionHelper.newHashSet(entries).hashCode();
        result = prime * result + (featureSetting == null ? 0 : featureSetting.hashCode());
        result = prime * result + numEntries;
        result = prime * result + priors.hashCode();
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
        DictionaryModel other = (DictionaryModel)obj;
        if (!equalIgnoreOrder(entries, other.entries)) {
            return false;
        }
        if (featureSetting == null) {
            if (other.featureSetting != null) {
                return false;
            }
        } else if (!featureSetting.equals(other.featureSetting)) {
            return false;
        }
        if (numEntries != other.numEntries) {
            return false;
        }
        if (!priors.equals(other.priors)) {
            return false;
        }
        return true;
    }
    
    // serialization code
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        SortedSet<String> categoryIndices = new TreeSet<String>(getCategories());
        // header; number of categories; [ (categoryName, count) , ...]
        out.writeInt(categoryIndices.size());
        for (String categoryName : categoryIndices) {
            out.writeObject(categoryName);
            out.writeInt(priors.getCount(categoryName));
        }
        // number of terms; list of terms: [ ( term, numProbabilityEntries, [ (categoryIdx, count), ... ] ), ... ]
        out.writeInt(numEntries);
        for (CountingCategoryEntries termEntry : this) {
            String term = termEntry.getTerm();
            int numProbabilityEntries = termEntry.size();
            out.writeObject(term);
            out.writeInt(numProbabilityEntries);
            for (Category category : termEntry) {
                int categoryIdx = categoryIndices.headSet(category.getName()).size();
                out.writeInt(categoryIdx);
                out.writeInt(category.getCount());
            }
        }
        // feature setting
        out.writeObject(featureSetting);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Map<Integer, String> categoryIndices = CollectionHelper.newHashMap();
        // header
        int numCategories = in.readInt();
        priors = new CountingCategoryEntries(null);
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String)in.readObject();
            int categoryCount = in.readInt();
            priors.increment(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        // terms
        int numberOfTerms = in.readInt();
        entries = new CountingCategoryEntries[(int)Math.ceil(numberOfTerms * MAX_LOAD_FACTOR)];
        for (int i = 0; i < numberOfTerms; i++) {
            String term = (String)in.readObject();
            CountingCategoryEntries categoryEntries = new CountingCategoryEntries(term);
            int numProbabilityEntries = in.readInt();
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                categoryEntries.increment(categoryName, categoryCount);
            }
            put(categoryEntries);
        }
        // feature setting
        featureSetting = (FeatureSetting)in.readObject();
    }
    
    // iterator over all entries

    private final class DictionaryIterator extends AbstractIterator<CountingCategoryEntries> {
        int entriesIdx = 0;
        CountingCategoryEntries next;

        @Override
        protected CountingCategoryEntries getNext() throws Finished {
            if (next != null) {
                CountingCategoryEntries result = next;
                next = next.next;
                return result;
            }
            for (entriesIdx++; entriesIdx < entries.length; entriesIdx++) {
                CountingCategoryEntries current = entries[entriesIdx];
                if (current != null) {
                    next = current.next;
                    return current;
                }
            }
            throw FINISHED;
        }
    }

    // utility

    /**
     * <p>
     * Check, if two arrays contain the same elements, no matter in what order and ignoring duplicates (e.g.
     * <code>[2,1,2].equalIgnoreOrder([1,2])</code>).
     * </p>
     * 
     * @param arrayA The first array, not <code>null</code>.
     * @param arrayB The second array, not <code>null</code>.
     * @return <code>true</code> in case both arrays contain exactly the same elements.
     */
    static boolean equalIgnoreOrder(Object[] arrayA, Object[] arrayB) {
        Set<Object> tempA = CollectionHelper.newHashSet(arrayA);
        Set<Object> tempB = CollectionHelper.newHashSet(arrayB);
        return tempA.equals(tempB);
    }

}
