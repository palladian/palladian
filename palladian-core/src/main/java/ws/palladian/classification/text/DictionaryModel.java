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
public final class DictionaryModel implements Model, Iterable<TermCategoryEntries> {

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

    /** The initial size of the hash table. */
    private static final int INITIAL_SIZE = 1024;

    /** The maximum load factor, until we do a re-hashing. */
    private static final float MAX_LOAD_FACTOR = 0.75f;

    /** Default, when no name is assigned. */
    private static final String NO_NAME = "NONAME";

    /** Hash table with term-category combinations with their counts. */
    private transient TermCategoryEntries[] entryArray;

    /** The number of terms in this dictionary. */
    private transient int numTerms;

    /** Categories with their counts. */
    private transient TermCategoryEntries priors;

    /** Configuration for the feature extraction. */
    private transient FeatureSetting featureSetting;

    /** The optional name of the model. */
    private transient String name;

    /**
     * Create a new {@link DictionaryModel}.
     * 
     * @param featureSetting The feature setting which was used for creating this model, may be <code>null</code>.
     */
    public DictionaryModel(FeatureSetting featureSetting) {
        this.entryArray = new TermCategoryEntries[INITIAL_SIZE];
        this.numTerms = 0;
        this.priors = new TermCategoryEntries(null);
        this.featureSetting = featureSetting;
        this.name = NO_NAME;
    }

    /**
     * @return The name of this model, or {@value #NO_NAME} in case no name was specified.
     */
    public String getName() {
        return name;
    }

    /**
     * Set a name for this model.
     * 
     * @param name The name, not <code>null</code>.
     */
    public void setName(String name) {
        Validate.notNull(name, "name must not be null");
        this.name = name;
    }

    /**
     * @return The feature setting which was used for extracting the features in this model, or <code>null</code> in
     *         case not specified.
     */
    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    /**
     * <p>
     * Add a document (represented by a {@link Collection} of terms) to this model.
     * </p>
     * 
     * @param terms The terms from the document, not <code>null</code>.
     * @param category The category of the document, not <code>null</code>.
     */
    public void addDocument(Collection<String> terms, String category) {
        Validate.notNull(terms, "terms must not be null");
        Validate.notNull(category, "category must not be null");
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
        Validate.notNull(term, "term must not be null");
        Validate.notNull(category, "category must not be null");
        TermCategoryEntries counts = get(term);
        if (counts == null) {
            put(new TermCategoryEntries(term, category));
        } else {
            counts.increment(category);
        }
    }

    /**
     * <p>
     * Get the probabilities for the given term in different categories.
     * </p>
     * 
     * @param term The term, not <code>null</code>.
     * @return The category probabilities for the specified term, or an empty {@link TermCategoryEntries} instance, in
     *         case the term is not present in this model. Never <code>null</code>.
     */
    public TermCategoryEntries getCategoryEntries(String term) {
        Validate.notNull(term, "term must not be null");
        TermCategoryEntries result = get(term);
        return result != null ? result : TermCategoryEntries.EMPTY;
    }

    private TermCategoryEntries get(String term) {
        for (TermCategoryEntries entry = entryArray[index(term.hashCode())]; entry != null; entry = entry.next) {
            if (entry.getTerm().equals(term)) {
                return entry;
            }
        }
        return null;
    }

    private void put(TermCategoryEntries entries) {
        numTerms++;
        if ((float)numTerms / entryArray.length > MAX_LOAD_FACTOR) {
            rehash();
        }
        internalAdd(index(entries.getTerm().hashCode()), entries);
    }

    private void internalAdd(int idx, TermCategoryEntries entries) {
        TermCategoryEntries current = entryArray[idx];
        entryArray[idx] = entries;
        entries.next = current;
    }

    private void rehash() {
        TermCategoryEntries[] oldArray = entryArray;
        entryArray = new TermCategoryEntries[oldArray.length * 2];
        for (TermCategoryEntries entry : oldArray) {
            while (entry != null) {
                TermCategoryEntries current = entry;
                entry = current.next;
                internalAdd(index(current.getTerm().hashCode()), current);
            }
        }
    }

