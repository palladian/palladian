package ws.palladian.extraction.token;

import java.util.List;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotationFactory;
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

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        String text = document.getContent();
        FeatureVector featureVector = document.getFeatureVector();
        List<String> tokens = Twokenize.tokenizeForTagger_J(text);
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(PROVIDED_FEATURE, document);
        int endPosition = 0;
        for (String token : tokens) {
            int startPosition = text.indexOf(token, endPosition);

            // XXX bugfix, as the tokenizer seems to transform &gt; to > automatically,
            // so we cannot determine the index for the annotation correctly. In this
            // case, set it by former endPosition which should be okay. I guess.
            if (startPosition == -1) {
                startPosition = endPosition + 1;
            }
            
            endPosition = startPosition + token.length();
            featureVector.add(annotationFactory.create(startPosition, endPosition));
        }
    }

}
