package ws.palladian.classification.text.evaluation;

import java.util.Collection;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Model;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.ThresholdAnalyzer;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Classified;

public final class ClassifierEvaluation {

    // XXX integrate in ClassificationUtils?

    private ClassifierEvaluation() {
        // no instances.
    }

    public static <M extends Model, C extends Classifiable & Classified> ConfusionMatrix evaluate(
            Classifier<M> classifier, M model, Collection<C> testData) {

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        int c = 1;
        for (C testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance.getFeatureVector(), model);
            String classifiedCategory = classification.getMostLikelyCategoryEntry().getName();
            String realCategory = testInstance.getTargetClass();
            confusionMatrix.add(realCategory, classifiedCategory);

            ProgressHelper.showProgress(c++, testData.size(), 1);
        }

        return confusionMatrix;

    }

    public static <M extends Model, C extends Classifiable & Classified> ThresholdAnalyzer thresholdAnalysis(Classifier<M> classifier, M model,
            Iterable<C> testData, String correctClass) {

        ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer(100);

        for (C testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance.getFeatureVector(), model);
            CategoryEntry categoryEntry = classification.getCategoryEntry(correctClass);
            String realCategory = testInstance.getTargetClass();
            thresholdAnalyzer.add(realCategory.equals(correctClass), categoryEntry.getProbability());
        }

        return thresholdAnalyzer;

    }
    
//    public static <M extends Model> ThresholdAnalyzer thresholdAnalysis(Classifier<M> classifier, M model,
//            Iterable<? extends Classified> testData) {
//
//        ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer(100);
//
//        for (Classified testInstance : testData) {
//            CategoryEntries classification = classifier.classify(testInstance.getFeatureVector(), model);
//            CategoryEntry categoryEntry = classification.getMostLikelyCategoryEntry();
//            String realCategory = testInstance.getTargetClass();
//            thresholdAnalyzer.add(realCategory.equals(categoryEntry.getName()), categoryEntry.getProbability());
//        }
//
//        return thresholdAnalyzer;
//
//    }

}