package tud.iir.news;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.web.Crawler;

// TODO introduce MIXED type?
public class FeedContentClassifier {

    private static final Logger LOGGER = Logger.getLogger(FeedClassifier.class);
    
    private static final double SIMILARITY_THRESHOLD = 0.9;
    
    private static final int MAX_ERRORS = 5;

    private static final int PAGES_TO_CHECK = 20;
    

    private Crawler crawler = new Crawler();

    private PageContentExtractor extractor = new PageContentExtractor();
    
    private NewsAggregator newsAggregator;
    
    public FeedContentClassifier() {
        newsAggregator = new NewsAggregator();
    }
    
    public FeedContentClassifier(FeedStore store) {
        newsAggregator = new NewsAggregator(store);
    }

    public int determineFeedTextType(String feedUrl) {
        try {
            Feed feed = newsAggregator.downloadFeed(feedUrl);
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
        while (entryIterator.hasNext() && count < PAGES_TO_CHECK && errors < MAX_ERRORS) {
            FeedEntry entry = entryIterator.next();

            String entryLink = entry.getLink();

            if (entryLink == null || entryLink.length() == 0) {
                continue;
            }

            // check type of linked file; ignore audio, video or pdf files ...
            String fileType = FileHelper.getFileType(entryLink);
            if (FileHelper.isAudioFile(fileType) || FileHelper.isVideoFile(fileType) || fileType.equals("pdf")) {
                LOGGER.debug("ignoring filetype " + fileType + " from " + entryLink);
                continue;
            }

            LOGGER.trace("checking " + entryLink);

            // entry contains no text at all
            String entryText = entry.getContent();
            if (entryText == null || entryText.length() == 0) {
                LOGGER.debug("entry " + entryLink + " contains no text");
                none++;
                count++;
                continue;
            }
            
            entryText = HTMLHelper.removeHTMLTags(entryText, true, true, true, true);
            entryText = StringEscapeUtils.unescapeHtml(entryText);
            
            // get text content from associated web page using
            // PageContentExtractor and compare with text we got from the feed
            try {

                InputStream inputStream = crawler.downloadInputStream(entryLink);
                extractor.setDocument(new InputSource(inputStream));

                Document pageContent = extractor.getResultDocument();
                String pageText = Helper.xmlToString(pageContent);
                pageText = HTMLHelper.removeHTMLTags(pageText, true, true, true, true);
                pageText = StringEscapeUtils.unescapeHtml(pageText);

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

            } catch (MalformedURLException e) {
                LOGGER.error("determineFeedTextType " + entryLink + " " + e.toString() + " " + e.getMessage());
                errors++;
            } catch (IOException e) {
                LOGGER.error("determineFeedTextType " + entryLink + " " + e.toString() + " " + e.getMessage());
                errors++;
            } catch (Exception e) {
                // FIXME in some rare cases PageContentExtractor throws a NPE,
                // I dont know yet where the problem lies, so we catch it here
                // and move an as if nothing happened :)
                LOGGER.error("determineFeedTextType " + entryLink + " " + e.toString() + " " + e.getMessage());
                errors++;
            }
        }

        // determine type of feed by using some simple heuristics ..:
        // if feed has no entries -> we cannot determine the type
        // if more than 60 % of feed's entries contain full text -> assume full text
        // if more than 80 % of feed's entries contain no text -> assume no text
        // else --> assume partial text
        int result = Feed.TEXT_TYPE_PARTIAL;
        String resultStr = "partial";
        if (feed.getEntries().isEmpty()) {
            result = Feed.TEXT_TYPE_UNDETERMINED;
            resultStr = "undetermined, feed has no entries";
        } else if ((float) full / count >= 0.6) {
            result = Feed.TEXT_TYPE_FULL;
            resultStr = "full";
        } else if ((float) none / count >= 0.8) {
            result = Feed.TEXT_TYPE_NONE;
            resultStr = "none";
        } else if (count == 0) {
            result = Feed.TEXT_TYPE_UNDETERMINED;
            resultStr = "undetermined, could not check entries";
        }

        LOGGER.info("feed " + feed.getFeedUrl() + " none:" + none + " partial:" + partial + " full:" + full + " -> "
                + resultStr);

        LOGGER.trace("<determineFeedTextType " + result);
        return result;
    }

    public String getReadableFeedTextType(int i) {
        switch (i) {
            case Feed.TEXT_TYPE_FULL:
                return "full text";
            case Feed.TEXT_TYPE_NONE:
                return "no text";
            case Feed.TEXT_TYPE_PARTIAL:
                return "partial text";
            case Feed.TEXT_TYPE_UNDETERMINED:
                return "undetermined";
            default:
                return "<invalid>";
        }

    }

    public static void main(String[] args) {

    }

}
