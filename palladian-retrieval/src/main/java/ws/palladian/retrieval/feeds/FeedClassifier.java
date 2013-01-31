package ws.palladian.retrieval.feeds;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedClassifier.class);

    public static void classifyFeedInStore(final FeedStore feedStore) {
        List<Feed> feeds = feedStore.getFeeds();

        StopWatch sw = new StopWatch();
        LOGGER.info("start classifying " + feeds.size() + " feeds");

        ExecutorService threadPool = Executors.newFixedThreadPool(500);

        for (final Feed feed : feeds) {
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
     * @param feedUrl The URL of the feed.
     * @return The class of the feed.
     */
    public static FeedActivityPattern classify(String feedUrl) {
        FeedActivityPattern ret;

        try {
            HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
            HttpResult httpResult = retriever.httpGet(feedUrl);
            
            FeedParser feedParser = new RomeFeedParser();
            Feed feed = feedParser.getFeed(httpResult);
            ret = classify(feed);
            
            
        } catch (HttpException e) {
            LOGGER.error("Feed could not be downloaded: " + feedUrl + ", " + e.getMessage());
            ret = FeedActivityPattern.CLASS_DEAD;
        } catch (FeedParserException e) {
            LOGGER.error("Feed could not be parsed: " + feedUrl + ", " + e.getMessage());
            ret = FeedActivityPattern.CLASS_UNKNOWN;
        }
        return ret;
    }


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