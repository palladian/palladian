package ws.palladian.retrieval.feeds.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import uk.org.catnip.eddie.Entry;
import uk.org.catnip.eddie.FeedData;
import uk.org.catnip.eddie.parser.Parser;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;

/**
 * <p>
 * Wrapper for Eddie feed parser. In comparison to Rome, Eddie is more reliable, when parsing ill-formed feeds. Quick
 * implementation, needs refinement.
 * </p>
 * 
 * @author Philipp Katz
 * @see http://www.davidpashley.com/projects/eddie.html
 */
public final class EddieFeedParser extends AbstractFeedParser implements FeedParser {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddieFeedParser.class);

    @Override
    public Feed getFeed(Document document) throws FeedParserException {
        throw new UnsupportedOperationException("Eddie is SAX based, therefore no Documents can be processed.");
    }

    @Override
    public Feed getFeed(InputStream inputStream) throws FeedParserException {
        Parser parser = new Parser();
        FeedData feedData;
        try {
            feedData = parser.parse(inputStream);
        } catch (IOException e) {
            throw new FeedParserException(e);
        }

        Feed feed = new Feed();
        feed.getMetaInformation().setTitle(feedData.getTitle().getValue());

        @SuppressWarnings("unchecked")
        Iterator<Entry> entryIterator = feedData.entries();
        while (entryIterator.hasNext()) {
            Entry entry = entryIterator.next();
            FeedItem feedItem = new FeedItem();
            feedItem.setFeed(feed);

            feedItem.setDescription(entry.getSummary().getValue());
            feedItem.setPublished(entry.getModified());
            feedItem.setRawId(entry.get("guid"));
            feedItem.setTitle(entry.getTitle().getValue());
            feedItem.setLink(entry.get("link"));

            Date publishDate = entry.getCreated();
            String rawDate = entry.get("modified");
            if (publishDate == null) {
                publishDate = entry.getModified();
            }
            try {
                ExtractedDate extractedDate = DateParser.findDate(rawDate);
                if (extractedDate != null) {
                    publishDate = extractedDate.getNormalizedDate();
                    LOGGER.debug("found publish date in original feed file: " + publishDate);
                }
            } catch (Throwable th) {
                LOGGER.warn("date format could not be parsed correctly: " + rawDate + ", feed: "
                        + feedItem.getFeedUrl() + ", " + th.getMessage());
            }
            feedItem.setPublished(publishDate);

            feed.addItem(feedItem);
        }
        return feed;
    }

    public static void main(String[] args) throws FeedParserException {
        FeedParser feedParser = new EddieFeedParser();
        Feed feed = feedParser.getFeed("http://rss.cnn.com/rss/edition.rss");
        CollectionHelper.print(feed.getItems());
    }

}
