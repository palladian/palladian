package ws.palladian.classification;

import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.numeric.KnnClassifier;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CountMap2D;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;


// FIXME remove inheritance from Classifier
public class UniversalClassifier extends Classifier<UniversalInstance> /* implements Predictor<UniversalClassifierModel>  */{

    /** The serialize version ID. */
    private static final long serialVersionUID = 6434885229397022001L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(UniversalClassifier.class);
    
    public static final FeatureDescriptor<NominalFeature> TEXT_FEATURE = FeatureDescriptorBuilder.build(
            "ws.palladian.feature.text", NominalFeature.class);

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private DictionaryClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private KnnClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private NaiveBayesClassifier nominalClassifier;

    /** Whether or not to use the text classifier. */
    private boolean useTextClassifier = true;

    /** Whether or not to use the numeric classifier. */
    private boolean useNumericClassifier = true;

    /** Whether or not to use the nominal classifier. */
    private boolean useNominalClassifier = true;

    private int[] correctlyClassified = new int[3];
    private double[] weights = new double[3];

    private CountMap2D correctlyClassified2 = new CountMap2D();
    // private Map<String, Double> weights2 = new HashMap<String, Double>();

    public UniversalClassifier() {

        textClassifier = new DictionaryClassifier();
        numericClassifier = new KnnClassifier();
//        nominalClassifier = new NaiveBayesClassifier();

        weights[0] = 1.0;
        weights[1] = 1.0;
        weights[2] = 1.0;

    }

    public void learnClassifierWeights(List<NominalInstance> instances) {

        correctlyClassified = new int[3];
        correctlyClassified[0] = 0;
        correctlyClassified[1] = 0;
        correctlyClassified[2] = 0;

        weights = new double[3];

        int c = 1;
        for (NominalInstance instance : instances) {
            classify(instance, true);
            ProgressHelper.showProgress(c++, instances.size(), 1);
        }

        weights[0] = correctlyClassified[0] / (double) instances.size();
        weights[1] = correctlyClassified[1] / (double) instances.size();
        weights[2] = correctlyClassified[2] / (double) instances.size();

        System.out.println("weight text   : " + weights[0]);
        System.out.println("weight numeric: " + weights[1]);
        System.out.println("weight nominal: " + weights[2]);

    }

    public void learnClassifierWeightsByCategory(Instances<NominalInstance> instances) {

        correctlyClassified2 = new CountMap2D();

        int c = 1;
        for (NominalInstance instance : instances) {
            classify(instance, true);
            ProgressHelper.showProgress(c++, instances.size(), 1);
        }

    }

    public void classify(NominalInstance instance) {
        classify(instance, false);
    }

