package ws.palladian.extraction.pos;

import java.util.List;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.TwokenizeTokenizer;
import ws.palladian.processing.features.Annotation;
import edu.cmu.cs.lti.ark.tweetnlp.TweetTaggerInstance;

/**
 * <p>
 * A POS tagger for Tweets, wrapping <a href="http://code.google.com/p/ark-tweet-nlp/">ark-tweet-nlp</a>.
 * <b>Important:</b> The following files from the original package need to be present in a directory <code>lib</code>:
 * <ul>
 * <li><code>lib/names</code></li>
 * <li><code>lib/embeddings.txt</code></li>
 * <li><code>lib/tagdict.txt</code></li>
 * <li><code>lib/tweetpos.model</code></li>
 * </ul>
 * </p>
 * 
 * @author Philipp Katz
 */
public class TweetNlpPosTagger extends BasePosTagger {

    private static final String TAGGER_NAME = "ark-tweet-nlp";
    private static final TwokenizeTokenizer TOKENIZER = new TwokenizeTokenizer();

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    @Override
    protected List<String> getTags(List<String> tokens) {
        TweetTaggerInstance tweetTagger = TweetTaggerInstance.getInstance();
        List<String> tags = tweetTagger.getTagsForOneSentence(tokens);
        assert tokens.size() == tags.size();
        return tags;
    }

    @Override
    protected BaseTokenizer getTokenizer() {
        return TOKENIZER;
    }

    public static void main(String[] args) {
        TweetNlpPosTagger posTagger = new TweetNlpPosTagger();
        List<Annotation> tags = posTagger
                .getAnnotations("I predict I won't win a single game I bet on. Got Cliff Lee today, so if he loses its on me RT @e_one: Texas (cont) http://tl.gd/6meogh");
        for (Annotation tagAnnotation : tags) {
            System.out.println(tagAnnotation);
        }
    }

}
