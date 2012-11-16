package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

public class TermCorpusBuilder extends StringDocumentPipelineProcessor {
    
    private final TermCorpus termCorpus;

    public TermCorpusBuilder() {
        this(new TermCorpus());
    }

    public TermCorpusBuilder(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        List<PositionAnnotation> annotations = featureVector.getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException("The required feature \""
//                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + "\" is missing");
//        }
//        List<Annotation<String>> annotations = annotationFeature.getValue();
        Set<String> tokenValues = new HashSet<String>();
        for (PositionAnnotation annotation : annotations) {
            tokenValues.add(annotation.getValue().toLowerCase());
        }
        termCorpus.addTermsFromDocument(tokenValues);
    }

    public TermCorpus getTermCorpus() {
        return termCorpus;
    }

}
