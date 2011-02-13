package tud.iir.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.Tensor;

public class BayesClassifier extends Classifier<UniversalInstance> {

    /** The serialize version ID. */
    private static final long serialVersionUID = 6975099985734139052L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(BayesClassifier.class);

    /**
     * A table holding the learned probabilities for each feature and class:<br>
     * Integer (feature index) | class1 (value1,value2,...,valueN), class2, ... , classN<br>
     * 1 | 0.3 (...), 0.6, ... , 0.1<br>
     * x = featureIndex<br>
     * y = classValue<br>
     * z = featureValue<br>
     * */
    private Tensor bayesProbabilityTensor;

    /**
     * Build the bayesProbabilityMap for nominal and numeric features.
     */
    public void train() {
        
        bayesProbabilityTensor = new Tensor();

        // this is the index of the first numeric attribute, we need this to distinguish between the calculation of
        // nominal probabilities and numeric density functions
        int firstNumericFeatureIndex = Integer.MAX_VALUE;

        // first we count how many times each feature value occurs with a class
        for (UniversalInstance instance : getTrainingInstances()) {
            
            int featureIndex = 0;
            Category classValue = instance.getInstanceCategory();

            // add the counts of the values of the nominal features to the tensor
            List<String> nominalFeatures = instance.getNominalFeatures();
            
            for (String nominalFeatureValue : nominalFeatures) {
                
                Double currentCount = (Double) bayesProbabilityTensor.get(featureIndex, classValue.getName(),
                        nominalFeatureValue);
                if (currentCount == null) {
                    currentCount = 1.0;
                } else {
                    currentCount++;
                }
                bayesProbabilityTensor.set(featureIndex, classValue.getName(), nominalFeatureValue, currentCount);
                
                featureIndex++;
            }
            
            // add the counts of the values of the numeric features to the tensor
            List<Double> numericFeatures = instance.getNumericFeatures();

            for (Double numericFeatureValue : numericFeatures) {
                firstNumericFeatureIndex = featureIndex;

                Double currentCount = (Double) bayesProbabilityTensor.get(featureIndex, classValue.getName(),
                        numericFeatureValue);
                if (currentCount == null) {
                    currentCount = 1.0;
                    bayesProbabilityTensor.set(featureIndex, classValue.getName(), numericFeatureValue, currentCount);
                } else {
                    currentCount++;
                }

                featureIndex++;
            }

        }

        // now we can transform the counts to actual probabilities for nominal values or density functions for numeric features
        for (Entry<Object, Map<Object, Map<Object, Object>>> featureAxis : bayesProbabilityTensor.getTensor()
                .entrySet()) {

            for (Entry<Object, Map<Object, Object>> classAxis : featureAxis.getValue().entrySet()) {

                if ((Integer) featureAxis.getKey() < firstNumericFeatureIndex) {
                    // use probability calculation for nominal attributes

                    // count total number of values for current feature - class combination
                    int total = 0;
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        total += (Double) valueAxis.getValue();
                    }

                    // replace each count with the real probability for the featureValue - class combination:
                    // p(value|Class)
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        valueAxis.setValue((Double) valueAxis.getValue() / (double) total);
                    }

                } else {
                    // use density function calculation for numeric attributes, we need the sampleMean and the
                    // standardDeviation to calculate the density function f(x) = 1/(sqrt(2*PI)*sd)*e^-(x-mean)²/2sd²

                    // count total number of values for current feature - class combination
                    int totalCount = 0;
                    int totalValues = 0;
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        totalCount += (Integer) valueAxis.getValue();
                        totalValues++;
                    }

                    double mean = totalValues / (double) totalCount;

                    // calculate the standard deviation
                    double squaredSum = 0;
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        squaredSum += Math.pow(((Double) valueAxis.getValue() - mean), 2);
                    }

                    double standardDeviation = squaredSum / (totalCount - 1);

                    // replace each count with the density function for the featureValue - class combination:
                    // f(x) = 1/(sqrt(2*PI)*sd)*e^-(x-mean)²/2sd²
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        double densityFunctionValue = 1
                                / (Math.sqrt(2 * Math.PI) * standardDeviation)
                                * Math.pow(Math.E, -(Math.pow((Double) valueAxis.getValue() - mean, 2) / (2 * Math.pow(
                                        standardDeviation, 2))));
                        valueAxis.setValue(densityFunctionValue);
                    }

                }

            }

        }
        
    }

    // public Instances<UniversalInstance> getTrainingInstances() {
    // return trainingInstances;
    // }
    //
    // public void setTrainingInstances(Instances<UniversalInstance> trainingInstances) {
    // this.trainingInstances = trainingInstances;
    // }
    //
    // public Instances<UniversalInstance> getTestInstances() {
    // return testInstances;
    // }
    //
    // public void setTestInstances(Instances<UniversalInstance> testInstances) {
    // this.testInstances = testInstances;
    // }

    /**
     * Fill the vector space with known instances. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be real values and the class must be nominal. Each line is one training instance.
     */
    public void trainFromCSV(String trainingFilePath) {
        setTrainingInstances(createInstances(trainingFilePath));
    }

    /**
     * Create instances from a file. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be nominal values and the class must be nominal. Each line is one training instance.
     */
    private Instances<UniversalInstance> createInstances(String filePath) {
        List<String> trainingLines = FileHelper.readFileToArray(filePath);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        UniversalInstance instance = null;
        List<String> features = null;

        for (String trainingLine : trainingLines) {
            String[] parts = trainingLine.split(";");

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

    public void classify(UniversalInstance instance) {

        // calculate the probability for each class given the feature values
        
        // category-probability map
        Map<Category, Double> probabilities = new HashMap<Category, Double>();
        
        // fill map with prior probabilities
        Categories categories = getCategories();
        for (Category category : categories) {
            probabilities.put(category, category.getPrior());
        }
        
        // multiply the probabilities from the probability/density tensor
        List<String> nominalFeatures = instance.getNominalFeatures();
        
        int featureIndex = 0;
        for (String nominalFeatureValue : nominalFeatures) {
            
            for (Category category : categories) {
                double prob = (Double) bayesProbabilityTensor
                        .get(featureIndex, category.getName(), nominalFeatureValue);
                probabilities.put(category, probabilities.get(category) * prob);
            }
            
            featureIndex++;
        }
        
        // create category entries
        
        CategoryEntries assignedEntries = new CategoryEntries();
        for (Category category : categories) {
            assignedEntries.add(new CategoryEntry(assignedEntries, category, probabilities.get(category)));
        }

        instance.assignCategoryEntries(assignedEntries);

    }

    @Override
    public void save(String classifierPath) {
        FileHelper.serialize(this, classifierPath + getName() + ".gz");
    }

    public static BayesClassifier load(String classifierPath) {
        LOGGER.info("deserialzing classifier from " + classifierPath);

        BayesClassifier classifier = (BayesClassifier) FileHelper.deserialize(classifierPath);

        return classifier;
    }

}