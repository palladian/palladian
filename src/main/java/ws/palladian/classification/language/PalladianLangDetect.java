package ws.palladian.classification.language;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.page.ClassifierManager;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.ClassifierPerformance;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.EvaluationSetting;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.StopWatch;

/**
 * The best setting for medium to long texts is to use word n-grams with 1<=n<=3.
 * Evaluation results can be found in the Palladian book.
 * 
 * @author David Urbansky
 * 
 */
public class PalladianLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianLangDetect.class);

    private TextClassifier palladianClassifier;

    /** We can specify which classes are possible and discard all others for the classification task. */
    private Set<String> possibleClasses = null;

    public PalladianLangDetect(String modelPath) {
        palladianClassifier = DictionaryClassifier.load(modelPath);
    }

    public PalladianLangDetect() {
        palladianClassifier = DictionaryClassifier.load("data/models/palladianLanguageJRC/palladianLanguageJRC.ser");
    }

    public Set<String> getPossibleClasses() {
        return possibleClasses;
    }

    public void setPossibleClasses(Set<String> possibleClasses) {
        this.possibleClasses = possibleClasses;
    }

    public ClassifierPerformance test(Dataset dataset) {
        ClassifierManager cm = new ClassifierManager();
        ClassifierPerformance cp = cm.testClassifier(dataset, palladianClassifier);
        LOGGER.info("Average Accuracy: " + cp.getAverageAccuracy(false));
        return cp;
    }

    public void evaluateBestSetting() {
        ClassifierManager classifierManager = new ClassifierManager();

        // build a set of classification type settings to evaluate
        List<ClassificationTypeSetting> classificationTypeSettings = new ArrayList<ClassificationTypeSetting>();
        ClassificationTypeSetting cts = new ClassificationTypeSetting();
        cts.setClassificationType(ClassificationTypeSetting.SINGLE);
        cts.setSerializeClassifier(false);
        classificationTypeSettings.add(cts);

        // build a set of classifiers to evaluate
        List<TextClassifier> classifiers = new ArrayList<TextClassifier>();
        TextClassifier classifier = null;
        classifier = new DictionaryClassifier();
        classifiers.add(classifier);

        // build a set of feature settings for evaluation
        List<FeatureSetting> featureSettings = new ArrayList<FeatureSetting>();
        FeatureSetting fs = null;
        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(3);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(7);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(4);
        fs.setMaxNGramLength(7);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        fs.setMinNGramLength(3);
        fs.setMaxNGramLength(8);
        featureSettings.add(fs);

        fs = new FeatureSetting();
        fs.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        fs.setMinNGramLength(1);
        fs.setMaxNGramLength(3);
        featureSettings.add(fs);

        // build a set of datasets that should be used for evaluation
        Set<Dataset> datasets = new HashSet<Dataset>();
        Dataset dataset = new Dataset();
        dataset.setPath("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages_ipc1000.txt");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        datasets.add(dataset);

        // set evaluation settings
        EvaluationSetting evaluationSetting = new EvaluationSetting();
        evaluationSetting.setTrainingPercentageMin(20);
        evaluationSetting.setTrainingPercentageMax(50);
        evaluationSetting.setTrainingPercentageStep(10);
        evaluationSetting.setkFolds(3);
        evaluationSetting.addDataset(dataset);

        // train and test all classifiers in all combinations
        StopWatch stopWatch = new StopWatch();

        // train + test
        classifierManager.learnBestClassifier(classificationTypeSettings, classifiers, featureSettings,
                evaluationSetting);

        LOGGER.info("finished training and testing classifier in " + stopWatch.getElapsedTimeString());
    }

    /**
     * Train the language detector on a dataset.
     * 
     * @param dataset The dataset to train on.
     * @param classifierName The name for the learned classifier.
     */
    public static void train(Dataset dataset, String classifierName) {

        // take the time for the learning
        StopWatch stopWatch = new StopWatch();

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // create a text classifier by giving a name and a path where it should be saved to
        TextClassifier classifier = new DictionaryClassifier(classifierName,
                "data/models/" + classifierName
                + "/");

        // specify the settings for the classification
        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

        // we use only a single category per document
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.SINGLE);

        // we want the classifier to be serialized in the end
        classificationTypeSetting.setSerializeClassifier(true);

        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = new FeatureSetting();

        // we want to create character-level n-grams
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);

        // the minimum length of our n-grams should be 2
        featureSetting.setMinNGramLength(1);

        // the maximum length of our n-grams should be 7
        featureSetting.setMaxNGramLength(5);

        // we assign the settings to our classifier
        classifier.setClassificationTypeSetting(classificationTypeSetting);
        classifier.setFeatureSetting(featureSetting);

        // now we can train the classifier using the given dataset
        classifierManager.trainClassifier(dataset, classifier);

        LOGGER.info("finished training classifier in " + stopWatch.getElapsedTimeString());
    }

    @Override
    public String classify(String text) {
        return palladianClassifier.classify(text, getPossibleClasses()).getAssignedCategoryEntryNames();
    }

    public static void main(String[] args) {

        // ///////////////// find the best performing settings ///////////////////
        // specify the dataset that should be used as training data
        // PalladianLangDetect pld0 = new PalladianLangDetect();
        // pld0.evaluateBestSetting();
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// learn from a given dataset ///////////////////
        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        dataset.setPath("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages_ipc100_split1.txt");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        PalladianLangDetect.train(dataset, "palladianLanguageJRC");
        // ////////////////////////////////////////////////////////////////

    }

}
