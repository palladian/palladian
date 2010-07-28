/**
 * Created on: 28.07.2010 17:43:02
 */
package tud.iir.news;

import java.io.File;
import java.io.IOException;
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
    private final File FILE_PATH;

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public MetaInformationCreator() {
        feedStore = FeedDatabase.getInstance();
        FILE_PATH = new File(DatasetCreator.DATASET_PATH);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    @SuppressWarnings("unchecked")
    public void createMetaInformation() {
        Iterator fileIterator = FileUtils.iterateFiles(FILE_PATH, new String[] { ".csv" }, false);
        while (fileIterator.hasNext()) {
            File datasetFile = (File) fileIterator.next();
            String fileName = datasetFile.getName();
            String feedId = fileName.substring(0, fileName.indexOf("_"));

            try {
                Feed feed = feedStore.getFeedByID(Integer.valueOf(feedId));
                Double averageFeedSize = calculateAverageSize(datasetFile,feed.getEntries().size());
                String feedMetaInformation = getFeedMetaInformation(feed, averageFeedSize);

                FileHelper.prependFile(FILE_PATH.getAbsolutePath() + fileName, feedMetaInformation);
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
    private Double calculateAverageSize(File datasetFile, Integer entriesPerFeedAccess) throws IOException {
        Double ret = 0.0;
        List<String> datasetEntries = FileUtils.readLines(datasetFile);
        for(int i=0;i<datasetEntries.size();i ++) {
        ret = getEntrySize(datasetEntries.get(i));
        }
        
        Double numberOfWindows = (new Integer(datasetEntries.size())).doubleValue()/entriesPerFeedAccess;
        ret = (ret+getHeaderSize(datasetEntries.get(0))*numberOfWindows)/(numberOfWindows);
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
        return Double.valueOf(entryInformation[4]);
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
        return Double.valueOf(entryInformation[3]);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    private String getFeedMetaInformation(Feed feed, Double averageSize) {
        // FEED_ID;FEED_URL;NUMBER_OF_ENTRIES(Window Size);AVERAGE_SIZE;FEED_UPDATE_CLASS
        StringBuilder headerLine = new StringBuilder();
        headerLine.append(feed.getId() + ";");
        headerLine.append(feed.getFeedUrl() + ";");
        headerLine.append(feed.getEntries().size() + ";");
        headerLine.append(averageSize + ";");
        headerLine.append(feed.getUpdateClass());
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
