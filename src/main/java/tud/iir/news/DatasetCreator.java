/**
 * Created on: 22.07.2010 15:10:31
 */
package tud.iir.news;

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
public class DatasetCreator {

    
    
    /**
     * <p>
     * 
     * </p>
     *
     * @param args
     */
    public static void main(String[] args) {
        FileHelper.readFileToArray(DatasetCreator.class.getResource("/feeds.txt"));
        
        FeedChecker feedChecker = FeedChecker.getInstance();
        
        feedChecker.startContinuousReading();
    }

}
