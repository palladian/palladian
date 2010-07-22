/**
 * Created on: 22.07.2010 15:10:31
 */
package tud.iir.news;

/**
 * Creates a dataset of feed entries.
 * 
 * @author Klemens Muthmann
 * 
 */
public class DatasetCreator {

    /**
     * Run creation of the feed dataset from all feeds in the database if possible.
     * 
     * @param args
     */
    public static void main(String[] args) {
        FeedChecker feedChecker = FeedChecker.getInstance();

        feedChecker.setCheckApproach(CheckApproach.CHECK_FIXED, true);
        feedChecker.startContinuousReading();

    }

}
