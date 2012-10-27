package ws.palladian.classification.text.evaluation;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Model;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Classified;

public final class ClassifierEvaluation {
    
    // XXX integrate in ClassificationUtils?
    
    private ClassifierEvaluation() {
        // no instances.
    }

    public static <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier, M model, Iterable<? extends Classified> testData) {

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        for (Classified testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance.getFeatureVector(), model);
            String classifiedCategory = classification.getMostLikelyCategoryEntry().getName();
            String realCategory = testInstance.getTargetClass();
            confusionMatrix.add(realCategory, classifiedCategory);
        }
        
        return confusionMatrix;

    }

}