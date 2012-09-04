package ws.palladian.classification;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.classification.numeric.MinMaxNormalization;
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
        if (!limitedCategories.isEmpty()) {
            return limitedCategories.get(0);
        } else {
            return null;
        }
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
        categories.sortByRelevance();
        int n = 0;
        for (CategoryEntry c : categories) {
            if (n < number && c.getRelevance() >= relevanceThreshold) {
                // XXX added by Philipp, lower memory consumption.
                c.setCategoryEntries(limitedCategories);
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
    public static List<NominalInstance> createInstances(String filePath, final boolean readHeader) {
        
        if (!new File(filePath).canRead()) {
            throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
        }

        final List<NominalInstance> instances = CollectionHelper.newArrayList();

        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            String[] headNames;

            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(SEPARATOR);

                if (readHeader && lineNumber == 0) {
                    headNames = parts;
                    return;
                }

                NominalInstance instance = new NominalInstance();
                instance.featureVector = new FeatureVector();

                for (int f = 0; f < parts.length - 1; f++) {
                    String name = headNames == null ? String.valueOf(f) : headNames[f];
                    String value = parts[f];
                    Double doubleValue;
                    // FIXME make better.
                    try {
                        doubleValue = Double.valueOf(value);
                        instance.featureVector.add(new NumericFeature(name, doubleValue));
                    } catch (NumberFormatException e) {
                        instance.featureVector.add(new NominalFeature(name, value));
                    }

                }

                instance.targetClass = parts[parts.length - 1];
                instances.add(instance);
            }
            
        });
        
        return instances;
    }

    /**
     * <p>
     * Perform a min-max normalization over the numeric values of the features. All values will be in the interval [0,1]
     * after normalization.
     * </p>
     * 
     * @param instances
     *            The {@code List} of {@link NominalInstance}s to normalize.
     * @return A {@link MinMaxNormalization} object carrying information to
     *         normalize further {@link NominalInstance}s or {@link FeatureVector}s based on this normalization.
     */
    public static MinMaxNormalization minMaxNormalize(List<NominalInstance> instances) {

        // hold the min value of each feature <featureName, minValue>
        Map<String, Double> featureMinValueMap = new HashMap<String, Double>();

        // hold the max value of each feature <featureIndex, maxValue>
        Map<String, Double> featureMaxValueMap = new HashMap<String, Double>();

        // find the min and max values
        for (NominalInstance instance : instances) {

            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            for (Feature<Double> feature:numericFeatures) {

                double featureValue = feature.getValue();

                // check min value
                if (featureMinValueMap.get(feature.getName()) != null) {
                    double currentMin = featureMinValueMap.get(feature.getName());
                    if (currentMin > featureValue) {
                        featureMinValueMap.put(feature.getName(), featureValue);
                    }
                } else {
                    featureMinValueMap.put(feature.getName(), featureValue);
                }

                // check max value
                if (featureMaxValueMap.get(feature.getName()) != null) {
                    double currentMax = featureMaxValueMap.get(feature.getName());
                    if (currentMax < featureValue) {
                        featureMaxValueMap.put(feature.getName(), featureValue);
                    }
                } else {
                    featureMaxValueMap.put(feature.getName(), featureValue);
                }

            }
        }

        // normalize the feature values
        MinMaxNormalization minMaxNormalization = new MinMaxNormalization();
        Map<String, Double> normalizationMap = new HashMap<String, Double>();
        // List<NominalInstance> normalizedInstances = new
        // ArrayList<NominalInstance>();
        for (NominalInstance instance : instances) {
            // NominalInstance normalizedInstance = new NominalInstance();

            // UniversalInstance nInstance = (UniversalInstance) instance;
            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            for (Feature<Double> numericFeature:numericFeatures) {
                String featureName = numericFeature.getName();
                Double minValue = featureMinValueMap.get(featureName);
                Double maxValue = featureMaxValueMap.get(featureName);
                double max_minus_min = maxValue - minValue;
                double featureValue = numericFeature.getValue();
                double normalizedValue = (featureValue - minValue) / max_minus_min;
                numericFeature.setValue(normalizedValue);

                // nInstance.getNumericFeatures().set(i, normalizedValue);
                // normalizedInstance.featureVector
                // .add(new NumericFeature(
                // FeatureDescriptorBuilder.build(
                // numericFeature.getName(),
                // NumericFeature.class), normalizedValue));

                normalizationMap.put(featureName, max_minus_min);
                minMaxNormalization.getMinValueMap().put(featureName, minValue);
            }
            // normalizedInstances.add(normalizedInstance);

        }

        // return normalizedInstances;

        minMaxNormalization.setNormalizationMap(normalizationMap);
        // setNormalized(true);
        return minMaxNormalization;
    }

}
