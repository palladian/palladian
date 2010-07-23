/**
 * Created on: 22.07.2010 15:10:31
 */
package tud.iir.news;

import org.apache.log4j.Logger;

/**
 * Creates a dataset of feed entries.
 * 
 * @author Klemens Muthmann
 * 
 */
public class DatasetCreator {
    /**
     * <p>
     * 
     * </p>
     */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    /**
     * Run creation of the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {
        FeedChecker feedChecker = new FeedChecker(new DBFeedSource());

        feedChecker.setCheckApproach(CheckApproach.CHECK_FIXED, true);
        
        LOGGER.debug("Start extracting feeds");
        feedChecker.startContinuousReading();

    }

}
