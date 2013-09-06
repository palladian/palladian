package ws.palladian.extraction.token;

import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

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

    /** Factory for creating a LingPipe tokenizer. */
    private final TokenizerFactory tokenizerFactory;

    public LingPipeTokenizer() {
        tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
    }

    @Override
    public void processDocument(TextDocument document) {
        String text = document.getContent();
        com.aliasi.tokenizer.Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
        String nextToken = tokenizer.nextToken();
        PositionAnnotationFactory factory = new PositionAnnotationFactory(document);
        ListFeature<PositionAnnotation> feature = new ListFeature<PositionAnnotation>(PROVIDED_FEATURE);
        while (nextToken != null) {
            int startPosition = tokenizer.lastTokenStartPosition();
            int endPosition = tokenizer.lastTokenEndPosition();
            feature.add(factory.create(startPosition, endPosition));
            nextToken = tokenizer.nextToken();
        }
        document.add(feature);
    }
}