    public CategoryEntries classify(NominalInstance instance, boolean learnWeights) {

        // separate instance in feature types
        String textFeature = "";
        if (instance.featureVector.get(TEXT_FEATURE) != null) {
            textFeature = instance.featureVector.get(TEXT_FEATURE).getValue();
        }
//        String textFeature = instance.getTextFeature();
//        List<Double> numericFeatures = instance.getNumericFeatures();
//        List<String> nominalFeatures = instance.getNominalFeatures();

        // classify text using the dictionary classifier
        TextInstance textInstance = null;
        if (useTextClassifier) {
            textInstance = textClassifier.classify(textFeature);
        }

        // classify numeric features with the KNN
        UniversalInstance numericInstance = null;
        if (useNumericClassifier) {
//            numericInstance = new UniversalInstance(null);
//            numericInstance.setNumericFeatures(numericFeatures);
//            numericClassifier.classify(numericInstance);
//            numericClassifier.predict(instance.featureVector, model);
        }

        // classify nominal features with the Bayes classifier
        UniversalInstance nominalInstance = null;
        if (useNominalClassifier) {
//            nominalInstance = new UniversalInstance(null);
//            nominalInstance.setNominalFeatures(nominalFeatures);
//            nominalClassifier.classify(nominalInstance);
        }

        CategoryEntries mergedCategoryEntries = new CategoryEntries();

        if (instance.targetClass != null && learnWeights) {

            if (useTextClassifier
                    && textInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.targetClass)) {
                correctlyClassified[0]++;
                correctlyClassified2.increment("0", instance.targetClass);
                mergedCategoryEntries.addAllRelative(textInstance.getAssignedCategoryEntries());
            }
            if (useNumericClassifier
                    && numericInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.targetClass)) {
                correctlyClassified[1]++;
                correctlyClassified2.increment("1", instance.targetClass);
                mergedCategoryEntries.addAllRelative(numericInstance.getAssignedCategoryEntries());
            }
            if (useNominalClassifier
                    && nominalInstance.getMainCategoryEntry().getCategory().getName()
                    .equals(instance.targetClass)) {
                correctlyClassified[2]++;
                correctlyClassified2.increment("2", instance.targetClass);
                mergedCategoryEntries.addAllRelative(nominalInstance.getAssignedCategoryEntries());
            }

        } else {

            double weight = 1.0;

            // merge classification results
            if (useTextClassifier) {
                weight = weights[0];
                mergedCategoryEntries.addAllRelative(weight, textInstance.getAssignedCategoryEntries());

            }
            if (useNumericClassifier) {
                weight = weights[1];
                mergedCategoryEntries.addAllRelative(weight, numericInstance.getAssignedCategoryEntries());
            }
            if (useNominalClassifier) {
                weight = weights[2];
                mergedCategoryEntries.addAllRelative(weight, nominalInstance.getAssignedCategoryEntries());
            }

        }
        
        return mergedCategoryEntries;
    }

    public DictionaryClassifier getTextClassifier() {
        return textClassifier;
    }
    
    public NaiveBayesClassifier getNominalClassifier() {
        return nominalClassifier;
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
        if (useTextClassifier) {
            getTextClassifier().train(getTrainingInstances());
        }

        // train the numeric classifier
        if (useNumericClassifier) {
            // getNumericClassifier().train();
        }

        // train the nominal classifier
        if (useNominalClassifier) {
//            getNominalClassifier().setTrainingInstances(getTrainingInstances());
//            getNominalClassifier().train();
        }

    }

    public void switchClassifiers(boolean text, boolean numeric, boolean nominal) {
        useTextClassifier = text;
        useNumericClassifier = numeric;
        useNominalClassifier = nominal;
    }
    
    
    /////////////////////////////////////////////////////////////////
    // legacy code, remove!!!
    /////////////////////////////////////////////////////////////////
    
//    private Instances<UniversalInstance> trainInstances;
//    private UniversalClassifierModel model;
//    
//    @Deprecated
//    public void setTrainingInstances(Instances<UniversalInstance> trainInstances) {
//        LOGGER.debug("set " + trainInstances.size() + " training instances");
//        this.trainInstances = trainInstances;
//    }

//    @Deprecated
//    public void trainAll() {
//        LOGGER.debug("train all");
//        
//        List<NominalInstance> convertedInstances = CollectionHelper.newArrayList();
//        for (UniversalInstance universalInstance : trainInstances) {
//            NominalInstance nominalInstance = new NominalInstance();
//            nominalInstance.targetClass = universalInstance.getInstanceCategoryName();
//            nominalInstance.featureVector = universalInstance.getFeatureVector();
//            convertedInstances.add(nominalInstance);
//            nominalInstance.featureVector.add(new NominalFeature(TEXT_FEATURE, universalInstance.getTextFeature()));
//        }
//        
//        // FIXME store the model somehow
//        UniversalClassifierModel model = learn(convertedInstances);
//        
//        // FIXME necessary for NER I guess.
//        // learnClassifierWeights(convertedInstances, model);
//        
//        this.model = model;
//    }

    @Deprecated
    public void classify(UniversalInstance universalInstance) {
        String textValue = universalInstance.getTextFeature();
        NominalFeature textFeature = new NominalFeature(TEXT_FEATURE, textValue);
        FeatureVector featureVector = universalInstance.getFeatureVector();
        featureVector.add(textFeature);
        NominalInstance nominalInstance = new NominalInstance();
        nominalInstance.featureVector  =featureVector;
        CategoryEntries result = classify(nominalInstance, false);
        result.sortByRelevance();
        universalInstance.setAssignedCategoryEntries(result);
    }
}