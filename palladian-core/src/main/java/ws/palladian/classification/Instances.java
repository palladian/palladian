package ws.palladian.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ws.palladian.classification.numeric.MinMaxNormalization;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;

public class Instances<T> extends ArrayList<T> {

    private static final long serialVersionUID = 9062002858891518522L;

    private boolean normalized = false;

    private Categories categories = new Categories();

    /**
     * This stores the max - min differences for each feature of the training instances. We need these values to
     * normalize test or unseen data. <featureIndex, max-min>
     */
    private MinMaxNormalization minMaxNormalization;

    /**
     * Get the number of documents that have been assigned to given category.
     * 
     * @param categoryName The name of the category.
     * @return number The number of documents classified in the given category.
     */
    public int getClassifiedNumberOfCategory(String categoryName) {
        return getClassifiedNumberOfCategory(new Category(categoryName));
    }

    /**
     * Get the number of documents that have been assigned to given category.
     * 
     * @param categoryName The category.
     * @return number The number of documents classified in the given category.
     */
    public int getClassifiedNumberOfCategory(Category category) {
        int number = 0;

        // skip categories that are not main categories because they are classified according to the main category
        if (category.getClassType() == ClassificationTypeSetting.HIERARCHICAL && !category.isMainCategory()) {

            return number;

        } else if (category.getClassType() == ClassificationTypeSetting.HIERARCHICAL && category.isMainCategory()
                || category.getClassType() == ClassificationTypeSetting.SINGLE) {

            for (Instance d : (Instances<Instance>) this) {
                if (d.getMainCategoryEntry().getCategory().getName().equals(category.getName())) {
                    ++number;
                }
            }

        } else {
            for (Instance d : (Instances<Instance>) this) {
                for (CategoryEntry c : d.getAssignedCategoryEntries()) {
                    if (c.getCategory().getName().equals(category.getName())) {
                        ++number;
                    }
                }
            }
        }

        return number;
    }

    /**
     * Perform a min-max normalization over the numeric values of the features. All values will between the interval
     * [0,1] after the
     * normalization.
     */
    public void normalize() {

        if (areNormalized()) {
            return;
        }

        // hold the min value of each feature <featureIndex, minValue>
        Map<Integer, Double> featureMinValueMap = new HashMap<Integer, Double>();

        // hold the max value of each feature <featureIndex, maxValue>
        Map<Integer, Double> featureMaxValueMap = new HashMap<Integer, Double>();

        // find the min and max values
        for (Instance instance : (Instances<Instance>) this) {

            UniversalInstance nInstance = (UniversalInstance)instance;

            for (int i = 0; i < nInstance.getNumericFeatures().size(); i++) {

                double featureValue = nInstance.getNumericFeatures().get(i);

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
        minMaxNormalization = new MinMaxNormalization();
        Map<Integer, Double> normalizationMap = new HashMap<Integer, Double>();
        for (Instance instance : (Instances<Instance>) this) {

            UniversalInstance nInstance = (UniversalInstance)instance;

            for (int i = 0; i < nInstance.getNumericFeatures().size(); i++) {

                double max_minus_min = featureMaxValueMap.get(i) - featureMinValueMap.get(i);
                double featureValue = nInstance.getNumericFeatures().get(i);
                double normalizedValue = (featureValue - featureMinValueMap.get(i)) / max_minus_min;

                nInstance.getNumericFeatures().set(i, normalizedValue);

                normalizationMap.put(i, max_minus_min);
                minMaxNormalization.getMinValueMap().put(i, featureMinValueMap.get(i));
            }

        }

        minMaxNormalization.setNormalizationMap(normalizationMap);
        setNormalized(true);
    }

    public boolean areNormalized() {
        return normalized;
    }

    public void setNormalized(boolean normalized) {
        this.normalized = normalized;
    }

    public MinMaxNormalization getMinMaxNormalization() {
        return minMaxNormalization;
    }

    public void setMinMaxNormalization(MinMaxNormalization minMaxNormalization) {
        this.minMaxNormalization = minMaxNormalization;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public Categories getCategories() {
        return categories;
    }
}
