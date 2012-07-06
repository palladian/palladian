package ws.palladian.extraction.token;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.TextAnnotationFeature;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A {@link BaseTokenizer} implementation based on <a href="http://alias-i.com/lingpipe/">LingPipe</a>'s <a
 * href="http://alias-i.com/lingpipe/docs/api/com/aliasi/tokenizer/IndoEuropeanTokenizerFactory.html"
 * >IndoEuropeanTokenizerFactory</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LingPipeTokenizer extends BaseTokenizer {

    private static final long serialVersionUID = 1L;

    /** Factory for creating a LingPipe tokenizer. */
    private final TokenizerFactory tokenizerFactory;

    public LingPipeTokenizer() {
        tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        String text = document.getContent();
        com.aliasi.tokenizer.Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
        TextAnnotationFeature annotationFeature = new TextAnnotationFeature(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        int index = 0;
        String nextToken = tokenizer.nextToken();
        while (nextToken != null) {
            int startPosition = tokenizer.lastTokenStartPosition();
            int endPosition = tokenizer.lastTokenEndPosition();
            annotationFeature.add(new PositionAnnotation(document, startPosition, endPosition, index++, nextToken));
            nextToken = tokenizer.nextToken();
        }
        document.getFeatureVector().add(annotationFeature);
    }
}
