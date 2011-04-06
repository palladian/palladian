package ws.palladian.retrieval.feeds.discovery;

import ws.palladian.classification.page.evaluation.TrainingDataSeparation;
import ws.palladian.helper.FileHelper;

/**
 * Quickndirty random selection of feed URLs to get the final set of feeds to use in our experiment
 * 
 * @author Sandro Reichert
 */
public class FeedRandomSelectURL {

    public static void main(String[] args) {
        String input = "/home/pk/Desktop/FeedDiscovery/foundFeedsRemovedNearDuplicates.txt";
        String finalFeeds = "/home/pk/Desktop/FeedDiscovery/foundFeedsRemovedNearDuplicatesRandomSampling.txt";
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
