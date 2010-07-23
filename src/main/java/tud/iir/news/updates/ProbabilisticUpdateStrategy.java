/**
 * Created on: 23.07.2010 09:10:58
 */
package tud.iir.news.updates;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tud.iir.helper.DateHelper;
import tud.iir.news.Feed;
import tud.iir.news.FeedDatabase;
import tud.iir.news.FeedEntry;
import tud.iir.news.FeedPostStatistics;

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
public class ProbabilisticUpdateStrategy implements UpdateStrategy {
    
    /**
     * If true, some output will be generated to evaluate the reading approaches.
     */
    private int benchmark = BENCHMARK_MAX_CHECK_TIME;

    /* (non-Javadoc)
     * @see tud.iir.news.UpdateStrategy#update(tud.iir.news.Feed, tud.iir.news.FeedPostStatistics)
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps) {
        List<FeedEntry> entries = feed.getEntries();

        if (feed.getChecks() == 0) {

            // learn the post distribution from the past to get initial check intervals
            // distribution minute of the day : frequency of news in that minute
            Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

            // since the feed has no post distribution yet, we fill all minutes with 0 posts
            for (int minute = 0; minute < 1440; minute++) {
                int[] postsChances = { 0, 0 };
                postDistribution.put(minute, postsChances);
            }

            // update the minutes where an entry could have been posted
            for (long t = fps.getTimeOldestPost(); t < fps.getTimeNewestPost() + DateHelper.MINUTE_MS; t += DateHelper.MINUTE_MS) {
                int minuteOfDay = (int) DateHelper.getTimeOfDay(t, Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[1] = postsChances[1] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }
            // for (Map.Entry<Integer, int[]> a : postDistribution.entrySet()) {
            // System.out.println(a.getKey()+":"+a.getValue()[0]+","+a.getValue()[1]);
            // }

            // update the minutes where an entry was actually posted
            for (FeedEntry entry : entries) {
                if (entry.getPublished() == null) {
                    continue;
                }
                int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[0] = postsChances[0] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }

            FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);

        } else if (feed.getChecks() > 0) {

            // learn the post distribution from the last seen entry to the newest one
            // distribution minute of the day : frequency of news in that minute
            Map<Integer, int[]> postDistribution = FeedDatabase.getInstance().getFeedPostDistribution(feed);

            // update the minutes where an entry could have been posted
            long timeLastSeenEntry = feed.getLastFeedEntry().getTime();

            for (long t = fps.getTimeOldestPost(); t <= fps.getTimeNewestPost(); t += DateHelper.MINUTE_MS) {
                // we have counted the chances for entries before the last seen
                // entry already, so we skip them here
                if (t <= timeLastSeenEntry) {
                    continue;
                }
                int minuteOfDay = (int) DateHelper.getTimeOfDay(t, Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[1] = postsChances[1] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }

            // update the minutes where an entry was actually posted
            for (FeedEntry entry : entries) {
                // we have counted the posts for entries before the last seen
                // entry already, so we skip them here
                if (entry.getPublished() == null || entry.getPublished().getTime() <= timeLastSeenEntry) {
                    continue;
                }
                int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[0] = postsChances[0] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }

            FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
            feed.setMeticulousPostDistribution(postDistribution);

            // only use calculated update intervals if one full day of distribution is available already
            if (feed.oneFullDayHasBeenSeen()) {

                int startMinute = (int) DateHelper.getTimeOfDay(System.currentTimeMillis(), Calendar.MINUTE);

                // // estimate time to next entry and time until list is full with
                // only new but one entries

                // set to thirty days maximum
                int minCheckInterval = 30 * 1440;
                boolean minCheckIntervalFound = false;

                // set to one hundred days maximum
                int maxCheckInterval = 100 * 1440;

                // add up all probabilities for the coming minutes until the
                // estimated post number is 1
                int currentMinute = startMinute;
                double estimatedPosts = 0;
                for (int c = 0; c < maxCheckInterval; c++) {

                    int[] postsChances = postDistribution.get(currentMinute);
                    double postProbability = 0;
                    if (postsChances[1] > 0) {
                        postProbability = (double) postsChances[0] / (double) postsChances[1];
                    }
                    estimatedPosts += postProbability;

                    if (estimatedPosts >= 1 && !minCheckIntervalFound) {
                        minCheckInterval = c;
                        minCheckIntervalFound = true;
                    }

                    if (estimatedPosts >= entries.size() - 1) {
                        maxCheckInterval = c;
                        break;
                    }

                    currentMinute = (currentMinute + 1) % 1440;
                }

                feed.setMinCheckInterval(minCheckInterval);
                feed.setMaxCheckInterval(maxCheckInterval);

//                // remember at which iteration the probabilistic approach took over
//                if (benchmark != BENCHMARK_OFF) {
//                    Integer iteration = probabilisticSwitchMap.get(feed.getId());
//                    if (iteration == null) {
//                        probabilisticSwitchMap.put(feed.getId(), feed.getChecks());
//                    }
//                }

            }
        }
    }

}
