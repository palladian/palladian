package ws.palladian.classification.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableDouble;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.extraction.feature.AbstractTokenRemover;
import ws.palladian.extraction.feature.CharNGramCreator;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.LowerCaser;
import ws.palladian.extraction.feature.NGramCreator;
import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.ClassifiedTextDocument;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * This classifier builds a weighed term look up table for the categories to
 * classify new documents. XXX add second dictionary: p(at|the), p(the|at) to
 * counter the problem of sparse categories
 * 
 * @author David Urbansky
 */
public class PalladianTextClassifier implements Classifier<DictionaryModel> {

    /** The logger for this class. */
    // private static final Logger LOGGER = LoggerFactory.getLogger(PalladianTextClassifier.class);
    
    private ProcessingPipeline createPipeline(final FeatureSetting featureSetting) {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.connectToPreviousProcessor(new LowerCaser());
        if (featureSetting.getTextFeatureType() == FeatureSetting.CHAR_NGRAMS) {
            pipeline.connectToPreviousProcessor(new CharNGramCreator(featureSetting.getMinNGramLength(), featureSetting.getMaxNGramLength()));            
        } else {
            pipeline.connectToPreviousProcessor(new RegExTokenizer());
            pipeline.connectToPreviousProcessor(new NGramCreator(featureSetting.getMinNGramLength(), featureSetting.getMaxNGramLength()));
        }
        if (featureSetting.getTextFeatureType() == FeatureSetting.WORD_NGRAMS) {
            pipeline.connectToPreviousProcessor(new LengthTokenRemover(featureSetting.getMinimumTermLength(), featureSetting.getMaximumTermLength()));
        }
        pipeline.connectToPreviousProcessor(new DuplicateTokenRemover());
        pipeline.connectToPreviousProcessor(new AbstractTokenRemover() {
            @Override
            protected boolean remove(PositionAnnotation annotation) {
                String tokenValue = annotation.getValue();
                return (StringHelper.containsAny(tokenValue, Arrays.asList("&", "/", "=")) || StringHelper
                        .isNumber(tokenValue));
            }
        });
        pipeline.connectToPreviousProcessor(new TextDocumentPipelineProcessor() {
            @Override
            public void processDocument(TextDocument document) throws DocumentUnprocessableException {
                List<PositionAnnotation> annotations = new ArrayList<PositionAnnotation>(BaseTokenizer.getTokenAnnotations(document));
                Collections.sort(annotations, new Comparator<PositionAnnotation>() {
                    @Override
                    public int compare(PositionAnnotation o1, PositionAnnotation o2) {
                        Integer count1 = o1.getStartPosition();
                        Integer count2 = o2.getStartPosition();
                        return count1.compareTo(count2);
                    }
                });
                
                List<PositionAnnotation> newAnnotations = CollectionHelper.newArrayList();
                for (int i = 0; i < Math.min(annotations.size(), featureSetting.getMaxTerms()); i++) {
                    newAnnotations.add(annotations.get(i));
                }
                document.getFeatureVector().removeAll(BaseTokenizer.PROVIDED_FEATURE);
                document.getFeatureVector().addAll(newAnnotations);
            }
        });
        return pipeline;
    }

    @Override
    public DictionaryModel train(Iterable<? extends Trainable> instances) {
        return train(instances, new FeatureSetting());
    }

    public DictionaryModel train(Iterable<? extends Trainable> instances, FeatureSetting featureSetting) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        
        ProcessingPipeline pipeline = createPipeline(featureSetting);

        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Trainable instance : instances) {
            if (instance instanceof ClassifiedTextDocument) {
                try {
                    ClassifiedTextDocument textDoc = ((ClassifiedTextDocument)instance);
                    pipeline.process(textDoc);
                    trainWithInstance(model, textDoc);
                } catch (DocumentUnprocessableException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                trainWithInstance(model, instance);
            }
        }
        return model;
    }

    private void trainWithInstance(DictionaryModel model, Trainable instance) {
        String targetClass = instance.getTargetClass();
        List<PositionAnnotation> tokenAnnotations = instance.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
        
        System.out.println("training with " + tokenAnnotations.size() + " tokens " + targetClass);
        
        for (NominalFeature tokenAnnotation : tokenAnnotations) {
            model.updateTerm(tokenAnnotation.getValue(), targetClass);
        }
        model.addCategory(targetClass);
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        try {
            ProcessingPipeline pipeline = createPipeline(model.getFeatureSetting());
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

        ProcessingPipeline pipeline = createPipeline(model.getFeatureSetting());
        
        FeatureVector featureVector = classifiable.getFeatureVector();
        if (classifiable instanceof TextDocument) {
            TextDocument textDoc = ((TextDocument)classifiable);
            try {
                pipeline.process(textDoc);
                featureVector = textDoc.getFeatureVector();
            } catch (DocumentUnprocessableException e) {
                throw new IllegalStateException(e);
            }
        }
        
        // initialize probability Map with mutable double objects, so we can add relevance values to them
        Map<String, MutableDouble> probabilities = CollectionHelper.newHashMap();
        for (String category : model.getCategories()) {
            probabilities.put(category, new MutableDouble());
        }

        // sum up the probabilities for normalization
        double probabilitySum = 0.;

        // iterate through all terms in the document
        for (NominalFeature termFeature : featureVector.getAll(NominalFeature.class, BaseTokenizer.PROVIDED_FEATURE)) {
            CategoryEntries categoryFrequencies = model.getCategoryEntries(termFeature.getValue());
            for (CategoryEntry category : categoryFrequencies) {
                double categoryFrequency = category.getProbability();
                if (categoryFrequency > 0) {
                    double weight = categoryFrequency * categoryFrequency;
                    probabilities.get(category.getName()).add(weight);
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

}