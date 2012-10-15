package ws.palladian.classification;

import java.util.List;

import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * The document representation.
 * 
 * @author David Urbansky
 */
public class UniversalInstance extends Instance {

    private String textFeature = "";

    /**
     * The constructor.
     */
    public UniversalInstance(String targetClass) {
        super(targetClass);
    }

//    public List<Double> getNumericFeatures() {
//        List<Double> result = CollectionHelper.newArrayList();
//        List<NumericFeature> numericFeatures = featureVector.getAll(NumericFeature.class);
//        for (NumericFeature numericFeature : numericFeatures) {
//            result.add(numericFeature.getValue());
//        }
//        return result;
//    }

    public void setNumericFeatures(List<Double> numericFeatures) {
        for (Double numericFeature : numericFeatures) {
            getFeatureVector().add(new NumericFeature("num" + getFeatureVector().size(), numericFeature));
        }
    }

//    public List<String> getNominalFeatures() {
//        List<String> result = CollectionHelper.newArrayList();
//        List<NominalFeature> nominalFeatures = featureVector.getAll(NominalFeature.class);
//        for (NominalFeature nominalFeature : nominalFeatures) {
//            result.add(nominalFeature.getValue());
//        }
//        return result;
//    }

    public void setNominalFeatures(List<String> nominalFeatures) {
        for (String nominalFeature : nominalFeatures) {
            getFeatureVector().add(new NominalFeature("nom" + getFeatureVector().size(), nominalFeature));
        }
    }

    public String getTextFeature() {
        return textFeature;
    }

    public void setTextFeature(String textFeature) {
        this.textFeature = textFeature;
    }

}
