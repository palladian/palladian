package tud.iir.news;

import java.util.List;

import tud.iir.helper.EasyMap;

/**
 * The FeedStatisticCreator creates a file with statistics about feeds from a feed store.
 * 
 * @author David Urbansky
 * 
 */
public class FeedStatisticCreator {


    public void createStatistics(FeedStore feedStore, String statisticOutputPath) {

        EasyMap updateClassCounts = new EasyMap();

        List<Feed> feeds = feedStore.getFeeds();
        for (Feed feed : feeds) {

            int updateClassCount = (Integer) updateClassCounts.get(feed.getUpdateClass());
            updateClassCount++;
            updateClassCounts.put(feed.getUpdateClass(), updateClassCount);

        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new FeedStatisticCreator().createStatistics(FeedDatabase.getInstance(), "data/temp/feedstats.txt");
    }

}
