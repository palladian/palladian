package ws.palladian.helper.math;

import java.util.Set;

import ws.palladian.helper.collection.CountMatrix;

public class ConfusionMatrix {

    private final CountMatrix<String> confusionMatrix;

    public ConfusionMatrix() {
        this.confusionMatrix = CountMatrix.create();
    }

    public void add(String realCategory, String predictedCategory) {
        add(realCategory, predictedCategory, 1);
    }

    public void add(String realCategory, String predictedCategory, int count) {
        confusionMatrix.add(predictedCategory, realCategory, count);
    }

    public double getAccuracy() {
        return (double)getCorrectlyClassified() / getTotalDocuments();
    }

    public int getCorrectlyClassified() {
        int correct = 0;
        for (String value : getCategories()) {
            correct += confusionMatrix.getCount(value, value);
        }
        return correct;
    }

    /**
     * <p>
     * Get the number of correct classified documents in a given category.
     * </p>
     * 
     * @param category The category.
     * @return Number of correct classified documents in a given category.
     */
    public int getNumberOfCorrectClassifiedDocumentsInCategory(String category) {
        return confusionMatrix.getCount(category, category);
    }

    public int getClassifiedNumberOfCategory(String category) {
        return confusionMatrix.getColumnSum(category);
    }

    public int getRealNumberOfCategory(String category) {
        return confusionMatrix.getRowSum(category);
    }

    public int getNumberOfConfusionsBetween(String realCategory, String predictedCategory) {
        return confusionMatrix.getCount(predictedCategory, realCategory);
    }

    public Set<String> getCategories() {
        return confusionMatrix.getKeysX();
    }

    public int getTotalDocuments() {
        int total = 0;
        for (String value : confusionMatrix.getKeysX()) {
            total += confusionMatrix.getRowSum(value);
        }
        return total;
    }

    /**
     * <p>
     * Get the prior of the most likely category. In a dataset with evenly distributed classes the highest prior should
     * be 1/#classes.
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
     * Superiority is the factor with which the classifier is better than the highest prior in the data set: Superiority
     * = correctlyClassified / percentHighestPrior. A superiority of 1 means it doesn't make sense classifying at all
     * since we could simply always take the category with the highest prior. A superiority smaller 1 means the
     * classifier is harmful.
     * </p>
     * 
     * @return the superiority.
     */
    public double getSuperiority() {
        return getAccuracy() / getHighestPrior();
    }

    /**
     * <p>
     * Calculate and return the precision for a given category.
     * </p>
     * 
     * @param category
     * @return the precision for a given category
     */
    public double getPrecisionForCategory(String category) {
        int correct = getNumberOfCorrectClassifiedDocumentsInCategory(category);
        int classified = getClassifiedNumberOfCategory(category);
        if (classified == 0) {
            return -1;
        }
        return (double)correct / classified;
    }

    /**
     * <p>
     * Calculate and return the recall for a given category.
     * </p>
     * 
     * @param category
     * @return the recall for a given category
     */
    public double getRecallForCategory(String category) {
        int correct = getNumberOfCorrectClassifiedDocumentsInCategory(category);
        int real = getRealNumberOfCategory(category);
        if (real == 1.0) {
            return -1;
        }
        return (double)correct / real;
    }

    /**
     * <p>
     * Calculate and return the F for a given category.
     * </p>
     * 
     * @param category The category.
     * @param alpha A value between 0 and 1 to weight precision and recall (0.5 for F1).
     * @return F for a given category.
     */
    public double getFForCategory(String category, double alpha) {
        double precision = getPrecisionForCategory(category);
        double recall = getRecallForCategory(category);
        if (precision < 0 || recall < 0) {
            return -1;
        }
        return 1.0 / (alpha * 1.0 / precision + (1.0 - alpha) * 1.0 / recall);
    }

    /**
     * <p>
     * Calculate the sensitivity for a given category. Sensitivity = TP / (TP + FN). Sensitivity specifies what
     * percentage of actual category members were found. 100% sensitivity means that all actual documents belonging to
     * the category were classified correctly.
     * </p>
     * 
     * @param category The category.
     * @return
     */
    public double getSensitivityForCategory(String category) {
        int truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
        int realPositives = getRealNumberOfCategory(category);
        int falseNegatives = realPositives - truePositives;
        if (truePositives + falseNegatives == 0) {
            return -1.0;
        }
        return (double)truePositives / (truePositives + falseNegatives);
    }

