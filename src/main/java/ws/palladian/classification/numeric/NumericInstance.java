package ws.palladian.classification.numeric;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.Category;
import ws.palladian.classification.Instance;
import ws.palladian.classification.Instances;

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

    // @Override
    // public int compareTo(NumericInstance o) {
    // StringBuilder featureString1 = new StringBuilder();
    // StringBuilder featureString2 = new StringBuilder();
    //
    // for (Double featureValue : getFeatures()) {
    // featureString1.append(featureValue);
    // }
    // for (Double featureValue : o.getFeatures()) {
    // featureString2.append(featureValue);
    // }
    //
    // if (featureString1.toString().hashCode() < featureString1.toString().hashCode()) {
    // return -1;
    // }
    //
    // return 1;
    // }



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

    // @Override
    // public int hashCode() {
    // final int prime = 31;
    // int result = 1;
    // result = prime * result + ((features == null) ? 0 : features.hashCode());
    // result = prime * result + ((instanceClass == null) ? 0 : instanceClass.hashCode());
    // return result;
    // }
    //
    // @Override
    // public boolean equals(Object obj) {
    // if (this == obj) {
    // return true;
    // }
    // if (obj == null) {
    // return false;
    // }
    // if (getClass() != obj.getClass()) {
    // return false;
    // }
    // NumericInstance other = (NumericInstance) obj;
    // if (features == null) {
    // if (other.features != null) {
    // return false;
    // }
    // } else if (!features.equals(other.features)) {
    // return false;
    // }
    // if (instanceClass == null) {
    // if (other.instanceClass != null) {
    // return false;
    // }
    // } else if (!instanceClass.equals(other.instanceClass)) {
    // return false;
    // }
    // return true;
    // }

}