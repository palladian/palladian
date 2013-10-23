package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

public class TermCorpusBuilder extends TextDocumentPipelineProcessor {
    
    private final MapTermCorpus termCorpus;

    public TermCorpusBuilder() {
        this(new MapTermCorpus());
    }

    public TermCorpusBuilder(MapTermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        @SuppressWarnings("unchecked")
        List<PositionAnnotation> annotations = featureVector.get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
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