    /**
     * <p>
     * Calculate the specificity for a given category. Specificity = (TN) / (TN + FP). Specificity specifies what
     * percentage of not-category members were recognized as such. 100% specificity means that there were no documents
     * classified as category member when they were actually not.
     * </p>
     * 
     * @param category The category.
     * @return The specificity.
     */
    public double getSpecificityForCategory(String category) {
        int truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
        int realPositives = getRealNumberOfCategory(category);
        int classifiedPositives = getClassifiedNumberOfCategory(category);

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
     * Calculate the accuracy for a given category. Accuracy = (TP + TN) / (TP + TN + FP + FN).
     * </p>
     * 
     * @param category The category.
     * @return The accuracy.
     */
    public double getAccuracyForCategory(String category) {
        int truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
        int realPositives = getRealNumberOfCategory(category);
        int classifiedPositives = getClassifiedNumberOfCategory(category);

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
     * in the training and test set and dividing it by the total number of documents.
     * </p>
     * 
     * @param category The category for which the prior should be determined.
     * @return The prior for the category.
     */
    public double getWeightForCategory(String category) {

        // the number of documents that belong to the given category
        int documentCount = getRealNumberOfCategory(category);

        // the total number of documents assigned to categories
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
     * @return The average precision of all categories.
     */
    public double getAveragePrecision(boolean weighted) {
        double precision = 0.0;

        int count = 0;

        for (String category : getCategories()) {

            double pfc = getPrecisionForCategory(category);
            if (pfc < 0) {
                continue;
            }

            if (weighted) {
                precision += getWeightForCategory(category) * pfc;
            } else {
                precision += pfc;
            }
            ++count;
        }

        if (weighted) {
            return precision;
        }

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
     * @return The average recall of all categories.
     */
    public double getAverageRecall(boolean weighted) {
        double recall = 0.0;

        int count = 0;

        for (String category : getCategories()) {

            double rfc = getRecallForCategory(category);
            if (rfc < 0.0) {
                continue;
            }

            if (weighted) {
                recall += getWeightForCategory(category) * rfc;
            } else {
                recall += rfc;
            }
            ++count;
        }

        if (weighted) {
            return recall;
        }

        if (count == 0) {
            return -1.0;
        }

        return recall / count;
    }

    /**
     * <p>
     * Get the average F of all categories.
     * </p>
     * 
     * @param alpha To weight precision and recall (0.5 for F1).
     * @return The average F of all categories.
     */
    public double getAverageF(double alpha, boolean weighted) {
        double f = 0.0;

        int count = 0;
        for (String category : getCategories()) {

            double ffc = getFForCategory(category, alpha);

            if (ffc < 0.0) {
                continue;
            }

            if (weighted) {
                f += getWeightForCategory(category) * ffc;
            } else {
                f += ffc;
            }

            ++count;
        }

        if (weighted) {
            return f;
        }

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
     * @param weighted If true, the average sensitivity is weighted using the priors of the categories.
     * @return The (weighted) average sensitivity.
     */
    public double getAverageSensitivity(boolean weighted) {
        double sensitivity = 0.0;

        int count = 0;
        for (String category : getCategories()) {

            double sfc = getSensitivityForCategory(category);

            if (sfc < 0.0) {
                continue;
            }

            if (weighted) {
                sensitivity += getWeightForCategory(category) * sfc;
            } else {
                sensitivity += sfc;
            }

            ++count;
        }

        if (weighted) {
            return sensitivity;
        }

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
     * @param weighted If true, the average accuracy is weighted using the priors of the categories.
     * @return The (weighted) average accuracy.
     */
    public double getAverageSpecificity(boolean weighted) {
        double specificity = 0.0;

        int count = 0;
        for (String category : getCategories()) {

            double sfc = getSpecificityForCategory(category);

            if (sfc < 0) {
                return -1.0;
            }

            if (weighted) {
                specificity += getWeightForCategory(category) * sfc;
            } else {
                specificity += sfc;
            }

            ++count;
        }

        if (weighted) {
            return specificity;
        }

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
     * @param weighted If true, the average accuracy is weighted using the priors of the categories.
     * @return The (weighted) average accuracy.
     */
    public double getAverageAccuracy(boolean weighted) {
        double accuracy = 0.0;

        int count = 0;
        for (String category : getCategories()) {

            double afc = getAccuracyForCategory(category);

            if (afc < 0.0) {
                return -1.0;
            }

            if (weighted) {
                accuracy += getWeightForCategory(category) * afc;
            } else {
                accuracy += afc;
            }

            ++count;
        }

        if (weighted) {
            return accuracy;
        }

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
                    builder.append(key).append("\t");
                }
                builder.append("<Precision>\n");

                headWritten = true;
            }

            builder.append(realCategory).append("\t");

            // iterate through all columns (x)
            for (String predictedCategory : getCategories()) {
                builder.append(getNumberOfConfusionsBetween(realCategory, predictedCategory)).append("\t");
            }
            builder.append(MathHelper.round(getPrecisionForCategory(realCategory), 4)).append("\t");
            builder.append("\n");
        }

        builder.append("<Recall>\t");
        for (String predictedCategory : getCategories()) {
            builder.append(MathHelper.round(getRecallForCategory(predictedCategory), 4)).append("\t");
        }

        builder.append("\n\n\n");

        builder.append("Average Precision:\t").append(MathHelper.round(getAveragePrecision(true), 4)).append("\n");
        builder.append("Average Recall:\t").append(MathHelper.round(getAverageRecall(true), 4)).append("\n");
        builder.append("Average F1:\t").append(MathHelper.round(getAverageF(0.5, true), 4)).append("\n");
        builder.append("Average Sensitivity:\t").append(MathHelper.round(getAverageSensitivity(true), 4)).append("\n");
        builder.append("Average Specificity:\t").append(MathHelper.round(getAverageSpecificity(true), 4)).append("\n");
        builder.append("Average Accuracy:\t").append(MathHelper.round(getAverageAccuracy(true), 4)).append("\n");
        builder.append("Highest Prior:\t").append(MathHelper.round(getHighestPrior(), 4)).append("\n");
        builder.append("Superiority:\t").append(MathHelper.round(getSuperiority(), 4)).append("\n");
        builder.append("# Documents:\t").append(getTotalDocuments()).append("\n");
        builder.append("# Correctly Classified:\t").append(getCorrectlyClassified()).append("\n");
        return builder.toString();

    }

}
