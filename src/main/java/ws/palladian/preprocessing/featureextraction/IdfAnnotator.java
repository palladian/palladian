package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class IdfAnnotator implements PipelineProcessor {

    public static final String PROVIDED_FEATURE = "ws.palladian.preprocessing.tokens.idf";

    private final TermCorpus termCorpus;

    public IdfAnnotator(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            double idf = termCorpus.getDf(annotation.getValue().toLowerCase());
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, idf);
            annotation.getFeatureVector().add(frequencyFeature);
        }
    }

}
