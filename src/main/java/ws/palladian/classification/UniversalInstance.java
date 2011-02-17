package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;

public class UniversalInstance extends Instance {

    private List<Double> numericFeatures = new ArrayList<Double>();
    private List<String> nominalFeatures = new ArrayList<String>();
    private String textFeature = "";

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

}
