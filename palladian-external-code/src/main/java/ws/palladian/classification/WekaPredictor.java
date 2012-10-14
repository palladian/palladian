package ws.palladian.classification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Predictor implementation using Weka.
 * </p>
 * <p>
 * Use {@link #train(List)} to train a new classifier based on a list of example instances and
 * {@link #predict(FeatureVector)} to classify a {@link FeatureVector}. The {@code FeatureVector} used for training
 * requires the class to be the last {@link Feature} in each instances {@code FeatureVector}.
 * </p>
 * 
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public final class WekaPredictor implements ws.palladian.classification.Classifier<WekaModel> {

    private final Classifier classifier;
    private FastVector featureVector;
    private weka.core.Instances trainInstances;

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
    public WekaModel train(List<Instance> instances) {
        if (instances.size() > 0) {
            featureVector = declareFeatureVector(instances);
            trainInstances = new weka.core.Instances("rel", featureVector, instances.size());
            // last is classindex
            trainInstances.setClassIndex(instances.get(0).getFeatureVector().size());

            for (Instance instance : instances) {
                weka.core.Instance wekaInstance = makeWekaInstance(featureVector, instance.getFeatureVector(),
                        instance.getTargetClass());
                trainInstances.add(wekaInstance);
            }
            try {
                classifier.buildClassifier(trainInstances);
            } catch (Exception e) {
                throw new IllegalStateException("An exception occurred while building the classifier: "
                        + e.getMessage(), e);
            }
        }
        return new WekaModel(classifier);
    }

    private weka.core.Instance makeWekaInstance(FastVector featureVector, FeatureVector fv, String target) {
        weka.core.Instance wekaInstance = new weka.core.Instance(fv.size() + 1);
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

    private FastVector declareFeatureVector(List<Instance> instances) {
        FeatureVector featureVector = instances.get(0).getFeatureVector();
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

    // get domain for nominal feature, i.e. possible values
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
        weka.core.Instance instance = makeWekaInstance(featureVector, vector, null);
        instance.setDataset(trainInstances);
        try {
            double[] distributionForInstance = classifier.distributionForInstance(instance);
            ret.add(new CategoryEntry("true", distributionForInstance[0]));
            ret.add(new CategoryEntry("false", distributionForInstance[1]));
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
