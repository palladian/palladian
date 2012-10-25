package ws.palladian.classification.text.evaluation;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Model;
import ws.palladian.helper.collection.CountMap2D;
import ws.palladian.processing.Classified;

public class ClassifierEvaluation {

    public static <M extends Model> ClassifierEvaluationResult evaluate(Classifier<M> classifier, M model, Iterable<? extends Classified> testData) {

        CountMap2D<String> confusionMatrix = CountMap2D.create();

        for (Classified testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance.getFeatureVector(), model);
            String classifiedCategory = classification.getMostLikelyCategoryEntry().getName();
            String realCategory = testInstance.getTargetClass();
            confusionMatrix.increment(classifiedCategory, realCategory);
        }
        
        return new ClassifierEvaluationResult(confusionMatrix);

    }

}