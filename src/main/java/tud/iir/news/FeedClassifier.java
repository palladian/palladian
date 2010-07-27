package tud.iir.news;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
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
    public static final int CLASS_SPONTANUOUS = 5;

    /** Feed posts are done at daytime with a longer gap at night. */
    public static final int CLASS_SLICED = 6;

    /** Feed posts are 24/7 at a similar interval. */
    public static final int CLASS_CONSTANT = 7;

    /** all posts in the feed are updated together at a certain time */
    public static final int CLASS_CHUNKED = 8;

    /** all post entries are generated at request time */
    public static final int CLASS_ON_THE_FLY = 9;

    /** FIXME delete when merging!!! */
    public static double totalKB = 0;
    public static long totalFeedEntries = 0;
    public static long totalFeeds = 0;

    /** FIXME delete when merging!!! */
    public static synchronized void addMetaInformation(double kb, int feedEntries) {
        totalKB += kb;
        totalFeedEntries += feedEntries;
        totalFeeds++;
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
            ThreadHelper.sleep(1 * DateHelper.MINUTE_MS);

        }

        LOGGER.info("classified " + feeds.size() + " feeds in " + sw.getElapsedTimeString() + ", traffic: "
                + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + "MB");

        // FIXME delete when merging!!!
        LOGGER.info("totalKB of feeds: " + totalKB + ", average feed size: " + totalKB / totalFeeds);
        LOGGER.info("total feed entries: " + totalFeedEntries + ", average feed entries: " + totalFeedEntries
                / (double) totalFeeds);
        LOGGER.info("total feeds: " + totalFeeds);
    }

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

        List<FeedEntry> entries = new ArrayList<FeedEntry>();
        FeedPostStatistics fps = null;

        // check if feed is not accessible, try 5 times
        Crawler crawler = new Crawler();

            try {

                newsAggregator.setUseScraping(false);
                entries = newsAggregator.getFeed(feedURL).getEntries();
                fps = new FeedPostStatistics(entries);

                LOGGER.debug(fps);

            } catch (FeedAggregatorException e) {
                LOGGER.error("feed could not be found and classified, feedURL: " + feedURL + ", " + e.getMessage());
                
                if (crawler.getResponseCode(feedURL) == 200) {
                    return CLASS_UNKNOWN;
                } else {
                    return CLASS_DEAD;
                }
            }



        // // use rule based classification

        if (!fps.isValidStatistics() && entries.size() > 0) {
            feedClass = CLASS_UNKNOWN;
        } else if (entries.size() == 0) {
            feedClass = CLASS_EMPTY;
        } else if (entries.size() == 1) {
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
                    feedClass = CLASS_SPONTANUOUS;
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
    public static void main(String[] args) {

//        FeedClassifier.classifyFeedInStore(FeedDatabase.getInstance());
//        System.exit(0);

//        System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.news-journalonline.com/atom.xml")));

        // final String className = fc.getClassName(fc.classify("http://feeds.nydailynews.com/nydnrss/news"));
        // final String className = fc.getClassName(fc.classify("http://feeds.gawker.com/lifehacker/full"));

         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.artnewscentral.com/rss.php"))); //

         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.oikotimes.com/v2/rss.php"))); //s
         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.gadgetsguru.in/rss/rss.aspx"))); //

         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://feeds.sophos.com/en/rss2_0-sophos-security-news.xml"))); //
         // sponanuous
         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.spacedaily.com/spacedaily.xml"))); //
         // sponanuous
         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.speedtv.com/rss/"))); // sponanuous
         System.out.println(FeedClassifier.getClassName(FeedClassifier
         .classify("http://www.charitynavigator.org/feeds/featured.xml")));
         // // sponanuous
         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.hindu.com/rss/01hdline.xml"))); //

         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://www.babygiftstoys.com/store/2183249/index.rss")));
         // // sponanuous
         System.out.println(FeedClassifier.getClassName(FeedClassifier.classify("http://feeds.feedburner.com/TexasStateNews")));


    }

}