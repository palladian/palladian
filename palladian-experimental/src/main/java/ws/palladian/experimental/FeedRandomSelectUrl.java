package ws.palladian.experimental;

import ws.palladian.classification.page.evaluation.TrainingDataSeparation;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>Quick'n'dirty random selection of feed URLs to get the final set of feeds to use in our experiment.</p>
 * 
 * @author Sandro Reichert
 */
public class FeedRandomSelectUrl {

    public static void main(String[] args) {
//        String input = "/home/pk/Desktop/FeedDiscovery/foundFeedsRemovedNearDuplicates.txt";
//        String finalFeeds = "/home/pk/Desktop/FeedDiscovery/foundFeedsRemovedNearDuplicatesRandomSampling.txt";
        String input = "F:\\Konferenzen und Meetings\\papers_(eigene)\\2011_feedDatasetPaper\\gathering_TUDCS6\\foundFeedsDeduplicatedSortedRemovedUnreachableAndNearDuplicates.txt";
        String finalFeeds = "F:\\Konferenzen und Meetings\\papers_(eigene)\\2011_feedDatasetPaper\\gathering_TUDCS6\\foundFeedsDeduplicatedSortedRemovedUnreachableAndNearDuplicatesRandomSampling.txt";
        String temp = "data/datasets/feedURLs/temp.txt";
        double targetNumberOfFeeds = 200000D;

        double percentage = (targetNumberOfFeeds / (double) FileHelper.getNumberOfLines(input)) * 100;

        System.out.println(percentage);

        TrainingDataSeparation tds = new TrainingDataSeparation();
        try {
            tds.separateFile(input, finalFeeds, temp, percentage, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileHelper.delete(temp);
    }
}
