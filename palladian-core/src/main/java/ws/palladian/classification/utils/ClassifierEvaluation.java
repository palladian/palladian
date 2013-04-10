package ws.palladian.classification.utils;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Model;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.ThresholdAnalyzer;
import ws.palladian.processing.Trainable;

public final class ClassifierEvaluation {

    private ClassifierEvaluation() {
        // no instances.
    }

    public static <M extends Model, T extends Trainable> ConfusionMatrix evaluate(Classifier<M> classifier, M model,
            Iterable<T> testData) {

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        for (T testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance, model);
            String classifiedCategory = classification.getMostLikelyCategory();
            String realCategory = testInstance.getTargetClass();
            confusionMatrix.add(realCategory, classifiedCategory);
        }

        return confusionMatrix;
    }

    public static <M extends Model, T extends Trainable> ThresholdAnalyzer thresholdAnalysis(Classifier<M> classifier,
            M model, Iterable<T> testData, String correctClass) {

        ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer(100);

        for (T testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance, model);
            double probability = classification.getProbability(correctClass);
            String realCategory = testInstance.getTargetClass();
            thresholdAnalyzer.add(realCategory.equals(correctClass), probability);
        }

        return thresholdAnalyzer;
    }

}