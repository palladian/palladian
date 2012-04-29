package ws.palladian.retrieval.feeds;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>The FeedClassifier classifies a feed in terms of their update intervals.</p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class FeedClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedClassifier.class);

//    // ////////////////// possible classes for feeds ////////////////////
//    /** Feed class is not known yet. */
//    public static final int CLASS_UNKNOWN = 0;
//
//    /** Feed is dead, that is, it does not return a valid document. */
//    public static final int CLASS_DEAD = 1;
//
//    /** Feed is alive but has zero entries. */
//    public static final int CLASS_EMPTY = 2;
//
//    /** Feed is alive but has only one single entry. */
//    public static final int CLASS_SINGLE_ENTRY = 3;
//
//    /** Feed was active but is not anymore. */
//    public static final int CLASS_ZOMBIE = 4;
//
//    /** Feed posts appear not often and at different intervals. */
//    public static final int CLASS_SPONTANEOUS = 5;
//
//    /** Feed posts are done at daytime with a longer gap at night. */
//    public static final int CLASS_SLICED = 6;
//
//    /** Feed posts are 24/7 at a similar interval. */
//    public static final int CLASS_CONSTANT = 7;
//
//    /** all posts in the feed are updated together at a certain time */
//    public static final int CLASS_CHUNKED = 8;
//
//    /** All post entries are generated at request time (have publish timestamps) */
//    public static final int CLASS_ON_THE_FLY = 9;

//    public static Integer[] getActivityPatternIDs() {
//        return new Integer[] { CLASS_UNKNOWN, CLASS_DEAD, CLASS_EMPTY, CLASS_SINGLE_ENTRY, CLASS_ZOMBIE,
//                CLASS_SPONTANEOUS, CLASS_SLICED, CLASS_CONSTANT, CLASS_CHUNKED, CLASS_ON_THE_FLY };
//    }

    public static void classifyFeedInStore(final FeedStore feedStore) {
        List<Feed> feeds = feedStore.getFeeds();

        StopWatch sw = new StopWatch();
        LOGGER.info("start classifying " + feeds.size() + " feeds");

        ExecutorService threadPool = Executors.newFixedThreadPool(500);

        for (final Feed feed : feeds) {
            // FeedClassificationThread classificationThread = new FeedClassificationThread(feed, feedStore);
            Runnable classificationThread = new Runnable() {
                @Override
                public void run() {
                    feed.setActivityPattern(classify(feed.getFeedUrl()));
                    feedStore.updateFeed(feed);
                }
            };
            threadPool.execute(classificationThread);
        }

        while (!threadPool.isTerminated()) {
            LOGGER.info(sw.getElapsedTimeString() + ", traffic: "
                    + HttpRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES) + "MB");

            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }

        }

        LOGGER.info("classified " + feeds.size() + " feeds in " + sw.getElapsedTimeString() + ", traffic: "
                + HttpRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES) + "MB");
    }

    /**
     * Classify a feed directly by its items. Make sure all properties such as windowSize pollTimestamp are set.
     * 
     * @param item The feed's items.
     * @return The classification as a numeric value.
     * @deprecated Classifying a feed directly by its items is dangerous since all feeds that do not provide item
     *             timestamps are classified as {@link #CLASS_ON_THE_FLY}. This is done since missing timestamps are
     *             replaced by the current timestamp to construct a {@link Feed} from the items in order to calculate
     *             the {@link FeedPostStatistics}. If you already have the {@link FeedItem}s to be used for
     *             classification, use {@link #classify(Feed)}.
     */
    @Deprecated
    public static FeedActivityPattern classify(List<FeedItem> items) {
        Feed feed = new Feed();
        feed.setItems(items);
        feed.setActivityPattern(classify(feed));
        return feed.getActivityPattern();
    }

    /**
     * Classify a feed by the items it already provides.
     * 
     * @param feed The feed.
     * @return The classification as a numeric value.
     */
    public static FeedActivityPattern classify(Feed feed) {
        FeedActivityPattern feedClass = FeedActivityPattern.CLASS_UNKNOWN;

        FeedPostStatistics fps = new FeedPostStatistics(feed);

        // use rule based classification

        if (feed.getCorrectedItemTimestamps().size() == 0) {
            feedClass = FeedActivityPattern.CLASS_EMPTY;
        } else if (feed.getCorrectedItemTimestamps().size() == 1) {
            feedClass = FeedActivityPattern.CLASS_SINGLE_ENTRY;
        } else if (fps.isValidStatistics()) {

            // if the post gap is 0 or extremely small, the feed is either updated on the fly or many entries posted at
            // the same time
            if (fps.getMedianPostGap() < TimeUnit.SECONDS.toMillis(5)) {
                if (fps.getTimeDifferenceNewestPostToLastPollTime() < TimeUnit.SECONDS.toMillis(5)) {
                    // TODO Sandro: getTimeDifferenceNewestPostToLastPollTime() should be replaced by using the date element from
                    // HTTP header, otherwise classification only works when done at same time the feed is fetched.
                    feedClass = FeedActivityPattern.CLASS_ON_THE_FLY;
                } else {
                    feedClass = FeedActivityPattern.CLASS_CHUNKED;
                }
            } else {

                // if the last entry is a long time ago and the post gap was not that big, the feed is a zombie
                if (fps.getTimeDifferenceNewestPostToLastPollTime() >= 8L * fps.getMedianPostGap()
                        && fps.getTimeDifferenceNewestPostToLastPollTime() > TimeUnit.DAYS.toMillis(8 * 7)) {
                    feedClass = FeedActivityPattern.CLASS_ZOMBIE;
                } else {

                    // if post intervals have large standard deviations the post is spontaneous
                    if (fps.getPostGapStandardDeviation() >= fps.getMedianPostGap() / 10.0
                            && fps.getMedianPostGap() > TimeUnit.DAYS.toMillis(1)) {
                        feedClass = FeedActivityPattern.CLASS_SPONTANEOUS;
                    } else {
                        // long gaps between posts (at night) indicate sliced feeds
                        if (fps.getLongestPostGap() < 12 * fps.getMedianPostGap()
                                && fps.getLongestPostGap() < TimeUnit.HOURS.toMillis(2) && fps.getAvgEntriesPerDay() >= 4) {
                            feedClass = FeedActivityPattern.CLASS_CONSTANT;
                        } else {
                            feedClass = FeedActivityPattern.CLASS_SLICED;
                        }

                    }

                }
            }

        }

        return feedClass;
    }

    /**
     * Classify a feed by its given URL. Retrieves and classifies the feed. The retrieved feed is wasted
     * 
     * @param feedURL The URL of the feed.
     * @return The class of the feed.
     */
    public static FeedActivityPattern classify(String feedURL) {

        FeedParser feedParser = new RomeFeedParser();
        Feed feed = new Feed();
        HttpRetriever retriever = new HttpRetriever();

        try {
            feed = feedParser.getFeed(feedURL);
        } catch (FeedParserException e) {
            LOGGER.error("feed could not be found and classified, feedURL: " + feedURL + ", " + e.getMessage());

            if (retriever.getResponseCode(feedURL) == 200) {
                return FeedActivityPattern.CLASS_UNKNOWN;
            } else {
                return FeedActivityPattern.CLASS_DEAD;
            }
        }

        return classify(feed);
    }


