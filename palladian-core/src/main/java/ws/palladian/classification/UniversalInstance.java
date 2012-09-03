package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.numeric.MinMaxNormalization;

public class UniversalInstance extends Instance {

    /** The serial versionID. */
    private static final long serialVersionUID = 55203846826273834L;

    private List<Double> numericFeatures = new ArrayList<Double>();
    private List<String> nominalFeatures = new ArrayList<String>();
    private String textFeature = "";

    /** The class of the instance. This can be nominal or numeric. */
    private Object instanceClass;

    /** Whether or not the class of the instance is nominal. */
    private boolean classNominal = false;

    public <T> UniversalInstance(Instances<T> instances) {
        setInstances(instances);
    }

    public List<Double> getNumericFeatures() {
        return numericFeatures;
    }

    public void setNumericFeatures(List<Double> numericFeatures) {
        this.numericFeatures = numericFeatures;
    }

    public List<String> getNominalFeatures() {
        return nominalFeatures;
    }

    public void setNominalFeatures(List<String> nominalFeatures) {
        this.nominalFeatures = nominalFeatures;
    }

    public String getTextFeature() {
        return textFeature;
    }

    public void setTextFeature(String textFeature) {
        this.textFeature = textFeature;
    }

    public boolean isClassNominal() {
        return classNominal;
    }

    public void setClassNominal(boolean classNominal) {
        this.classNominal = classNominal;
    }

    public void normalize(MinMaxNormalization normalization) {

        List<Double> features = getNumericFeatures();

        for (int i = 0; i < features.size(); i++) {

            double featureValue = features.get(i);
            double normalizedValue = (featureValue - normalization.getMinValueMap().get(i))
                    / normalization.getNormalizationMap().get(i);

            features.set(i, normalizedValue);
        }

    }

    /**
     * <p>
     * Free memory
     * </p>
     */
    public void empty() {
        setInstanceCategory("");
        setNumericFeatures(null);
        setNominalFeatures(null);
        setTextFeature(null);
    }

}
