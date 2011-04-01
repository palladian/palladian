package ws.palladian.retrieval.feeds;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.preprocessing.scraping.PageContentExtractor;
import ws.palladian.preprocessing.scraping.PageContentExtractorException;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.DocumentRetrieverCallback;
import ws.palladian.retrieval.HeaderInformation;

import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * The FeedRetriever is responsible for fetching RSS and Atom feeds. We use Palladians {@link DocumentRetriever} for downloading
 * the feeds and ROME for parsing the XML formats. This class implements various fallback mechanisms for parsing
 * problems caused by ROME or invalid feeds. This class also includes capabilities, to scrape links feed items, to fetch
 * additional content.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 * @see https://rome.dev.java.net/
 */
public class FeedRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedRetriever.class);

    /** Used for all downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * If enabled, we use the last poll time and the last ETag of the feed as HTTP headers when requesting the URL. This
     * way we can save bandwidth for feeds that support the HTTP 304 "Not modified" status code (about 83% of all feeds
     * do support either ETag or LastModifiedSince).
     */
    private boolean useBandwidthSavingHTTPHeaders = false;

    /** Whether to use additional date parsing techniques provided by Palladian. */
    private boolean useDateRecognition = true;

    /**
     * Whether to clean strings like text and title from feed's items; this means strip out HTML tags and entities. If
     * disabled, the raw content from the feed is aggregated without further treatment.
     */
    private boolean cleanStrings = true;

    public FeedRetriever() {
        // suXXX that I have to set this explicitly;
        // makes sense to have this setting for Neko,
        // but ROME generally has no problem with too big files ...
        // think this over?
        crawler.getDownloadFilter().setMaxFileSize(-1);
    }

    // ///////////////////////////////////////////////////
    // FeedRetriever API
    // ///////////////////////////////////////////////////

    /**
     * Download a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @return
     * @throws FeedRetrieverException
     */
    public Feed getFeed(String feedUrl) throws FeedRetrieverException {
        return getFeed(feedUrl, false, null);
    }

    /**
     * Download a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @param scrapePages set to <code>true</code>, to scrape the contents from the corresponding pages of each
     *            {@link FeedItem}.
     * @return
     * @throws FeedRetrieverException
     */
    public Feed getFeed(String feedUrl, boolean scrapePages) throws FeedRetrieverException {
        return getFeed(feedUrl, scrapePages, null);
    }

    /**
     * Download a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @param headerInformation header information containing ETag/lastModifiedSince data
     * @return
     * @throws FeedRetrieverException
     */
    public Feed getFeed(String feedUrl, HeaderInformation headerInformation) throws FeedRetrieverException {
        return getFeed(feedUrl, false, headerInformation);
    }

    /**
     * Downloads a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @param scrapePages set to <code>true</code>, to scrape the contents from the corresponding pages of each
     *            {@link FeedItem}.
     * @param headerInformation header information containing ETag/lastModifiedSince data.
     * @return
     * @throws FeedRetrieverException
     */
    public Feed getFeed(String feedUrl, boolean scrapePages, HeaderInformation headerInformation)
    throws FeedRetrieverException {

        StopWatch sw = new StopWatch();

        Document feedDocument = downloadFeedDocument(feedUrl, headerInformation);
        Feed feed = getFeed(feedDocument);

        if (scrapePages) {
            scrapePages(feed.getItems());
        }

        LOGGER.debug("downloaded feed from " + feedUrl + " in " + sw.getElapsedTimeString());
        return feed;

    }

    /**
     * Returns a feed from the specified Document.
     * 
     * @param document the Document containing the RSS or Atom feed.
     * @return
     * @throws FeedRetrieverException
     */
    public Feed getFeed(Document document) throws FeedRetrieverException {
        return getFeedWithRome(document);
    }

    /**
     * Updates the supplied {@link Feed} with new items. This means, the existing items (if any) are replaced by current
     * items downloaded from web.
     * 
     * @param feed
     * @throws FeedRetrieverException
     */
    public void updateFeed(Feed feed) throws FeedRetrieverException {
        Feed downloadedFeed = getFeed(feed.getFeedUrl());
        feed.setItems(downloadedFeed.getItems());
        feed.setDocument(downloadedFeed.getDocument());
    }

    /**
     * Fetch associated page content for specified {@link FeedItem}s. Do not download binary files, like PDFs, audio or
     * video files. Downloading is done concurrently.
     * 
     * @param feedItems the FeedItems to scrape.
     */
    public void scrapePages(List<FeedItem> feedItems) {
        LOGGER.debug("downloading " + feedItems.size() + " pages");

        DocumentRetriever downloader = new DocumentRetriever();
        downloader.setMaxThreads(5);

        final Map<String, FeedItem> entries = new HashMap<String, FeedItem>();

        for (FeedItem feedEntry : feedItems) {
            String entryLink = feedEntry.getLink();

            if (entryLink == null) {
                continue;
            }

            // check type of linked file; ignore audio, video or pdf files ...
            String fileType = FileHelper.getFileType(entryLink);
            boolean ignore = FileHelper.isAudioFile(fileType) || FileHelper.isVideoFile(fileType)
            || fileType.equals("pdf");
            if (ignore) {
                LOGGER.debug("ignoring filetype " + fileType + " from " + entryLink);
            } else {
                downloader.add(feedEntry.getLink());
                entries.put(feedEntry.getLink(), feedEntry);
            }
        }

        downloader.start(new DocumentRetrieverCallback() {
            @Override
            public void finished(String url, InputStream inputStream) {
                try {
                    PageContentExtractor extractor = new PageContentExtractor();
                    extractor.setDocument(new InputSource(inputStream));
                    String pageText = extractor.getResultText();
                    entries.get(url).setPageText(pageText);
                } catch (PageContentExtractorException e) {
                    LOGGER.error("PageContentExtractorException " + e);
                }
            }
        });
        LOGGER.debug("finished downloading");
    }

    // ///////////////////////////////////////////////////
    // Settings
    // ///////////////////////////////////////////////////

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * 
     * @param useBandwidthSavingHTTPHeaders
     */
    public void setUseBandwidthSavingHTTPHeaders(boolean useBandwidthSavingHTTPHeaders) {
        this.useBandwidthSavingHTTPHeaders = useBandwidthSavingHTTPHeaders;
    }

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * 
     * @return
     */
    public boolean isUseBandwidthSavingHTTPHeaders() {
        return useBandwidthSavingHTTPHeaders;
    }

    public void setUseDateRecognition(boolean useDateRecognition) {
        this.useDateRecognition = useDateRecognition;
    }

    public boolean isUseDateRecognition() {
        return useDateRecognition;
    }

    public void setCleanStrings(boolean cleanStrings) {
        this.cleanStrings = cleanStrings;
    }

    public boolean isCleanStrings() {
        return cleanStrings;
    }

    // ///////////////////////////////////////////////////
    // private ROME specific methods
    // ///////////////////////////////////////////////////

    /**
     * Get feed information about a Atom/RSS feed, using ROME library.
     * 
     * @param feedUrl
     * @return
     * @throws FeedRetrieverException
     */
    private Feed getFeedWithRome(Document feedDocument) throws FeedRetrieverException {

        Feed result = new Feed();

        SyndFeed syndFeed = buildSyndFeed(feedDocument);

        // URL of the feed itself
        String feedUrl = feedDocument.getDocumentURI();
        result.setFeedUrl(feedUrl);

        if (syndFeed.getLink() != null) {
            result.setSiteUrl(syndFeed.getLink().trim());
        }

        if (syndFeed.getTitle() != null && syndFeed.getTitle().length() > 0) {
            result.setTitle(syndFeed.getTitle().trim());
        } else {
            // fallback, use feedUrl as title
            result.setTitle(feedUrl);
        }

        result.setLanguage(syndFeed.getLanguage());

        // store the originating document in the feed
        result.setDocument(feedDocument);

        // get Feed items with ROME and assign to feed
        addFeedItems(result, syndFeed);

        // get the size of the feed
        if (feedDocument != null) {
            result.setByteSize(HTMLHelper.documentToHTMLString(feedDocument).getBytes().length);
        }

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

        for (SyndEntry syndEntry : syndEntries) {

            FeedItem item = new FeedItem();
            feed.addItem(item);

            String title = getEntryTitle(syndEntry);
            item.setTitle(title);

            String entryLink = getEntryLink(syndFeed, syndEntry);
            item.setLink(entryLink);

            String entryDescription = getEntryDescription(syndEntry);
            item.setItemDescription(entryDescription);

            String entryText = getEntryText(syndEntry);
            item.setItemText(entryText);

            String rawId = getEntryRawId(syndEntry);
            item.setRawId(rawId);

            String authors = getEntryAuthors(syndFeed, syndEntry);
            item.setAuthors(authors);

            // TODO only try a certain amount of times to extract a pub date, if none is found don't keep trying
            Date publishDate = getEntryPublishDate(syndEntry, item);
            item.setPublished(publishDate);
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
            entryLink = UrlHelper.makeFullURL(syndFeed.getLink(), entryLink);
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
        return cleanup(syndEntry.getTitle());
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
                    String contentText = cleanup(contentValue);
                    if (entryText == null || contentText.length() > entryText.length()) {
                        entryText = contentText;
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
            LOGGER.debug("id is missing, taking link instead");
        }

        // we could ultimately get no ID
        if (rawId == null) {
            LOGGER.debug("could not get id for entry");
        }

        return rawId;
    }

    /**
     * Get the publish date from {@link SyndEntry}. If ROME fails to parse the {@link SyndEntry}, try to get the date
     * using Palladian's sophisticated date recognition techniques.
     * 
     * @param syndEntry
     * @param item
     * @return
     */
    private Date getEntryPublishDate(SyndEntry syndEntry, FeedItem item) {

        Date publishDate = syndEntry.getPublishedDate();

        // ROME library failed to get the date, use DateGetter, which allows to parse more date formats.
        // There are still some feeds with entries where the publish date cannot be parsed though,
        // see FeedDownloaderTest for a list of test cases.
        if (publishDate == null && useDateRecognition) {

            Node node = item.getNode();
            if (node != null) {

                Node dateNode = XPathHelper.getChildNode(node, "*[contains(name(),'date') or contains(name(),'Date')]");
                if (dateNode != null) {

                    ExtractedDate extractedDate = DateGetterHelper.findDate(dateNode.getTextContent());
                    if (extractedDate != null) {
                        try {
                            publishDate = extractedDate.getNormalizedDate();
                            LOGGER.debug("found publish date in original feed file: " + publishDate);
                        } catch (Exception e) {
                            LOGGER.warn("date format could not be parsed correctly: " + dateNode + ", feed: "
                                    + item.getFeedUrl() + ", " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (publishDate == null) {
            // as a last resort, use the entry's updated date
            publishDate = syndEntry.getUpdatedDate();
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
                authors.add(syndPerson.getName());
            }
        }

        // try to get author as single item
        String author = syndEntry.getAuthor();
        if (authors.isEmpty() && author != null && !author.isEmpty()) {
            authors.add(author);
        }

        // if the entry provides no author data, try to get it from the feed

        if (authors.isEmpty()) {
            LOGGER.debug("entry contains no author; trying to take from feed");
            List<SyndPerson> syndFeedPersons = syndFeed.getAuthors();
            if (syndFeedPersons != null) {
                for (SyndPerson syndPerson : syndFeedPersons) {
                    authors.add(syndPerson.getName());
                }
            }
        }

        String feedAuthor = syndFeed.getAuthor();
        if (authors.isEmpty() && feedAuthor != null && !feedAuthor.isEmpty()) {
            authors.add(syndFeed.getAuthor());
        }

        String result = null;
        if (authors.size() > 0) {
            result = StringUtils.join(authors, "; ");
        }

        return result;
    }

    /**
     * Download a feed document from the web.
     * 
     * @param feedUrl
     * @param headerInformation
     * @return
     * @throws FeedRetrieverException
     */
    private Document downloadFeedDocument(String feedUrl, HeaderInformation headerInformation)
    throws FeedRetrieverException {

        Document feedDocument = crawler.getXMLDocument(feedUrl, false, headerInformation);
        if (feedDocument == null) {
            throw new FeedRetrieverException("could not get document from " + feedUrl);
            // if (crawler.getLastResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
            // // TODO
            // } else {
            // // TODO return cached document here (from disk or database)
            // LOGGER.debug("the feed was not modified: " + feedUrl);
            // return null;
            // }
        }

        return feedDocument;
    }

    /**
     * Builds a {@link SyndFeed} with ROME from the supplied {@link Document}.
     * 
     * @param feedDocument
     * @return
     * @throws FeedRetrieverException
     */
    private SyndFeed buildSyndFeed(Document feedDocument) throws FeedRetrieverException {

        try {

            SyndFeedInput feedInput = new SyndFeedInput();

            // this preserves the "raw" feed data and gives direct access to RSS/Atom specific elements see
            // http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
            feedInput.setPreserveWireFeed(true);

            SyndFeed syndFeed = feedInput.build(feedDocument);

            return syndFeed;

        } catch (IllegalArgumentException e) {
            LOGGER.error("getRomeFeed " + feedDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new FeedRetrieverException(e);
        } catch (FeedException e) {
            LOGGER.error("getRomeFeed " + feedDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new FeedRetrieverException(e);
        }

    }

    /**
     * Clean up method to strip out undesired characters from feed text contents, like HTML tags and escaped entities.
     * 
     * @param dirty
     * @return
     */
    private String cleanup(String dirty) {
        String result = null;
        if (cleanStrings) {
            if (dirty != null) {
                // TODO this causes trouble with special and foreign characters
                result = HTMLHelper.documentToReadableText(dirty, false);
                result = StringEscapeUtils.unescapeHtml(result);
                result = result.trim();
            }
        } else {
            result = dirty;
        }
        return result;
    }

    /**
     * Print feed with content in human readable form.
     * 
     * @param feed
     */
    public static void printFeed(Feed feed, boolean includeText) {

        StringBuilder sb = new StringBuilder();

        sb.append(feed.getTitle()).append("\n");
        sb.append("feedUrl : ").append(feed.getFeedUrl()).append("\n");
        sb.append("siteUrl : ").append(feed.getSiteUrl()).append("\n");
        sb.append("-----------------------------------").append("\n");
        List<FeedItem> items = feed.getItems();
        if (items != null) {
            for (FeedItem item : items) {
                sb.append(item.getTitle()).append("\t");
                if (includeText) {
                    sb.append(item.getItemDescription()).append("\t");
                    sb.append(item.getItemText()).append("\t");
                }
                sb.append(item.getLink()).append("\t");
                sb.append(item.getPublished()).append("\t");
                sb.append(item.getAuthors()).append("\n");
            }
            sb.append("-----------------------------------").append("\n");
            sb.append("# entries: ").append(items.size());
        }

        System.out.println(sb.toString());

    }

    public static void printFeed(Feed feed) {
        printFeed(feed, false);
    }

    public static void main(String[] args) throws Exception {

        // String clean =
        // cleanup("Anonymous created the <a href=\"/forum/message.php?msg_id=126707\" title=\"phpMyAdmin\">Welcome to Open Discussion</a> forum thread");
        // System.out.println(clean);
        // System.exit(0);

        FeedRetriever downloader = new FeedRetriever();
        downloader.setCleanStrings(false);
        // Feed feed = downloader.getFeed("http://badatsports.com/feed/");
        // Feed feed = downloader.getFeed("http://sourceforge.net/api/event/index/project-id/23067/rss");
        // Feed feed = downloader
        // .getFeed("http://sourceforge.net/api/message/index/list-name/phpmyadmin-svn/rss");
        // printFeed(feed);

        // Feed feed = downloader.getFeed(FeedRetriever.class.getResource("/feeds/atomSample1.xml").getFile());
        Feed feed = downloader.getFeed("http://sourceforge.net/api/event/index/project-id/23067/rss");
        FeedRetriever.printFeed(feed, true);

    }

}