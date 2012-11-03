package ws.palladian.helper.math;

import java.util.Set;

import ws.palladian.helper.collection.CountMatrix;

/**
 * <p>
 * A confusion matrix which can be used to evaluate classification results.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @see <a href="http://en.wikipedia.org/wiki/Confusion_matrix">Wikipedia: Confusion matrix</a>
 */
public class ConfusionMatrix {

    private final CountMatrix<String> confusionMatrix;

    /**
     * <p>
     * Create a new, empty confusion matrix.
     * </p>
     */
    public ConfusionMatrix() {
        this.confusionMatrix = CountMatrix.create();
    }

    /**
     * <p>
     * Add a classification result to this confusion matrix.
     * </p>
     * 
     * @param realCategory The real category of the item.
     * @param predictedCategory The category which was predicted by the classification.
     */
    public void add(String realCategory, String predictedCategory) {
        add(realCategory, predictedCategory, 1);
    }

    /**
     * <p>
     * Add a classification result to this confusion matrix.
     * </p>
     * 
     * @param realCategory The real category of the item.
     * @param predictedCategory The category which was predicted by the classification.
     * @param count The number of the classification.
     */
    public void add(String realCategory, String predictedCategory, int count) {
        confusionMatrix.add(predictedCategory, realCategory, count);
    }

    /**
     * <p>
     * Get the accuracy which is defined as <code>accuracy = |correctlyClassified| / |totalDocuments|</code>.
     * </p>
     * 
     * @return The accuracy.
     */
    public double getAccuracy() {
        return (double)getTotalCorrect() / getTotalDocuments();
    }

    /**
     * <p>
     * Get the number of correctly classified documents.
     * </p>
     * 
     * @return Number of correctly classified documents.
     */
    public int getTotalCorrect() {
        int correct = 0;
        for (String value : getCategories()) {
            correct += confusionMatrix.getCount(value, value);
        }
        return correct;
    }

    /**
     * <p>
     * Get the number of correctly classified documents in a given category.
     * </p>
     * 
     * @param category The category.
     * @return Number of correct classified documents in a given category.
     */
    public int getCorrectlyClassifiedDocuments(String category) {
        return confusionMatrix.getCount(category, category);
    }

    /**
     * <p>
     * Get the number of documents classified in a given category.
     * </p>
     * 
     * @param category The category.
     * @return Number of documents classified in the given category.
     */
    public int getClassifiedDocuments(String category) {
        return confusionMatrix.getColumnSum(category);
    }

    /**
     * <p>
     * Get the number of documents which are actually in the given category.
     * </p>
     * 
     * @param category The category.
     * @return Number of documents in the given category.
     */
    public int getRealDocuments(String category) {
        return confusionMatrix.getRowSum(category);
    }

    /**
     * <p>
     * Get the number of confusions between two categories.
     * </p>
     * 
     * @param realCategory The real category.
     * @param predictedCategory The category which was predicted by the classifier.
     * @return The number of confusions between the real and the predicted category.
     */
    public int getConfusions(String realCategory, String predictedCategory) {
        return confusionMatrix.getCount(predictedCategory, realCategory);
    }

    /**
     * <p>
     * Get all categories in the data set.
     * </p>
     * 
     * @return The categories in the data set.
     */
    public Set<String> getCategories() {
        return confusionMatrix.getKeysX();
    }

    /**
     * <p>
     * Get the total number of documents in this confusion matrix.
     * </p>
     * 
     * @return The number of documents in the matrix.
     */
    public int getTotalDocuments() {
        int total = 0;
        for (String value : confusionMatrix.getKeysX()) {
            total += confusionMatrix.getRowSum(value);
        }
        return total;
    }

