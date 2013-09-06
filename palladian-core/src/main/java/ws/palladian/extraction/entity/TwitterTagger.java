package ws.palladian.extraction.entity;

import java.util.regex.Pattern;

/**
 * <p>
 * Tag twitter hashtags and user name mentions (words preceded by # or @).
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TwitterTagger extends RegExTagger {

    public static final String TWITTER_TAG_NAME = "TWITTER";

    private static final Pattern TWITTER_PATTERN = Pattern.compile("[@#]\\w+");

    /** Get the singleton instance of this tagger. */
    public static final TwitterTagger INSTANCE = new TwitterTagger();

    private TwitterTagger() {
        super(TWITTER_PATTERN, TWITTER_TAG_NAME);
    }

}
