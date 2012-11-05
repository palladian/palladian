package ws.palladian.classification;


/**
 * The document representation.
 * 
 * @author David Urbansky
 */
@Deprecated
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

//    public void setNumericFeatures(List<Double> numericFeatures) {
//        for (Double numericFeature : numericFeatures) {
//            String name = "num" + getFeatureVector().size();
//            getFeatureVector().add(new NumericFeature(name.intern(), numericFeature));
//        }
//    }

//    public List<String> getNominalFeatures() {
//        List<String> result = CollectionHelper.newArrayList();
//        List<NominalFeature> nominalFeatures = featureVector.getAll(NominalFeature.class);
//        for (NominalFeature nominalFeature : nominalFeatures) {
//            result.add(nominalFeature.getValue());
//        }
//        return result;
//    }

//    public void setNominalFeatures(List<String> nominalFeatures) {
//        for (String nominalFeature : nominalFeatures) {
//            String name = "nom" + getFeatureVector().size();
//            getFeatureVector().add(new NominalFeature(name.intern(), nominalFeature));
//        }
//    }

    public String getTextFeature() {
        return textFeature;
    }

    public void setTextFeature(String textFeature) {
        this.textFeature = textFeature;
    }

}
