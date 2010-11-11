package tud.iir.classification.language;

import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.classification.page.ClassifierManager;
import tud.iir.classification.page.DictionaryClassifier;
import tud.iir.classification.page.TextClassifier;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.classification.page.evaluation.ClassifierPerformance;
import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.classification.page.evaluation.FeatureSetting;
import tud.iir.helper.StopWatch;

public class PalladianLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianLangDetect.class);

    private TextClassifier palladianClassifier;

    /** We can specify which classes are possible and discard all others for the classification task. */
    private Set<String> possibleClasses = null;

    public PalladianLangDetect(String modelPath) {
        palladianClassifier = ClassifierManager.load(modelPath);
    }

    public PalladianLangDetect() {
        palladianClassifier = ClassifierManager.load("data/models/palladianLanguageClassifier/LanguageClassifier.ser");
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

    /**
     * Train the language detector on a dataset.
     * 
     * @param dataset The dataset to train on.
     * @param classifierName The name for the learned classifier.
     */
    public void train(Dataset dataset, String classifierName) {

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
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);

        // the minimum length of our n-grams should be 4
        featureSetting.setMinNGramLength(4);

        // the maximum length of our n-grams should be 7
        featureSetting.setMaxNGramLength(7);

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

        // ///////////////// learn from a given dataset ///////////////////
        // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        dataset.setPath("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages_ipc100_split1.txt");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        PalladianLangDetect pld = new PalladianLangDetect();
        pld.train(dataset, "palladianLanguageJRC");
        // ////////////////////////////////////////////////////////////////

    }

}
