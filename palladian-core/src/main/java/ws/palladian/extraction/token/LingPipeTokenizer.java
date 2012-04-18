package ws.palladian.extraction.token;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.PositionAnnotation;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A {@link TokenizerInterface} implementation based on <a href="http://alias-i.com/lingpipe/">LingPipe</a>'s <a
 * href="http://alias-i.com/lingpipe/docs/api/com/aliasi/tokenizer/IndoEuropeanTokenizerFactory.html"
 * >IndoEuropeanTokenizerFactory</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LingPipeTokenizer implements TokenizerInterface {

    private static final long serialVersionUID = 1L;

    /** Factory for creating a LingPipe tokenizer. */
    private final TokenizerFactory tokenizerFactory;

    public LingPipeTokenizer() {
        tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    }

    @Override
    public void process(PipelineDocument document) {
        String text = document.getOriginalContent();
        com.aliasi.tokenizer.Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
        AnnotationFeature annotationFeature = new AnnotationFeature(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        int index = 0;
        while (tokenizer.nextToken() != null) {
            int startPosition = tokenizer.lastTokenStartPosition();
            int endPosition = tokenizer.lastTokenEndPosition();
            annotationFeature.add(new PositionAnnotation(document, startPosition, endPosition, index++));
        }
        document.getFeatureVector().add(annotationFeature);
    }

}
