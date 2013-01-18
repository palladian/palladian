package ws.palladian.classification.text;

import java.util.List;
import java.util.Map;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * This classifier builds a weighed term look up table for the categories to
 * classify new documents. XXX add second dictionary: p(at|the), p(the|at) to
 * counter the problem of sparse categories
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianTextClassifier implements Classifier<DictionaryModel> {

    private final ProcessingPipeline pipeline;

    private final FeatureSetting featureSetting;

    public PalladianTextClassifier(FeatureSetting featureSetting) {
        this.featureSetting = featureSetting;
        if (featureSetting != null) {
            this.pipeline = new PreprocessingPipeline(featureSetting);
        } else {
            this.pipeline = null;
        }
    }

    @Override
    public DictionaryModel train(Iterable<? extends Trainable> trainables) {
        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Trainable trainable : trainables) {
            process(trainable);
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

    @Override
    public CategoryEntries classify(Classifiable classifiable, DictionaryModel model) {

        process(classifiable);

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

    // XXX ugly -- in case we have text documents and feature settings have been defined, do the preprocessing here
    private void process(Classifiable classifiable) {
        if (pipeline != null && classifiable instanceof TextDocument) {
            try {
                pipeline.process((TextDocument)classifiable);
            } catch (DocumentUnprocessableException e) {
                throw new IllegalStateException("Error processing the document: " + e);
            }
        }
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        return classify(new TextDocument(text), model);
    }

}