package ws.palladian.classification.nb;

import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import ws.palladian.classification.Model;
import ws.palladian.helper.collection.CountMap;

public class NaiveBayesModel implements Model {

    private static final long serialVersionUID = 1L;

    private final CountMap<Triplet<String, String, String>> nominalCounts;

    private final CountMap<String> categories;

    private final Map<Pair<String, String>, Double> sampleMeans;

    private final Map<Pair<String, String>, Double> standardDeviations;

    NaiveBayesModel(CountMap<Triplet<String, String, String>> nominalCounts, CountMap<String> categories,
            Map<Pair<String, String>, Double> sampleMeans, Map<Pair<String, String>, Double> standardDeviations) {
        this.nominalCounts = nominalCounts;
        this.categories = categories;
        this.sampleMeans = sampleMeans;
        this.standardDeviations = standardDeviations;
    }

    public CountMap<Triplet<String, String, String>> getNominalCounts() {
        return nominalCounts;
    }

    public CountMap<String> getCategories() {
        return categories;
    }

    public Map<Pair<String, String>, Double> getSampleMeans() {
        return sampleMeans;
    }

    public Map<Pair<String, String>, Double> getStandardDeviations() {
        return standardDeviations;
    }

    /**
     * <p>
     * Get the prior for the specified category.
     * </p>
     * 
     * @param category The category for which to get the prior, not <code>null</code>.
     * @return The prior for the specified category.
     */
    public double getPrior(String category) {
        Validate.notNull(category, "category must not be null");
        return (double)categories.get(category) / categories.totalSize();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NaiveBayesModel [nominalCounts=");
        builder.append(nominalCounts);
        builder.append(", targets=");
        builder.append(categories);
        builder.append(", sampleMeans=");
        builder.append(sampleMeans);
        builder.append(", standardDeviations=");
        builder.append(standardDeviations);
        builder.append("]");
        return builder.toString();
    }

}
