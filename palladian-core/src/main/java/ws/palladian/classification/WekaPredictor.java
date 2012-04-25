package ws.palladian.classification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.NumericFeature;

public class WekaPredictor implements Predictor<String> {
    
    private final Classifier classifier;
    private FastVector featureVector;
    private Instances trainInstances;
    
    public WekaPredictor(Classifier classifier) {
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
                throw new IllegalStateException(e);
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
            } else {
                ret.addElement(new Attribute(feature.getName()));
            }
        }
        FastVector fvClassValue = new FastVector(2);
        fvClassValue.addElement("true");
        fvClassValue.addElement("false");
        ret.addElement(new Attribute("class",fvClassValue));
        return ret;
    }

    private FastVector getValues(String name, List<Instance2<String>> instances) {
        Set<String> nominalValues = new HashSet<String>();
        for (Instance2<String> instance : instances) {
            // FIXME
            Feature<?> feature2 = instance.featureVector.get(name);
            if (feature2 == null) {
                continue;
            }
            @SuppressWarnings("deprecation")
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
            throw new IllegalStateException(e);
        }
        return ret;
    }
    
    @Override
    public String toString() {
        return classifier.toString();
    }

}
