package ws.palladian.classification;

import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.meta.Bagging;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureVector;
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
            wekaInstance.setValue((Attribute)featureVector.elementAt(i), ((NumericFeature)f).getValue());
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
            ret.addElement(new Attribute(feature.getName()));
        }
        FastVector fvClassValue = new FastVector(2);
        fvClassValue.addElement("true");
        fvClassValue.addElement("false");
        ret.addElement(new Attribute("class",fvClassValue));
        return ret;
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

}
