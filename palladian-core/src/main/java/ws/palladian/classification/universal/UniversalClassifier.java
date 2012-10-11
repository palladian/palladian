package ws.palladian.classification.universal;

import java.util.List;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnClassifier;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

public class UniversalClassifier implements Classifier<UniversalClassifierModel> {

    /** The logger for this class. */
    // private static final Logger LOGGER = Logger.getLogger(UniversalClassifier.class);

    public static final FeatureDescriptor<NominalFeature> TEXT_FEATURE = FeatureDescriptorBuilder.build(
            "ws.palladian.feature.text", NominalFeature.class);

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private PalladianTextClassifier textClassifier;

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

    private final AbstractWeightingStrategy weightStrategy;

    private final FeatureSetting featureSetting;

    private final ClassificationTypeSetting classificationTypeSetting;

    // private Map<String, Double> weights2 = new HashMap<String, Double>();

    public UniversalClassifier(AbstractWeightingStrategy weightStrategy) {
        this(weightStrategy,new FeatureSetting(),new ClassificationTypeSetting());



    }

    // public void classify(Instance instance) {
    // classify(instance, false);
    // }

    // public CategoryEntries classify(Instance instance, boolean learnWeights) {
    // UniversalClassificationResult result = internalClassify(instance.getFeatureVector());
    //
    // if (instance.targetClass != null && learnWeights) {
    // return evaluateResults(instance,result);
    // } else {
    // return mergeResults(result);
    // }
    //
    // }

    public UniversalClassifier(AbstractWeightingStrategy weightStrategy, FeatureSetting featureSetting,
            ClassificationTypeSetting classificationTypeSetting) {
        
        textClassifier = new PalladianTextClassifier();
        this.featureSetting = featureSetting;
        this.classificationTypeSetting = classificationTypeSetting;
        numericClassifier = new KnnClassifier();
        nominalClassifier = new NaiveBayesClassifier();

        this.weightStrategy = weightStrategy;
        this.weightStrategy.setClassifier(this);
    }

    protected UniversalClassificationResult internalClassify(FeatureVector featureVector, UniversalClassifierModel model) {
        UniversalClassificationResult result = new UniversalClassificationResult();

        // separate instance in feature types
        String textFeature = "";
        if (featureVector.get(TEXT_FEATURE) != null) {
            textFeature = featureVector.get(TEXT_FEATURE).getValue();
        }
        // String textFeature = instance.getTextFeature();
        // List<Double> numericFeatures = instance.getNumericFeatures();
        // List<String> nominalFeatures = instance.getNominalFeatures();

        // classify text using the dictionary classifier
        if (model.getTextClassifier() != null) {
            result.setTextCategories(textClassifier.classify(textFeature, model.getTextClassifier()));
        }

        // classify numeric features with the KNN
        if (model.getKnnModel() != null) {
            result.setNumericResults(numericClassifier.classify(featureVector, model.getKnnModel()));
        }

        // classify nominal features with the Bayes classifier
        if (model.getBayesModel() != null) {
            result.setNominalResults(nominalClassifier.classify(featureVector, model.getBayesModel()));
        }

        return result;
    }

    private CategoryEntries mergeResults(UniversalClassificationResult result, UniversalClassifierModel model) {
        CategoryEntries mergedCategoryEntries = new CategoryEntries();

        double weight = 1.0;

        // merge classification results
        if (model.getTextClassifier() != null) {
            weight = model.getWeights()[0];
            mergedCategoryEntries.addAllRelative(weight, result.getTextCategories());

        }
        if (model.getKnnModel() != null) {
            weight = model.getWeights()[1];
            mergedCategoryEntries.addAllRelative(weight, result.getNumericResults());
        }
        if (model.getBayesModel() != null) {
            weight = model.getWeights()[2];
            mergedCategoryEntries.addAllRelative(weight, result.getNominalResults());
        }

        return mergedCategoryEntries;
    }

    // public PalladianTextClassifier getTextClassifier() {
    // return textClassifier;
    // }
    //
    // public NaiveBayesClassifier getNominalClassifier() {
    // return nominalClassifier;
    // }

    // @Override
    // public void save(String classifierPath) {
    // FileHelper.serialize(this, classifierPath + ".gz");
    // }
    //
    // public static UniversalClassifier load(String classifierPath) {
    // LOGGER.info("deserialzing classifier from " + classifierPath);
    // UniversalClassifier classifier = (UniversalClassifier)FileHelper.deserialize(classifierPath);
    // // classifier.getTextClassifier().reset();
    // return classifier;
    // }

