package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.Model;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.collection.CountMatrix.IntegerMatrixVector;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * The model implementation for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class DictionaryModel implements Model {

    private static final long serialVersionUID = 2L;

    /** The optional name of the model. */
    private String name = "NONAME";

    /** Term-category combinations with their counts. */
    private final CountMatrix<String> termCategories = CountMatrix.create();

    /** Categories with their counts. */
    private final Bag<String> categories = Bag.create();

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
        termCategories.add(category, new String(term));
    }

    @Deprecated
    public CategoryEntries getCategoryEntries(String term) {
        CategoryEntriesMap categoryFrequencies = new CategoryEntriesMap();
        IntegerMatrixVector<String> row = termCategories.getRow(term);
        if (row != null) {
            int sum = row.getSum();
            for (VectorEntry<String, Integer> entry : row) {
                categoryFrequencies.set(entry.key(), (double)entry.value() / sum);
            }
        }
        return categoryFrequencies;
    }

    /**
     * Get a vector denoting, how often the given term occurs in each category observed during training.
     * 
     * @param term The term for which to retrieve the counts.
     * @return A vector.
     */
    public IntegerMatrixVector<String> getCategoryCounts(String term) {
        return termCategories.getRow(term);
    }

//    public int getTermCount(String term) {
//        return termCategories.getRow(term).getSum();
//    }

    public int getNumTerms() {
        return termCategories.rowCount();
    }

    public int getNumCategories() {
        return termCategories.columnCount();
    }

    @Override
    public Set<String> getCategories() {
        return termCategories.getColumnKeys();
    }

    public Set<String> getTerms() {
        return termCategories.getRowKeys();
    }

    public void addCategory(String catgegory) {
        categories.add(catgegory);
    }

//    public double getPrior(String category) {
//        return (double)categories.count(category) / categories.size();
//    }

    public Map<String, Double> getPriors() {
        Map<String, Double> result = CollectionHelper.newHashMap();
        int sum = categories.size();
        for (Entry<String, Integer> category : categories.unique()) {
            result.put(category.getKey(), (double)category.getValue() / sum);
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
        for (String term : termCategories.getRowKeys()) {
            printStream.print(term);
            printStream.print(",");
            // get word frequency for each category and current term
            CategoryEntries frequencies = getCategoryEntries(term);
            boolean first = true;
            for (String category : categories.uniqueItems()) {
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

    @Override
    public String toString() {

        // ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // toCsv(new PrintStream(stream));
        // return stream.toString();

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
