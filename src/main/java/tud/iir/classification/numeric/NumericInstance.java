package tud.iir.classification.numeric;

import java.util.ArrayList;
import java.util.List;

import tud.iir.classification.Category;
import tud.iir.classification.Instance;
import tud.iir.classification.Instances;

/**
 * <p>
 * An instance contains a list of numeric values as features and a nominal or numeric class.
 * </p>
 * <p>
 * For example, an instance could look like this, f1...fn | class:<br>
 * 1, 4.8, 46 | A<br>
 * or with a numeric class value<br>
 * 5, 68, 8.4 | 4.6<br>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class NumericInstance extends Instance {

    /** The feature list of the instance. */
    private List<Double> features;

    /** The class of the instance. This can be nominal or numeric. */
    private Object instanceClass;

    /** Whether or not the class of the instance is nominal. */
    private boolean classNominal = false;

    public <T> NumericInstance(Instances<T> instances) {
        setInstances(instances);
        features = new ArrayList<Double>();
    }

    public List<Double> getFeatures() {
        return features;
    }

    public void setFeatures(List<Double> features) {
        this.features = features;
    }

    public Object getInstanceClass() {
        if (isClassNominal()) {
            if (getInstanceCategory() == null) {
                setInstanceCategory(new Category(instanceClass.toString()));
            }
            return getInstanceCategory();
        } else if (instanceClass != null) {
            return Double.valueOf(instanceClass.toString());
        }

        return null;
    }

    // public void setInstanceClass(Object instanceClass) {
    // this.instanceClass = instanceClass;
    // }

    public boolean isClassNominal() {
        return classNominal;
    }

    public void setClassNominal(boolean classNominal) {
        this.classNominal = classNominal;
    }

    public void normalize(MinMaxNormalization normalization) {

        for (int i = 0; i < features.size(); i++) {

            double featureValue = features.get(i);
            double normalizedValue = (featureValue - normalization.getMinValueMap().get(i))
            / normalization.getNormalizationMap().get(i);

            features.set(i, normalizedValue);
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NumericInstance [features=");
        builder.append(features);
        builder.append(", instanceClass=");
        builder.append(getInstanceClass());
        builder.append(", classNominal=");
        builder.append(classNominal);
        builder.append(", instanceCategory=");
        builder.append(getInstanceCategory());
        builder.append(", assignedCategories=");
        builder.append(getAssignedCategoryEntries());
        builder.append("]");
        return builder.toString();
    }

}