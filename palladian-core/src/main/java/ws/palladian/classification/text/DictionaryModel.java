package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Model;
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
    private final Map<String, CountingCategoryEntries> termCategories = CollectionHelper.newHashMap();

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
        CountingCategoryEntries counts = termCategories.get(term);
        if (counts == null) {
            termCategories.put(new String(term), new CountingCategoryEntries(category));
        } else {
            counts.increment(category);
        }
    }

    public CategoryEntries getCategoryEntries(String term) {
        CountingCategoryEntries result = termCategories.get(term);
        return result != null ? result : CountingCategoryEntries.EMPTY;
    }

    public int getNumTerms() {
        return termCategories.size();
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

    public Set<String> getTerms() {
        return termCategories.keySet();
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
        for (String term : termCategories.keySet()) {
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
