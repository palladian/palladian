package ws.palladian.preprocessing.nlp.tokenization;

import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.PositionAnnotation;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * A {@link Tokenizer} implementation based on <a href="http://alias-i.com/lingpipe/">LingPipe</a>'s <a
 * href="http://alias-i.com/lingpipe/docs/api/com/aliasi/tokenizer/IndoEuropeanTokenizerFactory.html"
 * >IndoEuropeanTokenizerFactory</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LingPipeTokenizer implements Tokenizer {

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
        AnnotationFeature annotationFeature = new AnnotationFeature(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        while (tokenizer.nextToken() != null) {
            int startPosition = tokenizer.lastTokenStartPosition();
            int endPosition = tokenizer.lastTokenEndPosition();
            annotationFeature.add(new PositionAnnotation(document, startPosition, endPosition));
        }
        document.getFeatureVector().add(annotationFeature);
    }

}
