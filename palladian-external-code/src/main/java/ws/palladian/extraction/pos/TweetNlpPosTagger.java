package ws.palladian.extraction.pos;

import java.util.List;

import ws.palladian.extraction.TagAnnotation;
import ws.palladian.extraction.TagAnnotations;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.TwokenizeTokenizer;
import ws.palladian.processing.features.PositionAnnotation;
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
    protected void tag(List<PositionAnnotation> annotations) {
        TweetTaggerInstance tweetTagger = TweetTaggerInstance.getInstance();
        List<String> words = getTokenList(annotations);
        List<String> tags = tweetTagger.getTagsForOneSentence(words);
        assert words.size() == tags.size();
        for (int i = 0; i < tags.size(); i++) {
            assignTag(annotations.get(i), tags.get(i));
        }
    }

    @Override
    protected BaseTokenizer getTokenizer() {
        return TOKENIZER;
    }

    public static void main(String[] args) {
        TweetNlpPosTagger posTagger = new TweetNlpPosTagger();
        TagAnnotations tags = posTagger
                .tag("I predict I won't win a single game I bet on. Got Cliff Lee today, so if he loses its on me RT @e_one: Texas (cont) http://tl.gd/6meogh");
        for (TagAnnotation tagAnnotation : tags) {
            System.out.println(tagAnnotation);
        }
    }

}
