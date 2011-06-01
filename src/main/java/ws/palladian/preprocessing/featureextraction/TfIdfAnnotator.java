package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class TfIdfAnnotator implements PipelineProcessor {
    
    public static final String PROVIDED_FEATURE = "ws.palladian.preprocessing.tokens.tfidf";

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            FeatureVector tokenFeatureVector = annotation.getFeatureVector();
            double tf = ((NumericFeature) tokenFeatureVector.get(FrequencyCalculator.PROVIDED_FEATURE)).getValue();
            double idf = ((NumericFeature) tokenFeatureVector.get(IdfAnnotator.PROVIDED_FEATURE)).getValue();
            NumericFeature tfidfFeature = new NumericFeature(PROVIDED_FEATURE, tf * idf);
            tokenFeatureVector.add(tfidfFeature);
        }
    }

}
