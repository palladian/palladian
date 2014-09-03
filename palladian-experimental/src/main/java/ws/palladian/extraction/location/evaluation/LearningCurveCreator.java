package ws.palladian.extraction.location.evaluation;

import java.util.List;

import quickdt.randomForest.RandomForestBuilder;
import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Instance;

/**
 * <p>
 * Create learning curves for location extraction dataset.
 * </p>
 * 
 * @author Philipp Katz
 */
final class LearningCurveCreator {

    public static void main(String[] args) {

        String trainPath = "/Users/pk/Desktop/Learning_Curves/fd_merged_reduced_train_all.csv";
        String testPath = "/Users/pk/Desktop/Learning_Curves/fd_merged_reduced_validation.csv";

        List<Instance> train = ClassificationUtils.readCsv(trainPath);
        List<Instance> validate = ClassificationUtils.readCsv(testPath);

        QuickDtLearner learner = new QuickDtLearner(new RandomForestBuilder().numTrees(10));
        QuickDtClassifier classifier = new QuickDtClassifier();

        ClassifierEvaluation.createLearningCurves(learner, classifier, train, validate, "true", 500);

    }

}
