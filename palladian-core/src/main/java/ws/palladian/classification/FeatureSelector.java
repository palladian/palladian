/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class FeatureSelector {

    /**
     * <p>
     * This is a static utility class. The constructor should never be called.
     * </p>
     */
    private FeatureSelector() {
        throw new UnsupportedOperationException("Unable to instantiate static utility class");
    }

    // chi square test funktioniert nur für nominelle merkmale
    // pca funktioniert nur für numerische merkmale
    public static <T> Map<T, Double> calculateChiSquareValues(ListFeatureDescriptor<T> descriptor,
            Collection<NominalInstance> instances) {
        WordCorrelationMatrix termClassCorrelationMatrix = new WordCorrelationMatrix();

        for (Instance2<String> instance : instances) {
            T value = instance.featureVector.get(descriptor).getValue();
            termClassCorrelationMatrix.updatePair(value.toString(), instance.target);
        }
    }

    public static Map<String, Double> calculateChiSquareValues(String featurePath, Class<?> featureType,
            Collection<NominalInstance> instances) {
        Map<String, Map<String, Integer>> termClassCorrelationMatrix = new HashMap<String, Map<String, Integer>>();
        Map<String, Map<String, Integer>> classTermCorrelationMatrix = new HashMap<String, Map<String, Integer>>();
        Map<String, Double> ret = new HashMap<String, Double>();

        for (NominalInstance instance : instances) {
            Collection features = instance.featureVector.getFeatures(featureType, featurePath);
            for(Object value:features) {
            addCooccurence(value.toString(), instance.targetClass, termClassCorrelationMatrix);
            addCooccurence(instance.targetClass, value.toString(), classTermCorrelationMatrix);
            }
        }

        // The following variables are uppercase because that is the way they are used in the literature.
        for (Map.Entry<String, Map<String, Integer>> termOccurence : termClassCorrelationMatrix.entrySet()) {
            int N = instances.size();
            for (Map.Entry<String, Integer> classOccurence : termOccurence.getValue().entrySet()) {
                int N_11 = classOccurence.getValue();
                int N_10 = sumOfRowExceptOne(termOccurence.getKey(), classOccurence.getKey(),
                        termClassCorrelationMatrix);
                int N_01 = sumOfRowExceptOne(classOccurence.getKey(), termOccurence.getKey(),
                        classTermCorrelationMatrix);
                int N_00 = N - (N_10 + N_01);

                double chiSquare = Double.valueOf(N_11 + N_10 + N_01 + N_00) * Math.pow(N_11 * N_00 - N_10 * N_01, 2)
                        / ((N_11 + N_01) * (N_11 + N_10) * (N_10 + N_00) * (N_01 + N_00));
                ret.put(termOccurence.getKey(), chiSquare);
            }
        }

        return ret;
    }

    /**
     * <p>
     * Sums up a row of a matrix leaving one column out. This is required to calculate N01 and N10 for Chi² test. N01 is
     * the amount of documents of a class without a certain term, while N10 is the amount of documents with a certain
     * term but not of the specified class.
     * </p>
     * 
     * @param rowValue The value of the row to create the sum for.
     * @param exception The column to leave out of the summation.
     * @param correlationMatrix A matrix where cells are the counts of
     *            how often a the row value and the column value occur together.
     * @return The sum of the class occurrence without the term.
     */
    private static int sumOfRowExceptOne(String rowValue, String exception,
            Map<String, Map<String, Integer>> correlationMatrix) {
        Map<String, Integer> occurencesOfClass = correlationMatrix.get(rowValue);
        // add up all occurences of the current class
        int ret = 0;
        for (Map.Entry<String, Integer> occurence : occurencesOfClass.entrySet()) {
            if (!occurence.getKey().equals(exception)) {
                ret += occurence.getValue();
            }
        }
        return ret;
    }

    private static void addCooccurence(String row, String column, Map<String, Map<String, Integer>> correlationMatrix) {
        Map<String, Integer> correlations = correlationMatrix.get(row);
        if (correlations == null) {
            correlations = new HashMap<String, Integer>();
        }
        Integer occurenceCount = correlations.get(column);
        if (occurenceCount == null) {
            occurenceCount = 0;
        }
        occurenceCount++;
        correlations.put(column, occurenceCount);
        correlationMatrix.put(row, correlations);

    }
    
    class SetOfTerms 

}
