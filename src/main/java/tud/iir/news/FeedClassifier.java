package tud.iir.news;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;

/**
 * The FeedClassifier classifies a feed in terms of their update intervals.
 * 
 * @author David Urbansky
 */
public class FeedClassifier {

    private static final Logger LOGGER = Logger.getLogger(FeedClassifier.class);

    // ////////////////// possible classes for feeds ////////////////////
    /** feed class cannot be determined (feed not reachable) */
    public static final int CLASS_UNKNOWN = 0;

    /** feed was active but is not anymore */
    public static final int CLASS_ZOMBIE = 1;

    /** feed posts appear not often and at different intervals */
    public static final int CLASS_SPONTANUOUS = 2;

    /** feed posts are done at daytime with a longer gap at night */
    public static final int CLASS_SLICED = 3;

    /** feed posts are 24/7 at a similar interval */
    public static final int CLASS_CONSTANT = 4;

    /** all posts in the feed are updated together at a certain time */
    public static final int CLASS_CHUNKED = 5;

    /** all post entries are generated at request time */
    public static final int CLASS_ON_THE_FLY = 6;

    public static int classify(String feedURL) {
        return classify(feedURL, null);
    }
    /**
     * Classify a feed by its given URL.
     * 
     * @param feedURL The URL of the feed.
     * @return The class of the feed.
     */
    public static int classify(String feedURL, FeedStore feedStore) {

        int feedClass = CLASS_UNKNOWN;

        NewsAggregator newsAggregator;

        if (feedStore == null) {
            newsAggregator = new NewsAggregator();
        } else {
            newsAggregator = new NewsAggregator(feedStore);
        }

        try {

            List<FeedEntry> entries = newsAggregator.downloadFeed(feedURL).getEntries();
            FeedPostStatistics fps = new FeedPostStatistics(entries);
            System.out.println(fps);

            // // use rule based classification

            // if the post gap is 0 or extremely small, the feed is either updated on the fly or many entries posted at the same time
            if (fps.getMedianPostGap() < 5 * DateHelper.SECOND_MS) {
                if (fps.getTimeDifferenceToNewestPost() < 5 * DateHelper.SECOND_MS) {
                    feedClass = CLASS_ON_THE_FLY;
                } else {
                    feedClass = CLASS_CHUNKED;
                }
            } else {

                // if the last entry is a long time ago and the post gap was not that big, the feed is a zombie
                if (fps.getTimeDifferenceToNewestPost() >= 5 * fps.getMedianPostGap() && fps.getTimeDifferenceToNewestPost() > DateHelper.WEEK_MS) {
                    feedClass = CLASS_ZOMBIE;
                } else {

                    // if post intervals have large standard deviations the post is spontaneous
                    if (fps.getPostGapStandardDeviation() >= (fps.getMedianPostGap() / 10.0) && fps.getMedianPostGap() > DateHelper.DAY_MS) {
                        feedClass = CLASS_SPONTANUOUS;
                    } else {
                        // long gaps between posts (at night) indicate sliced feeds
                        if (fps.getLongestPostGap() > (12 * fps.getMedianPostGap()) && fps.getLongestPostGap() > 2 * DateHelper.HOUR_MS) {
                            feedClass = CLASS_SLICED;
                        } else {
                            feedClass = CLASS_CONSTANT;
                        }

                    }

                }

            }

        } catch (NewsAggregatorException e) {
            LOGGER.error("feed could not be classified, feedURL: " + feedURL + ", " + e.getMessage());
        }

        return feedClass;
    }

    public static int classify(Feed feed) {
        feed.setUpdateClass(classify(feed.getFeedUrl()));
        return feed.getUpdateClass();
    }

    /**
     * Get the name of the feed's class.
     * 
     * @param classID The integer value of the class.
     * @return The name of the class.
     */
    public String getClassName(int classID) {
        switch (classID) {
            case CLASS_UNKNOWN:
                return "unknown";
            case CLASS_ZOMBIE:
                return "zombie";
            case CLASS_SPONTANUOUS:
                return "spontanuous";
            case CLASS_SLICED:
                return "sliced";
            case CLASS_CONSTANT:
                return "constant";
            case CLASS_CHUNKED:
                return "chunked";
            case CLASS_ON_THE_FLY:
                return "on the fly";
            default:
                break;
        }

        return "unknown";
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {

        FeedClassifier fc = new FeedClassifier();
        // final String className = fc.getClassName(fc.classify("http://feeds.nydailynews.com/nydnrss/news"));
        // final String className = fc.getClassName(fc.classify("http://feeds.gawker.com/lifehacker/full"));
        final String className = fc.getClassName(FeedClassifier.classify("http://www.reddit.com/new/.rss"));

        System.out.println("classified as " + className);
    }

}