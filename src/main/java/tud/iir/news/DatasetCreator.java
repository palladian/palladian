/**
 * Created on: 22.07.2010 15:10:31
 */
package tud.iir.news;

/**
 * <p>
 * Creates a dataset of feeds.
 * </p>
 * 
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 * 
 */
public class DatasetCreator {

    /**
     * <p>
     * Run creation of the feed dataset from all feeds in the database if possible.
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        FeedChecker feedChecker = FeedChecker.getInstance();

        feedChecker.setCheckApproach(CheckApproach.CHECK_FIXED, true);
        feedChecker.startContinuousReading();

    }

}
