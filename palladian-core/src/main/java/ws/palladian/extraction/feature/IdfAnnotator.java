package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

public class IdfAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE = "ws.palladian.preprocessing.tokens.idf";

    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NumericFeature.class);

    private final TermCorpus termCorpus;

    public IdfAnnotator(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            double idf = termCorpus.getDf(annotation.getValue().toLowerCase());
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, idf);
            annotation.getFeatureVector().add(frequencyFeature);
        }
    }

}
