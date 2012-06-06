package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureVector;

public class TermCorpusBuilder extends AbstractDefaultPipelineProcessor {

    private final TermCorpus termCorpus;

    public TermCorpusBuilder() {
        this(new TroveTermCorpus());
    }

    public TermCorpusBuilder(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(TokenizerInterface.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();

        Set<String> tokenValues = new HashSet<String>();

        for (Annotation annotation : annotations) {
            String tokenValue = annotation.getValue().toLowerCase();
            tokenValues.add(tokenValue);
        }

        termCorpus.addTermsFromDocument(tokenValues);

    }

    public TermCorpus getTermCorpus() {
        return termCorpus;
    }

}
