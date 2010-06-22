package tud.iir.classification;

import java.util.Map;
import java.util.Map.Entry;

/**
 * An object holding features.
 * 
 * @author David Urbansky
 * 
 */
public class FeatureObject {

    private Double[] features;
    private String[] featureNames;

    private int classAssociation;

    /**
     * Create a feature object with a feature vector of doubles. The last index of the features must be 0 or 1 and refers to the class.
     * 
     * @param features
     * @param featureNames
     */
    public FeatureObject(Double[] features, String[] featureNames) {
        setFeatures(features);
        setFeatureNames(featureNames);
        setClassAssociation((int) Math.floor((features[features.length - 1])));
    }

    public FeatureObject(Map<String, Double> features, Double classAssociation) {
        this(features);
        setClassAssociation((int) Math.floor(classAssociation));
    }

    public FeatureObject(Map<String, Double> features) {
        String[] featureNames = new String[features.size()];
        Double[] featureValues = new Double[features.size()];

        int i = 0;
        for (Entry<String, Double> feature : features.entrySet()) {
            featureNames[i] = feature.getKey();
            featureValues[i] = feature.getValue();
        }

        setFeatureNames(featureNames);
        setFeatures(featureValues);
    }

    public Double[] getFeatures() {
        return features;
    }

    public void setFeatures(Double[] features) { // NOPMD by David on 14.06.10 01:02
        this.features = features;
    }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public void setFeatureNames(String[] featureNames) {
        if (featureNames.length < features.length) {
            this.featureNames = new String[features.length];
            for (int i = 0; i < features.length; i++) {
                this.featureNames[i] = "feature" + i;
            }
        } else {
            this.featureNames = featureNames;
        }
    }

    public int getClassAssociation() {
        return classAssociation;
    }

    public String getClassAssociationAsString() {
        if (getClassAssociation() == 1.0) {
            return "positive";
        }
        return "negative";
    }

    public void setClassAssociation(int classAssociation) {
        this.classAssociation = classAssociation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Double[] features = getFeatures();
        for (int i = 0; i < features.length; i++) {
            // sb.append(features[i]).append(" ; ");
            sb.append(featureNames[i] + ":" + features[i]).append("\n");
        }
        return sb.toString();
    }

}