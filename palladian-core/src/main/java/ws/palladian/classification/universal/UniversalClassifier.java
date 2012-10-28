package ws.palladian.classification.universal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

public class UniversalClassifier implements Classifier<UniversalClassifierModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalClassifier.class);

    public static final FeatureDescriptor<NominalFeature> TEXT_FEATURE = FeatureDescriptorBuilder.build(
            "ws.palladian.feature.text", NominalFeature.class);

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private final PalladianTextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private final KnnClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private final NaiveBayesClassifier nominalClassifier;

    /** Whether or not to use the text classifier. */
    private boolean useTextClassifier = true;

    /** Whether or not to use the numeric classifier. */
    private boolean useNumericClassifier = true;

    /** Whether or not to use the nominal classifier. */
    private boolean useNominalClassifier = true;

    private final FeatureSetting featureSetting;

    private final int[] correctlyClassified = new int[3];

    public UniversalClassifier() {
        this(new FeatureSetting());

    }

    public UniversalClassifier(FeatureSetting featureSetting) {
        textClassifier = new PalladianTextClassifier();
        this.featureSetting = featureSetting;
        numericClassifier = new KnnClassifier();
        nominalClassifier = new NaiveBayesClassifier();
    }

    private void learnClassifierWeights(List<Instance> instances, UniversalClassifierModel model) {
        correctlyClassified[0] = 0;
        correctlyClassified[1] = 0;
        correctlyClassified[2] = 0;

        int c = 1;
        for (Instance instance : instances) {
            UniversalClassificationResult result = internalClassify(instance.getFeatureVector(), model);
            evaluateResults(instance, result, model);
//            ProgressHelper.showProgress(c++, instances.size(), 1);
        }

        model.setWeights(correctlyClassified[0] / (double)instances.size(),
                correctlyClassified[1] / (double)instances.size(), correctlyClassified[2] / (double)instances.size());

        LOGGER.debug("weight text   : " + model.getWeights()[0]);
        LOGGER.debug("weight numeric: " + model.getWeights()[1]);
        LOGGER.debug("weight nominal: " + model.getWeights()[2]);
    }

    private CategoryEntries evaluateResults(Instance instance, UniversalClassificationResult result,
            UniversalClassifierModel model) {
        Map<CategoryEntries, Double> weightedCategoryEntries = new HashMap<CategoryEntries, Double>();

        // Since there are not weights yet the classifier weights all results with one.
        CategoryEntries textCategories = result.getTextCategories();
        if (model.getTextClassifier() != null
                && textCategories.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            countCorrectlyClassified(0);
            weightedCategoryEntries.put(textCategories, 1.0);
        }
        CategoryEntries numericResults = result.getNumericResults();
        if (model.getKnnModel() != null
                && numericResults.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            countCorrectlyClassified(1);
            weightedCategoryEntries.put(numericResults, 1.0);
        }
        CategoryEntries nominalInstance = result.getNominalResults();
        if (model.getBayesModel() != null
                && nominalInstance.getMostLikelyCategoryEntry().getName().equals(instance.getTargetClass())) {
            countCorrectlyClassified(2);
            weightedCategoryEntries.put(nominalInstance, 1.0);
        }
        CategoryEntries mergedCategoryEntries = normalize(weightedCategoryEntries);

        return mergedCategoryEntries;
    }

    private void countCorrectlyClassified(int index) {
        correctlyClassified[index]++;
    }

    protected UniversalClassificationResult internalClassify(FeatureVector featureVector, UniversalClassifierModel model) {

        // separate instance in feature types
        String textFeature = "";
        if (featureVector.get(TEXT_FEATURE) != null) {
            textFeature = featureVector.get(TEXT_FEATURE).getValue();
        }
        
        CategoryEntries text=null;
        CategoryEntries numeric=null;
        CategoryEntries nominal=null;

        // classify text using the dictionary classifier
        if (model.getTextClassifier() != null) {
            text = textClassifier.classify(textFeature, model.getTextClassifier());
        }

        // classify numeric features with the KNN
        if (model.getKnnModel() != null) {
            numeric = numericClassifier.classify(featureVector, model.getKnnModel());
        }

        // classify nominal features with the Bayes classifier
        if (model.getBayesModel() != null) {
            nominal = nominalClassifier.classify(featureVector, model.getBayesModel());
        }
        return new UniversalClassificationResult(text, numeric, nominal);
    }

    private CategoryEntries mergeResults(UniversalClassificationResult result, UniversalClassifierModel model) {
        Map<CategoryEntries, Double> weightedCategoryEntries = LazyMap.create(new ConstantFactory<Double>(0.));

        // merge classification results
        if (model.getTextClassifier() != null) {
            double newWeight = weightedCategoryEntries.get(result.getTextCategories()) + model.getWeights()[0];
            weightedCategoryEntries.put(result.getTextCategories(), newWeight);
        }
        if (model.getKnnModel() != null) {
            double newWeight = weightedCategoryEntries.get(result.getNumericResults()) + model.getWeights()[1];
            weightedCategoryEntries.put(result.getNumericResults(), newWeight);
        }
        if (model.getBayesModel() != null) {
            double newWeight = weightedCategoryEntries.get(result.getNominalResults()) + model.getWeights()[2];
            weightedCategoryEntries.put(result.getNominalResults(), newWeight);
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
        Map<CategoryEntry, Double> mergedCategoryEntries = LazyMap.create(new ConstantFactory<Double>(0.));

        // merge entries from different classifiers
        for (Entry<CategoryEntries, Double> entries : weightedCategoryEntries.entrySet()) {
            for (CategoryEntry entry : entries.getKey()) {
                double relevance = entry.getProbability();
                double weight = entries.getValue();
                Double existingRelevance = mergedCategoryEntries.get(entry);
                mergedCategoryEntries.put(entry, existingRelevance + relevance * weight);
            }
        }

        // calculate normalization value.
        double totalRelevance = 0.0;
        for (Entry<CategoryEntry, Double> entry : mergedCategoryEntries.entrySet()) {
            totalRelevance += entry.getValue();
        }

        // normalize entries
        for (Entry<CategoryEntry, Double> entry : mergedCategoryEntries.entrySet()) {
            normalizedCategoryEntries
                    .add(new CategoryEntry(entry.getKey().getName(), entry.getValue() / totalRelevance));
        }

        return normalizedCategoryEntries;
    }

    @Override
    public UniversalClassifierModel train(List<Instance> instances) {
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
        learnClassifierWeights(instances, model);
        return model;
    }

    @Override
    public UniversalClassifierModel train(Dataset dataset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, UniversalClassifierModel model) {
        UniversalClassificationResult result = internalClassify(vector, model);
        return mergeResults(result, model);
    }

    // XXX not used?
//    public enum UniversalClassifierSettings {
//        USE_NUMERIC, USE_TEXT, USE_NOMINAL
//    }
}

class UniversalClassificationResult {
    private final CategoryEntries textCategories;
    private final CategoryEntries numericResults;
    private final CategoryEntries nominalResults;
    
    UniversalClassificationResult(CategoryEntries text, CategoryEntries numeric, CategoryEntries nominal) {
        this.textCategories = text;
        this.numericResults = numeric;
        this.nominalResults = nominal;
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UniversalClassificationResult [textCategories=");
        builder.append(textCategories);
        builder.append(", numericResults=");
        builder.append(numericResults);
        builder.append(", nominalResults=");
        builder.append(nominalResults);
        builder.append("]");
        return builder.toString();
    }
    
}