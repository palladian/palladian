package tud.iir.news;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import tud.iir.helper.CountMap;
import tud.iir.helper.FileHelper;
import tud.iir.web.Crawler;

/**
 * The FeedStatisticCreator creates a file with statistics about feeds from a feed store.
 * 
 * @author David Urbansky
 * 
 */
public class FeedStatisticCreator {


    public void createStatistics(FeedStore feedStore, String statisticOutputPath) {

        // colors for the google chart
        List<String> colors = new ArrayList<String>();
        colors.add("E72727");
        colors.add("34B434");
        colors.add("3072F3");
        colors.add("FF9900");
        colors.add("7777CC");
        colors.add("AA0033");
        colors.add("626262");
        colors.add("9ABE9A");
        colors.add("E6FF00");
        colors.add("F626B1");
        colors.add("FF9900");

        // count number of feeds in each updateClass
        CountMap updateClassCounts = new CountMap();

        // number of unique domain names
        Set<String> uniqueDomains = new HashSet<String>();

        List<Feed> feeds = feedStore.getFeeds();
        for (Feed feed : feeds) {

            // int updateClassCount = (Integer) updateClassCounts.get(feed.getUpdateClass());
            // updateClassCount++;
            // updateClassCounts.put(feed.getUpdateClass(), updateClassCount);

            updateClassCounts.increment(feed.getUpdateClass());

            uniqueDomains.add(Crawler.getDomain(feed.getFeedUrl()));
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Number of feeds:").append(feeds.size()).append("\n");
        stats.append("Number of unique domains:").append(uniqueDomains.size()).append("\n");

        String chartData = "";
        String chartDataLabels = "";
        String chartColors = "";
        for (Entry<Object, Integer> o : updateClassCounts.entrySet()) {
            stats.append("Number of feeds in update class ").append(o.getKey()).append(":").append(o.getValue())
                    .append("\n");
            chartData += o.getValue().intValue() + ",";
            chartDataLabels += o.getKey() + "|";
            chartColors += colors.get((Integer) o.getKey()) + "|";
        }
        chartData = chartData.substring(0, chartData.length() - 1);
        chartDataLabels = chartDataLabels.substring(0, chartDataLabels.length() - 1);
        chartColors = chartColors.substring(0, chartColors.length() - 1);

        stats.append("Google pie chart:").append("http://chart.apis.google.com/chart?chs=600x425&chco=").append(
                chartColors).append("&chdl=").append(
                chartDataLabels).append("&chds=0,").append(feeds.size()).append("&cht=p&chd=t:").append(
                chartData).append("\n");

        FileHelper.writeToFile(statisticOutputPath, stats);


    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new FeedStatisticCreator()
.createStatistics(FeedDatabase.getInstance(), "data/temp/feedstats_combined.txt");
    }

}
