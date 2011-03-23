package ws.palladian.classification;

import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.numeric.KNNClassifier;
import ws.palladian.classification.numeric.NumericClassifier;
import ws.palladian.classification.numeric.NumericInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.helper.FileHelper;


public class UniversalClassifier extends Classifier<UniversalInstance> {

    /** The serialize version ID. */
    private static final long serialVersionUID = 6434885229397022001L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(UniversalClassifier.class);

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private DictionaryClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private NumericClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private BayesClassifier nominalClassifier;

    /** Whether or not to use the text classifier. */
    private boolean useTextClassifier = true;

    /** Whether or not to use the numeric classifier. */
    private boolean useNumericClassifier = true;

    /** Whether or not to use the nominal classifier. */
    private boolean useNominalClassifier = true;

    private int[] correctlyClassified = new int[3];
    private double[] weights = new double[3];

    public UniversalClassifier() {

        // textClassifier = DictionaryClassifier.load("data/temp/textClassifier.gz");
        // numericClassifier = KNNClassifier.load("data/temp/numericClassifier.gz");
        // nominalClassifier = BayesClassifier.load("data/temp/nominalClassifier.gz");

        textClassifier = new DictionaryClassifier();
        numericClassifier = new KNNClassifier();
        nominalClassifier = new BayesClassifier();

        weights[0] = 1.0;
        weights[1] = 1.0;
        weights[2] = 1.0;

        // weights[0] = 0.9242047500643059;
        // weights[1] = 0.7517362599674183;
        // weights[2] = 0.501929177741576;

    }

    public void learnClassifierWeights(Annotations annotations) {

        correctlyClassified = new int[3];
        correctlyClassified[0] = 0;
        correctlyClassified[1] = 0;
        correctlyClassified[2] = 0;

        weights = new double[3];

        int c = 0;
        for (Annotation annotation : annotations) {
            classify(annotation);
            c++;
            if (c % 100 == 0) {
                System.out.println(100 * c / (double) annotations.size());
            }
        }

        weights[0] = correctlyClassified[0] / (double) annotations.size();
        weights[1] = correctlyClassified[1] / (double) annotations.size();
        weights[2] = correctlyClassified[2] / (double) annotations.size();

        System.out.println("weight text   : " + weights[0]);
        System.out.println("weight numeric: " + weights[1]);
        System.out.println("weight nominal: " + weights[2]);

    }

    public void classify(UniversalInstance instance) {

        // separate instance in feature types
        String textFeature = instance.getTextFeature();
        List<Double> numericFeatures = instance.getNumericFeatures();
        List<String> nominalFeatures = instance.getNominalFeatures();

        // classify text using the dictionary classifier
        TextInstance textInstance = null;
        if (isUseTextClassifier()) {
            textInstance = textClassifier.classify(textFeature);
        }

        // classify numeric features with the KNN
        NumericInstance numericInstance = null;
        if (isUseNumericClassifier()) {
            numericInstance = new NumericInstance(null);
            numericInstance.setFeatures(numericFeatures);
            numericClassifier.classify(numericInstance);
        }

        // classify nominal features with the Bayes classifier
        UniversalInstance nominalInstance = null;
        if (isUseNominalClassifier()) {
            nominalInstance = new UniversalInstance(null);
            nominalInstance.setNominalFeatures(nominalFeatures);
            nominalClassifier.classify(nominalInstance);
        }

        CategoryEntries mergedCategoryEntries = new CategoryEntries();

        if (instance.getInstanceCategory() != null && !instance.getInstanceCategoryName().equals("CANDIDATE")) {

            if (isUseTextClassifier()
                    && textInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.getInstanceCategoryName())) {
                correctlyClassified[0]++;
                mergedCategoryEntries.addAllRelative(textInstance.getAssignedCategoryEntries());
            }
            if (isUseNumericClassifier()
                    && numericInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.getInstanceCategoryName())) {
                correctlyClassified[1]++;
                mergedCategoryEntries.addAllRelative(numericInstance.getAssignedCategoryEntries());
            }
            if (isUseNominalClassifier()
                    && nominalInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.getInstanceCategoryName())) {
                correctlyClassified[2]++;
                mergedCategoryEntries.addAllRelative(nominalInstance.getAssignedCategoryEntries());
            }

        } else if (instance.getInstanceCategory() != null && instance.getInstanceCategoryName().equals("CANDIDATE")) {

            // merge classification results
            if (isUseTextClassifier()) {
                mergedCategoryEntries.addAllRelative(weights[0], textInstance.getAssignedCategoryEntries());
            }
            if (isUseNumericClassifier()) {
                mergedCategoryEntries.addAllRelative(weights[1], numericInstance.getAssignedCategoryEntries());
            }
            if (isUseNominalClassifier()) {
                mergedCategoryEntries.addAllRelative(weights[2], nominalInstance.getAssignedCategoryEntries());
            }

        }

        instance.assignCategoryEntries(mergedCategoryEntries);
    }

    public DictionaryClassifier getTextClassifier() {
        return textClassifier;
    }

    public void setTextClassifier(DictionaryClassifier textClassifier) {
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
        if (isUseTextClassifier()) {
            getTextClassifier().train();
        }

        // train the numeric classifier
        if (isUseNumericClassifier()) {
            // getNumericClassifier().train();
        }

        // train the nominal classifier
        if (isUseNominalClassifier()) {
            getNominalClassifier().train();
        }

    }

    public boolean isUseTextClassifier() {
        return useTextClassifier;
    }

    public void setUseTextClassifier(boolean useTextClassifier) {
        this.useTextClassifier = useTextClassifier;
    }

    public boolean isUseNumericClassifier() {
        return useNumericClassifier;
    }

    public void setUseNumericClassifier(boolean useNumericClassifier) {
        this.useNumericClassifier = useNumericClassifier;
    }

    public boolean isUseNominalClassifier() {
        return useNominalClassifier;
    }

    public void setUseNominalClassifier(boolean useNominalClassifier) {
        this.useNominalClassifier = useNominalClassifier;
    }

    public void switchClassifiers(boolean text, boolean numeric, boolean nominal) {
        setUseTextClassifier(text);
        setUseNumericClassifier(numeric);
        setUseNominalClassifier(nominal);
    }
}