package tud.iir.news;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;

/**
 * The FeedClassifier classifies a feed in terms of their update intervals.
 * 
 * @author David Urbansky
 */
public class FeedClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedClassifier.class);

    // ////////////////// possible classes for feeds ////////////////////
    /** Feed class is not known yet. */
    public static final int CLASS_UNKNOWN = 0;

    /** Feed is dead, that is, it does not return a valid document. */
    public static final int CLASS_DEAD = 1;

    /** Feed is alive but has zero entries. */
    public static final int CLASS_EMPTY = 2;

    /** Feed is alive but has only one single entry. */
    public static final int CLASS_SINGLE_ENTRY = 3;

    /** Feed was active but is not anymore. */
    public static final int CLASS_ZOMBIE = 4;

    /** Feed posts appear not often and at different intervals. */
    public static final int CLASS_SPONTANEOUS = 5;

    /** Feed posts are done at daytime with a longer gap at night. */
    public static final int CLASS_SLICED = 6;

    /** Feed posts are 24/7 at a similar interval. */
    public static final int CLASS_CONSTANT = 7;

    /** all posts in the feed are updated together at a certain time */
    public static final int CLASS_CHUNKED = 8;

    /** all post entries are generated at request time */
    public static final int CLASS_ON_THE_FLY = 9;

    public static Integer[] getActivityPatternIDs() {
        return new Integer[] { CLASS_UNKNOWN, CLASS_DEAD, CLASS_EMPTY, CLASS_SINGLE_ENTRY, CLASS_ZOMBIE,
                CLASS_SPONTANEOUS, CLASS_SLICED, CLASS_CONSTANT, CLASS_CHUNKED, CLASS_ON_THE_FLY };
    }

    public static void classifyFeedInStore(FeedStore feedStore) {
        List<Feed> feeds = feedStore.getFeeds();

        StopWatch sw = new StopWatch();
        LOGGER.info("start classifying " + feeds.size() + " feeds");

        ExecutorService threadPool = Executors.newFixedThreadPool(500);

        for (Feed feed : feeds) {
            FeedClassificationThread classificationThread = new FeedClassificationThread(feed, feedStore);
            threadPool.execute(classificationThread);
        }

        while (!threadPool.isTerminated()) {
            LOGGER.info(sw.getElapsedTimeString() + ", traffic: " + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES)
                    + "MB");

            try {
                Thread.sleep(1 * DateHelper.MINUTE_MS);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }

        }

        LOGGER.info("classified " + feeds.size() + " feeds in " + sw.getElapsedTimeString() + ", traffic: "
                + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + "MB");
    }

    /**
     * Classify a feed directly by its items.
     * 
     * @param item The feed's items.
     * @return The classification as a numeric value.
     */
    public static int classify(List<FeedItem> items) {
        int feedClass = CLASS_UNKNOWN;

        Feed feed = new Feed();
        feed.setItems(items);
        FeedPostStatistics fps = new FeedPostStatistics(feed);

        // // use rule based classification

        if (!fps.isValidStatistics() && items.size() > 0) {
            feedClass = CLASS_UNKNOWN;
        } else if (items.size() == 0) {
            feedClass = CLASS_EMPTY;
        } else if (items.size() == 1) {
            feedClass = CLASS_SINGLE_ENTRY;
        } else

            // if the post gap is 0 or extremely small, the feed is either updated on the fly or many entries posted at the
            // same time
            if (fps.getMedianPostGap() < 5 * DateHelper.SECOND_MS) {
                if (fps.getTimeDifferenceToNewestPost() < 5 * DateHelper.SECOND_MS) {
                    feedClass = CLASS_ON_THE_FLY;
                } else {
                    feedClass = CLASS_CHUNKED;
                }
            } else {

                // if the last entry is a long time ago and the post gap was not that big, the feed is a zombie
                if (fps.getTimeDifferenceToNewestPost() >= 8L * fps.getMedianPostGap()
                        && fps.getTimeDifferenceToNewestPost() > 8L * DateHelper.WEEK_MS) {
                    feedClass = CLASS_ZOMBIE;
                } else {

                    // if post intervals have large standard deviations the post is spontaneous
                    if (fps.getPostGapStandardDeviation() >= fps.getMedianPostGap() / 10.0
                            && fps.getMedianPostGap() > DateHelper.DAY_MS) {
                        feedClass = CLASS_SPONTANEOUS;
                    } else {
                        // long gaps between posts (at night) indicate sliced feeds
                        if (fps.getLongestPostGap() < 12 * fps.getMedianPostGap()
                                && fps.getLongestPostGap() < 2 * DateHelper.HOUR_MS && fps.getAvgEntriesPerDay() >= 4) {
                            feedClass = CLASS_CONSTANT;
                        } else {

                            feedClass = CLASS_SLICED;
                        }

                    }

                }

            }

        return feedClass;
    }

    /**
     * Classify a feed by its given URL.
     * 
     * @param feedURL The URL of the feed.
     * @return The class of the feed.
     */
    public static int classify(String feedURL) {

        FeedDownloader feedDownloader = new FeedDownloader();

        List<FeedItem> items = new ArrayList<FeedItem>();

        // check if feed is not accessible, try 5 times
        Crawler crawler = new Crawler();

        try {

            Feed feed = feedDownloader.getFeed(feedURL);
            items = feed.getItems();

        } catch (NewsAggregatorException e) {
            LOGGER.error("feed could not be found and classified, feedURL: " + feedURL + ", " + e.getMessage());

            if (crawler.getResponseCode(feedURL) == 200) {
                return CLASS_UNKNOWN;
            } else {
                return CLASS_DEAD;
            }
        }

        return classify(items);
    }

    public static int classify(Feed feed) {
        feed.setActivityPattern(classify(feed.getFeedUrl()));
        return feed.getActivityPattern();
    }

    /**
     * Get the name of the feed's class.
     * 
     * @param classID The integer value of the class.
     * @return The name of the class.
     */
    public static String getClassName(int classID) {
        switch (classID) {
            case CLASS_UNKNOWN:
                return "unknown";
            case CLASS_DEAD:
                return "dead";
            case CLASS_EMPTY:
                return "empty";
            case CLASS_SINGLE_ENTRY:
                return "single entry";
            case CLASS_ZOMBIE:
                return "zombie";
            case CLASS_SPONTANEOUS:
                return "spontaneous";
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
    public static void main(String[] args) {

        // FeedClassifier.classifyFeedInStore(FeedDatabase.getInstance());
        // System.exit(0);

        // System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.news-journalonline.com/atom.xml")));

        // final String className = fc.getClassName(fc.classify("http://feeds.nydailynews.com/nydnrss/news"));
        // final String className = fc.getClassName(fc.classify("http://feeds.gawker.com/lifehacker/full"));

        System.out
        .println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.artnewscentral.com/rss.php"))); //

        System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.oikotimes.com/v2/rss.php"))); // s
        System.out.println(FeedClassifier.getClassName(FeedClassifier
                .classify("http://www.gadgetsguru.in/rss/rss.aspx"))); //

        System.out.println(FeedClassifier.getClassName(FeedClassifier
                .classify("http://feeds.sophos.com/en/rss2_0-sophos-security-news.xml"))); //
        // sponanuous
        System.out.println(FeedClassifier.getClassName(FeedClassifier
                .classify("http://www.spacedaily.com/spacedaily.xml"))); //
        // sponanuous
        System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.speedtv.com/rss/"))); // sponanuous
        System.out.println(FeedClassifier.getClassName(FeedClassifier
                .classify("http://www.charitynavigator.org/feeds/featured.xml")));
        // // sponanuous
        System.out
        .println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.hindu.com/rss/01hdline.xml"))); //

        System.out.println(FeedClassifier.getClassName(FeedClassifier
                .classify("http://www.babygiftstoys.com/store/2183249/index.rss")));
        // // sponanuous
        System.out.println(FeedClassifier.getClassName(FeedClassifier
                .classify("http://feeds.feedburner.com/TexasStateNews")));

    }

}