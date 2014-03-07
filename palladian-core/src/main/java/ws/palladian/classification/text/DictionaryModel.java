package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

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
    
    private static final int INITIAL_SIZE = 1024;
    
    private static final float MAX_LOAD_FACTOR = 0.75f;

    /** The optional name of the model. */
    private String name = "NONAME";
    
    /** Term-category combinations with their counts. */
    private CountingCategoryEntries[] entries = new CountingCategoryEntries[INITIAL_SIZE];
    
    /** The number of terms in this dictionary. */
    private int numTerms = 0;

    /** Categories with their counts. */
    private final CountingCategoryEntries priors = new CountingCategoryEntries();

    /** Configuration for the feature extraction. */
    private final FeatureSetting featureSetting;

    /**
     * @param featureSetting The feature setting which was used for creating this model.
     */
    public DictionaryModel(FeatureSetting featureSetting) {
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

    public void updateTerm(String term, String category) {
        CountingCategoryEntries counts = get(term);
        if (counts == null) {
            put(term, new CountingCategoryEntries(term, category));
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

    private void put(String term, CountingCategoryEntries entries) {
        numTerms++;
        if ((float)numTerms / this.entries.length > MAX_LOAD_FACTOR) {
            rehash();
        }
        internalAdd(index(term.hashCode()), entries);
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
        return numTerms;
    }

    @Override
    public Iterator<CountingCategoryEntries> iterator() {
        return new DictionaryIterator();
    }

    public int getNumCategories() {
        return getCategories().size();
    }

    @Override
    public Set<String> getCategories() {
        Set<String> categories = CollectionHelper.newHashSet();
        for (Category category : priors) {
            categories.add(category.getName());
        }
        return categories;
    }

    public void addCategory(String category) {
        priors.increment(category);
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

}
