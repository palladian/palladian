package tud.iir.news;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;

// TODO introduce MIXED type?
public class FeedContentClassifier {

    private static final Logger LOGGER = Logger.getLogger(FeedClassifier.class);

    private static final boolean DEBUG = false;

    private static final double SIMILARITY_THRESHOLD = 0.9;

    private static final int MAX_ERRORS = 5;

    private static final int ENTRIES_TO_CHECK = 20;

    private NewsAggregator newsAggregator;

    public FeedContentClassifier() {
        newsAggregator = new NewsAggregator();
    }

    public FeedContentClassifier(FeedStore store) {
        newsAggregator = new NewsAggregator(store);
    }

    public int determineFeedTextType(String feedUrl) {
        try {
            Feed feed = newsAggregator.downloadFeed(feedUrl, true);
            return determineFeedTextType(feed);
        } catch (NewsAggregatorException e) {
            return -1;
        }
    }

    /**
     * Try to determine the extent of text within a feed. We distinguish between no text {@link Feed#TEXT_TYPE_NONE},
     * partial text {@link Feed#TEXT_TYPE_PARTIAL} and full text {@link Feed#TEXT_TYPE_FULL}.
     * 
     * @param syndFeed
     * @param feedUrl
     * @return
     */
    public int determineFeedTextType(Feed feed) {
        LOGGER.trace(">determineFeedTextType " + feed);

        // count iterations
        int count = 0;
        // count types
        int none = 0, partial = 0, full = 0;
        // count # errors
        int errors = 0;

        // check max. 20 feed entries.
        // stop analyzing if we have more than 5 errors
        Iterator<FeedEntry> entryIterator = feed.getEntries().iterator();
        while (entryIterator.hasNext() && count < ENTRIES_TO_CHECK && errors < MAX_ERRORS) {
            FeedEntry entry = entryIterator.next();

            String entryLink = entry.getLink();

            if (entryLink == null || entryLink.length() == 0) {
                continue;
            }

            LOGGER.trace("checking " + entryLink);

            String entryText = entry.getEntryText();
            if (entryText == null || entryText.length() == 0) {
                LOGGER.debug("entry " + entryLink + " contains no text");
                none++;
                count++;
                continue;
            }

            String pageText = entry.getPageText();
            if (pageText == null) {
                pageText = "";
            }

            if (DEBUG) {
                try {
                    FileHelper.appendFile("data/temp/fccDebug.txt", "=========================\n");
                    FileHelper.appendFile("data/temp/fccDebug.txt", entryText + "\n");
                    FileHelper.appendFile("data/temp/fccDebug.txt", "-------------------------\n");
                    FileHelper.appendFile("data/temp/fccDebug.txt", pageText + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // first, calculate a similarity based solely on text lengths
            float lengthSim = Helper.getLengthSim(entryText, pageText);

            // only compare text similarities, if lengths of texts do not differ too much
            if (lengthSim >= SIMILARITY_THRESHOLD) {

                // if text from feed entry and from web page are very
                // similar, we can assume that we have a full text feed
                float textSim = Helper.getLevenshteinSim(entryText, pageText);
                if (textSim >= 0.9) {
                    LOGGER.debug("entry " + entryLink + " seems to contain full text (textSim:" + textSim + ")");
                    full++;
                    count++;
                    continue;
                }
            }

            // feed and page were not similar enough, looks like partial text feed
            LOGGER.debug("entry " + entryLink + " seems to contain partial text (lengthSim:" + lengthSim + ")");
            partial++;
            count++;

        }

        // determine type of feed by using some simple heuristics ..:
        // if feed has no entries -> we cannot determine the type
        // if more than 60 % of feed's entries contain full text -> assume full text
        // if more than 80 % of feed's entries contain no text -> assume no text
        // else --> assume partial text
        int result = Feed.TEXT_TYPE_PARTIAL;
        if (feed.getEntries().isEmpty()) {
            result = Feed.TEXT_TYPE_UNDETERMINED;
        } else if ((float) full / count >= 0.6) {
            result = Feed.TEXT_TYPE_FULL;
        } else if ((float) none / count >= 0.8) {
            result = Feed.TEXT_TYPE_NONE;
        } else if (count == 0) {
            result = Feed.TEXT_TYPE_UNDETERMINED;
        }

        LOGGER.debug("feed " + feed.getFeedUrl() + " none:" + none + " partial:" + partial + " full:" + full + " -> "
                + getReadableFeedTextType(result));

        LOGGER.trace("<determineFeedTextType " + result);
        return result;
    }

    public String getReadableFeedTextType(int i) {
        switch (i) {
            case Feed.TEXT_TYPE_FULL:
                return "full";
            case Feed.TEXT_TYPE_NONE:
                return "none";
            case Feed.TEXT_TYPE_PARTIAL:
                return "partial";
            case Feed.TEXT_TYPE_UNDETERMINED:
                return "undetermined";
            default:
                return "<invalid>";
        }

    }

    public static void main(String[] args) {

        FeedContentClassifier feedContentClassifier = new FeedContentClassifier();
        int type = feedContentClassifier.determineFeedTextType("http://daringfireball.net/index.xml");
        System.out.println(feedContentClassifier.getReadableFeedTextType(type));

    }

}