    // /**
    // * Train all classifiers.
    // */
    // public void trainAll(ClassificationTypeSetting cts, FeatureSetting fs) {
    // // train the text classifier
    // // ClassifierManager cm = new ClassifierManager();
    // // cm.trainClassifier(dataset, classifier)
    //
    // // train the text classifier
    // if (useTextClassifier) {
    // // FIXME getTextClassifier().learn(getTrainingInstances(), cts, fs);
    // }
    //
    // // train the numeric classifier
    // if (useNumericClassifier) {
    // // getNumericClassifier().train();
    // }
    //
    // // train the nominal classifier
    // if (useNominalClassifier) {
    // // getNominalClassifier().setTrainingInstances(getTrainingInstances());
    // // getNominalClassifier().train();
    // }
    //
    // }

    // public void switchClassifiers(boolean text, boolean numeric, boolean nominal) {
    // useTextClassifier = text;
    // useNumericClassifier = numeric;
    // useNominalClassifier = nominal;
    // }

    // ///////////////////////////////////////////////////////////////
    // legacy code, remove!!!
    // ///////////////////////////////////////////////////////////////

    // private Instances<UniversalInstance> trainInstances;
    // private UniversalClassifierModel model;
    //
    // @Deprecated
    // public void setTrainingInstances(Instances<UniversalInstance> trainInstances) {
    // LOGGER.debug("set " + trainInstances.size() + " training instances");
    // this.trainInstances = trainInstances;
    // }

    // @Deprecated
    // public void trainAll() {
    // LOGGER.debug("train all");
    //
    // List<NominalInstance> convertedInstances = CollectionHelper.newArrayList();
    // for (UniversalInstance universalInstance : trainInstances) {
    // NominalInstance nominalInstance = new NominalInstance();
    // nominalInstance.targetClass = universalInstance.getInstanceCategoryName();
    // nominalInstance.featureVector = universalInstance.getFeatureVector();
    // convertedInstances.add(nominalInstance);
    // nominalInstance.featureVector.add(new NominalFeature(TEXT_FEATURE, universalInstance.getTextFeature()));
    // }
    //
    // // FIXME store the model somehow
    // UniversalClassifierModel model = learn(convertedInstances);
    //
    // // FIXME necessary for NER I guess.
    // // learnClassifierWeights(convertedInstances, model);
    //
    // this.model = model;
    // }

    // @Deprecated
    // public void classify(UniversalInstance universalInstance) {
    // String textValue = universalInstance.getTextFeature();
    // NominalFeature textFeature = new NominalFeature(TEXT_FEATURE, textValue);
    // FeatureVector featureVector = universalInstance.getFeatureVector();
    // featureVector.add(textFeature);
    // // Instance nominalInstance = new Instance();
    // // nominalInstance.featureVector = featureVector;
    // CategoryEntries result = classify(featureVector, false);
    // result.sortByRelevance();
    // universalInstance.assignCategoryEntries(result);
    // }

    @Override
    public UniversalClassifierModel train(List<Instance> instances) {
        // train the text classifier
        // ClassifierManager cm = new ClassifierManager();
        // cm.trainClassifier(dataset, classifier)
        NaiveBayesModel nominalModel = null;
        KnnModel numericModel = null;
        DictionaryModel textModel = null;

        // train the text classifier
        if (useTextClassifier) {
            textModel = textClassifier.train(instances, classificationTypeSetting, featureSetting);
        }

        // train the numeric classifier
        if (useNumericClassifier) {
            numericModel = numericClassifier.train(instances);
        }

        // train the nominal classifier
        if (useNominalClassifier) {
            nominalModel = nominalClassifier.train(instances);
        }

        UniversalClassifierModel model = new UniversalClassifierModel(nominalModel, numericModel, textModel);
        weightStrategy.learnClassifierWeights(instances, model);
        return model;
    }

    @Override
    public UniversalClassifierModel train(Dataset dataset) {
        return null;
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, UniversalClassifierModel model) {
        UniversalClassificationResult result = internalClassify(vector, model);
        return mergeResults(result, model);
    }
    
    public enum UniversalClassifierSettings {
        USE_NUMERIC,USE_TEXT,USE_NOMINAL
    }
}

class UniversalClassificationResult {
    private CategoryEntries textCategories = null;
    private CategoryEntries numericResults = null;
    private CategoryEntries nominalResults = null;

    public void setTextCategories(CategoryEntries textCategories) {
        this.textCategories = textCategories;
    }

    public CategoryEntries getNominalResults() {
        return nominalResults;
    }

    public CategoryEntries getNumericResults() {
        return numericResults;
    }

    public CategoryEntries getTextCategories() {
        return textCategories;
    }

    public void setNominalResults(CategoryEntries nominalResults) {
        this.nominalResults = nominalResults;
    }

    public void setNumericResults(CategoryEntries numericResults) {
        this.numericResults = numericResults;
    }
}