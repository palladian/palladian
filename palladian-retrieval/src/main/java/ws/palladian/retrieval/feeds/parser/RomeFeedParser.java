package ws.palladian.retrieval.feeds.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.rome.RawDateModule;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.parser.XmlParser;

import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * <p>
 * The RomeFeedParser is responsible for parsing RSS and Atom feeds. We use Palladian's {@link HttpRetriever} for
 * downloading the feeds and ROME in conjunctions with Palladian's {@link XmlParser} for parsing the XML formats. This
 * class implements various fallback mechanisms for parsing problems caused by ROME or invalid feeds.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 * @see <a href="http://rometools.org/">The ROME Project</a>
 */
public class RomeFeedParser extends AbstractFeedParser {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RomeFeedParser.class);

    /** Whether to use additional date parsing techniques provided by Palladian. */
    private boolean useDateRecognition = true;

    /** If we cant find a pubdate in this many consecutive items, give up. */
    private static final int MAX_DATE_RETRIES = 5;

    // ///////////////////////////////////////////////////
    // RomeFeedParser API
    // ///////////////////////////////////////////////////

    
    /* (non-Javadoc)
     * @see ws.palladian.retrieval.feeds.parser.FeedParser#getFeed(org.w3c.dom.Document)
     */
    @Override
    public Feed getFeed(Document document) throws FeedParserException {
        SyndFeed syndFeed = buildSyndFeed(document);
        return getFeed(syndFeed, document.getDocumentURI());
    }
    
    @Override
    public Feed getFeed(InputStream inputStream) throws FeedParserException {
        SyndFeed syndFeed = getSyndFeed(inputStream);
        return getFeed(syndFeed, null);
    }

    // ///////////////////////////////////////////////////
    // Settings
    // ///////////////////////////////////////////////////

    public void setUseDateRecognition(boolean useDateRecognition) {
        this.useDateRecognition = useDateRecognition;
    }

    public boolean isUseDateRecognition() {
        return useDateRecognition;
    }

    // ///////////////////////////////////////////////////
    // private ROME specific methods
    // ///////////////////////////////////////////////////

    private SyndFeed getSyndFeed(InputStream inputStream) throws FeedParserException {
        try {
            DocumentParser xmlParser = ParserFactory.createXmlParser();
            Document document = xmlParser.parse(inputStream);
            SyndFeed syndFeed = buildSyndFeed(document);
            return syndFeed;
        } catch (ParserException e) {
            throw new FeedParserException(e);
        }
    }

    /**
     * Get feed information about a Atom/RSS feed, using ROME library.
     * 
     * @param feedUrl
     * @return
     * @throws FeedParserException
     * 
     */
    private Feed getFeed(SyndFeed syndFeed, String feedUrl) throws FeedParserException {

        Feed result = new Feed();

        // URL of the feed itself
        result.setFeedUrl(feedUrl);

        if (syndFeed.getLink() != null) {
            result.getMetaInformation().setSiteUrl(syndFeed.getLink().trim());
        }

        if (syndFeed.getTitle() != null && syndFeed.getTitle().length() > 0) {
            result.getMetaInformation().setTitle(syndFeed.getTitle().trim());
        }

        result.getMetaInformation().setLanguage(syndFeed.getLanguage());

        // get Feed items with ROME and assign to feed
        addFeedItems(result, syndFeed);

        Map<String, Object> additionalData = getAdditionalData(syndFeed);
        result.setAdditionalData(additionalData);

        return result;
    }

    /**
     * Add {@link FeedItem}s to the {@link Feed} from the specified {@link SyndFeed}.
     * 
     * @param feed
     * @param syndFeed
     * @return
     */
    private void addFeedItems(Feed feed, SyndFeed syndFeed) {

        @SuppressWarnings("unchecked")
        List<SyndEntry> syndEntries = syndFeed.getEntries();

        int dateRetries = 0;

        for (SyndEntry syndEntry : syndEntries) {

            FeedItem item = new FeedItem();

            String title = getEntryTitle(syndEntry);
            item.setTitle(title);

            String entryLink = getEntryLink(syndFeed, syndEntry);
            item.setLink(entryLink);

            String entryDescription = getEntryDescription(syndEntry);
            item.setDescription(entryDescription);

            String entryText = getEntryText(syndEntry);
            item.setText(entryText);

            String rawId = getEntryRawId(syndEntry);
            item.setRawId(rawId);

            String authors = getEntryAuthors(syndFeed, syndEntry);
            item.setAuthors(authors);

            // only try a certain amount of times to extract a pub date, if none is found don't keep trying
            if (dateRetries < MAX_DATE_RETRIES) {
                Date publishDate = getEntryPublishDate(syndEntry, item);
                if (publishDate == null) {
                    dateRetries++;
                } else {
                    item.setPublished(publishDate);
                    dateRetries = 0;
                }
            }

            Map<String, Object> additionalData = getAdditionalData(syndEntry);
            item.setAdditionalData(additionalData);
            
            feed.addItem(item);
        }
    }

    /**
     * Get link from {@link SyndEntry}, some feeds provide relative URLs, which we need to convert.
     * TODO also consider feed's URL here?
     * 
     * @param syndFeed
     * @param syndEntry
     * @return
     */
    private String getEntryLink(SyndFeed syndFeed, SyndEntry syndEntry) {
        String entryLink = syndEntry.getLink();
        if (entryLink != null && entryLink.length() > 0) {
            entryLink = entryLink.trim();
            entryLink = UrlHelper.makeFullUrl(syndFeed.getLink(), entryLink);
        }
        return entryLink;
    }

    /**
     * Get title from {@link SyndEntry}, remove HTML tags and unescape HTML entities from title.
     * 
     * @param syndEntry
     * @return
     */
    private String getEntryTitle(SyndEntry syndEntry) {
        String title = syndEntry.getTitle();
        if (title != null) {
            title = title.trim();
        }
        return title;
    }

    /**
     * Get text description from {@link SyndEntry}.
     * 
     * @param syndEntry
     * @return description, or <code>null</code> if no description.
     */
    private String getEntryDescription(SyndEntry syndEntry) {
        String description = null;
        if (syndEntry.getDescription() != null) {
            description = syndEntry.getDescription().getValue();
        }
        return description;
    }

    /**
     * Get text content from {@link SyndEntry}. ROME also considers RSS content module.
     * 
     * @see http://web.resource.org/rss/1.0/modules/content/
     * 
     * @param syndEntry
     * @return text content or <code>null</code> if no content found.
     */
    @SuppressWarnings("unchecked")
    private String getEntryText(SyndEntry syndEntry) {

        // I modified this method to return the *longest* text fragment which we can retrieve
        // from the feed item. -- Philipp, 2011-01-28.

        String entryText = null;
        List<SyndContent> contents = syndEntry.getContents();
        if (contents != null) {
            for (SyndContent content : contents) {
                String contentValue = content.getValue();
                if (contentValue != null && contentValue.length() != 0) {
                    if (entryText == null || contentValue.length() > entryText.length()) {
                        entryText = contentValue;
                    }
                }
            }
        }

        return entryText;
    }

    /**
     * Get ID from {@link SyndEntry}. This is the "raw" ID which is assigned in the feed itself, either as
     * <code>guid</code> element in RSS or as <code>id</code> element in Atom.
     * 
     * @param syndEntry
     * @return raw id or <code>null</code> if no id found
     */
    private String getEntryRawId(SyndEntry syndEntry) {

        String rawId = null;
        Object wireEntry = syndEntry.getWireEntry();

        if (wireEntry instanceof com.sun.syndication.feed.atom.Entry) {
            com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) wireEntry;
            rawId = atomEntry.getId();
        } else if (wireEntry instanceof com.sun.syndication.feed.rss.Item) {
            com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) wireEntry;
            Guid guid = rssItem.getGuid();
            if (guid != null) {
                rawId = guid.getValue();
            }
        }

        // we could not get the ID from the SyndEntry, so we take the link as identification instead
        if (rawId == null) {
            rawId = syndEntry.getLink();
            LOGGER.trace("id is missing, taking link instead");
        }

        // we could ultimately get no ID
        if (rawId == null) {
            LOGGER.debug("could not get id for entry");
        }

        return rawId;
    }

    /**
     * Get the publish or updated (Atom) date from {@link SyndEntry}. First, try to get published date from
     * {@link SyndEntry}, if there is none (or it couldn't be parsed), try to get the publish date. If ROME fails to
     * get either, try to get a date using Palladian's sophisticated date recognition techniques.
     * 
     * @param syndEntry
     * @param item
     * @return
     */
    private Date getEntryPublishDate(SyndEntry syndEntry, FeedItem item) {

        Date publishDate = syndEntry.getPublishedDate();

        // try to get updated date before entering the expensive search via XPath and DateGetterHelper
        // since atom feeds must have an updated field and may have an additional publish date.
        if (publishDate == null) {
            publishDate = syndEntry.getUpdatedDate();
        }

        // ROME library failed to get the date, use DateGetter, which allows to parse more date formats.
        // There are still some feeds with entries where the publish date cannot be parsed though,
        // see FeedDownloaderTest for a list of test cases.
        if (publishDate == null && useDateRecognition) {

            RawDateModule rawDateModule = (RawDateModule) syndEntry.getModule(RawDateModule.URI);
            String rawDate = null;
            if (rawDateModule != null) {
                rawDate = rawDateModule.getRawDate();
            }

            if (rawDate != null) {

                try {
                    ExtractedDate extractedDate = DateParser.findDate(rawDate);
                    if (extractedDate != null) {
                        publishDate = extractedDate.getNormalizedDate();
                        LOGGER.debug("found publish date in original feed file: " + publishDate);
                    }
                } catch (Throwable th) {
                    LOGGER.warn("date format could not be parsed correctly: " + rawDate + ", feed: "
                            + item.getFeedUrl() + ", " + th.getMessage());
                }
            }
        }

        return publishDate;
    }

    /**
     * Get author information from the supplied {@link SyndEntry}. If multiple authors are provided, all of them are
     * concatenated together using semicolons as separator. If the {@link SyndEntry} has no authors, the author data
     * from the {@link SyndFeed} is considered instead.
     * 
     * @param syndFeed
     * @param syndEntry
     * @return authors, or <code>null</code> if no authors provided.
     */
    @SuppressWarnings("unchecked")
    private String getEntryAuthors(SyndFeed syndFeed, SyndEntry syndEntry) {

        List<String> authors = new ArrayList<String>();

        // try to get authors as list
        List<SyndPerson> syndPersons = syndEntry.getAuthors();
        if (syndPersons != null) {
            for (SyndPerson syndPerson : syndPersons) {
                authors.add(syndPerson.getName().trim());
            }
        }

        // try to get author as single item
        String author = syndEntry.getAuthor();
        if (authors.isEmpty() && author != null && !author.isEmpty()) {
            authors.add(author.trim());
        }

        // if the entry provides no author data, try to get it from the feed

        if (authors.isEmpty()) {
            // LOGGER.debug("entry contains no author; trying to take from feed");
            List<SyndPerson> syndFeedPersons = syndFeed.getAuthors();
            if (syndFeedPersons != null) {
                for (SyndPerson syndPerson : syndFeedPersons) {
                    authors.add(syndPerson.getName().trim());
                }
            }
        }

        String feedAuthor = syndFeed.getAuthor();
        if (authors.isEmpty() && feedAuthor != null && !feedAuthor.isEmpty()) {
            authors.add(syndFeed.getAuthor().trim());
        }

        String result = null;
        if (authors.size() > 0) {
            result = StringUtils.join(authors, "; ");
        }

        return result;
    }

    /**
     * Possibility for subclasses to retrieve additional data from the {@link SyndEntry}. Override if necessary.
     * 
     * @param syndEntry
     * @return
     */
    protected Map<String, Object> getAdditionalData(SyndEntry syndEntry) {
        return new HashMap<String, Object>();
    }

    /**
     * Possibility for subclasses to retrieve additional data from the {@link SyndFeed}. Override if necessary.
     * 
     * @param syndFeed
     * @return
     */
    protected Map<String, Object> getAdditionalData(SyndFeed syndFeed) {
        return new HashMap<String, Object>();
    }

    /**
     * Builds a {@link SyndFeed} with ROME from the supplied {@link Document}.
     * 
     * @param feedDocument
     * @return
     * @throws FeedParserException
     */
    private SyndFeed buildSyndFeed(Document feedDocument) throws FeedParserException {

        try {

            SyndFeedInput feedInput = new SyndFeedInput();

            // this preserves the "raw" feed data and gives direct access to RSS/Atom specific elements see
            // http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
            feedInput.setPreserveWireFeed(true);

            SyndFeed syndFeed = feedInput.build(feedDocument);
            LOGGER.trace("feed type is " + syndFeed.getFeedType());

            return syndFeed;

        } catch (IllegalArgumentException e) {
            LOGGER.error("getRomeFeed " + feedDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new FeedParserException(e);
        } catch (FeedException e) {
            LOGGER.error("getRomeFeed " + feedDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new FeedParserException(e);
        }

    }

}