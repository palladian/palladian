/**
 * Created on: 28.07.2010 17:43:02
 */
package tud.iir.news;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import tud.iir.helper.MathHelper;
import tud.iir.persistence.DatabaseManager;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 * 
 */
public class MetaInformationCreator {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    private FeedStore feedStore;
    
    private DatabaseManager dbManager;
    private Connection connection;
    private PreparedStatement supportsCG;
    private PreparedStatement supportsEtag;
    private PreparedStatement responseSize;

    public MetaInformationCreator() {
        feedStore = FeedDatabase.getInstance();
        
        dbManager = DatabaseManager.getInstance();
        connection = dbManager.getConnection();
        
        try {
			supportsCG = connection.prepareStatement("UPDATE feeds SET supportsConditionalGet=? WHERE id=?");
			supportsEtag = connection.prepareStatement("UPDATE feeds SET supportsETag=? WHERE id=?");
	        responseSize = connection.prepareStatement("UPDATE feeds SET conditionGetResponseSize=? WHERE id=?");
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
			System.exit(1);
		}
        
    }

    /**
     * <p>
     * Create meta information, that is, find:
     * <ul>
     *   <li>Etag support</li>
     *   <li>conditional get support</li>
     * </ul>
     * </p>
     * 
     */
    @SuppressWarnings("unchecked")
    public void createMetaInformation() {
       
    	Collection<Feed> feedCollection = feedStore.getFeeds();
    	
    	LOGGER.info("start meta information gathering process");
    	
    	int c = 0;
    	for (Feed feed : feedCollection) {
            
            try {
                feed.updateEntries(false);
                URL feedURL = new URL(feed.getFeedUrl());
                URLConnection connection = feedURL.openConnection();
                connection.setIfModifiedSince(System.currentTimeMillis() + 60000);
                connection.connect();

                Boolean supportsETag = getSupportsETag(connection);
                Boolean supports304 = getFeedSupports304((HttpURLConnection)connection);
                Integer responseSize = getFeedResponseSize((HttpURLConnection)connection);

                writeMetaInformationToDatabase(feed, supports304, supportsETag, responseSize);
            } catch (SQLException e) {
                throw new RuntimeException("Unable to store results to Database.", e);
            } catch (MalformedURLException e) {
                LOGGER.error("URL of feed with id: " + feed.getId() + " is malformed!",e);
            } catch (IOException e) {
                LOGGER.error("Could not get HTTP header information for feed with id: " + feed.getId() + ".");
            }
            c++;
            LOGGER.info("percent done: " + MathHelper.round(100*c/(double)feedCollection.size(), 2));
        }
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

    /**
     * <p>
     * 
     * </p>
     *
     * @param feed
     * @param supportsConditionalGet
     * @param supportsETag
     * @param responseSize
     * @throws SQLException
     */
    private void writeMetaInformationToDatabase(final Feed feed, final Boolean supportsConditionalGet,
            final Boolean supportsETag, final Integer responseSizeValue) throws SQLException {
        
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
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        MetaInformationCreator metaInformationCreator = new MetaInformationCreator();
        metaInformationCreator.createMetaInformation();
    }
}
