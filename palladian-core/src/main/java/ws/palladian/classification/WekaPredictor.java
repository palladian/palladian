package ws.palladian.classification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Predictor implementation using Weka.
 * </p>
 * 
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
 * @author Philipp Katz
 */
public final class WekaPredictor implements Predictor<String> {

    private final Classifier classifier;
    private FastVector featureVector;
    private Instances trainInstances;

    /**
     * <p>
     * Create a new {@link WekaPredictor} with the specified Weka {@link Classifier} implementation.
     * </p>
     * 
     * @param classifier The classifier to use, not <code>null</code>.
     */
    public WekaPredictor(Classifier classifier) {
        Validate.notNull(classifier, "classifier must not be null.");
        this.classifier = classifier;
    }

    @Override
    public void learn(List<Instance2<String>> instances) {
        if (instances.size() > 0) {
            featureVector = declareFeatureVector(instances);
            trainInstances = new Instances("rel", featureVector, instances.size());
            // last is classindex
            trainInstances.setClassIndex(instances.get(0).featureVector.size());

            for (Instance2<String> instance2 : instances) {
                Instance wekaInstance = makeWekaInstance(featureVector, instance2.featureVector, instance2.target);
                trainInstances.add(wekaInstance);
            }
            try {
                classifier.buildClassifier(trainInstances);
            } catch (Exception e) {
                throw new IllegalStateException("An exception occurred while building the classifier: "
                        + e.getMessage(), e);
            }
        }
    }

    private Instance makeWekaInstance(FastVector featureVector, FeatureVector fv, String target) {
        Instance wekaInstance = new Instance(fv.size() + 1);
        int i = 0;

        for (Feature<?> f : fv.toArray()) {
            if (f instanceof NumericFeature) {
                wekaInstance.setValue((Attribute)featureVector.elementAt(i), ((NumericFeature)f).getValue());
            } else {
                try {
                    wekaInstance.setValue((Attribute)featureVector.elementAt(i), ((NominalFeature)f).getValue());
                } catch (IllegalArgumentException e) {
                    wekaInstance.setMissing(i);
                }
            }
            i++;
        }
        if (target != null) {
            wekaInstance.setValue((Attribute)featureVector.elementAt(i), target);
        }
        return wekaInstance;
    }

    private FastVector declareFeatureVector(List<Instance2<String>> instances) {
        FeatureVector featureVector = instances.get(0).featureVector;
        FastVector ret = new FastVector(featureVector.size() + 1);
        for (Feature<?> feature : featureVector.toArray()) {
            if (feature instanceof NominalFeature) {
                // if it's a nominal feature, we must determine possible attributes (call this "domain").
                FastVector fvNominalValues = getValues(feature.getName(), instances);
                ret.addElement(new Attribute(feature.getName(), fvNominalValues));
            } else if (feature instanceof NumericFeature) {
                ret.addElement(new Attribute(feature.getName()));
            } else {
                // skip.
            }
        }
        FastVector fvClassValue = new FastVector(2);
        fvClassValue.addElement("true");
        fvClassValue.addElement("false");
        ret.addElement(new Attribute("class", fvClassValue));
        return ret;
    }

    private FastVector getValues(String name, List<Instance2<String>> instances) {
        Set<String> nominalValues = new HashSet<String>();
        for (Instance2<String> instance : instances) {
            @SuppressWarnings("deprecation")
            Feature<?> feature2 = instance.featureVector.get(name);
            if (feature2 == null) {
                continue;
            }
            NominalFeature feature = (NominalFeature)feature2;
            nominalValues.add(feature.getValue());
        }
        FastVector fvNominalValues = new FastVector(nominalValues.size());
        for (String nominalValue : nominalValues) {
            fvNominalValues.addElement(nominalValue);
        }
        return fvNominalValues;
    }

    @Override
    public CategoryEntries predict(FeatureVector vector) {
        CategoryEntries ret = new CategoryEntries();
        Instance instance = makeWekaInstance(featureVector, vector, null);
        instance.setDataset(trainInstances);
        try {
            double[] distributionForInstance = classifier.distributionForInstance(instance);
            ret.add(new CategoryEntry(ret, new Category("true"), distributionForInstance[0]));
            ret.add(new CategoryEntry(ret, new Category("false"), distributionForInstance[1]));
        } catch (Exception e) {
            throw new IllegalStateException("An exception occurred while predicting: " + e.getMessage(), e);
        }
        return ret;
    }

    @Override
    public String toString() {
        return classifier.toString();
    }

}