//    /**
//     * Get the name of the feed's class.
//     * 
//     * @param classID The integer value of the class.
//     * @return The name of the class.
//     */
//    public static String getClassName(int classID) {
//        switch (classID) {
//            case CLASS_UNKNOWN:
//                return "unknown";
//            case CLASS_DEAD:
//                return "dead";
//            case CLASS_EMPTY:
//                return "empty";
//            case CLASS_SINGLE_ENTRY:
//                return "single entry";
//            case CLASS_ZOMBIE:
//                return "zombie";
//            case CLASS_SPONTANEOUS:
//                return "spontaneous";
//            case CLASS_SLICED:
//                return "sliced";
//            case CLASS_CONSTANT:
//                return "constant";
//            case CLASS_CHUNKED:
//                return "chunked";
//            case CLASS_ON_THE_FLY:
//                return "on the fly";
//            default:
//                break;
//        }
//
//        return "unknown";
//    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // FeedClassifier.classifyFeedInStore(FeedDatabase.getInstance());
        // System.exit(0);

        // System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.news-journalonline.com/atom.xml")));

        final FeedActivityPattern feedClass = FeedClassifier.classify("http://absolutebailbond.com/feed/atom");
        System.out.println(feedClass);

        // final String className = fc.getClassName(fc.classify("http://feeds.gawker.com/lifehacker/full"));

        // System.out
        // .println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.artnewscentral.com/rss.php"))); //
        //
        // System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.oikotimes.com/v2/rss.php")));
        // // s
        // System.out.println(FeedClassifier.getClassName(FeedClassifier
        // .classify("http://www.gadgetsguru.in/rss/rss.aspx"))); //
        //
        // System.out.println(FeedClassifier.getClassName(FeedClassifier
        // .classify("http://feeds.sophos.com/en/rss2_0-sophos-security-news.xml"))); //
        // // sponanuous
        // System.out.println(FeedClassifier.getClassName(FeedClassifier
        // .classify("http://www.spacedaily.com/spacedaily.xml"))); //
        // // sponanuous
        // System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.speedtv.com/rss/"))); //
        // sponanuous
        // System.out.println(FeedClassifier.getClassName(FeedClassifier
        // .classify("http://www.charitynavigator.org/feeds/featured.xml")));
        // // // sponanuous
        // System.out
        // .println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.hindu.com/rss/01hdline.xml"))); //
        //
        // System.out.println(FeedClassifier.getClassName(FeedClassifier
        // .classify("http://www.babygiftstoys.com/store/2183249/index.rss")));
        // // // sponanuous
        // System.out.println(FeedClassifier.getClassName(FeedClassifier
        // .classify("http://feeds.feedburner.com/TexasStateNews")));

    }

}