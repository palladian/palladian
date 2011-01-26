package tud.iir.web.feeds;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;

// TODO introduce MIXED type?
// TODO create a trained classifier.
public class FeedContentClassifier {
    
    public static enum FeedContentType {
        
        UNDETERMINED(0), NONE(1), PARTIAL(2), FULL(3);
        
        private int identifier;

        private FeedContentType(int identifier) {
            this.identifier = identifier;
        }
        public int getIdentifier() {
            return identifier;
        }
        public static FeedContentType getByIdentifier(int identifier) {
            for (FeedContentType t : FeedContentType.values()) {
                if (t.getIdentifier() == identifier) {
                    return t;
                }
            }
            throw new NoSuchElementException("no content type with identifier " + identifier);
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedClassifier.class);

    private static final boolean DEBUG = false;

    private static final double SIMILARITY_THRESHOLD = 0.9;

    private static final int MAX_ERRORS = 5;

    private static final int ENTRIES_TO_CHECK = 20;

    private FeedDownloader feedDownloader;
    
    public FeedContentClassifier(FeedDownloader feedDownloader) {
        this.feedDownloader = feedDownloader;
    }

    /** Testing constructor. */
    FeedContentClassifier() {
        feedDownloader = new FeedDownloader();
    }

    public FeedContentType determineContentType(String feedUrl) {
        try {
            Feed feed = feedDownloader.getFeed(feedUrl);
            feedDownloader.scrapePages(feed.getItems());
            return determineContentType(feed);
        } catch (FeedDownloaderException e) {
            return FeedContentType.UNDETERMINED;
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
    public FeedContentType determineContentType(Feed feed) {
        LOGGER.trace(">determineFeedTextType " + feed);

        // count iterations
        int count = 0;
        // count types
        int none = 0, partial = 0, full = 0;
        // count # errors
        int errors = 0;

        // check max. 20 feed entries.
        // stop analyzing if we have more than 5 errors
        Iterator<FeedItem> entryIterator = feed.getItems().iterator();
        while (entryIterator.hasNext() && count < ENTRIES_TO_CHECK && errors < MAX_ERRORS) {
            FeedItem entry = entryIterator.next();

            String entryLink = entry.getLink();

            // TODO neccessary?
            if (entryLink == null || entryLink.length() == 0) {
                continue;
            }

            LOGGER.trace("checking " + entryLink);

            String entryText = entry.getItemText();
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
            float lengthSim = StringHelper.getLengthSim(entryText, pageText);

            // only compare text similarities, if lengths of texts do not differ too much
            if (lengthSim >= SIMILARITY_THRESHOLD) {

                // if text from feed entry and from web page are very
                // similar, we can assume that we have a full text feed
                float textSim = StringHelper.getLevenshteinSim(entryText, pageText);
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
        FeedContentType result = FeedContentType.PARTIAL;
        if (feed.getItems().isEmpty()) {
            result = FeedContentType.UNDETERMINED;
        } else if ((float) full / count >= 0.6) {
            result = FeedContentType.FULL;
        } else if ((float) none / count >= 0.8) {
            result = FeedContentType.NONE;
        } else if (count == 0) {
            result = FeedContentType.UNDETERMINED;
        }

        LOGGER.debug("feed " + feed.getFeedUrl() + " none:" + none + " partial:" + partial + " full:" + full + " -> "
                + result);

        LOGGER.trace("<determineFeedTextType " + result);
        return result;
    }

//    public String getReadableFeedTextType(FeedContentType t) {
//        switch (t) {
//            case FULL:
//                return "full";
//            case NONE:
//                return "none";
//            case PARTIAL:
//                return "partial";
//            case UNDETERMINED:
//                return "undetermined";
//            default:
//                return "<invalid>";
//        }
//
//    }

    public static void main(String[] args) {

        FeedContentClassifier feedContentClassifier = new FeedContentClassifier();
        FeedContentType type = feedContentClassifier.determineContentType("http://daringfireball.net/index.xml");
        System.out.println(type);

    }

}