    /**
     * <p>
     * Get the prior of the most likely category. In a data set with evenly distributed classes the highest prior should
     * be <code>1/|categories|</code>.
     * </p>
     * 
     * @return The highest prior.
     */
    public double getHighestPrior() {
        int max = 0;
        for (String value : confusionMatrix.getKeysX()) {
            max = Math.max(max, confusionMatrix.getRowSum(value));
        }
        int sum = getTotalDocuments();
        if (sum == 0) {
            return 0;
        }
        return (double)max / sum;
    }

    /**
     * <p>
     * Get the superiority for the classification result. Superiority is the factor with which the classifier is better
     * than the highest prior in the data set: <code>superiority
     * = percentCorrectlyClassified / percentHighestPrior</code>. A superiority of 1 means it doesn't make sense
     * classifying at all since we could simply always take the category with the highest prior. A superiority smaller 1
     * means the classifier is harmful.
     * </p>
     * 
     * @return The superiority.
     */
    public double getSuperiority() {
        return getAccuracy() / getHighestPrior();
    }

    /**
     * <p>
     * Get the precision for a given category. <code>precision = |TP| / (|TP| + |FP|)</code>.
     * </p>
     * 
     * @param category The category.
     * @return The precision for a given category.
     */
    public double getPrecision(String category) {
        int correct = getCorrectlyClassifiedDocuments(category);
        int classified = getClassifiedDocuments(category);
        if (classified == 0) {
            return -1;
        }
        return (double)correct / classified;
    }

    /**
     * <p>
     * Get the recall for a given category. <code>recall = |TP| / (|TP| + |FN|)</code>.
     * </p>
     * 
     * @param category The category.
     * @return The recall for a given category.
     */
    public double getRecall(String category) {
        int correct = getCorrectlyClassifiedDocuments(category);
        int real = getRealDocuments(category);
        if (real == 0) {
            return -1;
        }
        return (double)correct / real;
    }

    /**
     * <p>
     * Get the F measure for a given category.
     * </p>
     * 
     * @param category The category.
     * @param alpha A value between 0 and 1 to weight precision and recall (0.5 for F1).
     * @return The F measure for a given category.
     */
    public double getF(String category, double alpha) {
        double precision = getPrecision(category);
        double recall = getRecall(category);
        if (precision < 0 || recall < 0) {
            return -1;
        }
        return 1.0 / (alpha * 1.0 / precision + (1.0 - alpha) * 1.0 / recall);
    }

    /**
     * <p>
     * Calculate the sensitivity for a given category. <code>sensitivity = |TP| / (|TP| + |FN|)</code>. Sensitivity
     * specifies what percentage of actual category members were found. 100 % sensitivity means that all actual
     * documents belonging to the category were classified correctly.
     * </p>
     * 
     * @param category The category.
     * @return The sensitivity for the given category.
     */
    public double getSensitivity(String category) {
        int truePositives = getCorrectlyClassifiedDocuments(category);
        int realPositives = getRealDocuments(category);
        int falseNegatives = realPositives - truePositives;
        if (truePositives + falseNegatives == 0) {
            return -1.0;
        }
        return (double)truePositives / (truePositives + falseNegatives);
    }

    /**
     * <p>
     * Calculate the specificity for a given category. <code>specificity = |TN| / (|TN| + |FP|)</code>. Specificity
     * specifies what percentage of not-category members were recognized as such. 100 % specificity means that there
     * were no documents classified as category member when they were actually not.
     * </p>
     * 
     * @param category The category.
     * @return The specificity for the given category.
     */
    public double getSpecificity(String category) {
        int truePositives = getCorrectlyClassifiedDocuments(category);
        int realPositives = getRealDocuments(category);
        int classifiedPositives = getClassifiedDocuments(category);

        int falsePositives = classifiedPositives - truePositives;
        int falseNegatives = realPositives - truePositives;
        int trueNegatives = getTotalDocuments() - classifiedPositives - falseNegatives;

        if (trueNegatives + falsePositives == 0) {
            return -1.0;
        }

        return (double)trueNegatives / (trueNegatives + falsePositives);
    }

