package ws.palladian.retrieval.feeds;

import org.apache.log4j.Logger;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This class is responsible for calculating a {@link FeedItem}'s hash. The strategy for calculating the hash can be
 * altered, by subclassing this class, implementing {@link #hash(FeedItem)} according to your needs and setting the
 * static field {@value #STRATEGY} to an instance of your subclass.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class FeedItemHashGenerator {

    // XXX Setting this via static field is not nice, necessary changes for making this nice would be too big for now.

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedItemHashGenerator.class);

    /** The strategy used for creating a {@link FeedItem}'s hash. */
    public static FeedItemHashGenerator STRATEGY = new FeedItemHashGenerator() {
        @Override
        public String hash(FeedItem feedItem) {
            String newHash = null;

            StringBuilder hash = new StringBuilder();
            hash.append(feedItem.getTitle());
            hash.append(UrlHelper.removeSessionId(feedItem.getLink()));
            hash.append(UrlHelper.removeSessionId(feedItem.getRawId()));
            // if (getFeed().getActivityPattern() != FeedClassifier.CLASS_UNKNOWN
            // && getFeed().getActivityPattern() != FeedClassifier.CLASS_ON_THE_FLY) {
            // hash.append(getPublished().toString());
            // }
            if (feedItem.getTitle() != null || feedItem.getLink() != null || feedItem.getRawId() != null) {
                newHash = StringHelper.sha1(hash.toString());

            } else {
                LOGGER.error("Could not generate custom item hash, all values are null or empty. Feed id "
                        + feedItem.getFeedId());
            }

            return newHash;
        }
    };

    /**
     * <p>
     * Generate a hash for a {@link FeedItem} using the {@link #STRATEGY}.
     * </p>
     * 
     * @param feedItem The {@link FeedItem} for which to create the hash.
     * @return The hash for the {@link FeedItem}, or <code>null</code> if no hash could be calculated.
     */
    public final static String generateHash(FeedItem feedItem) {
        return STRATEGY.hash(feedItem);
    }

    //
    // Strategy method to be overridden.
    //
    
    /**
     * <p>
     * Calculate a hash for the specified {@link FeedItem}. The implementation needs to be <b>Thread-safe</b>!
     * </p>
     * 
     * @param feedItem The {@link FeedItem} for which to create the hash.
     * @return The hash for the {@link FeedItem}, or <code>null</code> if no hash could be calculated.
     */
    public abstract String hash(FeedItem feedItem);

}
