package tud.iir.news;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
import tud.iir.web.HeaderInformation;
import tud.iir.web.URLDownloader;
import tud.iir.web.URLDownloader.URLDownloaderCallback;

import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * The FeedDownloader is responsible for fetching RSS and Atom feeds. We use Palladians {@link Crawler} for downloading
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
public class FeedDownloader {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDownloader.class);

    /** Used for all downloading purposes. */
    private Crawler crawler = new Crawler();

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * If enabled, we use the last poll time and the last ETag of the feed as HTTP headers when requesting the URL. This
     * way we can save bandwidth for feeds that support the HTTP 304 "Not modified" status code (about 83% of all feeds
     * do support either ETag or LastModifiedSince).
     */
    private boolean useBandwidthSavingHTTPHeaders = false;

    // ///////////////////////////////////////////////////
    // FeedDownloader API
    // ///////////////////////////////////////////////////

    /**
     * Download a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @return
     * @throws FeedDownloaderException
     */
    public Feed getFeed(String feedUrl) throws FeedDownloaderException {
        return getFeed(feedUrl, false, null);
    }

    /**
     * Download a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @param scrapePages set to <code>true</code>, to scrape the contents from the corresponding pages of each
     *            {@link FeedItem}.
     * @return
     * @throws FeedDownloaderException
     */
    public Feed getFeed(String feedUrl, boolean scrapePages) throws FeedDownloaderException {
        return getFeed(feedUrl, scrapePages, null);
    }

    /**
     * Download a feed from the specified URL.
     * 
     * @param feedUrl the URL to the RSS or Atom feed.
     * @param headerInformation header information containing ETag/lastModifiedSince data
     * @return
     * @throws FeedDownloaderException
     */
    public Feed getFeed(String feedUrl, HeaderInformation headerInformation) throws FeedDownloaderException {
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
     * @throws FeedDownloaderException
     */
    public Feed getFeed(String feedUrl, boolean scrapePages, HeaderInformation headerInformation)
            throws FeedDownloaderException {
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
     * Updates the supplied {@link Feed} with new items. This means, the existing items (if any) are replaced by current
     * items downloaded from web.
     * 
     * @param feed
     * @throws FeedDownloaderException
     */
    public void updateFeed(Feed feed) throws FeedDownloaderException {
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

        URLDownloader downloader = new URLDownloader();
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

        downloader.start(new URLDownloaderCallback() {
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

    // ///////////////////////////////////////////////////
    // private ROME specific methods
    // ///////////////////////////////////////////////////

    /**
     * Get feed information about a Atom/RSS feed, using ROME library.
     * 
     * @param feedUrl
     * @return
     * @throws FeedDownloaderException
     */
    private Feed getFeed(Document feedDocument) throws FeedDownloaderException {

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

        // get Feed items with ROME and assign to feed
        List<FeedItem> entries = getFeedItems(syndFeed, feedDocument);
        result.setItems(entries);

        // get the size of the feed
        if (feedDocument != null) {
            result.setByteSize(PageAnalyzer.getRawMarkup(feedDocument).getBytes().length);
        }

        return result;
    }

    /**
     * Get {@link FeedItem}s from the specified {@link SyndFeed}.
     * 
     * @param syndFeed
     * @param feedDocument
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<FeedItem> getFeedItems(SyndFeed syndFeed, Document feedDocument) {

        List<FeedItem> result = new LinkedList<FeedItem>();
        List<SyndEntry> syndEntries = syndFeed.getEntries();

        for (SyndEntry syndEntry : syndEntries) {

            FeedItem item = new FeedItem();

            String title = getEntryTitle(syndEntry);
            item.setTitle(title);

            String entryLink = getEntryLink(syndFeed, syndEntry);
            item.setLink(entryLink);

            String entryText = getEntryText(syndEntry);
            item.setItemText(entryText);

            String rawId = getEntryRawId(syndEntry);
            item.setRawId(rawId);

            // TODO only try a certain amount of times to extract a pub date, if none is found don't keep trying
            Date publishDate = getEntryPublishDate(syndEntry, item);
            item.setPublished(publishDate);

            result.add(item);
        }

        return result;
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
            entryLink = Crawler.makeFullURL(syndFeed.getLink(), entryLink);
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
            title = HTMLHelper.removeHTMLTags(title);
            title = StringEscapeUtils.unescapeHtml(title);
            title = title.trim();
        }
        return title;
    }

    /**
     * Get text content from {@link SyndEntry}; either from content/summary/description element.
     * 
     * @param syndEntry
     * @return text content or <code>null</code> if no content found.
     */
    @SuppressWarnings("unchecked")
    private String getEntryText(SyndEntry syndEntry) {

        // get the content from SyndEntry; either from content or from description
        String entryText = null;
        List<SyndContent> contents = syndEntry.getContents();
        if (contents != null) {
            for (SyndContent content : contents) {
                if (content.getValue() != null && content.getValue().length() != 0) {
                    entryText = content.getValue();
                }
            }
        }
        if (entryText == null && syndEntry.getDescription() != null) {
            entryText = syndEntry.getDescription().getValue();
        }

        // clean up: strip out HTML tags, unescape HTML code
        if (entryText != null) {
            entryText = HTMLHelper.htmlToString(entryText, false);
            entryText = StringEscapeUtils.unescapeHtml(entryText);
            entryText = entryText.trim();
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
            LOGGER.warn("id is missing, taking link instead");
        }

        // we could ultimately get no ID
        if (rawId == null) {
            LOGGER.warn("could not get id for entry");
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

        // ROME library failed to get the date, use DateGetter
        // FIXME there are still some entries without date (David: why? does rome not get some date formats?)
        if (publishDate == null) {

            Node node = item.getNode();
            Node pubDateNode = XPathHelper.getChildNode(node, "*[contains(name(),'date') or contains(name(),'Date')]");

            try {
                publishDate = DateGetterHelper.findDate(pubDateNode.getTextContent()).getNormalizedDate();
            } catch (NullPointerException e) {
                LOGGER.warn("date format could not be parsed correctly: " + pubDateNode + ", feed: "
                        + item.getFeedUrl() + ", " + e.getMessage());
            } catch (DOMException e) {
                LOGGER.warn("date format could not be parsed correctly: " + pubDateNode + ", feed: "
                        + item.getFeedUrl() + ", " + e.getMessage());
            } catch (Exception e) {
                LOGGER.warn("date format could not be parsed correctly: " + pubDateNode + ", feed: "
                        + item.getFeedUrl() + ", " + e.getMessage());
            }

        }

        if (publishDate != null) {
            LOGGER.debug("found publish date in original feed file: " + publishDate);
        } else {
            // as a last resort, use the entry's updated date
            publishDate = syndEntry.getUpdatedDate();
        }

        return publishDate;
    }

    private Document downloadFeedDocument(String feedUrl, HeaderInformation headerInformation)
            throws FeedDownloaderException {

        Document feedDocument = crawler.getXMLDocument(feedUrl, false, headerInformation);
        if (feedDocument == null) {
            throw new FeedDownloaderException("could not get document from " + feedUrl);
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

    private SyndFeed buildSyndFeed(Document feedDocument) throws FeedDownloaderException {

        try {

            SyndFeedInput feedInput = new SyndFeedInput();

            // this preserves the "raw" feed data and gives direct access to RSS/Atom specific elements see
            // http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
            feedInput.setPreserveWireFeed(true);

            SyndFeed syndFeed = feedInput.build(feedDocument);

            return syndFeed;

        } catch (IllegalArgumentException e) {
            LOGGER.error("getRomeFeed " + feedDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new FeedDownloaderException(e);
        } catch (FeedException e) {
            LOGGER.error("getRomeFeed " + feedDocument.getDocumentURI() + " " + e.toString() + " " + e.getMessage());
            throw new FeedDownloaderException(e);
        }

    }

    /**
     * Print feed with content in human readable form.
     * 
     * @param feed
     */
    public static void printFeed(Feed feed) {

        StringBuilder sb = new StringBuilder();

        sb.append(feed.getTitle()).append("\n");
        sb.append("feedUrl : ").append(feed.getFeedUrl()).append("\n");
        sb.append("siteUrl : ").append(feed.getSiteUrl()).append("\n");
        sb.append("-----------------------------------").append("\n");
        List<FeedItem> items = feed.getItems();
        if (items != null) {
            for (FeedItem item : items) {
                sb.append(item.getTitle()).append("\t");
                sb.append(item.getLink()).append("\n");
            }
            sb.append("-----------------------------------").append("\n");
            sb.append("# entries: ").append(items.size());
        }

        System.out.println(sb.toString());

    }

    public static void main(String[] args) throws Exception {

        FeedDownloader downloader = new FeedDownloader();
        Feed feed = downloader.getFeed("http://badatsports.com/feed/");
        printFeed(feed);

    }

}