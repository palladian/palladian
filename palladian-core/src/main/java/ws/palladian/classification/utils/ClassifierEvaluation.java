package ws.palladian.classification.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.math.ThresholdAnalyzer;

/**
 * <p>
 * Various evaluation methods for classifiers.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class ClassifierEvaluation {

    private ClassifierEvaluation() {
        // no instances.
    }

    // XXX misleading; I thought that would test multiple models, but it acutally combines the models?
    // document, and also have a look at comment in ClassificationUtils.classifyWithMultipleModels
    public static <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier,
            Iterable<? extends Instance> testData, M... models) {

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        for (Instance testInstance : testData) {
            CategoryEntries classification = ClassificationUtils.classifyWithMultipleModels(classifier,
                    testInstance.getVector(), models);
            String classifiedCategory = classification.getMostLikelyCategory();
            String realCategory = testInstance.getCategory();
            confusionMatrix.add(realCategory, classifiedCategory);
        }

        return confusionMatrix;
    }

    /**
     * @deprecated use a single or multiple models as the last parameter ClassifierEvaluation#evaluate(Classifier<M>
     *             classifier, Iterable<T> testData, M... models)
     * */
    @Deprecated
    public static <M extends Model> ConfusionMatrix evaluate(Classifier<M> classifier, M model,
            Iterable<? extends Instance> testData) {

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        for (Instance testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance.getVector(), model);
            String classifiedCategory = classification.getMostLikelyCategory();
            String realCategory = testInstance.getCategory();
            confusionMatrix.add(realCategory, classifiedCategory);
        }

        return confusionMatrix;
    }

    /**
     * <p>
     * Evaluation with 50:50 split for the given {@link Learner}, {@link Classifier} combination.
     * </p>
     * 
     * @param learner The learner, not <code>null</code>.
     * @param classifier The classifier, not <code>null</code>.
     * @param instances The dataset, not <code>null</code> or empty.
     * @return The {@link ConfusionMatrix} with the evaluation results.
     */
    @SuppressWarnings("unchecked")
    public static <M extends Model> ConfusionMatrix evaluate(Learner<M> learner, Classifier<M> classifier,
            List<? extends Instance> instances) {
        Validate.notNull(learner, "learner must not be null");
        Validate.notNull(classifier, "classifier must not be null");
        Validate.notNull(instances, "instances must not be null");
        Validate.isTrue(instances.size() > 2, "instances must contain at least two elements");

        List<? extends Instance> train = instances.subList(0, instances.size() / 2);
        List<? extends Instance> test = instances.subList(instances.size() / 2, instances.size() - 1);
        M model = learner.train(train);
        return evaluate(classifier, test, model);
    }

    public static <M extends Model> ThresholdAnalyzer thresholdAnalysis(Classifier<M> classifier, M model,
            Iterable<? extends Instance> testData, String correctClass) {
        Validate.isTrue(model.getCategories().size() == 2, "binary model required");

        ThresholdAnalyzer thresholdAnalyzer = new ThresholdAnalyzer(100);

        for (Instance testInstance : testData) {
            CategoryEntries classification = classifier.classify(testInstance.getVector(), model);
            double probability = classification.getProbability(correctClass);
            String realCategory = testInstance.getCategory();
            thresholdAnalyzer.add(realCategory.equals(correctClass), probability);
        }

        return thresholdAnalyzer;
    }

    /**
     * <p>
     * Run the learning curve creation loop. Use an increasing amount of training instances to build the model, and then
     * evaluate the model using the training set and the testing set. This way, one can decide whether whether one has a
     * high bias or high variance problem exists. See the link below for more information in the underlying idea behind
     * learning curves. In contrast to the learning curves described by Ng, we use Pr/Rc/F1 instead of the squared error
     * for evaluation.
     * </p>
     * 
     * @param learner The {@link Learner}, not <code>null</code>.
     * @param classifier The {@link Classifier}, not <code>null</code>.
     * @param trainSet The training set with classified instances for building the {@link Model} with the
     *            {@link Learner}, and for evaluation, not <code>null</code>.
     * @param testSet The test set for evaluation, not <code>null</code>.
     * @param correctClass The name of the correct class, e.g. <code>true</code>, not <code>null</code>.
     * @param stepSize The increment for the size of the trainSet in each iteration, larger/equal one.
     * @see <a href="https://class.coursera.org/ml/lecture/64">Learning curves explained by Andrew Ng</a>
     */
    @SuppressWarnings("unchecked")
    public static <M extends Model> void createLearningCurves(Learner<M> learner, Classifier<M> classifier,
            Collection<? extends Instance> trainSet, Collection<? extends Instance> testSet, String correctClass,
            int stepSize) {
        Validate.notNull(learner, "learner must not be null");
        Validate.notNull(classifier, "classifier must not be null");
        Validate.notNull(trainSet, "trainSet must not be null");
        Validate.notNull(testSet, "testSet must not be null");
        Validate.isTrue(stepSize >= 1, "stepSize must be greater/equal one");
        Validate.notNull(correctClass, "correctClass must not be null");

        String outputFile = String.format("learningCurves_%s.csv", System.currentTimeMillis());
        ProgressReporter reporter = new ProgressMonitor();
        reporter.startTask("Creating learning curves", (int)Math.ceil((double)trainSet.size() / stepSize));

        List<Instance> trainList = new ArrayList<Instance>(trainSet);
        Collections.shuffle(trainList);

        FileHelper.appendFile(outputFile,
                "trainItems;trainPercent;trainPrecision;trainRecall;trainF1;testPrecision;testRecall;testF1;\n");

        for (int i = stepSize; i < trainSet.size(); i += stepSize) {
            List<Instance> currentTrainSet = trainList.subList(0, i);
            M model = learner.train(currentTrainSet);
            ConfusionMatrix trainResult = evaluate(classifier, currentTrainSet, model);
            ConfusionMatrix testResult = evaluate(classifier, testSet, model);
            StringBuilder resultLine = new StringBuilder();
            resultLine.append(i).append(';');
            resultLine.append(MathHelper.round((double)i * 100 / trainSet.size(), 2)).append(';');
            resultLine.append(trainResult.getPrecision(correctClass)).append(';');
            resultLine.append(trainResult.getRecall(correctClass)).append(';');
            resultLine.append(trainResult.getF(1, correctClass)).append(';');
            resultLine.append(testResult.getPrecision(correctClass)).append(';');
            resultLine.append(testResult.getRecall(correctClass)).append(';');
            resultLine.append(testResult.getF(1, correctClass)).append(';');
            // System.out.println(resultLine);
            resultLine.append('\n');
            FileHelper.appendFile(outputFile, resultLine);
            reporter.increment();
        }

    }

}
