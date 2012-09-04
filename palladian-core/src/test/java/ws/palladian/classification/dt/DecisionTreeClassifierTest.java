package ws.palladian.classification.dt;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.NominalInstance;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class DecisionTreeClassifierTest {

    @Test
    public void testDecisionTreeClassifier() {

        // sample data taken from https://github.com/sanity/quickdt
        List<NominalInstance> instances = new ArrayList<NominalInstance>();

        NominalInstance instance = new NominalInstance();
        FeatureVector fv = new FeatureVector();
        fv.add(new NumericFeature("height", 55.));
        fv.add(new NumericFeature("weight", 168.));
        fv.add(new NominalFeature("gender", "male"));
        instance.featureVector = fv;
        instance.targetClass = "overweight";
        instances.add(instance);

        instance = new NominalInstance();
        fv = new FeatureVector();
        fv.add(new NumericFeature("height", 75.));
        fv.add(new NumericFeature("weight", 168.));
        fv.add(new NominalFeature("gender", "female"));
        instance.featureVector = fv;
        instance.targetClass = "healthy";
        instances.add(instance);

        instance = new NominalInstance();
        fv = new FeatureVector();
        fv.add(new NumericFeature("height", 74.));
        fv.add(new NumericFeature("weight", 143.));
        fv.add(new NominalFeature("gender", "male"));
        instance.featureVector = fv;
        instance.targetClass = "underweight";
        instances.add(instance);

        instance = new NominalInstance();
        fv = new FeatureVector();
        fv.add(new NumericFeature("height", 49.));
        fv.add(new NumericFeature("weight", 144.));
        fv.add(new NominalFeature("gender", "female"));
        instance.featureVector = fv;
        instance.targetClass = "underweight";
        instances.add(instance);

        instance = new NominalInstance();
        fv = new FeatureVector();
        fv.add(new NumericFeature("height", 83.));
        fv.add(new NumericFeature("weight", 223.));
        fv.add(new NominalFeature("gender", "male"));
        instance.featureVector = fv;
        instance.targetClass = "healthy";
        instances.add(instance);

        DecisionTreeClassifier classifier = new DecisionTreeClassifier();
        DecisionTreeModel model = classifier.learn(instances);

        FeatureVector featureVector2 = new FeatureVector();
        featureVector2.add(new NumericFeature("height", 62.));
        featureVector2.add(new NumericFeature("weight", 201.));
        featureVector2.add(new NominalFeature("gender", "female"));
        CategoryEntries prediction = classifier.predict(featureVector2, model);

        assertEquals(1., prediction.getMostLikelyCategoryEntry().getRelevance(), 0);
        assertEquals("underweight", prediction.getMostLikelyCategoryEntry().getCategory().getName());
    }

}
