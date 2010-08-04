/**
 * Created on: 28.07.2010 17:43:02
 */
package tud.iir.news;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;

/**
 * <p>
 * 
 * </p>
 * 
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 * 
 */
public class MetaInformationCreator {

    /**
     * <p>
     * 
     * </p>
     */
    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    /**
     * <p>
     * 
     * </p>
     */
    private FeedStore feedStore;

    /**
     * <p>
     * 
     * </p>
     */
    private File FILE_PATH;

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public MetaInformationCreator() {
        feedStore = FeedDatabase.getInstance();
        URL datasetFilePath = MetaInformationCreator.class.getResource("/datasets/feedPosts/");
        try {
            FILE_PATH = new File(datasetFilePath.toURI());
        } catch (URISyntaxException e) {
            new RuntimeException("File at location: /" + DatasetCreator.DATASET_PATH + " not accessible.");
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    @SuppressWarnings("unchecked")
    public void createMetaInformation() {
        Iterator fileIterator = FileUtils.iterateFiles(FILE_PATH, new String[] { "csv" }, false);
        while (fileIterator.hasNext()) {
            File datasetFile = (File) fileIterator.next();
            String fileName = datasetFile.getName();
            String feedId = fileName.substring(0, fileName.indexOf("_"));

            try {
                Feed feed = feedStore.getFeedByID(Integer.valueOf(feedId));
                feed.updateEntries(false);

                Double averageFeedSize = calculateAverageSize(datasetFile, feed.getEntries().size());
                String feedMetaInformation = getFeedMetaInformation(feed, averageFeedSize);

                FileHelper.prependFile(datasetFile.getAbsolutePath(), feedMetaInformation);
            } catch (IOException e) {
                LOGGER.error("Could not add meta information to file: " + fileName, e);
            }
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param datasetFile
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected Double calculateAverageSize(File datasetFile, Integer entriesPerFeedAccess) throws IOException {
        Double ret = 0.0;
        List<String> datasetEntries = FileUtils.readLines(datasetFile);
        for (int i = 0; i < (datasetEntries.size() - entriesPerFeedAccess); i++) {
            Double windowSum = 0.0;
            for (int j = 0; j < entriesPerFeedAccess; j++) {
                Double entrySize = getEntrySize(datasetEntries.get(i + j));
                if (entrySize != null) {
                    windowSum += getEntrySize(datasetEntries.get(i + j));
                }
            }
            ret += windowSum;
        }

        Double numberOfWindows = (new Integer(datasetEntries.size())).doubleValue() - entriesPerFeedAccess;
        ret = (ret + getHeaderSize(datasetEntries.get(0)) * numberOfWindows) / (numberOfWindows);
        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param string
     * @return
     */
    private Double getHeaderSize(String string) {
        String[] entryInformation = string.split(";");
        try {
            return Double.valueOf(entryInformation[4]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param string
     * @return
     */
    private Double getEntrySize(String string) {
        String[] entryInformation = string.split(";");
        try {
            return Double.valueOf(entryInformation[3]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    private String getFeedMetaInformation(Feed feed, Double averageSize) {
        // FEED_ID;FEED_URL;NUMBER_OF_ENTRIES(Window Size);AVERAGE_SIZE;FEED_UPDATE_CLASS;SUPPORTS_LAST_MODIFIED_SINCE;SUPPORTS_ETAG;SUPPORTS_COMPRESSION;NUMBER_OF_CHECKS
        StringBuilder headerLine = new StringBuilder();
        headerLine.append(feed.getId() + ";");
        headerLine.append(feed.getFeedUrl() + ";");
        headerLine.append(feed.getEntries().size() + ";");
        headerLine.append(averageSize + ";");
        headerLine.append(feed.getUpdateClass()+";");
        boolean lastModified = false;
        boolean etag = false;
        boolean compression = false;
        try {
            URL feedURL = new URL(feed.getFeedUrl());
            URLConnection connection = feedURL.openConnection();
            lastModified = connection.getHeaderField("Last-Modified")==null;
            etag = connection.getHeaderField("Etag")==null;
            compression = connection.getHeaderField("Content-Encoding")==null;
        } catch (MalformedURLException e) {
            LOGGER.error("URL of feed: "+feed.getFeedUrl()+" is malformed!");
        } catch (IOException e) {
            LOGGER.error("Could not get HTTP header information for feed at address: "+feed.getFeedUrl()+".");
        }
        headerLine.append(lastModified+";");
        headerLine.append(etag+";");
        headerLine.append(compression+";");
        headerLine.append(feed.getChecks());
        headerLine.append("\n");

        return headerLine.toString();
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
