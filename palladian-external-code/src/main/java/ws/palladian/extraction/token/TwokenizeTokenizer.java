package ws.palladian.extraction.token;

import java.util.List;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.TextAnnotationFeature;
import edu.cmu.cs.lti.ark.tweetnlp.Twokenize;

/**
 * <p>
 * Tokenizer based on the <i>Twokenize</i> algorithm available from <a
 * href="https://github.com/brendano/tweetmotif">here</a>. This class uses the ported Scala version delivered with <a
 * href="http://code.google.com/p/ark-tweet-nlp/">ark-tweet-nlp</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TwokenizeTokenizer extends BaseTokenizer {

    private static final long serialVersionUID = 1L;

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        String text = document.getContent();
        List<String> tokens = Twokenize.tokenizeForTagger_J(text);
        TextAnnotationFeature annotationFeature = new TextAnnotationFeature(PROVIDED_FEATURE_DESCRIPTOR);
        int endPosition = 0;
        int index = 0;
        for (String token : tokens) {
            int startPosition = text.indexOf(token, endPosition);

            // XXX bugfix, as the tokenizer seems to transform &gt; to > automatically,
            // so we cannot determine the index for the annotation correctly. In this
            // case, set it by former endPosition which should be okay. I guess.
            if (startPosition == -1) {
                startPosition = endPosition + 1;
            }
            
            endPosition = startPosition + token.length();
            annotationFeature.add(new PositionAnnotation(document, startPosition, endPosition, index++));
        }
        document.getFeatureVector().add(annotationFeature);
    }

}
