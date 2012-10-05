package ws.palladian.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CountMap2D;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class NominalClassifier extends ClassifierOld<UniversalInstance> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(NominalClassifier.class);

    private static final long serialVersionUID = 4344586029342718983L;

    private CountMap2D cooccurrenceMatrix;

    /**
     * FIXME belongs to BaseClassifier
     * @param instances
     */
    public final void train(Instances<UniversalInstance> instances) {

        setTrainingInstances(instances);

        cooccurrenceMatrix = new CountMap2D();

        StopWatch sw = new StopWatch();

        for (UniversalInstance instance : instances) {
            String className = instance.getInstanceCategoryName();
            List<String> nominalFeatures = instance.getNominalFeatures();

            for (String nominalFeature : nominalFeatures) {
                cooccurrenceMatrix.increment(className, nominalFeature);
            }

        }

        LOGGER.info("trained in " + sw.getElapsedTimeString());
    }

    public final CategoryEntries classify(FeatureVector fv) {
        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        
        UniversalInstance universalInstance = new UniversalInstance(instances);
        
        List<NumericFeature> numericFeatures = fv.getAll(NumericFeature.class);
        
        // add numeric features
        for (Feature<Double> numericFeature : numericFeatures) {            
            universalInstance.getNumericFeatures().add(numericFeature.getValue());            
        }
        
        List<NominalFeature> nominalFeatures = fv.getAll(NominalFeature.class);
        
        // add nominal features
        for (Feature<String> nominalFeature : nominalFeatures) {            
            universalInstance.getNominalFeatures().add(nominalFeature.getValue());            
        }
        
        classify(universalInstance);
        
        return universalInstance.getAssignedCategoryEntries();
    }
    
    public final void classify(UniversalInstance instance) {

        if (categories == null) {
            getPossibleCategories(getTrainingInstances());
        }

        // category-probability map
        Map<Category, Double> scores = new HashMap<Category, Double>();
        
        List<String> nominalFeatures = instance.getNominalFeatures();

        for (String nominalFeatureValue : nominalFeatures) {

            for (Category category : categories) {
                
                int cooccurrences = cooccurrenceMatrix.getCount(category.getName(), nominalFeatureValue);
                int rowSum = cooccurrenceMatrix.getRowSum(nominalFeatureValue);
                
                double score = (double)cooccurrences / (double)rowSum;

                Double currentScore = scores.get(category);
                if (currentScore == null) {
                    currentScore = 0.0;
                }
                scores.put(category, currentScore + score);
            }

        }
        
        // create category entries
        CategoryEntries assignedEntries = new CategoryEntries();
        for (Category category : categories) {
            assignedEntries.add(new CategoryEntry(assignedEntries, category, scores.get(category)));
        }

        instance.assignCategoryEntries(assignedEntries);
    }
    
    @Override
    public void save(String classifierPath) {
        FileHelper.serialize(this, classifierPath);
    }
    
    /**
     * TODO: duplicate code with NaiveBayesClassifier
     * <p>
     * </p>
     * 
     * @param trainingFilePath
     */
    public final void trainFromCSV(String trainingFilePath, String separator) {
        train(createInstances(trainingFilePath, separator));
    }
    
    /**
     * TODO: duplicate code with NaiveBayesClassifier
     * Create instances from a file. The instances must be given in a CSV file
     * in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be nominal values and the class must be nominal. Each
     * line is one training instance.
     */
    private Instances<UniversalInstance> createInstances(String filePath, String separator) {
        List<String> trainingLines = FileHelper.readFileToArray(filePath);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        UniversalInstance instance = null;
        List<String> features = null;

        for (String trainingLine : trainingLines) {
            String[] parts = trainingLine.split(separator);

            instance = new UniversalInstance(instances);
            features = new ArrayList<String>();

            for (int f = 0; f < parts.length - 1; f++) {
                features.add(parts[f]);
            }

            instance.setNominalFeatures(features);

            instance.setInstanceCategory(parts[parts.length - 1]);
            instances.add(instance);
        }

        return instances;
    }

    public static void main(String[] args) {

        NominalClassifier bc = new NominalClassifier();
        bc.trainFromCSV("data/train.txt", " ");

        int correct = 0;
        Instances<UniversalInstance> testInstances = bc.createInstances("data/test.txt", " ");
        for (UniversalInstance universalInstance : testInstances) {
            bc.classify(universalInstance);
            if (universalInstance.getMainCategoryEntry().getCategory().getName()
                    .equalsIgnoreCase(universalInstance.getInstanceCategoryName())) {
                correct++;
            }
        }
        System.out.println(correct / (double)testInstances.size());

        System.exit(0);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();

        List<String> nominalFeatures = new ArrayList<String>();

        // create an instance to classify
        UniversalInstance newInstance = new UniversalInstance(instances);
        newInstance.setInstanceCategory("A");
        nominalFeatures.add("f1");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        
        newInstance = new UniversalInstance(instances);
        nominalFeatures = new ArrayList<String>();
        newInstance.setInstanceCategory("B");
        nominalFeatures.add("f1");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        instances.add(newInstance);

        newInstance = new UniversalInstance(instances);
        nominalFeatures = new ArrayList<String>();
        newInstance.setInstanceCategory("A");
        nominalFeatures.add("f2");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        instances.add(newInstance);
        instances.add(newInstance);

        newInstance = new UniversalInstance(instances);
        nominalFeatures = new ArrayList<String>();
        newInstance.setInstanceCategory("B");
        nominalFeatures.add("f2");
        newInstance.setNominalFeatures(nominalFeatures);
        instances.add(newInstance);
        instances.add(newInstance);
        instances.add(newInstance);
        instances.add(newInstance);

        bc.train(instances);

        newInstance = new UniversalInstance(instances);
        nominalFeatures = new ArrayList<String>();
        nominalFeatures.add("f2");
        newInstance.setNominalFeatures(nominalFeatures);

        bc.classify(newInstance);

        System.out.println(newInstance.getAssignedCategoryEntries());
    }
    
}