    /**
     * <p>
     * Calculate the accuracy for a given category. <code>accuracy = (|TP| + |TN|) / (|TP| + |TN| + |FP| + |FN|)</code>.
     * </p>
     * 
     * @param category The category.
     * @return The accuracy for the given category.
     */
    public double getAccuracy(String category) {
        int truePositives = getCorrectlyClassifiedDocuments(category);
        int realPositives = getRealDocuments(category);
        int classifiedPositives = getClassifiedDocuments(category);

        int falsePositives = classifiedPositives - truePositives;
        int falseNegatives = realPositives - truePositives;
        int trueNegatives = getTotalDocuments() - classifiedPositives - falseNegatives;

        if (truePositives + trueNegatives + falsePositives + falseNegatives == 0) {
            return -1.0;
        }

        return (double)(truePositives + trueNegatives)
                / (truePositives + trueNegatives + falsePositives + falseNegatives);
    }

    /**
     * <p>
     * Calculate the prior for the given category. The prior is determined by calculating the frequency of the category
     * in the data set and dividing it by the total number of documents.
     * </p>
     * 
     * @param category The category for which the prior should be determined.
     * @return The prior for the given category.
     */
    public double getPrior(String category) {
        int documentCount = getRealDocuments(category);
        int totalAssigned = getTotalDocuments();
        if (totalAssigned == 0) {
            return 0;
        }
        return (double)documentCount / totalAssigned;
    }

    /**
     * <p>
     * Get the average precision of all categories.
     * </p>
     * 
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *            each category equally.
     * @return The average precision of all categories.
     */
    public double getAveragePrecision(boolean weighted) {
        double precision = 0.0;
        for (String category : getCategories()) {
            double precisionForCategory = getPrecision(category);
            if (precisionForCategory < 0) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            precision += precisionForCategory * weight;
        }
        if (weighted) {
            return precision;
        }
        int count = getCategories().size();
        if (count == 0) {
            return -1.0;
        }
        return precision / count;
    }

    /**
     * <p>
     * Get the average recall of all categories.
     * </p>
     * 
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *            each category equally.
     * @return The average recall of all categories.
     */
    public double getAverageRecall(boolean weighted) {
        double recall = 0.0;
        for (String category : getCategories()) {
            double recallForCategory = getRecall(category);
            if (recallForCategory < 0.0) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            recall += recallForCategory * weight;
        }
        if (weighted) {
            return recall;
        }
        int count = getCategories().size();
        if (count == 0) {
            return -1.0;
        }
        return recall / count;
    }

    /**
     * <p>
     * Get the average F measure of all categories.
     * </p>
     * 
     * @param alpha To weight precision and recall (0.5 for F1 measure).
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *            each category equally.
     * @return The average F of all categories.
     */
    public double getAverageF(double alpha, boolean weighted) {
        double f = 0.0;
        for (String category : getCategories()) {
            double fForCategory = getF(category, alpha);
            if (fForCategory < 0.0) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            f += fForCategory * weight;
        }
        if (weighted) {
            return f;
        }
        int count = getCategories().size();
        if (count == 0) {
            return -1.0;
        }
        return f / count;
    }

    /**
     * <p>
     * Calculate the average sensitivity.
     * </p>
     * 
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *            each category equally.
     * @return The average sensitivity for all categories.
     */
    public double getAverageSensitivity(boolean weighted) {
        double sensitivity = 0.0;
        for (String category : getCategories()) {
            double sensitivityForCategory = getSensitivity(category);
            if (sensitivityForCategory < 0.0) {
                continue;
            }
            double weight = weighted ? getPrior(category) : 1;
            sensitivity += sensitivityForCategory * weight;
        }
        if (weighted) {
            return sensitivity;
        }
        int count = getCategories().size();
        if (count == 0) {
            return -1.0;
        }
        return sensitivity / count;
    }

