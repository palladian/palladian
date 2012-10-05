package ws.palladian.classification;

import java.util.List;

import ws.palladian.classification.numeric.MinMaxNormalization;
import ws.palladian.classification.text.TextInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class UniversalInstance extends TextInstance {

    /** The serial versionID. */
    // private static final long serialVersionUID = 55203846826273834L;

    // private List<Double> numericFeatures = new ArrayList<Double>();
    // private List<String> nominalFeatures = new ArrayList<String>();

    private String textFeature = "";

    /** The class of the instance. This can be nominal or numeric. */
    // private Object instanceClass;

    /** Whether or not the class of the instance is nominal. */
    private boolean classNominal = false;

    public UniversalInstance(Instances instances) {
        setInstances(instances);
    }

    public List<Double> getNumericFeatures() {
        // return numericFeatures;
        List<Double> result = CollectionHelper.newArrayList();
        List<NumericFeature> numericFeatures = featureVector.getAll(NumericFeature.class);
        for (NumericFeature numericFeature : numericFeatures) {
            result.add(numericFeature.getValue());
        }
        return result;
    }

    public List<NumericFeature> getNumericFeatures2() {
        return featureVector.getAll(NumericFeature.class);
    }

    public void setNumericFeatures(List<Double> numericFeatures) {
        // this.numericFeatures = numericFeatures;
        for (Double numericFeature : numericFeatures) {
            featureVector.add(new NumericFeature("num" + featureVector.size(), numericFeature));
        }
    }

    public List<String> getNominalFeatures() {
        // return nominalFeatures;
        List<String> result = CollectionHelper.newArrayList();
        List<NominalFeature> nominalFeatures = featureVector.getAll(NominalFeature.class);
        for (NominalFeature nominalFeature : nominalFeatures) {
            result.add(nominalFeature.getValue());
        }
        return result;
    }

    public void setNominalFeatures(List<String> nominalFeatures) {
        // this.nominalFeatures = nominalFeatures;
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

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

}
