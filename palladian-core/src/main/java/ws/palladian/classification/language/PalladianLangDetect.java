package ws.palladian.classification.language;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

/**
 * The best setting for medium to long texts is to use word n-grams with 1<=n<=3.
 * Evaluation results can be found in the Palladian book.
 * 
 * @author David Urbansky
 * 
 */
public class PalladianLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianLangDetect.class);

    private final PalladianTextClassifier textClassifier;
    
    private final DictionaryModel dictionaryModel;

    /** We can specify which classes are possible and discard all others for the classification task. */
    private Set<String> possibleClasses = null;

    public PalladianLangDetect(String modelPath) {
        dictionaryModel = FileHelper.deserialize(modelPath);
        textClassifier = new PalladianTextClassifier(dictionaryModel.getFeatureSetting());
    }

    public Set<String> getPossibleClasses() {
        return possibleClasses;
    }

    public void setPossibleClasses(Set<String> possibleClasses) {
        this.possibleClasses = possibleClasses;
    }

    // public ClassifierPerformance evaluate(Dataset dataset) {
    // FIXME!!!
    // return ClassifierEvaluator.evaluate(palladianClassifier, dataset);
    // return palladianClassifier.evaluate(dataset);
    // }

    /**
     * Train the language detector on a dataset.
     * 
     * @param dataset The dataset to train on.
     * @param classifierName The name of the classifier. The name is added to the classifierPath.
     * @param classifierPath The path where the classifier should be saved to. For example, <tt>data/models/</tt>
     */
    public static void train(Dataset dataset, String classifierName, String classifierPath) {
        train(dataset, classifierName, classifierPath, null);
    }

    public static void train(Dataset dataset, String classifierName, String classifierPath, FeatureSetting fs) {

        // take the time for the learning
        StopWatch stopWatch = new StopWatch();


        // specify feature settings that should be used by the classifier
        FeatureSetting featureSetting = fs;

        if (featureSetting == null) {
            // we want to create character-level n-grams
            // the minimum length of our n-grams should be 4
            // the maximum length of our n-grams should be 7
            featureSetting = new FeatureSetting(TextFeatureType.CHAR_NGRAMS, 4, 7);
        }
        
        // create a text classifier by giving a name and a path where it should be saved to
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting);
        // TextClassifier classifier = new DictionaryClassifier(classifierName,classifierPath);

        // now we can train the classifier using the given dataset
        // classifier.train(dataset);
        // classifier.save(classifierPath);
        // classifierManager.trainClassifier(dataset, classifier);

        TextDatasetIterator datasetIterator = TextDatasetIterator.createIterator(dataset);
        DictionaryModel trainedModel = classifier.train(datasetIterator);

        // test the classifier
        // Dataset testDataset = new Dataset();
        //
        // // set the path to the dataset, the first field is a link, and columns are separated with a space
        // testDataset.setPath("C:\\Data\\datasets\\JRCLanguageCorpus\\indexAll22Languages_ipc20_split2.txt");
        //
        // testDataset.setFirstFieldLink(true);
        // testDataset.setSeparationString(" ");
        //
        // System.out.println(classifier.evaluate(testDataset));

        FileHelper.serialize(trainedModel, classifierPath + classifierName + ".gz");

        LOGGER.info("finished training classifier in " + stopWatch.getElapsedTimeString());
    }

    @Override
    public String classify(String text) {
        return classifyAsCategoryEntry(text).getMostLikelyCategory();
    }

    public CategoryEntries classifyAsCategoryEntry(String text) {
        CategoryEntries categoryEntries = textClassifier.classify(text, dictionaryModel);
        categoryEntries = narrowCategories(categoryEntries);
        return categoryEntries;
    }
    
    private CategoryEntries narrowCategories(CategoryEntries categoryEntries) {
        if (possibleClasses == null) {
            return categoryEntries;
        }
        CategoryEntriesMap narrowedCategories = new CategoryEntriesMap();
        for (String categoryName : categoryEntries) {
            if (possibleClasses.contains(categoryName)) {
                narrowedCategories.set(categoryName, categoryEntries.getProbability(categoryName));
            }
        }
        return narrowedCategories;
    }

    public static void main(String[] args) throws IOException {

        // ///////////////// use the language classifier ///////////////////
        // String languageModelPath = "data/models/palladianLanguageClassifier/LanguageClassifier.gz";
        // String languageModelPath = "data/models/palladianLanguageJRC/palladianLanguageJRC.gz";
        // String languageModelPath =
        // "C:\\My Dropbox\\KeywordExtraction\\palladianLanguageJRC_o\\palladianLanguageJRC.gz";
        //
        // PalladianLangDetect pld0 = new PalladianLangDetect("data/models/language/wikipedia76Languages20ipc.gz");
        // // PalladianLangDetect pld0 = new PalladianLangDetect("data/models/language/languageMicroblogging.gz");
        // String language = pld0.classify("This is a sample text in English");
        // System.out.println("The text was classified as: " + language);
        // language = pld0.classify("Das ist ein Beispieltext auf Deutsch");
        // System.out.println("The text was classified as: " + language);
        // language = pld0.classify("Se trata de un texto de muestra en español");
        // System.out.println("The text was classified as: " + language);
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// find the best performing settings ///////////////////
        // specify the dataset that should be used as training data
        // PalladianLangDetect pld0 = new PalladianLangDetect();
        // pld0.evaluateBestSetting();
        // System.exit(0);
        // ////////////////////////////////////////////////////////////////

        // ///////////////// learn from a given dataset ///////////////////
        // String datasetRootFolder = "H:\\PalladianData\\Datasets\\JRCLanguageCorpus";

        // // create an index over the dataset
        // DatasetManager dsManager = new DatasetManager();
        // String path = dsManager.createIndex(datasetRootFolder, new String[] { "en", "es", "de" });
        // String path = dsManager.createIndex(datasetRootFolder);
        //
        // // create an excerpt with 1000 instances per class
        // String indexExcerpt = dsManager.createIndexExcerpt(
        // "H:\\PalladianData\\Datasets\\Wikipedia76Languages\\languageDocumentIndex.txt", " ", 20);
        //
        // // specify the dataset that should be used as training data
        Dataset dataset = new Dataset();

        // tell the preprocessor that the first field in the file is a link to the actual document
        dataset.setFirstFieldLink(true);

        // set the path to the dataset, the first field is a link, and columns are separated with a space
        dataset.setPath("H:\\PalladianData\\Datasets\\JRCLanguageCorpus\\indexAll22Languages_ipc20.txt");
        // dataset.setPath("H:\\PalladianData\\Datasets\\Microblogging35Languages\\languageDocumentIndex.txt");
        // dataset.setPath("H:\\PalladianData\\Datasets\\Wikipedia76Languages\\languageDocumentIndex.txt");
        // dataset.setPath(indexExcerpt);

        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");

        PalladianLangDetect.train(dataset, "jrc22Languages20ipc", "data/models/palladian/language/");
        // PalladianLangDetect.train(dataset, "microblogging35Languages", "data/models/palladian/language/");
        // PalladianLangDetect.train(dataset, "wikipedia76Languages20ipc", "data/models/palladian/language/");
        // ////////////////////////////////////////////////////////////////

    }

}
