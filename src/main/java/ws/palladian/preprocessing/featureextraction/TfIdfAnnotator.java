package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.nlp.tokenization.Tokenizer;

public class TfIdfAnnotator implements PipelineProcessor {

    public static final String PROVIDED_FEATURE = "ws.palladian.preprocessing.tokens.tfidf";
    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NumericFeature.class);

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException("The required feature \"" + Tokenizer.PROVIDED_FEATURE + "\" is missing.");
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            FeatureVector tokenFeatureVector = annotation.getFeatureVector();
            double tf = tokenFeatureVector.get(FrequencyCalculator.PROVIDED_FEATURE_DESCRIPTOR).getValue();
            double idf = tokenFeatureVector.get(IdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR).getValue();
            NumericFeature tfidfFeature = new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, tf * idf);
            tokenFeatureVector.add(tfidfFeature);
        }
    }

}
