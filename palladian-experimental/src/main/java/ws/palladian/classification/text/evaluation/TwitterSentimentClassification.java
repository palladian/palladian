package ws.palladian.classification.text.evaluation;

import ws.palladian.classification.text.*;
import ws.palladian.classification.text.BayesScorer.Options;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;

public class TwitterSentimentClassification {

    public static void main(String[] args) throws IOException {
        File trainFile = new File("/Users/pk/Desktop/trainingandtestdata/training.1600000.processed.noemoticon.csv");
        File testFile = new File("/Users/pk/Desktop/trainingandtestdata/testdata.manual.2009.06.14.csv");
        Iterable<Instance> trainData = new TwitterSentimentDatasetIterator(trainFile);

        Iterable<Instance> testData = new TwitterSentimentDatasetIterator(testFile);

        // skip "neutral" class in test data
        testData = CollectionHelper.filter(testData, new Predicate<Instance>() {
            @Override
            public boolean test(Instance item) {
                return !item.getCategory().equals("2");
            }
        });

        // XXX for feature optimization, run with smaller subset
        // trainData = ReservoirSampler.sample(trainData, 10000);
        // Set<FeatureSetting> featureSettings = new FeatureSettingGenerator().chars(3, 10).words(1, 5).create();
        // FeatureSettingOptimizer.evaluateFeatureSettings(featureSettings, trainData, testData);

        FeatureSetting featureSetting = FeatureSettingBuilder.words(1, 2).create();
        // FeatureSetting featureSetting = FeatureSettingBuilder.chars(4, 10).create();
        PalladianTextClassifier textClassifier = new PalladianTextClassifier(featureSetting);
        DictionaryModel model = textClassifier.train(trainData);
        // FileHelper.serialize(model, "twitter_sentiment_model.ser.gz");

        ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(textClassifier, testData, model);
        FileHelper.writeToFile("defaultScorerConfusions.txt", confusionMatrix.toString());

        textClassifier = new PalladianTextClassifier(model.getFeatureSetting(), new BayesScorer());
        confusionMatrix = ClassifierEvaluation.evaluate(textClassifier, testData, model);
        FileHelper.writeToFile("bayesScorerConfusions.txt", confusionMatrix.toString());

        textClassifier = new PalladianTextClassifier(model.getFeatureSetting(), new BayesScorer(Options.LAPLACE));
        confusionMatrix = ClassifierEvaluation.evaluate(textClassifier, testData, model);
        FileHelper.writeToFile("bayesScorerConfusions_noFreqencies.txt", confusionMatrix.toString());

        // textClassifier = new PalladianTextClassifier(model.getFeatureSetting(), KLScorer.INSTANCE);
        // confusionMatrix = ClassifierEvaluation.evaluate(textClassifier, testData, model);
        // FileHelper.writeToFile("KLConfusions.txt", confusionMatrix.toString());
    }

}
