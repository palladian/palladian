/**
 * 
 */
package ws.palladian.classification;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ws.palladian.classification.numeric.MinMaxNormalization;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
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
     * Create instances from a file. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass
     * </p>
     * <p>
     * All features must be real values and the class must be nominal. Each line is one training instance.
     * </p>
     * 
     * @param filePath
     *            The path to the CSV file to load.
     */
    public static List<NominalInstance> createInstances(String filePath) {
        List<NominalInstance> instances = new LinkedList<NominalInstance>();
        File csvFile = new File(filePath);
        List<String> trainingLines = FileHelper.readFileToArray(csvFile);

        NominalInstance instance = null;

        for (String trainingLine : trainingLines) {
            String[] parts = trainingLine.split(";");

            instance = new NominalInstance();// (instances);
            instance.featureVector = new FeatureVector();

            for (int f = 0; f < parts.length - 1; f++) {
                instance.featureVector.add(new NumericFeature(String.valueOf(f), Double.valueOf(parts[f])));
            }

            instance.targetClass = parts[parts.length - 1];
            instances.add(instance);
        }

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

        // hold the min value of each feature <featureIndex, minValue>
        Map<Integer, Double> featureMinValueMap = new HashMap<Integer, Double>();

        // hold the max value of each feature <featureIndex, maxValue>
        Map<Integer, Double> featureMaxValueMap = new HashMap<Integer, Double>();

        // find the min and max values
        for (NominalInstance instance : instances) {

            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            for (int i = 0; i < numericFeatures.size(); i++) {

                double featureValue = numericFeatures.get(i).getValue();

                // check min value
                if (featureMinValueMap.get(i) != null) {
                    double currentMin = featureMinValueMap.get(i);
                    if (currentMin > featureValue) {
                        featureMinValueMap.put(i, featureValue);
                    }
                } else {
                    featureMinValueMap.put(i, featureValue);
                }

                // check max value
                if (featureMaxValueMap.get(i) != null) {
                    double currentMax = featureMaxValueMap.get(i);
                    if (currentMax < featureValue) {
                        featureMaxValueMap.put(i, featureValue);
                    }
                } else {
                    featureMaxValueMap.put(i, featureValue);
                }

            }
        }

        // normalize the feature values
        MinMaxNormalization minMaxNormalization = new MinMaxNormalization();
        Map<Integer, Double> normalizationMap = new HashMap<Integer, Double>();
        // List<NominalInstance> normalizedInstances = new
        // ArrayList<NominalInstance>();
        for (NominalInstance instance : instances) {
            // NominalInstance normalizedInstance = new NominalInstance();

            // UniversalInstance nInstance = (UniversalInstance) instance;
            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            for (int i = 0; i < numericFeatures.size(); i++) {
                Feature<Double> numericFeature = numericFeatures.get(i);

                double max_minus_min = featureMaxValueMap.get(i) - featureMinValueMap.get(i);
                double featureValue = numericFeature.getValue();
                double normalizedValue = (featureValue - featureMinValueMap.get(i)) / max_minus_min;
                numericFeature.setValue(normalizedValue);

                // nInstance.getNumericFeatures().set(i, normalizedValue);
                // normalizedInstance.featureVector
                // .add(new NumericFeature(
                // FeatureDescriptorBuilder.build(
                // numericFeature.getName(),
                // NumericFeature.class), normalizedValue));

                normalizationMap.put(i, max_minus_min);
                minMaxNormalization.getMinValueMap().put(i, featureMinValueMap.get(i));
            }
            // normalizedInstances.add(normalizedInstance);

        }

        // return normalizedInstances;

        minMaxNormalization.setNormalizationMap(normalizationMap);
        // setNormalized(true);
        return minMaxNormalization;
    }

}
