package ws.palladian.classification.text.evaluation;

import java.util.Set;

import ws.palladian.helper.collection.CountMap2D;

public class ClassifierEvaluationResult {

    private final CountMap2D<String> confusionMatrix;

    public ClassifierEvaluationResult() {
        this.confusionMatrix = CountMap2D.create();
    }

    ClassifierEvaluationResult(CountMap2D<String> confusionMatrix) {
        this.confusionMatrix = confusionMatrix;
    }

    public void add(String realCategory, String predictedCategory) {
        add(realCategory, predictedCategory, 1);
    }

    public void add(String realCategory, String predictedCategory, int count) {
        confusionMatrix.increment(predictedCategory, realCategory, count);
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
     * Get the number of correct classified documents in a given category.
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
        return confusionMatrix.getColumnValues();
    }

    public int getTotalDocuments() {
        int total = 0;
        for (String value : confusionMatrix.getColumnValues()) {
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
        int sum = 0;
        for (String value : confusionMatrix.getColumnValues()) {
            int categoryCount = confusionMatrix.getRowSum(value);
            max = Math.max(max, categoryCount);
            sum += categoryCount;
        }
        if (sum == 0) {
            return 0;
        }
        return (double)max / sum;
    }

    public double getSuperiority() {
        return getAccuracy() / getHighestPrior();
    }

    /**
     * calculate and return the precision for a given category
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
     * calculate and return the recall for a given category
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
     * Calculate and return the F for a given category.
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
     * Calculate the sensitivity for a given category. Sensitivity = TP / (TP + FN). Sensitivity specifies what
     * percentage of actual category members were
     * found. 100% sensitivity means that all actual documents belonging to the category were classified correctly.
     * 
     * @param c
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
     * Calculate the specificity for a given category. Specificity = (TN) / (TN + FP). Specificity specifies what
     * percentage of not-category members were
     * recognized as such. 100% specificity means that there were no documents classified as category member when they
     * were actually not.
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
     * Calculate the accuracy for a given category. Accuracy = (TP + TN) / (TP + TN + FP + FN).
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
     * Calculate the prior for the given category. The prior is determined by calculating the frequency of the category
     * in the training and test set and
     * dividing it by the total number of documents. XXX use only test documents to determine prior?
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
     * Get the average precision of all categories.
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
     * Get the average recall of all categories.
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
     * Get the average F of all categories.
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
     * Calculate the average sensitivity.
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
     * Calculate the average specificity.
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
     * Calculate the average accuracy.
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
        builder.append("ClassifierEvaluationResult [confusionMatrix=");
        builder.append(confusionMatrix);
        builder.append(", getAccuracy()=");
        builder.append(getAccuracy());
        builder.append(", getCategories()=");
        builder.append(getCategories());
        builder.append(", getTotalDocuments()=");
        builder.append(getTotalDocuments());
        builder.append(", getHighestPrior()=");
        builder.append(getHighestPrior());
        builder.append("]");
        return builder.toString();
    }

    public String toReadableString() {

        StringBuilder builder = new StringBuilder();
        builder.append("Precision: ").append(getAveragePrecision(true)).append("\n");
        builder.append("Recall: ").append(getAverageRecall(true)).append("\n");
        builder.append("F1: ").append(getAverageF(0.5, true)).append("\n");
        builder.append("Sensitivity: ").append(getAverageSensitivity(true)).append("\n");
        builder.append("Specificity: ").append(getAverageSpecificity(true)).append("\n");
        builder.append("Accuracy: ").append(getAverageAccuracy(true)).append("\n");
        builder.append("Correctly Classified: ").append(getCorrectlyClassified()).append("\n");
        builder.append("Superiority: ").append(getSuperiority()).append("\n");
        return builder.toString();

    }

}