package ws.palladian.extraction.token;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.helper.collection.CollectionHelper;
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
public final class TwokenizeTokenizer extends AbstractTokenizer {

    @Override
    public List<Annotation> getAnnotations(String text) {

        List<String> tokens = Twokenize.tokenizeForTagger_J(text);
        List<Annotation> annotations = CollectionHelper.newArrayList();

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
            annotations.add(new ImmutableAnnotation(startPosition, token, StringUtils.EMPTY));
        }

        return annotations;
    }

}
