package ws.palladian.classification.text;

import java.util.List;
import java.util.Map;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * This classifier builds a weighed term look up table for the categories to
 * classify new documents. XXX add second dictionary: p(at|the), p(the|at) to
 * counter the problem of sparse categories
 * 
 * @author David Urbansky
 */
public class PalladianTextClassifier implements Classifier<DictionaryModel> {

    private /* final */ PreprocessingPipeline pipeline;

    private /* final */ FeatureSetting featureSetting;

    public PalladianTextClassifier(FeatureSetting featureSetting) {
        this.featureSetting = featureSetting;
        if (featureSetting != null) {
            this.pipeline = new PreprocessingPipeline(featureSetting);
        } else {
            this.pipeline = null;
        }
    }
    
    @Deprecated
    public PalladianTextClassifier() {
        this.featureSetting = null;
        this.pipeline = null;
    }

    @Override
    public DictionaryModel train(Iterable<? extends Trainable> trainables) {
        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Trainable trainable : trainables) {
            if (pipeline != null) {
                pipeline.process(trainable);
            }
            String targetClass = trainable.getTargetClass();
            List<PositionAnnotation> annotations = trainable.getFeatureVector().getAll(PositionAnnotation.class,
                    BaseTokenizer.PROVIDED_FEATURE);
            for (PositionAnnotation annotation : annotations) {
                model.updateTerm(annotation.getValue(), targetClass);
            }
            model.addCategory(targetClass);
        }
        return model;
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        if (featureSetting == null) {
            this.featureSetting = new FeatureSetting();
            this.pipeline = new PreprocessingPipeline(featureSetting);
        }
        try {
            TextDocument textDocument = new TextDocument(text);
            pipeline.process(textDocument);
            FeatureVector featureVector = textDocument.getFeatureVector();
            return classify(featureVector, model);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, DictionaryModel model) {

        if (pipeline != null) {
            pipeline.process(classifiable);
        }

        // initialize probability Map with mutable double objects, so we can add relevance values to them
        Map<String, Double> probabilities = LazyMap.create(ConstantFactory.create(0.));

        // sum up the probabilities for normalization
        double probabilitySum = 0.;

        // iterate through all terms in the document
        for (PositionAnnotation annotation : classifiable.getFeatureVector().getAll(PositionAnnotation.class,
                BaseTokenizer.PROVIDED_FEATURE)) {
            CategoryEntries categoryFrequencies = model.getCategoryEntries(annotation.getValue());
            for (CategoryEntry category : categoryFrequencies) {
                double categoryFrequency = category.getProbability();
                if (categoryFrequency > 0) {
                    double weight = categoryFrequency * categoryFrequency;
                    probabilities.put(category.getName(), probabilities.get(category.getName()) + weight);
                    probabilitySum += weight;
                }
            }
        }

        CategoryEntries categories = new CategoryEntries();

        // If we have a category weight by matching terms from the document, use them to create the probability
        // distribution. Else wise return the prior probability distribution of the categories.
        if (probabilitySum > 0) {
            for (String category : model.getCategories()) {
                categories.add(new CategoryEntry(category, probabilities.get(category).doubleValue() / probabilitySum));
            }
        } else {
            for (String category : model.getCategories()) {
                categories.add(new CategoryEntry(category, model.getPrior(category)));
            }
        }

        categories.sort();
        return categories;
    }

    @Deprecated
    public DictionaryModel train(List<Instance> convertInstances, FeatureSetting featureSetting2) {
        this.featureSetting = featureSetting2;
        this.pipeline = new PreprocessingPipeline(featureSetting2);
        return train(convertInstances);
    }

}