    /**
     * <p>
     * Calculate the average specificity.
     * </p>
     * 
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *            each category equally.
     * @return The average accuracy for all categories.
     */
    public double getAverageSpecificity(boolean weighted) {
        double specificity = 0.0;
        for (String category : getCategories()) {
            double specifityForCategory = getSpecificity(category);
            if (specifityForCategory < 0) {
                return -1.0;
            }
            double weight = weighted ? getPrior(category) : 1;
            specificity += specifityForCategory * weight;
        }
        if (weighted) {
            return specificity;
        }
        int count = getCategories().size();
        if (count == 0) {
            return -1.0;
        }
        return specificity / count;
    }

    /**
     * <p>
     * Calculate the average accuracy.
     * </p>
     * 
     * @param weighted <code>true</code> to weight each category by its prior probability, <code>false</code> to weight
     *            each category equally.
     * @return The average accuracy for all categories.
     */
    public double getAverageAccuracy(boolean weighted) {
        double accuracy = 0.0;
        for (String category : getCategories()) {
            double accuracyForCategory = getAccuracy(category);
            if (accuracyForCategory < 0.0) {
                return -1.0;
            }
            double weight = weighted ? getPrior(category) : 1;
            accuracy += accuracyForCategory * weight;
        }
        if (weighted) {
            return accuracy;
        }
        int count = getCategories().size();
        if (count == 0) {
            return -1.0;
        }
        return accuracy / count;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        boolean headWritten = false;

        // iterate through all rows (y)
        for (String realCategory : getCategories()) {

            // write table head
            if (!headWritten) {
                builder.append("Real\\Predicted\t");

                for (String key : getCategories()) {
                    builder.append(key).append('\t');
                }
                builder.append('\n');
                // builder.append("<Precision>\n");

                headWritten = true;
            }

            builder.append(realCategory).append('\t');

            // iterate through all columns (x)
            for (String predictedCategory : getCategories()) {
                builder.append(getConfusions(realCategory, predictedCategory)).append('\t');
            }
//            builder.append(MathHelper.round(getPrecision(realCategory), 4)).append("\t");
            builder.append('\n');
        }

//        builder.append("<Recall>\t");
//        for (String predictedCategory : getCategories()) {
//            builder.append(MathHelper.round(getRecall(predictedCategory), 4)).append("\t");
//        }

        builder.append("\n\n\n");
        
        builder.append("Category\tPrior\tPrecision\tRecall\tF1\n");
        for (String category : getCategories()) {
            builder.append(category).append('\t');
            builder.append(MathHelper.round(getPrior(category), 4)).append('\t');
            builder.append(MathHelper.round(getPrecision(category), 4)).append('\t');
            builder.append(MathHelper.round(getRecall(category), 4)).append('\t');
            builder.append(MathHelper.round(getF(category, 0.5), 4)).append('\n');
        }
        
        builder.append("\n\n\n");
        builder.append("Average Precision:\t").append(MathHelper.round(getAveragePrecision(true), 4)).append('\n');
        builder.append("Average Recall:\t").append(MathHelper.round(getAverageRecall(true), 4)).append('\n');
        builder.append("Average F1:\t").append(MathHelper.round(getAverageF(0.5, true), 4)).append('\n');
        builder.append("Average Sensitivity:\t").append(MathHelper.round(getAverageSensitivity(true), 4)).append('\n');
        builder.append("Average Specificity:\t").append(MathHelper.round(getAverageSpecificity(true), 4)).append('\n');
        builder.append("Average Accuracy:\t").append(MathHelper.round(getAverageAccuracy(true), 4)).append('\n');
        builder.append("Highest Prior:\t").append(MathHelper.round(getHighestPrior(), 4)).append('\n');
        builder.append("Superiority:\t").append(MathHelper.round(getSuperiority(), 4)).append('\n');
        builder.append("# Documents:\t").append(getTotalDocuments()).append('\n');
        builder.append("# Correctly Classified:\t").append(getTotalCorrect()).append('\n');
        return builder.toString();

    }

}
