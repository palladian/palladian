package ws.palladian.classification;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

public class NaiveBayesClassifierTest {

//    @Test
//    public void testBayesClassifierNominal() throws FileNotFoundException {
//
//        // create training instances from the "Play Dataset", see here on page 34
//        // http://www.pierlucalanzi.net/wp-content/teaching/dmtm/DMTM0809-13-ClassificationIBLNaiveBayes.pdf
//        // nominal features are: Outlook, Temp, Humidity, and Windy, nominal class is Play (true or false)
//        List<String> nominalFeatures = null;
//
//        NaiveBayesClassifier bc = new NaiveBayesClassifier();
//        bc.trainFromCSV(ResourceHelper.getResourcePath("/classifier/playData.txt"), ";");
//
//        // create an instance to classify
//        UniversalInstance newInstance = new UniversalInstance(null);
//        nominalFeatures = new ArrayList<String>();
//        nominalFeatures.add("Sunny");
//        nominalFeatures.add("Cool");
//        nominalFeatures.add("High");
//        nominalFeatures.add("True");
//        newInstance.setNominalFeatures(nominalFeatures);
//
//        bc.train();
//
//        // test saving and loading
//        bc.setName("testBayesClassifier");
//        bc.save("data/temp/");
//        NaiveBayesClassifier loadedBC = NaiveBayesClassifier.load("data/temp/testBayesClassifier.gz");
//
//        // classify
//        loadedBC.classify(newInstance);
//
//        assertEquals(0.795417348608838, newInstance.getMainCategoryEntry().getRelevance(), 0);
//        assertEquals("No", newInstance.getMainCategoryEntry().getCategory().getName());
//
//    }

//    @Test
//    public void testBayesClassifierNumeric() {
//
//        NaiveBayesClassifier bc = new NaiveBayesClassifier();
//
//        Instances<Object> instances = new Instances<Object>();
//
//        UniversalInstance instance = new UniversalInstance(instances);
//        ArrayList<Double> numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(3.0);
//        instance.setNumericFeatures(numericFeatures);
//        instance.setInstanceCategory("Case");
//        bc.addTrainingInstance(instance);
//
//        instance = new UniversalInstance(instances);
//        numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(6.0);
//        instance.setNumericFeatures(numericFeatures);
//        instance.setInstanceCategory("Case");
//        bc.addTrainingInstance(instance);
//
//        instance = new UniversalInstance(instances);
//        numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(20.0);
//        instance.setNumericFeatures(numericFeatures);
//        instance.setInstanceCategory("Case");
//        bc.addTrainingInstance(instance);
//
//        instance = new UniversalInstance(instances);
//        numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(18.0);
//        instance.setNumericFeatures(numericFeatures);
//        instance.setInstanceCategory("Phone");
//        bc.addTrainingInstance(instance);
//
//        instance = new UniversalInstance(instances);
//        numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(66.0);
//        instance.setNumericFeatures(numericFeatures);
//        instance.setInstanceCategory("Phone");
//        bc.addTrainingInstance(instance);
//
//        instance = new UniversalInstance(instances);
//        numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(290.0);
//        instance.setNumericFeatures(numericFeatures);
//        instance.setInstanceCategory("Phone");
//        bc.addTrainingInstance(instance);
//
//        bc.train();
//
//        // test saving and loading
//        bc.setName("testBayesClassifier");
//        bc.save("data/temp/");
//        NaiveBayesClassifier loadedBC = NaiveBayesClassifier.load("data/temp/testBayesClassifier.gz");
//
//        // create an instance to classify
//        UniversalInstance newInstance = new UniversalInstance(null);
//        numericFeatures = new ArrayList<Double>();
//        numericFeatures.add(16.0);
//        newInstance.setNumericFeatures(numericFeatures);
//
//        // classify
//        loadedBC.classify(newInstance);
//
//        // System.out.println(newInstance);
//
//        assertEquals(0.944, MathHelper.round(newInstance.getMainCategoryEntry().getRelevance(), 3), 0);
//        assertEquals("Case", newInstance.getMainCategoryEntry().getCategory().getName());
//    }
    
    
    /**
     * TODO duplicated code of {@link #testBayesClassifierNumeric()} with new API. Remove later.
     */
    @Test
    public void testBayesClassifierNumericNewApi() {
        
        NaiveBayesClassifier bc = new NaiveBayesClassifier();
        List<NominalInstance> instances = CollectionHelper.newArrayList();
        
        NominalInstance instance = new NominalInstance();
        FeatureVector featureVector = new FeatureVector();
        featureVector.add(new NumericFeature("f", 3.0));
        instance.featureVector = featureVector;
        instance.target = "Case";
        instances.add(instance);
        
        instance = new NominalInstance();
        featureVector = new FeatureVector();
        featureVector.add(new NumericFeature("f", 6.0));
        instance.featureVector = featureVector;
        instance.target = "Case";
        instances.add(instance);
        
        instance = new NominalInstance();
        featureVector = new FeatureVector();
        featureVector.add(new NumericFeature("f", 20.));
        instance.featureVector = featureVector;
        instance.target = "Case";
        instances.add(instance);
        
        instance = new NominalInstance();
        featureVector = new FeatureVector();
        featureVector.add(new NumericFeature("f", 18.));
        instance.featureVector = featureVector;
        instance.target = "Phone";
        instances.add(instance);
        
        instance = new NominalInstance();
        featureVector = new FeatureVector();
        featureVector.add(new NumericFeature("f", 66.));
        instance.featureVector = featureVector;
        instance.target = "Phone";
        instances.add(instance);
        
        instance = new NominalInstance();
        featureVector = new FeatureVector();
        featureVector.add(new NumericFeature("f", 290.));
        instance.featureVector = featureVector;
        instance.target = "Phone";
        instances.add(instance);
        
        NaiveBayesModel model = bc.learn(instances);
        
        // create an instance to classify
        NominalInstance classificationInstance = new NominalInstance();
        FeatureVector classificationFeatureVector = new FeatureVector();
        classificationFeatureVector.add(new NumericFeature("f", 16.));
        classificationInstance.featureVector = classificationFeatureVector;
        CategoryEntries prediction = bc.predict(classificationFeatureVector, model);
        
        assertEquals(0.944, MathHelper.round(prediction.getMostLikelyCategoryEntry().getRelevance(), 3), 0);
        assertEquals("Case", prediction.getMostLikelyCategoryEntry().getCategory().getName());
    }

}