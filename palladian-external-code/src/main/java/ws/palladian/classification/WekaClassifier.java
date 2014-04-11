//package ws.palladian.classification;
//
//import java.util.Map;
//import java.util.SortedMap;
//import java.util.TreeMap;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import weka.core.Attribute;
//import weka.core.SparseInstance;
//import ws.palladian.core.CategoryEntries;
//import ws.palladian.core.Classifier;
//import ws.palladian.processing.Classifiable;
//import ws.palladian.processing.features.Feature;
//import ws.palladian.processing.features.ListFeature;
//import ws.palladian.processing.features.NumericFeature;
//
///**
// * <p>
// * Classifier wrapper for Weka.
// * </p>
// * 
// * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
// * @author Philipp Katz
// * @author Klemens Muthmann
// * @version 3.1
// * @since 0.1.7
// */
//public final class WekaClassifier implements Classifier<WekaModel> {
//
//    /** The logger for this class. */
//    private static final Logger LOGGER = LoggerFactory.getLogger(WekaClassifier.class);
//
//    @Override
//    public CategoryEntries classify(Classifiable classifiable, WekaModel model) {
//        CategoryEntriesBuilder ret = new CategoryEntriesBuilder();
//
//        SortedMap<Integer, Double> indices = new TreeMap<Integer, Double>();
//        Map<String, Attribute> schema = model.getSchema();
//        for (Feature<?> feature : classifiable.getFeatureVector()) {
//            if (feature instanceof ListFeature) {
//                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
//                for (Feature<?> sparseFeature : listFeature.getValue()) {
//                    String featureName = listFeature.getName() + sparseFeature.getName();
//                    Attribute featureAttribute = schema.get(featureName);
//                    if (featureAttribute == null) {
//                        LOGGER.info("Ignoring sparse feature {} since it was not in training set.", featureName);
//                        continue;
//                    }
//                    int indexOfSparseFeature = featureAttribute.index();
//                    indices.put(indexOfSparseFeature, 1.0);
//                }
//            } else {
//                Attribute attribute = schema.get(feature.getName());
//                int attributeIndex = attribute.index();
//
//                if (!(feature instanceof NumericFeature)) {
//                    double value = Integer.valueOf(attribute.indexOfValue(feature.getValue().toString())).doubleValue();
//                    // consider feature as missing if value was not in the training set.
//                    if (!(value < .0)) {
//                        indices.put(attributeIndex, value);
//                    }
//                } else {
//                    indices.put(attributeIndex, Double.valueOf(feature.getValue().toString()));
//                }
//            }
//        }
//
//        double[] valuesArray = new double[indices.size()];
//        int[] indicesArray = new int[indices.size()];
//        int index = 0;
//        for (Map.Entry<Integer, Double> entry : indices.entrySet()) {
//            valuesArray[index] = entry.getValue();
//            indicesArray[index] = entry.getKey();
//            index++;
//        }
//        SparseInstance instance = new SparseInstance(1.0, valuesArray, indicesArray, indices.size());
//        instance.setDataset(model.getDataset());
//
//        try {
//            double[] distribution = model.getClassifier().distributionForInstance(instance);
//            for (int i = 0; i < distribution.length; i++) {
//                String className = model.getDataset().classAttribute().value(i);
//                ret.set(className, distribution[i]);
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException("An exception occurred during classification.", e);
//        }
//        return ret.create();
//    }
//
//}
