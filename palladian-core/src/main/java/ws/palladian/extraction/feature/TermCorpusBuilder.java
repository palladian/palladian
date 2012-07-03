package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureVector;

public class TermCorpusBuilder extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;
    private final TermCorpus termCorpus;

    public TermCorpusBuilder() {
        this(new TermCorpus());
    }

    public TermCorpusBuilder(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \""
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + "\" is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Set<String> tokenValues = new HashSet<String>();
        for (Annotation annotation : annotations) {
            tokenValues.add(annotation.getValue().toLowerCase());
        }
        termCorpus.addTermsFromDocument(tokenValues);
    }

    public TermCorpus getTermCorpus() {
        return termCorpus;
    }

}
