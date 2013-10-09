package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Model;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.CountMatrix;

/**
 * <p>
 * The model implementation for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DictionaryModel implements Model {

    private static final long serialVersionUID = 1L;

    /** The optional name of the model. */
    private String name = "NONAME";

    /** Term-category combinations with their counts. */
    private final CountMatrix<String> termCategories = CountMatrix.create();

    /** Categories with their counts. */
    private final CountMap<String> categories = CountMap.create();

    /** Configuration for the feature extraction. */
    private final FeatureSetting featureSetting;

    /**
     * @param featureSetting
     * @param classificationTypeSetting
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
        termCategories.add(category, new String(term));
    }

    public CategoryEntries getCategoryEntries(String term) {
        CategoryEntriesMap categoryFrequencies = new CategoryEntriesMap();
        List<Pair<String, Integer>> termRow = termCategories.getRow(term);
        int sum = 0;
        for (Pair<String, Integer> categoryValue : termRow) {
            sum += categoryValue.getValue();
        }
        for (Pair<String, Integer> categoryValue : termRow) {
            categoryFrequencies.set(categoryValue.getKey(), (double)categoryValue.getValue() / sum);
        }
        return categoryFrequencies;
    }

    public int getTermCount(String term) {
        return termCategories.getRowSum(term);
    }

    public int getNumTerms() {
        return termCategories.sizeY();
    }

    public int getNumCategories() {
        return termCategories.sizeX();
    }

    public Set<String> getCategories() {
        return termCategories.getKeysX();
    }

    public Set<String> getTerms() {
        return termCategories.getKeysY();
    }

    public void addCategory(String catgegory) {
        categories.add(catgegory);
    }

    public double getPrior(String category) {
        return (double)categories.getCount(category) / categories.totalSize();
    }

    public Map<String, Double> getPriors() {
        Map<String, Double> result = CollectionHelper.newHashMap();
        for (String category : categories) {
            result.put(category, (double)categories.getCount(category) / categories.totalSize());
        }
        return result;
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
        printStream.print(StringUtils.join(categories, ","));
        printStream.print("\n");

        // one word per line with term frequencies per category
        for (String term : termCategories.getKeysY()) {
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
                if (probability == null) {
                    printStream.print("0.0");
                } else {
                    printStream.print(probability);
                }
            }
            printStream.print("\n");
        }
        printStream.flush();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        // ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // toCsv(new PrintStream(stream));
        // return stream.toString();

        StringBuilder builder = new StringBuilder();
        builder.append("DictionaryModel [featureSetting=");
        builder.append(featureSetting);
        builder.append(", getNumTerms()=");
        builder.append(getNumTerms());
        builder.append(", getNumCategories()=");
        builder.append(getNumCategories());
        builder.append("]");
        return builder.toString();
    }

}