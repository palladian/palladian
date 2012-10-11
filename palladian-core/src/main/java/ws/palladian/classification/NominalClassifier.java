package ws.palladian.classification;

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
    public final void train(List<UniversalInstance> instances) {

        setTrainingInstances(instances);

        cooccurrenceMatrix = new CountMap2D();

        StopWatch sw = new StopWatch();

        for (UniversalInstance instance : instances) {
            String className = instance.getInstanceCategory();
            List<String> nominalFeatures = instance.getNominalFeatures();

            for (String nominalFeature : nominalFeatures) {
                cooccurrenceMatrix.increment(className, nominalFeature);
            }

        }

        LOGGER.info("trained in " + sw.getElapsedTimeString());
    }

    public final CategoryEntries classify(FeatureVector fv) {
        
        UniversalInstance universalInstance = new UniversalInstance();
        
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
        Map<String, Double> scores = new HashMap<String, Double>();
        
        List<String> nominalFeatures = instance.getNominalFeatures();

        for (String nominalFeatureValue : nominalFeatures) {

            for (String category : categories.uniqueItems()) {
                
                int cooccurrences = cooccurrenceMatrix.getCount(category, nominalFeatureValue);
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
        for (String category : categories.uniqueItems()) {
            assignedEntries.add(new CategoryEntry(assignedEntries, category, scores.get(category)));
        }

        instance.assignCategoryEntries(assignedEntries);
    }
    
    @Override
    public void save(String classifierPath) {
        FileHelper.serialize(this, classifierPath);
    }
    
//    /**
//     * TODO: duplicate code with NaiveBayesClassifier
//     * <p>
//     * </p>
//     * 
//     * @param trainingFilePath
//     */
//    public final void trainFromCSV(String trainingFilePath, String separator) {
//        train(createInstances(trainingFilePath, separator));
//    }
//    
//    /**
//     * TODO: duplicate code with NaiveBayesClassifier
//     * Create instances from a file. The instances must be given in a CSV file
//     * in the following format:<br>
//     * feature1;..;featureN;NominalClass<br>
//     * All features must be nominal values and the class must be nominal. Each
//     * line is one training instance.
//     */
//    private List<UniversalInstance> createInstances(String filePath, String separator) {
//        List<String> trainingLines = FileHelper.readFileToArray(filePath);
//
//        List<UniversalInstance> instances = CollectionHelper.newArrayList();
//
//        for (String trainingLine : trainingLines) {
//            String[] parts = trainingLine.split(separator);
//
//            UniversalInstance instance = new UniversalInstance();
//            List<String> features = new ArrayList<String>();
//            for (int f = 0; f < parts.length - 1; f++) {
//                features.add(parts[f]);
//            }
//
//            instance.setNominalFeatures(features);
//
//            instance.setInstanceCategory(parts[parts.length - 1]);
//            instances.add(instance);
//        }
//
//        return instances;
//    }

    
}
