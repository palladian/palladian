package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.Tensor;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A simple implementation of the Bayes Classifier. This classifier supports nominal and numeric input. The output is
 * nominal.
 * </p>
 * 
 * <p>
 * More information about Naive Bayes can be found here:
 * http://www.pierlucalanzi.net/wp-content/teaching/dmtm/DMTM0809-13-ClassificationIBLNaiveBayes.pdf
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class NaiveBayesClassifier extends Classifier<UniversalInstance> implements Predictor<String> {

    /** The serialize version ID. */
    private static final long serialVersionUID = 6975099985734139052L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(NaiveBayesClassifier.class);

    /**
     * <p>
     * A table holding the learned probabilities for each feature and class:<br>
     * Integer (feature index) | class1 (value1,value2,...,valueN), class2, ... , classN
     * </p>
     * 
     * <pre>
     * 1 | 0.3 (...), 0.6, ... , 0.1
     * x = featureIndex
     * y = classValue
     * z = featureValue
     * </pre>
     * */
    private Tensor bayesProbabilityTensor;

    /**
     * <p>
     * The Bayes classifier is capable of classifying instances with numeric features too. For all learned numeric
     * features we need to store the mean and standard deviation. The probability tensor holds the index of this list in
     * the field for the numeric feature. We then need to lookup this list to find the mean in the first entry and the
     * standard deviation in the second entry of the array at the given index.
     * </p>
     */
    private List<Double[]> meansAndStandardDeviations = new ArrayList<Double[]>();

    /**
     * FIXME this needs to go into BaseClassifier
     * @param fv
     * @return
     */
    public final CategoryEntries classify(FeatureVector fv) {
        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        
        UniversalInstance universalInstance = createUniversalInstnace(fv, instances);
        
        classify(universalInstance);
        
        return universalInstance.getAssignedCategoryEntries();
    }

    private UniversalInstance createUniversalInstnace(FeatureVector fv, Instances<UniversalInstance> instances) {
        UniversalInstance universalInstance = new UniversalInstance(instances);
        
        //Collection<Feature<Double>> numericFeatures = fv.getNumericFeatures();
        Collection<Feature<Number>> numericFeatures = fv.getAll(Number.class);
        
        // add numeric features
        for (Feature<Number> numericFeature : numericFeatures) {
            universalInstance.getNumericFeatures().add(numericFeature.getValue().doubleValue());
        }
        
        //Collection<Feature<String>> nominalFeatures = fv.getNominalFeatures();
        Collection<Feature<String>> nominalFeatures = fv.getAll(String.class);
        
        // add nominal features
        for (Feature<String> nominalFeature : nominalFeatures) {
            universalInstance.getNominalFeatures().add(nominalFeature.getValue());
        }
        return universalInstance;
    }
    
    /**
     * Build the bayesProbabilityMap for nominal and numeric features.
     */
    public final void train() {

        bayesProbabilityTensor = new Tensor();

        // this is the index of the first numeric attribute, we need this to
        // distinguish between the calculation of
        // nominal probabilities and numeric density functions
        int firstNumericFeatureIndex = Integer.MAX_VALUE;

        // first we count how many times each feature value occurs with a class
        int c = 1;
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

            firstNumericFeatureIndex = featureIndex;
            for (Double numericFeatureValue : numericFeatures) {

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

            ProgressHelper.showProgress(c++, getTrainingInstances().size(), 1);
        }

        // now we can transform the counts to actual probabilities for nominal values or pointers to mean and standard
        // deviation for numeric features (can be used in density function later on)
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

                    // replace each count with the real probability for the
                    // featureValue - class combination: p(value|Class)
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        valueAxis.setValue((Double) valueAxis.getValue() / (double) total);
                    }

                } else {
                    // use density function calculation for numeric attributes,
                    // we need the sampleMean and the standardDeviation to calculate the density function f(x) =
                    // 1/(sqrt(2*PI)*sd)*e^-(x-mean)²/2sd²

                    // count total number of values for current feature - class combination
                    int totalCount = 0;
                    int totalValues = 0;
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        totalCount += (Double)valueAxis.getKey() * (Double)valueAxis.getValue();
                        totalValues++;
                    }

                    double mean = totalCount / (double)totalValues;

                    // calculate the standard deviation
                    double squaredSum = 0;
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        squaredSum += Math.pow((((Double)valueAxis.getKey() * (Double)valueAxis.getValue()) - mean), 2);
                    }

                    double standardDeviation = Math.sqrt(squaredSum / totalValues);

                    Double[] entry = new Double[2];
                    entry[0] = mean;
                    entry[1] = standardDeviation;

                    // get the index on which we enter the entry
                    int indexPointer = meansAndStandardDeviations.size();
                    meansAndStandardDeviations.add(entry);

                    // save the index in the probability tensor at position 0 by convention
                    bayesProbabilityTensor.set(featureAxis.getKey(), classAxis.getKey(), 0, indexPointer);

                    // replace each count with the pointer (index) of mean and standard deviation for the feature -
                    // class combination
                    // for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                    // // save the index in the probability tensor
                    // valueAxis.setValue(indexPointer);
                    // }

                }

            }

        }

    }

    // public Instances<UniversalInstance> getTrainingInstances() {
    // return trainingInstances;
    // }
    //
    // public void setTrainingInstances(Instances<UniversalInstance>
    // trainingInstances) {
    // this.trainingInstances = trainingInstances;
    // }
    //
    // public Instances<UniversalInstance> getTestInstances() {
    // return testInstances;
    // }
    //
    // public void setTestInstances(Instances<UniversalInstance> testInstances)
    // {
    // this.testInstances = testInstances;
    // }

    /**
     * Fill the vector space with known instances. The instances must be given
     * in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be real values and the class must be nominal. Each line
     * is one training instance.
     */
    public final void trainFromCSV(String trainingFilePath, String separator) {
        setTrainingInstances(createInstances(trainingFilePath, separator));
    }

    /**
     * Create instances from a file. The instances must be given in a CSV file
     * in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be nominal values and the class must be nominal. Each
     * line is one training instance.
     */
    public Instances<UniversalInstance> createInstances(String filePath, String separator) {
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

    public final void classify(UniversalInstance instance) {

        StopWatch sw = new StopWatch();

        if (categories == null) {
            // FIX this is a problem since training instances are transient and after loading the model it will crash
            // here, training instances should be transient though
            getPossibleCategories(getTrainingInstances());
        }

        int classType = getClassificationType();

        // calculate the probability for each class given the feature values

        // category-probability map
        Map<Category, Double> probabilities = new HashMap<Category, Double>();

        // fill map with prior probabilities
        Categories categories = getCategories();
        for (Category category : categories) {
            probabilities.put(category, category.getPrior());
        }

        // // multiply the probabilities from the probability/density tensor

        // do this for nominal features
        List<String> nominalFeatures = instance.getNominalFeatures();

        int featureIndex = 0;
        for (String nominalFeatureValue : nominalFeatures) {

            for (Category category : categories) {
                Double prob = (Double) bayesProbabilityTensor
                        .get(featureIndex, category.getName(), nominalFeatureValue);

                if (category.getName().equals("nr$") || category.getName().equals("at")) {
                    System.out.print("stop");
                }
                
                // if there was nothing learned for the featureValue class combination, we set the probability to 0
                if (prob == null) {
                    // TODO La Place Smoothing
                    prob = 0.0000000000001; // <-- better
                    // prob = 0.; <-- leads to similar results as in branch
                }
                double prior = probabilities.get(category);
                probabilities.put(category, prior * prob);

            }

            featureIndex++;
        }

        // do this for numeric features
        List<Double> numericFeatures = instance.getNumericFeatures();

        for (Double numericFeatureValue : numericFeatures) {

            for (Category category : categories) {
                // get the index to the mean and standard deviation that is stored for the feature class combination
                int index = (Integer)bayesProbabilityTensor.get(featureIndex, category.getName(), 0);

                Double[] entry = meansAndStandardDeviations.get(index);

                // ignore if there was nothing learned for the featureValue class combination
                if (entry == null) {
                    continue;
                }

                double mean = entry[0];
                double standardDeviation = entry[1] + 0.01;

                // calculate the probability using the density function
                double densityFunctionValue = 1
                        / (Math.sqrt(2 * Math.PI) * standardDeviation)
                        * Math.pow(Math.E,
                                -(Math.pow(numericFeatureValue - mean, 2) / (2 * Math.pow(standardDeviation, 2))));

                // avoid zero probabilities -> XXX how can la place smoothing be applied here?
                if (densityFunctionValue > 0.0) {
                    probabilities.put(category, probabilities.get(category) * densityFunctionValue);
                }

            }

            featureIndex++;
        }

        // create category entries
        CategoryEntries assignedEntries = new CategoryEntries();
        for (Category category : categories) {
            assignedEntries.add(new CategoryEntry(assignedEntries, category, probabilities.get(category)));
        }

        instance.assignCategoryEntries(assignedEntries);

        LOGGER.debug("classified document (classType " + classType + ") in " + sw.getElapsedTimeString() + " " + " ("
                + instance.getAssignedCategoryEntriesByRelevance(classType) + ")");
    }

    @Override
    public final void save(String classifierPath) {
        FileHelper.serialize(this, classifierPath + getName() + ".gz");
    }

    public static NaiveBayesClassifier load(String classifierPath) {
        LOGGER.info("deserialzing classifier from " + classifierPath);

        NaiveBayesClassifier classifier = (NaiveBayesClassifier) FileHelper.deserialize(classifierPath);

        return classifier;
    }

    public static void main(String[] args) {
        NaiveBayesClassifier bc = new NaiveBayesClassifier();
        bc.trainFromCSV("data/train.txt", " ");
        bc.train();

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
    }

    @Override
    public void learn(List<Instance2<String>> instances) {
        Instances<UniversalInstance> trainingInstances = new Instances<UniversalInstance>();
        for (Instance2<String> instance : instances) {
            UniversalInstance universalInstance = createUniversalInstnace(instance.featureVector, trainingInstances);
            trainingInstances.add(universalInstance);
            universalInstance.setInstanceCategory(instance.target);
        }
        addTrainingInstances(trainingInstances);
        train();
    }

    @Override
    public CategoryEntries predict(FeatureVector vector) {
        return classify(vector);
    }
}