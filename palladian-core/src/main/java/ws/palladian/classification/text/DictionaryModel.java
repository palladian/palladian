package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Model;
import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * The model implementation for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DictionaryModel implements Model {

    private static final long serialVersionUID = 3L;

    /** The optional name of the model. */
    private String name = "NONAME";
    
    /** Term-category combinations with their counts. */
    private CountingCategoryEntries[] termCategories = new CountingCategoryEntries[2];
    
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

    public CountingCategoryEntries get(String term) {
        int hash = hash(term.hashCode());
        CountingCategoryEntries current = termCategories[index(hash)];
        if (current == null) {
            return null;
        }
        for (;;) {
            if (current == null) {
                return null;
            }
            if (current.getTerm().equals(term)) {
                return current;
            }
            current = current.next;
        }
    }

    private int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    public void put(String term, CountingCategoryEntries entries) {
        numTerms++;
        double load = (double)numTerms / termCategories.length;
        if (load > 0.75) {
            rehash();
        }
        int hash = hash(term.hashCode());
        CountingCategoryEntries current = termCategories[index(hash)];
        if (current == null) {
            termCategories[index(hash)] = entries;
            return;
        }
        for (;;) {
            if (current.next == null) {
                current.next = entries;
                return;
            }
            current = current.next;
        }
    }

    private void rehash() {
        // System.out.println("start rehash, " + termCategories.length);
        CountingCategoryEntries[] oldArray = termCategories;
        termCategories = new CountingCategoryEntries[oldArray.length * 2];
        for (CountingCategoryEntries entry : oldArray) {
//            System.out.println(entry);
            for (;;) {
                if (entry == null) {
                    break;
                }
                int hash = hash(entry.getTerm().hashCode());
                int idx = index(hash);
                if (termCategories[idx] == null) {
                    termCategories[idx] = entry;
                    break;
                } else {
                    // add at end
                    CountingCategoryEntries temp = termCategories[idx];
                    for (;;) {
                        if (temp.next == null) {
                            temp.next = entry;
                            break;
                        }else{
                            temp = temp.next;
                        }
                    }
                }
                
            }
        }
        // System.out.println("end rehash, " + termCategories.length);
    }
    
    private int index(int hash) {
        return hash & (termCategories.length-1);
    }

    public int getNumTerms() {
        return numTerms;
    }
    
    private Iterable<String> terms() {
        // TODO Auto-generated method stub
        return null;
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
     * @param printStream
     */
    public void toCsv(PrintStream printStream) {
        // create the file head
        printStream.print("Term,");
        printStream.print(StringUtils.join(priors, ","));
        printStream.print("\n");
        // one word per line with term frequencies per category
        Set<String> categories = getCategories();
        for (String term : terms()) {
            printStream.print(term);
            printStream.print(",");
            // get word frequency for each category and current term
            CategoryEntries frequencies = getCategoryEntries(term);
            boolean first = true;
            for (String category : categories) {
                Double probability = frequencies.getProbability(category);
                if (!first) {
                    printStream.print(",");
                } else {
                    first = false;
                }
                printStream.print(probability != null ? probability : "0.0");
            }
            printStream.print("\n");
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

}
