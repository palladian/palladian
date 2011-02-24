package ws.palladian.classification;

import java.util.List;

import org.apache.log4j.Logger;

import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.FastVector;
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

    // /////////////////////////////////////
    LinearRegression linearRegression = new LinearRegression();
    weka.core.Instances wekaInstances;
    private int[] correctlyClassified = new int[3];
    private double[] weights = new double[3];

    public UniversalClassifier() {

        // textClassifier = DictionaryClassifier.load("data/temp/textClassifier.gz");
        // numericClassifier = KNNClassifier.load("data/temp/numericClassifier.gz");
        // nominalClassifier = BayesClassifier.load("data/temp/nominalClassifier.gz");

        textClassifier = new DictionaryClassifier();
        numericClassifier = new KNNClassifier();
        nominalClassifier = new BayesClassifier();

    }

    public void learnClassifierWeights(Annotations annotations) {

        correctlyClassified = new int[3];
        correctlyClassified[0] = 0;
        correctlyClassified[1] = 0;
        correctlyClassified[2] = 0;

        weights = new double[3];

        Attribute textAttribute = new Attribute("text");
        Attribute numericAttribute = new Attribute("numeric");
        Attribute nominalAttribute = new Attribute("nominal");
        Attribute classAttribute = new Attribute("theClass");

        // Declare the feature vector
        FastVector fvWekaAttributes = new FastVector(4);
        fvWekaAttributes.addElement(textAttribute);
        fvWekaAttributes.addElement(numericAttribute);
        fvWekaAttributes.addElement(nominalAttribute);
        fvWekaAttributes.addElement(classAttribute);

        wekaInstances = new weka.core.Instances("Rel", fvWekaAttributes, annotations.size());
        wekaInstances.setClassIndex(3);

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

    }

    public void classify(UniversalInstance instance) {

        // separate instance in feature types
        String textFeature = instance.getTextFeature();
        List<Double> numericFeatures = instance.getNumericFeatures();
        List<String> nominalFeatures = instance.getNominalFeatures();

        // classify text using the dictionary classifier
        TextInstance textInstance = textClassifier.classify(textFeature);

        // classify numeric features with the KNN
        NumericInstance numericInstance = new NumericInstance(null);
        numericInstance.setFeatures(numericFeatures);
        numericClassifier.classify(numericInstance);

        // classify nominal features with the Bayes classifier
        UniversalInstance nominalInstance = new UniversalInstance(null);
        nominalInstance.setNominalFeatures(nominalFeatures);
        nominalClassifier.classify(nominalInstance);

        CategoryEntries mergedCategoryEntries = new CategoryEntries();

        if (instance.getInstanceCategory() != null && !instance.getInstanceCategoryName().equals("CANDIDATE")) {
            double text = 0;
            double numeric = 0;
            double nominal = 0;

            if (textInstance.getMainCategoryEntry().getCategory().getName().equals(instance.getInstanceCategoryName())) {
                text = 1.0;
                correctlyClassified[0]++;
            }
            if (numericInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.getInstanceCategoryName())) {
                numeric = 1.0;
                correctlyClassified[1]++;
            }
            if (nominalInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.getInstanceCategoryName())) {
                nominal = 1.0;
                correctlyClassified[2]++;
            }

            // weightClassifierOutputs(text, numeric, nominal);

            mergedCategoryEntries.addAllRelative(textInstance.getAssignedCategoryEntries());
            mergedCategoryEntries.addAllRelative(numericInstance.getAssignedCategoryEntries());
            mergedCategoryEntries.addAllRelative(nominalInstance.getAssignedCategoryEntries());

        } else if (instance.getInstanceCategory() != null && instance.getInstanceCategoryName().equals("CANDIDATE")) {

            // double[] coefficients = linearRegression.coefficients();
            double[] coefficients = new double[3];
            coefficients[0] = 1.0;
            coefficients[1] = 1.0;
            coefficients[2] = 1.0;

            // merge classification results
            mergedCategoryEntries.addAllRelative(weights[0], textInstance.getAssignedCategoryEntries());
            mergedCategoryEntries.addAllRelative(weights[1], numericInstance.getAssignedCategoryEntries());
            mergedCategoryEntries.addAllRelative(weights[2], nominalInstance.getAssignedCategoryEntries());

        }

        instance.assignCategoryEntries(mergedCategoryEntries);
    }

    public void weightClassifierOutputs(double text, double numeric, double nominal) {

        weka.core.Instance instance;
        instance = new weka.core.Instance(4);
        instance.setDataset(wekaInstances);
        instance.setValue(0, text);
        instance.setValue(1, numeric);
        instance.setValue(2, nominal);
        instance.setClassValue(1.0);
        wekaInstances.add(instance);

        try {
            linearRegression.buildClassifier(wekaInstances);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
        getTextClassifier().train();

        // train the numeric classifier
        // getNumericClassifier().train();

        // train the nominal classifier
        getNominalClassifier().train();

    }

}