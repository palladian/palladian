package ws.palladian.classification.utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A utility class providing convenience methods for working with classifiers and their results.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public final class ClassificationUtils {

    private static final String SEPARATOR = ";";

    /**
     * <p>
     * Should not be instantiated.
     * </p>
     */
    private ClassificationUtils() {
        throw new UnsupportedOperationException(
                "Unable to instantiate ClassificationUtils. This class is a utility class. It makes no sense to instantiate it.");
    }

    public static CategoryEntry getSingleBestCategoryEntry(CategoryEntries entries) {
        CategoryEntries limitedCategories = limitCategories(entries, 1, 0.0);
        if (limitedCategories.iterator().hasNext()) {
            return limitedCategories.iterator().next();
        }
        return null;
    }

    /**
     * <p>
     * Creates a new CategoryEntries object by limiting an existing one to a number of different categories, which need
     * to have a relevance score above a provided threshold.
     * </p>
     * 
     * @param number
     *            Number of categories to keep.
     * @param relevanceThreshold
     *            Categories must have at least this much relevance to be kept.
     */
    public static CategoryEntries limitCategories(CategoryEntries categories, int number, double relevanceThreshold) {
        CategoryEntries limitedCategories = new CategoryEntries();
        int n = 0;
        for (CategoryEntry c : categories) {
            if (n < number && c.getProbability() >= relevanceThreshold) {
                // XXX added by Philipp, lower memory consumption.
                // c.setCategoryEntries(limitedCategories);
                limitedCategories.add(c);
            }
            n++;
        }
        return limitedCategories;
    }

    /**
     * <p>
     * Create instances from a file. The instances must be given in a CSV file in the following format:
     * <code>feature1;..;featureN;NominalClass</code>. Each line is one training instance.
     * </p>
     * 
     * @param filePath The path to the CSV file to load either specified as path on the file system or as Java resource
     *            path.
     * @param readHeader <code>true</code> to treat the first line as column headers, <code>false</code> otherwise
     *            (column names are generated automatically).
     */
    public static List<Instance> createInstances(String filePath, final boolean readHeader) {

        if (!new File(filePath).canRead()) {
            throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
        }

        final List<Instance> instances = CollectionHelper.newArrayList();

        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            String[] headNames;

            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(SEPARATOR);

                if (readHeader && lineNumber == 0) {
                    headNames = parts;
                    return;
                }

                FeatureVector featureVector = new FeatureVector();

                for (int f = 0; f < parts.length - 1; f++) {
                    String name = headNames == null ? String.valueOf(f) : headNames[f];
                    String value = parts[f];
                    // FIXME make better.
                    if (value.equals("?")) {
                        // missing value, TODO maybe rethink what to do here and how
                        // to handle missing values in general.
                        continue;
                    }
                    try {
                        Double doubleValue = Double.valueOf(value);
                        featureVector.add(new NumericFeature(name, doubleValue));
                    } catch (NumberFormatException e) {
                        featureVector.add(new NominalFeature(name, value));
                    }

                }

                String targetClass = parts[parts.length - 1];
                Instance instance = new Instance(targetClass, featureVector);

                instances.add(instance);
            }

        });

        return instances;
    }

    /**
     * <p>
     * Calculate Min-Max normalization information over the numeric values of the given features (i.e. calculate the
     * minimum and maximum values for each feature). The {@link MinMaxNormalization} instance can then be used to
     * normalize numeric instances to an interval of [0,1].
     * </p>
     * 
     * @param instances The {@code List} of {@link Instance}s to normalize, not <code>null</code>.
     * @return A {@link MinMaxNormalization} instance carrying information to normalize {@link Instance}s based on the
     *         calculated normalization information.
     */
    public static MinMaxNormalization calculateMinMaxNormalization(List<Instance> instances) {
        Validate.notNull(instances, "instances must not be null");

        // hold the min value of each feature <featureName, minValue>
        Map<String, Double> minValues = CollectionHelper.newHashMap();

        // hold the max value of each feature <featureIndex, maxValue>
        Map<String, Double> maxValues = CollectionHelper.newHashMap();

        // find the min and max values
        for (Instance instance : instances) {

            List<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);

            for (Feature<Double> feature : numericFeatures) {

                String featureName = feature.getName();
                double featureValue = feature.getValue();

                // check min value
                if (minValues.get(featureName) != null) {
                    double currentMin = minValues.get(featureName);
                    if (currentMin > featureValue) {
                        minValues.put(featureName, featureValue);
                    }
                } else {
                    minValues.put(featureName, featureValue);
                }

                // check max value
                if (maxValues.get(featureName) != null) {
                    double currentMax = maxValues.get(featureName);
                    if (currentMax < featureValue) {
                        maxValues.put(featureName, featureValue);
                    }
                } else {
                    maxValues.put(featureName, featureValue);
                }

            }
        }

        return new MinMaxNormalization(maxValues, minValues);
    }

}
