package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.Model;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.CountMap2D;

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

    /** Term-category combinations with their counts. */
    private final CountMap2D<String> termCategories = CountMap2D.create();

    /** Categories with their counts. */
    private final CountMap<String> categories = CountMap.create();

    /** Configuration for the feature extraction. */
    private final FeatureSetting featureSetting;

    /** Configurations for the classification type. */
    private final ClassificationTypeSetting classificationTypeSetting;

    /**
     * @param featureSetting
     * @param classificationTypeSetting
     */
    public DictionaryModel(FeatureSetting featureSetting, ClassificationTypeSetting classificationTypeSetting) {
        this.featureSetting = featureSetting;
        this.classificationTypeSetting = classificationTypeSetting;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    public ClassificationTypeSetting getClassificationTypeSetting() {
        return classificationTypeSetting;
    }

    public void updateTerm(String term, String category) {
        termCategories.increment(category, term);
    }

    public Map<String, Double> getCategoryFrequencies(String term) {
        Map<String, Double> categoryFrequencies = CollectionHelper.newHashMap();
        Map<String, Integer> categoryCounts = termCategories.get(term);
        if (categoryCounts != null) {
            int sum = 0;
            for (Entry<String, Integer> categoryCount : categoryCounts.entrySet()) {
                categoryFrequencies.put(categoryCount.getKey(), categoryCount.getValue().doubleValue());
                sum += categoryCount.getValue();
            }
            for (Entry<String, Double> categoryFrequency : categoryFrequencies.entrySet()) {
                categoryFrequency.setValue(categoryFrequency.getValue() / sum);
            }
        }
        return categoryFrequencies;
    }

    public int getNumTerms() {
        return termCategories.sizeY();
    }

    public int getNumCategories() {
        // return termCategories.sizeX();
        return categories.uniqueSize();
    }

    public void addCategory(String catgegory) {
        categories.add(catgegory);
    }

    public CountMap<String> getCategories() {
        return categories;
    }

    public double getPrior(String category) {
        return (double)categories.get(category) / categories.totalSize();
    }

    @Deprecated
    public Map<String, Map<String, Integer>> getMap() {
        return termCategories.getMap();
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
        for (String term : termCategories.keySet()) {
            printStream.print(term);
            printStream.print(",");
            // get word frequency for each category and current term
            Map<String, Double> frequencies = getCategoryFrequencies(term);
            boolean first = true;
            for (String category : categories) {
                Double frequency = frequencies.get(category);
                if (!first) {
                    printStream.print(",");
                } else {
                    first = false;
                }
                if (frequency == null) {
                    printStream.print("0.0");
                } else {
                    printStream.print(frequency);
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
        builder.append(", classificationTypeSetting=");
        builder.append(classificationTypeSetting);
        builder.append(", getNumTerms()=");
        builder.append(getNumTerms());
        builder.append(", getNumCategories()=");
        builder.append(getNumCategories());
        builder.append("]");
        return builder.toString();
    }

}