package ws.palladian.classification;

import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.numeric.KNNClassifier;
import ws.palladian.classification.numeric.NumericClassifier;
import ws.palladian.classification.numeric.NumericInstance;
import ws.palladian.classification.page.ClassificationDocument;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.helper.FileHelper;


public class UniversalClassifier extends Classifier<UniversalInstance> {
    
    /** The serialize version ID. */
    private static final long serialVersionUID = 6434885229397022001L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(UniversalClassifier.class);
    
    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private TextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private NumericClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private BayesClassifier nominalClassifier;

    public UniversalClassifier() {

        // textClassifier = DictionaryClassifier.load("data/temp/textClassifier.gz");
        // numericClassifier = KNNClassifier.load("data/temp/numericClassifier.gz");
        // nominalClassifier = BayesClassifier.load("data/temp/nominalClassifier.gz");

        textClassifier = new DictionaryClassifier();
        numericClassifier = new KNNClassifier();
        nominalClassifier = new BayesClassifier();

    }

    public void classify(UniversalInstance instance) {

        // separate instance in feature types
        String textFeature = instance.getTextFeature();
        List<Double> numericFeatures = instance.getNumericFeatures();
        List<String> nominalFeatures = instance.getNominalFeatures();

        // classify text using the dictionary classifier
        ClassificationDocument textResult = textClassifier.classify(textFeature);

        // classify numeric features with the KNN
        NumericInstance numericInstance = new NumericInstance(null);
        numericInstance.setFeatures(numericFeatures);
        numericClassifier.classify(numericInstance);

        // classify nominal features with the Bayes classifier
        UniversalInstance nominalInstance = new UniversalInstance(null);
        nominalInstance.setNominalFeatures(nominalFeatures);
        // nominalClassifier.classify(nominalInstance);

        // merge classification results
        CategoryEntries mergedCategoryEntries = new CategoryEntries();
        mergedCategoryEntries.addAll(textResult.getAssignedCategoryEntries());
        mergedCategoryEntries.addAll(numericInstance.getAssignedCategoryEntries());
        // mergedCategoryEntries.addAll(nominalInstance.getAssignedCategoryEntries());

        instance.assignCategoryEntries(mergedCategoryEntries);
    }

    public TextClassifier getTextClassifier() {
        return textClassifier;
    }

    public void setTextClassifier(TextClassifier textClassifier) {
        this.textClassifier = textClassifier;
    }

    public NumericClassifier getNumericClassifier() {
        return numericClassifier;
    }

    public void setNumericClassifier(NumericClassifier numericClassifier) {
        this.numericClassifier = numericClassifier;
    }

    public BayesClassifier getNominalClassifier() {
        return nominalClassifier;
    }

    public void setNominalClassifier(BayesClassifier nominalClassifier) {
        this.nominalClassifier = nominalClassifier;
    }

    @Override
    public void save(String classifierPath) {
        FileHelper.serialize(this, classifierPath + getName() + ".gz");
    }

    public static UniversalClassifier load(String classifierPath) {
        LOGGER.info("deserialzing classifier from " + classifierPath);
        UniversalClassifier classifier = (UniversalClassifier) FileHelper.deserialize(classifierPath);
        // classifier.getTextClassifier().reset();
        return classifier;
    }

    /**
     * Train all classifiers.
     */
    public void trainAll() {
        // train the text classifier
        // ClassifierManager cm = new ClassifierManager();
        // cm.trainClassifier(dataset, classifier)

        // train the numeric classifier
        // getNumericClassifier().train();

        // train the nominal classifier
        getNominalClassifier().train();

    }

    /**
     * Perform actions that make sure all classifiers work properly.
     */
    // public void init() {
    // getTextClassifier().
    //
    // }
    
}