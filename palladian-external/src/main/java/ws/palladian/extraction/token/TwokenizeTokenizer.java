package ws.palladian.extraction.token;

import java.util.Iterator;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.TextTokenizer;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.AbstractIterator2;
import cmu.arktweetnlp.Twokenize;

/**
 * <p>
 * Tokenizer based on the <i>Twokenize</i> algorithm available from <a
 * href="https://github.com/brendano/tweetmotif">here</a>. This class uses the ported Scala version delivered with <a
 * href="http://code.google.com/p/ark-tweet-nlp/">ark-tweet-nlp</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TwokenizeTokenizer implements TextTokenizer {

    @Override
    public Iterator<Token> iterateTokens(final String text) {

        final Iterator<String> tokens = Twokenize.tokenizeRawTweetText(text).iterator();
        return new AbstractIterator2<Token>() {

            int endPosition = 0;

            @Override
            protected Token getNext() {
                if (tokens.hasNext()) {
                    String token = tokens.next();
                    int startPosition = text.indexOf(token, endPosition);

                    // XXX bugfix, as the tokenizer seems to transform &gt; to > automatically,
                    // so we cannot determine the index for the annotation correctly. In this
                    // case, set it by former endPosition which should be okay. I guess.
                    if (startPosition == -1) {
                        startPosition = endPosition + 1;
                    }

                    endPosition = startPosition + token.length();
                    return new ImmutableToken(startPosition, token);

                }
                return finished();
            }
        };
    }

}