    private int index(int hash) {
        return Math.abs(hash % entryArray.length);
    }

    /**
     * @return The number of distinct terms in this model.
     */
    public int getNumTerms() {
        return numTerms;
    }

    /**
     * @return The number of distinct categories in this model.
     */
    public int getNumCategories() {
        return getCategories().size();
    }

    @Override
    public Iterator<TermCategoryEntries> iterator() {
        return new DictionaryIterator();
    }

    @Override
    public Set<String> getCategories() {
        Set<String> categories = CollectionHelper.newHashSet();
        for (Category category : priors) {
            categories.add(category.getName());
        }
        if (categories.isEmpty()) {
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

    /**
     * @return The prior probabilities for the trained categories. (e.g. category "A" appeared 10 times, category "B"
     *         appeared 15 times during training would make a prior 10/25=0.4 for category "A").
     */
    public CategoryEntries getPriors() {
        return priors;
    }

    /**
     * <p>
     * Dump the {@link DictionaryModel} as CSV format to a {@link PrintStream}. This is more memory efficient than
     * invoking {@link #toString()} as the dictionary can be written directly to a file or console.
     * </p>
     * 
     * @param printStream The print stream to which to write the model, not <code>null</code>.
     */
    public void toCsv(PrintStream printStream) {
        Validate.notNull(printStream, "printStream must not be null");
        printStream.print("Term,");
        printStream.print(StringUtils.join(priors, ","));
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

    // toString

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DictionaryModel [featureSetting=").append(featureSetting);
        builder.append(", #terms=").append(getNumTerms());
        builder.append(", #categories=").append(getNumCategories()).append("]");
        return builder.toString();
    }

    // hashCode + equals

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CollectionHelper.newHashSet(entryArray).hashCode();
        result = prime * result + (featureSetting == null ? 0 : featureSetting.hashCode());
        result = prime * result + numTerms;
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
        if (!priors.equals(other.priors)) {
            return false;
        }
        for (TermCategoryEntries thisEntries : this) {
            TermCategoryEntries otherEntries = getCategoryEntries(thisEntries.getTerm());
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
        SortedSet<String> categoryIndices = new TreeSet<String>(getCategories());
        // version (for being able to provide backwards compatibility from now on)
        out.writeInt(VERSION);
        // header; number of categories; [ (categoryName, count) , ...]
        out.writeInt(categoryIndices.size());
        for (String categoryName : categoryIndices) {
            out.writeObject(categoryName);
            out.writeInt(priors.getCount(categoryName));
        }
        // number of terms; list of terms: [ ( term, numProbabilityEntries, [ (categoryIdx, count), ... ] ), ... ]
        out.writeInt(numTerms);
        for (TermCategoryEntries termEntry : this) {
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
        // header
        int numCategories = in.readInt();
        priors = new TermCategoryEntries(null);
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String)in.readObject();
            int categoryCount = in.readInt();
            priors.increment(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        // terms
        int numberOfTerms = in.readInt();
        entryArray = new TermCategoryEntries[INITIAL_SIZE];
        for (int i = 0; i < numberOfTerms; i++) {
            String term = (String)in.readObject();
            TermCategoryEntries categoryEntries = new TermCategoryEntries(term);
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
        // name
        name = (String)in.readObject();
    }

    // iterator over all entries

    private final class DictionaryIterator extends AbstractIterator<TermCategoryEntries> {
        int entriesIdx = 0;
        TermCategoryEntries next;

        @Override
        protected TermCategoryEntries getNext() throws Finished {
            if (next != null) {
                TermCategoryEntries result = next;
                next = next.next;
                return result;
            }
            while (entriesIdx < entryArray.length) {
                TermCategoryEntries current = entryArray[entriesIdx++];
                if (current != null) {
                    next = current.next;
                    return current;
                }
            }
            throw FINISHED;
        }
    }

}
