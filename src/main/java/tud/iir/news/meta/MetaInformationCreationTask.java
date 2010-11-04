/**
 * Created on: 25.10.2010 18:24:49
 */
package tud.iir.news.meta;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.helper.MathHelper;
import tud.iir.news.Feed;
import tud.iir.persistence.DatabaseManager;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @version 1.0
 * @since 1.0
 * 
 */
public final class MetaInformationCreationTask implements Runnable {

    /**
     * <p>
     * 
     * </p>
     */
    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    private Feed feed;

    /**
     * <p>
     * 
     * </p>
     */
    private DatabaseManager dbManager;

    /**
     * <p>
     * 
     * </p>
     */
    private Connection connection;

    /**
     * <p>
     * 
     * </p>
     */
    private PreparedStatement supportsCG;

    /**
     * <p>
     * 
     * </p>
     */
    private PreparedStatement supportsEtag;

    /**
     * <p>
     * 
     * </p>
     */
    private PreparedStatement responseSize;

    /**
     * <p>
     * 
     * </p>
     */
    private PreparedStatement eTagResponseSize;

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public MetaInformationCreationTask(final Feed feed) {
        this.feed = feed;
        dbManager = DatabaseManager.getInstance();
        connection = dbManager.getConnection();

        try {
            supportsCG = connection.prepareStatement("UPDATE feeds SET supportsConditionalGet=? WHERE id=?");
            supportsEtag = connection.prepareStatement("UPDATE feeds SET supportsETag=? WHERE id=?");
            responseSize = connection.prepareStatement("UPDATE feeds SET conditionGetResponseSize=? WHERE id=?");
            eTagResponseSize = connection.prepareStatement("UPDATE feeds SET etagResponseSize=? WHERE id=?");
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * <p>
     * Create meta information, that is, find:
     * <ul>
     * <li>Etag support</li>
     * <li>conditional get support</li>
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
        Integer etagResponseSize = -1;
        if (supportsETag) {
            etagResponseSize = getEtagFeedResponseSize((HttpURLConnection) connection);
        }
        Integer responseSize = -1;
        if (supports304) {
            responseSize = getFeedResponseSize((HttpURLConnection) connection);
        }

        try {
            writeMetaInformationToDatabase(feed, supports304, supportsETag, responseSize, etagResponseSize);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to store results to Database.", e);
        }

        feed.freeMemory();
        feed.setLastHeadlines("");
        feed.setEntries(null);

        MetaInformationCreator.counter++;
        LOGGER.info("percent done: "
                + MathHelper.round(100 * MetaInformationCreator.counter
                        / (double) MetaInformationCreator.collectionSize, 2) + "(" + MetaInformationCreator.counter
                + ")");
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param connection
     * @return
     */
    private Integer getEtagFeedResponseSize(HttpURLConnection connection) {
        int ret = 0;
        if (getSupportsETag(connection)) {
            ret = new Integer((connection.getContentLength() == -1 ? 0 : connection.getContentLength())
                    + sumHeaderFieldSize(connection.getHeaderFields()));
        } else {
            ret = new Integer(-1);
        }
        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param responseSize
     * @return
     */
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

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feed
     * @return
     */
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

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feed
     * @param supportsConditionalGet
     * @param supportsETag
     * @param etagResponseSize2
     * @param responseSize
     * @throws SQLException
     */
    private void writeMetaInformationToDatabase(final Feed feed, final Boolean supportsConditionalGet,
            final Boolean supportsETag, final Integer responseSizeValue, Integer etagResponseSizeValue)
    throws SQLException {

        Integer id = feed.getId();

        supportsCG.setBoolean(1, supportsConditionalGet);
        supportsCG.setInt(2, id);
        dbManager.runUpdate(supportsCG);

        supportsEtag.setBoolean(1, supportsETag);
        supportsEtag.setInt(2, id);
        dbManager.runUpdate(supportsEtag);

        responseSize.setInt(1, responseSizeValue);
        responseSize.setInt(2, id);
        dbManager.runUpdate(responseSize);

        eTagResponseSize.setInt(1, etagResponseSizeValue);
        eTagResponseSize.setInt(2, id);
        dbManager.runUpdate(eTagResponseSize);
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
