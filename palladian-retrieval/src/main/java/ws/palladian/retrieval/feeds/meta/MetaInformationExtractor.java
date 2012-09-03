package ws.palladian.retrieval.feeds.meta;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.XmlParser;

import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * <p>Helper to process a {@link HttpResult} and extract some Headers. Code taken from {@link MetaInformationCreationTask}.</p>
 * 
 * @author Sandro Reichert
 * @author Philipp Katz
 * 
 */
public class MetaInformationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MetaInformationExtractor.class);

    private static final Pattern[] VALID_FEED_PATTERNS = new Pattern[] { Pattern.compile("<rss"),
            Pattern.compile("<feed"), Pattern.compile("<rdf:RDF") };

    private HttpResult httpResult = null;

    private String httpContent = "";

    private SyndFeed syndFeed = null;

    public MetaInformationExtractor(HttpResult httpResult) {
        super();
        this.httpResult = httpResult;
    }

    /**
     * Retrieves the following information from the local {@link HttpResult} and updates the {@link FeedMetaInformation}
     * 
     * <ul>
     * <li>isAccessibleFeed</li>
     * <li>supportsPubSubHubBub</li>
     * <li>feedFormat</li>
     * <li>hasItemIds</li>
     * <li>hasUpdated</li>
     * <li>hasPublished</li>
     * <li>hasPubDate</li>
     * <li>hasCloud</li>
     * <li>ttl</li>
     * <li>hasSkipDays</li>
     * <li>hasSkipHours</li>
     * <li>conditionalGetResponseSize</li>
     * </ul>
     * 
     * @param feedMetaInformation The {@link FeedMetaInformation} to update.
     */
    public void updateFeedMetaInformation(FeedMetaInformation feedMetaInformation) {
        if (httpContent.equals("")) {
            httpContent = getHttpContent();
        }
        if (syndFeed == null) {
            syndFeed = getSyndFeed();
        }

        feedMetaInformation.setAccessible(isAccessibleFeed());
        feedMetaInformation.setSupportsPubSubHubBub(getFeedSupportsPubSubHubBub());

        String feedFormat = getFeedFormat();
        feedMetaInformation.setFeedFormat(feedFormat);

        if (feedFormat != null) {
            if (feedFormat.toLowerCase().contains("rss")) {
                determineRssMetaInformation(feedMetaInformation);
            } else if (feedFormat.toLowerCase().contains("atom")) {
                determineAtomMetaInformation(feedMetaInformation);
            }
        }

        feedMetaInformation.setCgHeaderSize(getHeaderFieldSize());
    }

    /**
     * Get the {@link HttpResult}'s content as {@link String} using UTF-8 encoding.
     * 
     * @param httpResult *
     * @return
     */
    private String getHttpContent() {
        String content = "";
        InputStream inputStream = null;
        Writer writer = null;
        Reader reader = null;
        try {
            inputStream = new ByteArrayInputStream(httpResult.getContent());

            if (inputStream != null) {
                writer = new StringWriter();
                char[] buffer = new char[1024];
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
                content = writer.toString();
            }
        } catch (IOException e) {
            LOGGER.error("Could not process content in http result. " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(writer);
        }
        return content;
    }

    /**
     * Checks whether the {@link HttpResult} contains an accessible feed. Returns <code>true</code> if the status code
     * is nor 403 or 404 and if the content contains one of the patterns that are typical for feeds, <code>false</code>
     * otherwise.
     * 
     * @return <code>true</code> if the result seems to contain a feed.
     */
    private boolean isAccessibleFeed() {
        if (HttpURLConnection.HTTP_NOT_FOUND == httpResult.getStatusCode()
                || HttpURLConnection.HTTP_FORBIDDEN == httpResult.getStatusCode()) {
            return false;
        }

        if (httpContent != null) {
            for (Pattern pattern : VALID_FEED_PATTERNS) {
                Matcher matcher = pattern.matcher(httpContent);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the feed supports the PubSubHubbub protocol.
     * 
     * @return <code>true</code> if the feed supports the PubSubHubbub protocol.
     */
    private boolean getFeedSupportsPubSubHubBub() {
        if (httpContent != null && httpContent.contains("rel=\"hub\"")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the feed format and version, e.g. RSS 2.0
     * 
     * @return The feed format and version, e.g. RSS 2.0
     */
    private String getFeedFormat() {
        String feedVersion = null;

        if (syndFeed != null) {
            feedVersion = syndFeed.getFeedType();
        }
        return feedVersion;
    }

    /**
     * Tries to build a {@link SyndFeed} from the {@link HttpResult}'s content.
     * 
     * @return
     */
    private SyndFeed getSyndFeed() {

        SyndFeedInput input = new SyndFeedInput();
        input.setPreserveWireFeed(true);
        SyndFeed feed = null;
        try {
            XmlParser parser = new XmlParser();
            InputStream inputStream = new ByteArrayInputStream(httpResult.getContent());
            Document document = parser.parse(inputStream);
            feed = input.build(document);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to determine feed version. " + e.getMessage());
        } catch (FeedException e) {
            LOGGER.error("Unable to determine feed version. " + e.getMessage());
        } catch (ParserException e) {
            LOGGER.error("Unable to determine feed version. " + e.getMessage());
        }
        return feed;
    }

    /**
     * Determine Atom specific meta information from internal {@link HttpResult}:
     * <ul>
     * <li>hasItemIds</li>
     * <li>hasUpdated</li>
     * <li>hasPublished</li>
     * </ul>
     * The meta information is written to the feed's meta information.
     * 
     * @param feedMetaInformation The feed to update.
     */
    private void determineAtomMetaInformation(FeedMetaInformation feedMetaInformation) {
        if (syndFeed != null) {
            Iterator<?> it = syndFeed.getEntries().iterator();
            if (it.hasNext()) {
                SyndEntry syndEntry = (SyndEntry) it.next();
                com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) syndEntry
                        .getWireEntry();
                String rawId = atomEntry.getId();

                feedMetaInformation.setHasItemIds(rawId != null && !rawId.isEmpty());
                feedMetaInformation.setHasUpdated(atomEntry.getUpdated() != null);
                feedMetaInformation.setHasPublished(atomEntry.getPublished() != null);
            }
        }
    }

    /**
     * Determine RSS specific meta information from internal {@link HttpResult}:
     * <ul>
     * <li>hasItemIds</li>
     * <li>hasPubDate</li>
     * <li>hasCloud</li>
     * <li>ttl</li>
     * <li>hasSkipDays</li>
     * <li>hasSkipHours</li>
     * </ul>
     * The meta information is written to the feed's meta information.
     * 
     * @param syndFeed
     * @param metaInformation
     */
    private void determineRssMetaInformation(FeedMetaInformation feedMetaInformation) {
        Iterator<?> it = syndFeed.getEntries().iterator();
        if (it.hasNext()) {
            SyndEntry syndEntry = (SyndEntry) it.next();

            com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) syndEntry.getWireEntry();
            com.sun.syndication.feed.rss.Channel channel = (com.sun.syndication.feed.rss.Channel) syndFeed
                    .originalWireFeed();
            Guid guid = rssItem.getGuid();

            feedMetaInformation.setHasItemIds(guid != null && !guid.getValue().isEmpty());
            feedMetaInformation.setHasPubDate(rssItem.getPubDate() != null);
            feedMetaInformation.setHasCloud(channel.getCloud() != null);
            feedMetaInformation.setTtl(channel.getTtl());
            feedMetaInformation.setHasSkipDays(!channel.getSkipDays().isEmpty());
            feedMetaInformation.setHasSkipHours(!channel.getSkipHours().isEmpty());
        }
    }

    /**
     * @return The summed up size of all header fields in bytes.
     */
    private int getHeaderFieldSize() {

        Map<String, List<String>> headerFields = httpResult.getHeaders();

        Integer ret = 0;
        for (Entry<String, List<String>> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                byte[] bytes = key.getBytes();
                ret += bytes.length;
            }
            for (String value : entry.getValue()) {
                ret += value.getBytes().length;
            }
        }
        return ret;
    }

}
