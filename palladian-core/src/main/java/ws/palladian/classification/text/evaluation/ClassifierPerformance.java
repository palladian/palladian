package ws.palladian.classification.text.evaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ws.palladian.classification.UniversalInstance;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * This class calculates scores for a given classifier such as precision, recall, and F1 on one given dataset.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class ClassifierPerformance implements Serializable {

    private static final long serialVersionUID = -7375053843995436850L;

    /**
     * A list of pairs of [correct,threshold] where correct is 0 or 1 and the threshold is the threshold of the document
     * that was classified. From this list we can later calculate threshold analysis charts.
     */
    private List<Double[]> correctThresholds = null;


//    /**
//     * <p>
//     * Calculate a confusion matrix.
//     * </p>
//     * <p>
//     * One such matrix could look like this:
//     * </p>
//     * 
//     * <pre>
//     * classified\actual    | mobile phone  | camera | tv set
//     * ----------------------------------------------------------
//     * mobile phone         | 10            | 11     | 3
//     * camera               | 4             | 20     | 5
//     * tv set               | 1             | 1      | 31
//     * </pre>
//     * 
//     * @return The confusion matrix.
//     */
//    public ConfusionMatrix getConfusionMatrix() {
//
//        // x = actual category, y = classified category
//        ConfusionMatrix confusionMatrix = new ConfusionMatrix();
//
//        for (String actualCategory : categories) {
//
//            for (String classifiedCategory : categories) {
//
//                int count = getNumberOfConfusionsBetween(actualCategory, classifiedCategory);
//
//                confusionMatrix.set(actualCategory, classifiedCategory, count);
//            }
//
//        }
//
//        return confusionMatrix;
//    }



    private List<Double[]> getCorrectThresholds() {

        if (correctThresholds == null) {

            correctThresholds = new ArrayList<Double[]>();
            
            // XXX

//            for (UniversalInstance document : testDocuments) {
//                // pair containing correct (0 or 1) and the threshold [0,1]
//                Double[] pair = new Double[2];
//
//                pair[0] = 0.0;
//                if (isCorrectClassified(document)) {
//                    pair[0] = 1.0;
//                }
//
//                pair[1] = document.getMainCategoryEntry().getProbability();
//
//                correctThresholds.add(pair);
//            }

        }

        return correctThresholds;
    }

    /**
     * <p>
     * Get a map holding the thresholds in .01 steps with the correctlyClassified value of all documents with a
     * threshold >= the threshold.
     * </p>
     * 
     * <pre>
     * threshold    | correctly classified % | number of documents >= threshold
     * -------------|------------------------|---------------------------------
     * 0.01         |                        |
     * ...          |                        |
     * 1.00         |                        |
     * </pre>
     * 
     * @return A map with 101 entries (0.00 - 1.00) of thresholds, the percentage ofdocuments that were classified
     *         correctly having this or a greater threshold and the number of documents greater or equal the threshold.
     */
    private Map<Double, Double[]> getThresholdAccumulativeMap() {

        TreeMap<Double, Double[]> map = new TreeMap<Double, Double[]>();

        List<Double[]> ct = getCorrectThresholds();

        for (double t = 0.00; t <= 1.00; t += 0.01) {
            int correctlyClassified = 0;
            int numberOverThreshold = 0;

            for (Double[] doubles : ct) {

                if (doubles[1] >= t) {
                    numberOverThreshold++;
                    if (doubles[0] > 0.0) {
                        correctlyClassified++;
                    }
                }

            }

            Double[] entry = new Double[2];
            entry[0] = correctlyClassified / (double)numberOverThreshold;
            entry[1] = (double)numberOverThreshold;
            map.put(t, entry);
        }

        return map;
    }

    /**
     * <p>
     * Get a map holding the thresholds in buckets 0-0.1,0.1-0.2... with the buckets correctlyClassified. We will be
     * able to see how correct classified items are when having a trust within the bucket's threshold.
     * </p>
     * 
     * <pre>
     * threshold bucket | correctly classified % | number of documents in the bucket
     * -----------------|------------------------|---------------------------------
     * 0.1-0.2          |                        |
     * ...              |                        |
     * 0.9-1.00         |                        |
     * </pre>
     * 
     * @return A map with threshold buckets, the correctly classified documents in the bucket and the total number of
     *         documents in the bucket.
     */
    private Map<Double, Double[]> getThresholdBucketMap() {

        TreeMap<Double, Double[]> map = new TreeMap<Double, Double[]>();

        List<Double[]> ct = getCorrectThresholds();

        for (double t = 0.00; t <= 0.90; t += 0.1) {
            int correctlyClassified = 0;
            int numberInBucket = 0;

            for (Double[] doubles : ct) {

                if (doubles[1] > t && doubles[1] <= t + 0.1) {
                    numberInBucket++;
                    if (doubles[0] > 0.0) {
                        correctlyClassified++;
                    }
                }

            }

            Double[] entry = new Double[2];
            entry[0] = correctlyClassified / (double)numberInBucket;
            entry[1] = (double)numberInBucket;
            map.put(t, entry);
        }

        return map;

    }



}
