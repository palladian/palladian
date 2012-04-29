package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

public class IdfAnnotator extends AbstractPipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE = "ws.palladian.preprocessing.tokens.idf";

    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NumericFeature.class);

    private final TermCorpus termCorpus;

    public IdfAnnotator(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    protected void processDocument(PipelineDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \""
                    + TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR + " \" is missing.");
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            double idf = termCorpus.getIdf(annotation.getValue().toLowerCase());
            annotation.getFeatureVector().add(new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, idf));
        }
    }

}
