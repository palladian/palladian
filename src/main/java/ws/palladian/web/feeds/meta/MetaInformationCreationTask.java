/**
 * Created on: 25.10.2010 18:24:49
 */
package ws.palladian.web.feeds.meta;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.web.feeds.Feed;

/**
 * <p>
 * The MetaInformationCreator gets information about last modified since and ETag support as well as information about
 * the header size.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @version 1.0
 * @since 1.0
 * 
 */
public final class MetaInformationCreationTask implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);
    

    private Feed feed;

    private DatabaseManager dbManager = new DatabaseManager();

    private static final String psSupportsLMS = "UPDATE feeds SET supportsLMS=? WHERE id=?";

    private static final String psSupportsEtag = "UPDATE feeds SET supportsETag=? WHERE id=?";

    private static final String psResponseSize = "UPDATE feeds SET conditionGetResponseSize=? WHERE id=?";

    private static final String psSupportsPubSubHubBub = "UPDATE feeds SET supportsPubSubHubBub=? WHERE id=?";

    public MetaInformationCreationTask(Feed feed) {
        this.feed = feed;
    }

    /**
     * <p>
     * Create meta information, that is, find:
     * <ul>
     * <li>Etag support</li>
     * <li>last modified since support</li>
     * <li>If conditional get is supported also the size of the reply.</li>
     * </ul>
     * </p>
     * 
     */
    @Override
    public void run() {

        URLConnection connection = null;

        try {
            URL feedURL = new URL(feed.getFeedUrl());
            connection = feedURL.openConnection();
            connection.setIfModifiedSince(System.currentTimeMillis() + 60000);
            connection.connect();

        } catch (MalformedURLException e) {
            LOGGER.error("URL of feed with id: " + feed.getId() + " is malformed!", e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Boolean supports304 = false;

        try {

            supports304 = getFeedSupports304((HttpURLConnection) connection);

        } catch (IOException e) {
            LOGGER.error("Could not get HTTP header information for feed with id: " + feed.getId() + ".");
        }

        Boolean supportsETag = getSupportsETag(connection);
        Integer responseSize = -1;
        if (supports304) {
            responseSize = getFeedResponseSize((HttpURLConnection) connection);
        }
        Boolean supportsPubSubHubBub = Boolean.valueOf(false);

        try {
            supportsPubSubHubBub = getFeedSupportsPubSubHubBub(connection);
        } catch (IOException e1) {
            LOGGER.error("Could not get Content with information about PubSubHubBub information for feed with id: "+feed.getId()+".");
        }

        try {
            writeMetaInformationToDatabase(feed, supports304, supportsETag, responseSize, supportsPubSubHubBub);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to store results to Database.", e);
        }

        feed.freeMemory();
        feed.setLastHeadlines("");
        feed.setItems(null);

        MetaInformationCreator.counter++;
        LOGGER.info("percent done: "
                + MathHelper.round(100 * MetaInformationCreator.counter
                        / (double) MetaInformationCreator.collectionSize, 2) + "(" + MetaInformationCreator.counter
                        + ")");
    }

    private Boolean getFeedSupportsPubSubHubBub(final URLConnection connection) throws IOException {
        final InputStream plainXMLInput = connection.getInputStream();
        final String plainXML = IOUtils.toString(plainXMLInput);
        IOUtils.closeQuietly(plainXMLInput);
        if (plainXML.contains("rel=\"hub\"")) {
            return Boolean.valueOf(true);
        } else {
            return Boolean.valueOf(false);
        }
    }

    private Integer getFeedResponseSize(final HttpURLConnection connection) {
        int ret = 0;
        try {
            if (HttpURLConnection.HTTP_NOT_MODIFIED == connection.getResponseCode()) {
                ret = new Integer((connection.getContentLength() == -1 ? 0 : connection.getContentLength())
                        + sumHeaderFieldSize(connection.getHeaderFields()));
            } else {
                ret = new Integer(-1);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read header fields");
        }
        return ret;
    }

    private Boolean getSupportsETag(final URLConnection connection) {
        boolean ret = false;
        ret = connection.getHeaderField("Etag") == null;
        return ret;
    }

    /**
     * <p>
     * Checks if a web feeds server does support condition gets.
     * </p>
     * 
     * @param feed The feed to check.
     * @param responseSize Contains the size of the returned message with HTTP Status code 304 or -1 if conditional get
     *            is not supported.
     * @return {@code true} if the feed supports conditional gets, {@code false} otherwise.
     * @throws IOException
     */
    private Boolean getFeedSupports304(final HttpURLConnection connection) throws IOException {
        Boolean ret = false;

        if (HttpURLConnection.HTTP_NOT_MODIFIED == connection.getResponseCode()) {
            ret = true;
        }
        return ret;
    }

    private void writeMetaInformationToDatabase(Feed feed, Boolean supportsLMS, Boolean supportsETag,
            Integer responseSizeValue, Boolean supportsPubSubHubBub) throws SQLException {

        Integer id = feed.getId();
        
        dbManager.runUpdate(psSupportsLMS, supportsLMS, id);
        dbManager.runUpdate(psSupportsEtag, supportsETag, id);
        dbManager.runUpdate(psResponseSize, responseSizeValue, id);
        dbManager.runUpdate(psSupportsPubSubHubBub, supportsPubSubHubBub, id);
    }

    /**
     * @param headerFields The header fields from an http connection.
     * @return The summed up size of all header fields in bytes
     */
    private int sumHeaderFieldSize(Map<String, List<String>> headerFields) {
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
