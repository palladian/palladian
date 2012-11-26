package ws.palladian.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureUtils;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Predictor implementation using Weka.
 * </p>
 * <p>
 * Use {@link #train(List)} to train a new classifier based on a list of example instances and
 * {@link #predict(FeatureVector)} to classify a {@link FeatureVector}.
 * </p>
 * 
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 3.0
 * @since 0.1.7
 */
public final class WekaPredictor implements ws.palladian.classification.Classifier<WekaModel> {

    private final weka.classifiers.Classifier classifier;
    private final List<String> normalFeaturePaths;
    private final List<String> sparseFeaturePaths;
    private final static Logger LOGGER = LoggerFactory.getLogger(WekaPredictor.class);

    /**
     * <p>
     * Create a new {@link WekaPredictor} with the specified Weka {@link Classifier} implementation.
     * </p>
     * 
     * @param classifier The classifier to use, not <code>null</code>.
     */
    public WekaPredictor(weka.classifiers.Classifier classifier, List<String> normalFeaturePaths,
            List<String> sparseFeaturePaths) {
        super();
        Validate.notNull(classifier, "classifier must not be null.");
        Validate.notNull(normalFeaturePaths);

        this.classifier = classifier;
        this.normalFeaturePaths = normalFeaturePaths;
        this.sparseFeaturePaths = sparseFeaturePaths;
    }

    @Override
    public WekaModel train(List<Instance> instances) {
        Validate.notEmpty(instances);
        FastVector schema = new FastVector(normalFeaturePaths.size() + sparseFeaturePaths.size());
        Instances data = new Instances("dataset", schema, instances.size());

        // Create schema for weka dataset.
        List<Map<Integer, Double>> wekaFeatureSets = new ArrayList<Map<Integer, Double>>(instances.size());
        Set<String> classes = new HashSet<String>();
        List<String> instanceClasses = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            Map<Integer, Double> wekaFeatureSet = new HashMap<Integer, Double>();
            for (String featurePath : normalFeaturePaths) {
                List<Feature<?>> featureList = FeatureUtils.getFeaturesAtPath(instance.getFeatureVector(), featurePath);
                Validate.isTrue(featureList.size() == 1);
                wekaFeatureSet.putAll(handleFeature(featureList.get(0), data, instances));
            }

            for (String sparseFeaturePath : sparseFeaturePaths) {
                List<Feature<?>> sparseFeatures = FeatureUtils.getFeaturesAtPath(instance.getFeatureVector(),
                        sparseFeaturePath);

                wekaFeatureSet.putAll(handleFeature(sparseFeatures, data));
            }
            wekaFeatureSets.add(wekaFeatureSet);
            classes.add(instance.getTargetClass());
            instanceClasses.add(instance.getTargetClass());
        }

        // add attribute for the classification target
        FastVector targetClassVector = new FastVector();
        for (String targetClass : classes) {
            targetClassVector.addElement(targetClass);
        }
        Attribute classAttribute = new Attribute("palladianWekaTargetClass", targetClassVector);
        data.insertAttributeAt(classAttribute, data.numAttributes());

        // Add instances to weka dataset.
        int classIndex = data.numAttributes();
        for (int i = 0; i < wekaFeatureSets.size(); i++) {
            Map<Integer, Double> wekaFeatureSet = wekaFeatureSets.get(i);
            String targetClass = instanceClasses.get(i);
            int[] indices = new int[wekaFeatureSet.size() + 1];
            double[] values = new double[wekaFeatureSet.size() + 1];
            int j = 0;
            for (Map.Entry<Integer, Double> featureValue : wekaFeatureSet.entrySet()) {
                indices[j] = featureValue.getKey();
                values[j] = featureValue.getValue();
                j++;
            }
            indices[j] = classIndex;
            values[j] = classAttribute.indexOfValue(targetClass);
            SparseInstance wekaInstance = new SparseInstance(1.0, values, indices, wekaFeatureSet.size());
            wekaInstance.setDataset(data);
            data.add(wekaInstance);
        }

        data.compactify();
        data.setClassIndex(classIndex - 1);
        try {
            classifier.buildClassifier(data);
        } catch (Exception e) {
            throw new IllegalStateException("An exception occurred while building the classifier: " + e.getMessage(), e);
        }
        return new WekaModel(classifier, data);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param sparseFeatures
     * @param data
     * @return
     */
    private Map<Integer, Double> handleFeature(List<Feature<?>> sparseFeatures, Instances data) {
        Map<Integer, Double> ret = new HashMap<Integer, Double>();
        for (Feature<?> sparseFeature : sparseFeatures) {
            Attribute featureAttribute = data.attribute(sparseFeature.getValue().toString());

            if (featureAttribute == null) {
                featureAttribute = new Attribute(sparseFeature.getValue().toString());
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(sparseFeature.getValue().toString());
            }

            ret.put(featureAttribute.index(), 1.0);
        }
        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param data
     */
    private Map<Integer, Double> handleFeature(Feature<?> feature, Instances data, List<Instance> instances) {
        Attribute featureAttribute = data.attribute(feature.getName());
        Double featureValue = null;
        if (feature instanceof NominalFeature) {
            if (featureAttribute == null) {
                FastVector possibleValues = getValues(feature.getName(), instances);
                featureAttribute = new Attribute(feature.getName(), possibleValues);
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(feature.getName());

            }

            featureValue = Integer.valueOf(featureAttribute.indexOfValue(feature.getValue().toString())).doubleValue();
        } else if (feature instanceof BooleanFeature) {
            if (featureAttribute == null) {
                FastVector booleanValues = new FastVector(2);
                booleanValues.addElement("true");
                booleanValues.addElement("false");
                featureAttribute = new Attribute(feature.getName(), booleanValues);
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(feature.getName());
            }

            featureValue = Integer.valueOf(featureAttribute.indexOfValue(feature.getValue().toString())).doubleValue();
        } else {
            if (featureAttribute == null) {
                featureAttribute = new Attribute(feature.getName());
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(feature.getName());
            }

            featureValue = Double.valueOf(feature.getValue().toString());
        }

        Map<Integer, Double> ret = new HashMap<Integer, Double>();
        ret.put(featureAttribute.index(), featureValue);
        return ret;
    }

    // get domain for nominal feature, i.e. possible values
    /**
     * <p>
     * Extracts all possible values for a {@link NominalFeature}. This is necessary for Weka to know the domain and thus
     * declare all valid values within its schema.
     * </p>
     * 
     * @param name The name of the {@link NominalFeature} to create the domain for.
     * @param instances The instances to use to create the domain.
     * @return A {@link FastVector} containing all values.
     */
    private FastVector getValues(String name, List<Instance> instances) {
        Set<String> nominalValues = new HashSet<String>();
        for (Instance instance : instances) {
            NominalFeature feature = instance.getFeatureVector().getFeature(NominalFeature.class, name);
            if (feature == null) {
                continue;
            }
            nominalValues.add(feature.getValue());
        }
        FastVector fvNominalValues = new FastVector(nominalValues.size());
        for (String nominalValue : nominalValues) {
            fvNominalValues.addElement(nominalValue);
        }
        return fvNominalValues;
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, WekaModel model) {
        CategoryEntries ret = new CategoryEntries();

        SortedMap<Integer, Double> indices = new TreeMap<Integer, Double>();
        Map<String, Attribute> schema = model.getSchema();
        for (String sparseFeaturePath : sparseFeaturePaths) {
            List<Feature<?>> sparseFeatures = FeatureUtils.getFeaturesAtPath(vector, sparseFeaturePath);
            for (Feature<?> sparseFeature : sparseFeatures) {
                String featureName = sparseFeature.getValue().toString();
                Attribute featureAttribute = schema.get(featureName);
                if (featureAttribute == null) {
                    LOGGER.info("Ignoring sparse feature " + featureName + " since it was not in training set.");
                    continue;
                }
                int indexOfSparseFeature = featureAttribute.index();
                indices.put(indexOfSparseFeature, 1.0);
            }
        }

        for (String featurePath : normalFeaturePaths) {
            List<Feature<?>> features = FeatureUtils.getFeaturesAtPath(vector, featurePath);
            Validate.isTrue(features.size() == 1);
            // int indexOfFeature = model.getSchema().get(features.get(0).getName());
            Feature<?> feature = features.get(0);
            Attribute attribute = schema.get(feature.getName());
            if (!(feature instanceof NumericFeature)) {
                // Attribute attribute = (Attribute)model.getSchema().elementAt(indexOfFeature);
                // int indexOfValue = attribute.indexOfValue(features.get(0).getValue().toString());
                indices.put(attribute.index(), Integer.valueOf(attribute.indexOfValue(feature.getValue().toString()))
                        .doubleValue());
            } else {
                indices.put(attribute.index(), Double.valueOf(feature.getValue().toString()));
            }
        }

        double[] valuesArray = new double[indices.size()];
        int[] indicesArray = new int[indices.size()];
        int index = 0;
        for (Map.Entry<Integer, Double> entry : indices.entrySet()) {
            valuesArray[index] = entry.getValue();
            indicesArray[index] = entry.getKey();
            index++;
        }
        SparseInstance instance = new SparseInstance(1.0, valuesArray, indicesArray, indices.size());
        instance.setDataset(model.getDataset());

        // weka.core.Instance instance = makeWekaInstance(featureVector, vector, null);
        // instance.setDataset(model.getSchema());
        try {
            double[] distribution = classifier.distributionForInstance(instance);
            for (int i = 0; i < distribution.length; i++) {
                String className = model.getDataset().classAttribute().value(i);
                ret.add(new CategoryEntry(className, distribution[i]));
            }
        } catch (Exception e) {
            throw new IllegalStateException("An exception occurred while predicting: " + e.getMessage(), e);
        }
        return ret;
    }

    @Override
    public String toString() {
        return classifier.toString();
    }

    @Override
    public WekaModel train(Dataset dataset) {
        // FIXME
        return null;
    }

}
