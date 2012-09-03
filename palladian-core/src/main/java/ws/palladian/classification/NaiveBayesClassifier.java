package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.Tensor;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A simple implementation of the Bayes Classifier. This classifier supports nominal and numeric input. The output is
 * nominal. More information about Naive Bayes can be found <a
 * href="http://www.pierlucalanzi.net/wp-content/teaching/dmtm/DMTM0809-13-ClassificationIBLNaiveBayes.pdf">here</a>.
 * </p>
 * 
 * @author David Urbansky
 */
public class NaiveBayesClassifier implements Predictor<NaiveBayesModel> {
    
    /**
     * Build the bayesProbabilityMap for nominal and numeric features.
     */
    @Override
    public NaiveBayesModel learn(List<NominalInstance> instances) {

        Tensor bayesProbabilityTensor = new Tensor();
        
        List<Double[]> meansAndStandardDeviations = new ArrayList<Double[]>();
//        Categories categories = new Categories();
        Bag<String> categories = new HashBag<String>();

        // this is the index of the first numeric attribute, we need this to
        // distinguish between the calculation of
        // nominal probabilities and numeric density functions
        int firstNumericFeatureIndex = Integer.MAX_VALUE;

        // first we count how many times each feature value occurs with a class
        int c = 1;
        for (NominalInstance instance : instances) {

            int featureIndex = 0;
            String target = instance.target;
//            categories.add(new Category(target));
            categories.add(target);

            // add the counts of the values of the nominal features to the tensor
            List<Feature<String>> nominalFeatures = instance.featureVector.getAll(String.class);

            for (Feature<String> nominalFeatureValue : nominalFeatures) {

                Double currentCount = (Double) bayesProbabilityTensor.get(featureIndex, target,
                        nominalFeatureValue.getValue());
                if (currentCount == null) {
                    currentCount = 1.0;
                } else {
                    currentCount++;
                }
                bayesProbabilityTensor.set(featureIndex, target, nominalFeatureValue, currentCount);

                featureIndex++;
            }

            // add the counts of the values of the numeric features to the tensor
            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            firstNumericFeatureIndex = featureIndex;
            for (Feature<Double> numericFeatureValue : numericFeatures) {

                Double currentCount = (Double) bayesProbabilityTensor.get(featureIndex, target,
                        numericFeatureValue.getValue());
                if (currentCount == null) {
                    currentCount = 1.0;
                    bayesProbabilityTensor.set(featureIndex, target, numericFeatureValue, currentCount);
                } else {
                    currentCount++;
                }

                featureIndex++;
            }

            ProgressHelper.showProgress(c++, instances.size(), 1);
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
                        totalCount += ((NumericFeature)valueAxis.getKey()).getValue() * (Double)valueAxis.getValue();
                        totalValues++;
                    }

                    double mean = totalCount / (double)totalValues;

                    // calculate the standard deviation
                    double squaredSum = 0;
                    for (Entry<Object, Object> valueAxis : classAxis.getValue().entrySet()) {
                        squaredSum += Math.pow(((((NumericFeature)valueAxis.getKey()).getValue() * (Double)valueAxis.getValue()) - mean), 2);
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
        
//        categories.calculatePriors();
        
        return new NaiveBayesModel(bayesProbabilityTensor, meansAndStandardDeviations, categories);


//        Instances<UniversalInstance> trainingInstances = new Instances<UniversalInstance>();
//        for (NominalInstance instance : instances) {
//            UniversalInstance universalInstance = createUniversalInstnace(instance.featureVector, trainingInstances);
//            trainingInstances.add(universalInstance);
//            universalInstance.setInstanceCategory(instance.target);
//        }
    }

    @Override
    public CategoryEntries predict(FeatureVector vector, NaiveBayesModel model) {
//        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        
//        UniversalInstance universalInstance = createUniversalInstnace(vector, instances);

//        if (categories == null) {
//            // FIX this is a problem since training instances are transient and after loading the model it will crash
//            // here, training instances should be transient though
//            getPossibleCategories(getTrainingInstances());
//        }

//        int classType = getClassificationType();

        // calculate the probability for each class given the feature values

        // category-probability map
        Map<String, Double> probabilities = CollectionHelper.newHashMap();

        // fill map with prior probabilities
        Bag<String> categories = model.getCategories();
        for (String category : categories.uniqueSet()) {
            probabilities.put(category, (double) categories.getCount(category)/categories.size());
        }

        // // multiply the probabilities from the probability/density tensor

        // do this for nominal features
        List<Feature<String>> nominalFeatures = vector.getAll(String.class);

        int featureIndex = 0;
        for (Feature<String> nominalFeatureValue : nominalFeatures) {

            for (String category : categories.uniqueSet()) {
                Double prob = (Double) model.getBayesProbabilityTensor()
                        .get(featureIndex, category, nominalFeatureValue.getValue());

                // if there was nothing learned for the featureValue class combination, we set the probability to 0
                if (prob == null) {
                    // TODO La Place Smoothing
                    prob = 0.0000000000001;
                }
                probabilities.put(category, probabilities.get(category) * prob);

            }

            featureIndex++;
        }

        // do this for numeric features
        List<Feature<Double>> numericFeatures = vector.getAll(Double.class);

        for (Feature<Double> numericFeatureValue : numericFeatures) {

            for (String category : categories.uniqueSet()) {
                // get the index to the mean and standard deviation that is stored for the feature class combination
                int index = (Integer)model.getBayesProbabilityTensor().get(featureIndex, category, 0);

                Double[] entry = model.getMeansAndStandardDeviations().get(index);

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
                                -(Math.pow(numericFeatureValue.getValue() - mean, 2) / (2 * Math.pow(standardDeviation, 2))));

                // avoid zero probabilities -> XXX how can la place smoothing be applied here?
                if (densityFunctionValue > 0.0) {
                    probabilities.put(category, probabilities.get(category) * densityFunctionValue);
                }

            }

            featureIndex++;
        }

        // create category entries
        CategoryEntries assignedEntries = new CategoryEntries();
        for (String category : categories.uniqueSet()) {
            CategoryEntry categoryEntry = new CategoryEntry(assignedEntries, new Category(category), probabilities.get(category));
            assignedEntries.add(categoryEntry);
        }

//        instance.assignCategoryEntries(assignedEntries);

//        LOGGER.debug("classified document (classType " + classType + ") in " + sw.getElapsedTimeString() + " " + " ("
//                + instance.getAssignedCategoryEntriesByRelevance(classType) + ")");
        
        
//        return universalInstance.getAssignedCategoryEntries();
        
        return assignedEntries;
        
    }
}