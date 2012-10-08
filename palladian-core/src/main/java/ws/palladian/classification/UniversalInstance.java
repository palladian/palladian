package ws.palladian.classification;

import java.util.List;

import ws.palladian.classification.text.TextInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class UniversalInstance extends TextInstance {

    private String textFeature = "";

    public UniversalInstance(List<? extends UniversalInstance> instances) {
        setInstances(instances);
    }

    public List<Double> getNumericFeatures() {
        List<Double> result = CollectionHelper.newArrayList();
        List<NumericFeature> numericFeatures = featureVector.getAll(NumericFeature.class);
        for (NumericFeature numericFeature : numericFeatures) {
            result.add(numericFeature.getValue());
        }
        return result;
    }

    public void setNumericFeatures(List<Double> numericFeatures) {
        for (Double numericFeature : numericFeatures) {
            featureVector.add(new NumericFeature("num" + featureVector.size(), numericFeature));
        }
    }

    public List<String> getNominalFeatures() {
        List<String> result = CollectionHelper.newArrayList();
        List<NominalFeature> nominalFeatures = featureVector.getAll(NominalFeature.class);
        for (NominalFeature nominalFeature : nominalFeatures) {
            result.add(nominalFeature.getValue());
        }
        return result;
    }

    public void setNominalFeatures(List<String> nominalFeatures) {
        for (String nominalFeature : nominalFeatures) {
            featureVector.add(new NominalFeature("nom" + featureVector.size(), nominalFeature));
        }
    }

    public String getTextFeature() {
        return textFeature;
    }

    public void setTextFeature(String textFeature) {
        this.textFeature = textFeature;
    }
    
    public FeatureVector getFeatureVector() {
        return featureVector;
    }

}
