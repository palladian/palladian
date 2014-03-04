package ws.palladian.classification.text;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * This text classifier builds a dictionary from a pre-categorized list of text documents which can then be used to
 * categorize new, uncategorized text documents. During learning, a weighted term look up table is created, to learn how
 * probable each n-gram is for a given category. This look up table is used by during classification.
 * 
 * <p>
 * This classifier won the first Research Garden competition where the goal was to classify product descriptions into
 * eight different categories. See <a href=
 * "https://web.archive.org/web/20120122045250/http://www.research-garden.de/c/document_library/get_file?uuid=e60fa8da-4f76-4e64-a692-f74d5ffcf475&amp;groupId=10137"
 * >press release</a> (via archive.org).
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianTextClassifier implements Learner<DictionaryModel>, Classifier<DictionaryModel> {

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

    public PalladianTextClassifier(FeatureSetting featureSetting, ProcessingPipeline pipeline) {
        this.featureSetting = featureSetting;
        this.pipeline = pipeline;
    }

    @Override
    public DictionaryModel train(Iterable<? extends Trainable> trainables) {
        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Trainable trainable : trainables) {
            updateModel(trainable, model);
        }
        return model;
    }

    public DictionaryModel updateModel(Trainable trainable, DictionaryModel model) {
        process(trainable);
        String targetClass = trainable.getTargetClass();
        @SuppressWarnings("unchecked")
        ListFeature<PositionAnnotation> annotations = trainable.getFeatureVector().get(ListFeature.class,
                BaseTokenizer.PROVIDED_FEATURE);
        if (annotations != null) {
            for (PositionAnnotation annotation : annotations) {
                model.updateTerm(annotation.getValue(), targetClass);
            }
        }
        model.addCategory(targetClass);
        return model;
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, DictionaryModel model) {

        process(classifiable);

        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

        // iterate through all terms in the document
        @SuppressWarnings("unchecked")
        ListFeature<PositionAnnotation> annotations = classifiable.getFeatureVector().get(ListFeature.class,
                BaseTokenizer.PROVIDED_FEATURE);

        if (annotations != null) {
            for (PositionAnnotation annotation : annotations) {
                CategoryEntries categoryVector = model.getCategoryEntries(annotation.getValue());
                for (Category category : categoryVector) {
                    double frequency = category.getProbability();
                    builder.add(category.getName(), frequency * frequency);
                }
            }
        }

        // If we have a category weight by matching terms from the document, use them to create the probability
        // distribution. Else wise return the prior probability distribution of the categories.
        if (builder.getTotalScore() == 0) {
            return model.getPriors();
        }
        
        return builder.create();
    }

    // XXX ugly -- in case we have text documents and feature settings have been defined, do the preprocessing here
    // FIXME!!!
    private void process(Classifiable classifiable) {
        if (pipeline != null && classifiable instanceof TextDocument) {
            try {
                pipeline.process((TextDocument)classifiable);
            } catch (DocumentUnprocessableException e) {
                throw new IllegalStateException("error processing the document: " + e);
            }
        }
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        return classify(new TextDocument(text), model);
    }

}