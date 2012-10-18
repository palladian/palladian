package ws.palladian.classification.universal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnClassifier;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.PalladianTextClassifier;
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

    // private Map<String, Double> weights2 = new HashMap<String, Double>();

    public UniversalClassifier(AbstractWeightingStrategy weightStrategy) {
        this(weightStrategy, new FeatureSetting());

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

    public UniversalClassifier(AbstractWeightingStrategy weightStrategy, FeatureSetting featureSetting) {

        textClassifier = new PalladianTextClassifier();
        this.featureSetting = featureSetting;
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
        // CategoryEntries mergedCategoryEntries = new CategoryEntries();
        Map<CategoryEntries, Double> weightedCategoryEntries = new HashMap<CategoryEntries, Double>();

        double weight = 1.0;

        // merge classification results
        if (model.getTextClassifier() != null) {
            weight = model.getWeights()[0];
            weightedCategoryEntries.put(result.getTextCategories(), weight);
            // addAllRelative(mergedCategoryEntries, weight, result.getTextCategories());

        }
        if (model.getKnnModel() != null) {
            weight = model.getWeights()[1];
            weightedCategoryEntries.put(result.getTextCategories(), weight);
            // addAllRelative(mergedCategoryEntries, weight, result.getNumericResults());
        }
        if (model.getBayesModel() != null) {
            weight = model.getWeights()[2];
            weightedCategoryEntries.put(result.getTextCategories(), weight);
            // addAllRelative(mergedCategoryEntries, weight, result.getNominalResults());
        }
        CategoryEntries normalizedCategoryEntries = normalize(weightedCategoryEntries);

        return normalizedCategoryEntries;
    }

    /**
     * <p>
     * Merges the results of the different classifiers and normalizes the resulting relevance score for each entry to be
     * in [0,1] and sum up to 1.
     * </p>
     * 
     * @param weightedCategoryEntries The classification result together with the weights for each of the classifiers.
     * @return Merged and normalized {@code CategoryEntries}.
     */
    protected CategoryEntries normalize(Map<CategoryEntries, Double> weightedCategoryEntries) {
        CategoryEntries normalizedCategoryEntries = new CategoryEntries();
        Map<CategoryEntry, Double> mergedCategoryEntries = new HashMap<CategoryEntry, Double>();

        // merge entries from different classifiers
        for (Map.Entry<CategoryEntries, Double> entries : weightedCategoryEntries.entrySet()) {
            for (CategoryEntry entry : entries.getKey()) {
                double relevance = entry.getProbability();
                double weight = entries.getValue();
                if (!mergedCategoryEntries.containsKey(entry)) {
                    mergedCategoryEntries.put(entry, relevance * weight);
                } else {
                    Double existingRelevance = mergedCategoryEntries.get(entry);
                    mergedCategoryEntries.put(entry, existingRelevance + relevance * weight);
                }
            }
        }

        // calculate normalization value.
        double totalRelevance = 0.0;
        for (Map.Entry<CategoryEntry, Double> entry : mergedCategoryEntries.entrySet()) {
            double mergedRelevance = entry.getValue();
            totalRelevance += mergedRelevance;
        }

        // normalize entries
        for (Map.Entry<CategoryEntry, Double> entry : mergedCategoryEntries.entrySet()) {
            CategoryEntry normalizedEntry = new CategoryEntry(entry.getKey().getName(), entry.getValue()
                    / totalRelevance);
            normalizedCategoryEntries.add(normalizedEntry);
        }

        return normalizedCategoryEntries;
    }

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
            textModel = textClassifier.train(instances, featureSetting);
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
        USE_NUMERIC, USE_TEXT, USE_NOMINAL
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