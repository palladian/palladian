package ws.palladian.retrieval.feeds.evaluation.gzPorcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.math.SizeUnit;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedClassificationThread;
import ws.palladian.retrieval.feeds.FeedClassifier;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedRetriever;
import ws.palladian.retrieval.feeds.FeedRetrieverException;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * The FeedClassifier classifies a feed in terms of their update intervals.
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class ExperimentalFeedClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ExperimentalFeedClassifier.class);

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

    /** All post entries are generated at request time (have publish timestamps) */
    public static final int CLASS_ON_THE_FLY = 9;

    /**
     * The maximum time span between the newest item in one post and the httpDate. Used to distinguish between
     * {@link #CLASS_ON_THE_FLY} and {@link #CLASS_CHUNKED}.
     */
    private static final Long OTF_MAX_DELAY = 2 * DateHelper.SECOND_MS;

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
            LOGGER.info(sw.getElapsedTimeString() + ", traffic: "
                    + DocumentRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES) + "MB");

            try {
                Thread.sleep(1 * DateHelper.MINUTE_MS);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }

        }

        LOGGER.info("classified " + feeds.size() + " feeds in " + sw.getElapsedTimeString() + ", traffic: "
                + DocumentRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES) + "MB");
    }

    /**
     * Classify a feed directly by its items. Make sure all properties such as windowSize httpDate are set.
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
    public static int classify(List<FeedItem> items) {
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
    public static int classify(Feed feed) {
        int feedClass = CLASS_UNKNOWN;

        FeedPostStatistics fps = new FeedPostStatistics(feed);

        // use rule based classification

        if (feed.getCorrectedItemTimestamps().size() == 0) {
            feedClass = CLASS_EMPTY;
        } else if (feed.getCorrectedItemTimestamps().size() == 1) {
            feedClass = CLASS_SINGLE_ENTRY;
        } else if (fps.isValidStatistics()) {

            // do special calculation for on-the-fly and chunked since we need to use the items' original publish
            // timestamps. Structure Map<httpDate_of_poll, List<item_publish_date>>

            // TODO: add pollTimestamp to FeedItem and use it if httpDate is not available
            // TODO: use windowSize to identify CHUNKED.

            // first, collect all new items per poll
            Map<Date, List<Long>> httpDateAndPollTimestamps = new HashMap<Date, List<Long>>();
            Map<Date, Integer> httpDateWindowSizes = new HashMap<Date, Integer>();
            for (FeedItem item : feed.getItems()) {
                // skip items with missing timestamps
                if (item.getHttpDate() == null || item.getPublished() == null) {
                    continue;
                }

                Date httpDate = item.getHttpDate();
                Long publishTimestamp = item.getPublished().getTime();

                httpDateWindowSizes.put(httpDate, item.getWindowSize());
                if (httpDateAndPollTimestamps.containsKey(httpDate)) {
                    httpDateAndPollTimestamps.get(httpDate).add(publishTimestamp);
                } else {
                    List<Long> newList = new ArrayList<Long>();
                    newList.add(publishTimestamp);
                    httpDateAndPollTimestamps.put(httpDate, newList);
                }
            }

            boolean otfCandidate = true; // feed is on-the-fly candidate
            boolean chunkedCandidate = true; //
            Long medianPostGapAllPolls = null;

            List<Long> medianPostGapPerPollList = new ArrayList<Long>();
            if (!httpDateAndPollTimestamps.isEmpty()) {

                // second, get median post gap per poll
                // iterate over polls
                for (Date httpDate : httpDateAndPollTimestamps.keySet()) {

                    // stop processing if feed is neither OTF, nor chunked
                    if (!chunkedCandidate && !otfCandidate) {
                        break;
                    }

                    List<Long> publishTimestampsPerPoll = httpDateAndPollTimestamps.get(httpDate);
                    Collections.sort(publishTimestampsPerPoll);
                    // medianPostGapPerPollList.add(MathHelper.getMedianDifference(publishTimestampsPerPoll));

                    // one requirement for chunked and OTF is that if a poll contains new items, the whole window is
                    // new.
                    // TODO: there is still a problem with feeds that update say weekly and set all item publish dates
                    // to the same value but contain overlapping windows. If a feed contains the top 10 movies and is
                    // updated on a weekly basis, a movie might be in subsequent windows. The current item duplicate
                    // detection ignores dates so in this case, the feed is not classified as chunked.
                    // -- Sandro 15.08.2011
                    // int windowSizeCurrentPoll = httpDateWindowSizes.get(httpDate);
                    // int numNewItemsCurrentPoll = publishTimestampsPerPoll.size();
                    // if (windowSizeCurrentPoll != numNewItemsCurrentPoll) {
                    // chunkedCandidate = false;
                    // otfCandidate = false;
                    // }

                    int windowSizeCurrentPoll = httpDateWindowSizes.get(httpDate);

                    if (windowSizeCurrentPoll > 1) {
                        medianPostGapPerPollList.add(MathHelper.getMedianDifference(publishTimestampsPerPoll));
                    }

                    // Check OTF feed. Once failed, do not check again.
                    if (otfCandidate) {
                        Long newesItem = Collections.max(publishTimestampsPerPoll);
                        Long timeToNewestItem = Math.abs(newesItem - httpDate.getTime());
                        if (timeToNewestItem > OTF_MAX_DELAY) {
                            otfCandidate = false;
                        }
                    }
                }

                // finally, get median post gap over all polls
                medianPostGapAllPolls = MathHelper.getMedianDifference(medianPostGapPerPollList);
            } else {
                otfCandidate = false;
                chunkedCandidate = false;
            }

            // if the post gap is 0 or extremely small and
            if (medianPostGapAllPolls != null && medianPostGapAllPolls < 1 * DateHelper.SECOND_MS
                    && (otfCandidate || chunkedCandidate)) {
                if (otfCandidate) {
                    feedClass = CLASS_ON_THE_FLY;
                } else {
                    feedClass = CLASS_CHUNKED;
                }
            } else {

                // if the last entry is a long time ago and the post gap was not that big, the feed is a zombie
                if (fps.getTimeDifferenceNewestPostToLastPollHttpDate() >= 8L * fps.getMedianPostGap()
                        && fps.getTimeDifferenceNewestPostToLastPollHttpDate() > 8L * DateHelper.WEEK_MS) {
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

        }

        return feedClass;
    }

    /**
     * Classify a feed by its given URL. Retrieves and classifies the feed. The retrieved feed is wasted
     * 
     * @param feedURL The URL of the feed.
     * @return The class of the feed.
     */
    public static int classify(String feedURL) {

        FeedRetriever feedRetriever = new FeedRetriever();
        Feed feed = new Feed();
        DocumentRetriever crawler = new DocumentRetriever();

        try {
            feed = feedRetriever.getFeed(feedURL);
        } catch (FeedRetrieverException e) {
            LOGGER.error("feed could not be found and classified, feedURL: " + feedURL + ", " + e.getMessage());

            if (crawler.getResponseCode(feedURL) == 200) {
                return CLASS_UNKNOWN;
            } else {
                return CLASS_DEAD;
            }
        }

        return classify(feed);
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

        final String className = FeedClassifier.getClassName(FeedClassifier
                .classify("http://absolutebailbond.com/feed/atom"));
        System.out.println(className);

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