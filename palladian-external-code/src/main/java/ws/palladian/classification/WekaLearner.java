package ws.palladian.classification;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * Learner wrapper for Weka classifiers.
 * </p>
 * 
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 3.1
 * @since 0.1.7
 */
public final class WekaLearner implements Learner<WekaModel> {

    /**
     * This is necessary, because there is a bug when using sparse features in Weka ARFF files, therefore we need to add
     * one dummy attribute first. Says Klemens. See also Weka documentation, section 9.3 'Sparse ARFF files'.
     */
    static final String DUMMY_CLASS = "wekadummyclass";

    /** The prediction target, where the classification result goes. */
    private static final String TARGET_CLASS_ATTRIBUTE = "palladianWekaTargetClass";

    /** The Weka classifier this classifier wraps. */
    private final weka.classifiers.Classifier classifier;

    /**
     * <p>
     * Create a new {@link WekaLearner} with the specified Weka {@link Classifier} implementation.
     * </p>
     * 
     * @param classifier The classifier to use, not <code>null</code>.
     */
    public WekaLearner(weka.classifiers.Classifier classifier) {
        Validate.notNull(classifier, "classifier must not be null.");
        this.classifier = classifier;
    }

    @Override
    public WekaModel train(Iterable<? extends Instance> instances) {
        Validate.notNull(instances, "instances must not be null");
        int numInstances = CollectionHelper.count(instances.iterator());
        FastVector schema = new FastVector();
        Instances data = new Instances("dataset", schema, numInstances);

        // Create schema for weka dataset.
        List<Map<Integer, Double>> wekaFeatureSets = CollectionHelper.newArrayList();
        Set<String> classes = CollectionHelper.newHashSet();
        List<String> instanceClasses = CollectionHelper.newArrayList();
        for (Instance instance : instances) {
            Map<Integer,Double>wekaFeatureSet=createWekaFeatureSet(instance.getVector(),data,instances);
            wekaFeatureSets.add(wekaFeatureSet);
            classes.add(instance.getCategory());
            instanceClasses.add(instance.getCategory());
        }

        // add attribute for the classification target
        FastVector targetClassVector = new FastVector();
        targetClassVector.addElement(DUMMY_CLASS);
        for (String targetClass : classes) {
            targetClassVector.addElement(targetClass);
        }
        Attribute classAttribute = new Attribute(TARGET_CLASS_ATTRIBUTE, targetClassVector);
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
            indices[j] = classIndex - 1;
            values[j] = classAttribute.indexOfValue(targetClass);
            SparseInstance wekaInstance = new SparseInstance(1.0, values, indices, wekaFeatureSet.size());
            wekaInstance.setDataset(data);
            data.add(wekaInstance);
        }

        data.compactify();
        Attribute palladianWekaTargetClass = data.attribute(TARGET_CLASS_ATTRIBUTE);
        data.setClassIndex(palladianWekaTargetClass.index());
        try {
            classifier.buildClassifier(data);
        } catch (Exception e) {
            throw new IllegalStateException("An exception occurred while building the classifier: " + e.getMessage(), e);
        }
        return new WekaModel(classifier, data);
    }

    private Map<Integer, Double> createWekaFeatureSet(FeatureVector vector, Instances data,
            Iterable<? extends Instance> instances) {
        Map<Integer, Double> ret = CollectionHelper.newHashMap();
        for (VectorEntry<String, Value> entry : vector) {
            String effectiveFeatureName = entry.key();
            Value value = entry.value();
            if (value instanceof NominalValue) {
                Attribute featureAttribute = data.attribute(effectiveFeatureName);
                if (featureAttribute == null) {
                    FastVector possibleValues = getValues(effectiveFeatureName, instances);
                    featureAttribute = new Attribute(effectiveFeatureName, possibleValues);
                    data.insertAttributeAt(featureAttribute, data.numAttributes());
                    featureAttribute = data.attribute(effectiveFeatureName);
                }
                NominalValue nominalValue = (NominalValue)value;
                double featureValue = featureAttribute.indexOfValue(nominalValue.getString());
                ret.put(featureAttribute.index(), featureValue);
            } else if (value instanceof NumericValue) {
                Attribute featureAttribute = data.attribute(effectiveFeatureName);
                if (featureAttribute == null) {
                    featureAttribute = new Attribute(effectiveFeatureName);
                    data.insertAttributeAt(featureAttribute, data.numAttributes());
                    featureAttribute = data.attribute(effectiveFeatureName);
                }
                NumericValue numericValue = (NumericValue)value;
                ret.put(featureAttribute.index(), numericValue.getDouble());
            }
        }
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
     * @param trainables The instances to use to create the domain.
     * @return A {@link FastVector} containing all values.
     */
    private FastVector getValues(String name, Iterable<? extends Instance> instances) {
        Set<String> nominalValues = new HashSet<String>();
        for (Instance instance : instances) {
            Value value = instance.getVector().get(name);
            if (value == null || value == NullValue.NULL) {
                continue;
            }
            NominalValue nominalValue = (NominalValue)value;
            nominalValues.add(nominalValue.getString());
        }
        FastVector fvNominalValues = new FastVector(nominalValues.size());
        for (String nominalValue : nominalValues) {
            fvNominalValues.addElement(nominalValue);
        }
        return fvNominalValues;
    }

    @Override
    public String toString() {
        return classifier.toString();
    }

}
