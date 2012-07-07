package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;

/**
 * @author David Urbansky
 * @deprecated Class was used by evaluation of ICWSM2011-paper.
 */
@Deprecated
public class FeedBenchmarkFileReader {

    protected static final Logger LOGGER = Logger.getLogger(FeedBenchmarkFileReader.class);

    private Feed feed;
    private FeedReader feedChecker;
    private List<String> historyFileLines;
    private String historyFilePath = "";
    private int totalEntries = 0;

    /** The cumulated delay of early lookups + the last cumulatedPoll delay when at least one new item was found. */
    private long cumulatedDelay = 0l;

    /** The cumulated delay of early lookups. */
    private long cumulatedEarlyDelay = 0l;

    /**
     * We need to loop through the file many times, to expedite the process we save the last index position where the
     * window started (actually one entry before the window so we can calculate a delay to the next entry).
     */
    private int lastStartIndex = 1;

    public FeedBenchmarkFileReader(Feed feed, FeedReader feedChecker) {
        this.feed = feed;
        this.feedChecker = feedChecker;

        String safeFeedName = feed.getId()
                + "_"
                + StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst("www.", ""),
                        30);

        this.historyFilePath = FeedReaderEvaluator.findHistoryFile(safeFeedName);

        // if file doesn't exist skip the feed
        if (!new File(historyFilePath).exists()) {
            feed.setHistoryFileCompletelyRead(true);
        } else {
            try {
                this.historyFileLines = FileHelper.readFileToArray(historyFilePath);
                this.totalEntries = historyFileLines.size();
            } catch (Exception e) {
                LOGGER.error("out of memory error for feed " + feed.getId() + ", " + e.getMessage());
            }
        }
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public FeedReader getFeedChecker() {
        return feedChecker;
    }

    public void setFeedChecker(FeedReader feedChecker) {
        this.feedChecker = feedChecker;
    }

    public String getHistoryFilePath() {
        return historyFilePath;
    }

    public void setHistoryFilePath(String historyFilePath) {
        this.historyFilePath = historyFilePath;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    /**
     * <p>
     * For benchmarking purposes, we created a dataset of feed post histories and stored it on disk. We can now run the
     * feed reader on this dataset and evaluate the updateInterval techniques.
     * </p>
     * FIXME: calculation of delay may be broken due to refactoring of PollData
     * <p>
     * We need to handle "On-The-Fly" feeds special (we disregard them here), because their timestamps should be at
     * simulation time.
     * </p>
     */
    public void updateEntriesFromDisk() {

        try {
            List<FeedItem> entries = new ArrayList<FeedItem>();

            // the timestamp of the first item in the window
            long firstItemInWindowTimestamp = -1l;

            // the timestamp of the last item that has been looked at
            long lastItemTimestamp = -1l;

            // the timestamp of the second item in the window (which is different from the first), we need this to
            // calculate the length of the last interval between the first and second item in the window
            long secondItemInWindowTimestamp = -1l;

            // the timestamp of the last item in the window
            long lastItemInWindowTimestamp = Long.MIN_VALUE;

            int misses = 0;
            long totalBytes = 0;
            boolean footHeadAdded = false;

            // the cumulated delay of the lookup times in milliseconds (in benchmark min mode), this can happen when we
            // read too early or too late
            long cumulatedPollDelay = 0l;

            // get hold of the post entry just before the window starts, this way we can determine the delay in case
            // there are no new entries between lookup time and last lookup time
            long nextItemBeforeWindowTimestamp = 0l;

            // count new entries, they must be between lookup time and last lookup time
            int numberNewItems = 0;

            // for each new/missed item we save the timestamp and the interval to the next item in order to calculate
            // the average timeliness
            // Long[timestamp,delay,interval]
            List<Long[]> newItems = new ArrayList<Long[]>();

            boolean windowStartIndexFound = false;

            
            

            // ---------- Start of main loop -----------
            for (int i = lastStartIndex; i <= totalEntries; i++) {

                String line = historyFileLines.get(i - 1);

                String[] parts = line.split(";");

                if (feed.getWindowSize() == -1) {
                    int windowSize = Integer.valueOf(parts[5]);
                    if (windowSize > totalEntries) {
                        windowSize = totalEntries;
                    }
                    if (windowSize > 1000) {
                        LOGGER.info("feed has a window size of " + windowSize + " and will be discarded");
                        feed.setHistoryFileCompletelyRead(true);
                        feed.setBenchmarkLastLookupTime(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND);
                        return;
                    }
                    feed.setWindowSize(windowSize);
                    // feed.setWindowSize(Math.min(windowSize, 5));
                }

                long entryTimestamp = Long.valueOf(parts[0]);

                // get hold of the post entry just before the window starts
                if (entryTimestamp > feed.getBenchmarkLookupTime()) {
                    nextItemBeforeWindowTimestamp = entryTimestamp;
                    lastStartIndex = i;
                    windowStartIndexFound = true;
                } else if (!windowStartIndexFound && i >= 2) {
                    i -= 2;
                    continue;
                }

                // process post entries that are in the current window
                // if ((entryTimestamp < feed.getBenchmarkLookupTime() && entries.size() < feed.getWindowSize()) ||
                // totalEntries - i < feed.getWindowSize()) {
                if ((entryTimestamp <= feed.getBenchmarkLookupTime() || totalEntries - i < feed.getWindowSize())
                        && entries.size() < feed.getWindowSize()) {

                    windowStartIndexFound = true;

                    if (firstItemInWindowTimestamp < 0) {
                        firstItemInWindowTimestamp = entryTimestamp;
                    } else if (secondItemInWindowTimestamp < 0 && entryTimestamp != firstItemInWindowTimestamp) {
                        secondItemInWindowTimestamp = entryTimestamp;
                    }

                    // find the first lookup date if the file has not been read yet
                    if (feed.getBenchmarkLookupTime() == Long.MIN_VALUE && totalEntries - i + 1 == feed.getWindowSize()) {
                        feed.setBenchmarkLookupTime(entryTimestamp);
                    }

                    if (FeedReaderEvaluator.benchmarkMode == FeedReaderEvaluator.BENCHMARK_TIME
                            && entryTimestamp > FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND
                            && totalEntries - i + 1 == feed.getWindowSize()) {
                        LOGGER.error("we disregard this feed (" + feed.getId()
                                + ") since it does not comply with our start date " + entryTimestamp);
                        feed.setHistoryFileCompletelyRead(true);
                        feed.setBenchmarkLastLookupTime(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND);
                        return;
                    }

                    // add up download size (head and foot if necessary and post size itself)
                    if (!footHeadAdded) {
                        totalBytes = totalBytes + Math.max(100, Integer.valueOf(parts[4]));
                        footHeadAdded = true;
                    }

                    try {
                        totalBytes += Math.max(0, Integer.valueOf(parts[3]));
                    } catch (NumberFormatException e) {
                        // e.printStackTrace();
                        LOGGER.error(e.getMessage());
                    }

                    // create feed entry
                    FeedItem feedEntry = new FeedItem();
                    feedEntry.setPublished(new Date(entryTimestamp));
                    feedEntry.setTitle(parts[1]);
                    feedEntry.setLink(parts[2]);

                    entries.add(feedEntry);

                    // for all post entries in the window that are newer than the last lookup time we need to sum up the
                    // delay to the current lookup time (and weight it)
                    if (entryTimestamp > feed.getBenchmarkLastLookupTime() && feed.getChecks() > 0) {

                        cumulatedPollDelay += feed.getBenchmarkLookupTime() - entryTimestamp;

                        // update interval of last new item if it exists
                        // if (newItems.size() > 0) {
                        // newItems.get(newItems.size() - 1)[2] = entryTimestamp
                        // - newItems.get(newItems.size() - 1)[0];
                        // }

                        // add new item, we don't know the interval to the next one yet so we update it when we find the
                        // next new item or if there is none, we take the time of the item before the window
                        Long interval = null;
                        if (newItems.size() > 0) {
                            interval = lastItemTimestamp - entryTimestamp;
                        } else if (nextItemBeforeWindowTimestamp > 0) {
                            interval = nextItemBeforeWindowTimestamp - entryTimestamp;
                        }
                        newItems.add(new Long[] { entryTimestamp, feed.getBenchmarkLookupTime() - entryTimestamp,
                                interval });

                        // count new entry
                        numberNewItems++;
                    }

                    // if top of the file is reached, we read the file completely and can stop scheduling reading this
                    // feed
                    if (i == 1) {
                        LOGGER.debug("complete history has been read for feed " + feed.getId() + " ("
                                + feed.getFeedUrl() + ")");
                        feed.setHistoryFileCompletelyRead(true);
                    }

                    // check whether current post entry is the last one in the window
                    if (entries.size() == feed.getWindowSize()) {
                        lastItemInWindowTimestamp = entryTimestamp;
                    }
                }

                // process post entries between the end of the current window and the last lookup time (misses)
                else if (entryTimestamp <= lastItemInWindowTimestamp
                        && entryTimestamp > feed.getBenchmarkLastLookupTime()) {

                    cumulatedPollDelay += feed.getBenchmarkLookupTime() - entryTimestamp;

                    // update interval of last new item if it exists
                    // if (newItems.size() > 0) {
                    // newItems.get(newItems.size() - 1)[2] = entryTimestamp - newItems.get(newItems.size() - 1)[0];
                    // }

                    // add new item, we don't know the interval to the next one yet so we update it when we find the
                    // next new item or if there is none, we take the time of the item before the window
                    Long interval = null;
                    if (newItems.size() > 0) {
                        interval = lastItemTimestamp - entryTimestamp;
                    } else if (nextItemBeforeWindowTimestamp > 0) {
                        interval = nextItemBeforeWindowTimestamp - entryTimestamp;
                    }
                    newItems.add(new Long[] { entryTimestamp, feed.getBenchmarkLookupTime() - entryTimestamp, interval });

                    // count post entry as miss
                    misses = misses + 1;
                    // System.out.println("miss");
                } else if (entryTimestamp <= feed.getBenchmarkLastLookupTime()) {
                    break;
                }

                lastItemTimestamp = entryTimestamp;
            }
            // ---------- End of main loop -----------

            // if no new entry was found, we add the delay to the next new post entry
            if (numberNewItems == 0 && nextItemBeforeWindowTimestamp != 0l && feed.getChecks() > 0) {
                cumulatedPollDelay = feed.getBenchmarkLookupTime() - nextItemBeforeWindowTimestamp;
                cumulatedEarlyDelay += feed.getBenchmarkLookupTime() - nextItemBeforeWindowTimestamp;
            }


            cumulatedDelay += Math.abs(cumulatedPollDelay);

            feed.setItems(entries);

            // now that we set the entries we can add information about the poll to the poll series
            PollData pollData = new PollData();

            pollData.setBenchmarkType(FeedReaderEvaluator.benchmarkPolicy);

            // only checks after the first one get a min score
            if (feed.getChecks() > 0 && numberNewItems > 0) {

                // if (feed.getChecks() > 0 && secondItemInWindowTimestamp < 0) {
                // secondItemInWindowTimestamp = feed.getLastFeedEntry().getTime();
                // }

                // long currentInterval = nextItemBeforeWindowTimestamp - firstItemInWindowTimestamp;
                //
                // if (newItems.get(newItems.size() - 1) != null) {
                // newItems.get(newItems.size() - 1)[2] = currentInterval;
                // }

                // calculate timeliness
                Double totalSum = 0.0;
                int c = 0;
                for (Long[] newItem : newItems) {

                    if (newItem[2] == null) {
                        continue;
                    }

                    // add the cumulated delay for early polls to the first new item
                    if (c == newItems.size() - 1) {
                        totalSum += 1 / ((newItem[1] + Math.abs(cumulatedEarlyDelay)) / (double) newItem[2] + 1);
                    } else {
                        totalSum += 1 / (newItem[1] / (double) newItem[2] + 1);
                    }

                    c++;
                }
                // Double timeliness = null;
                // if (c > 0) {
                // timeliness = totalSum / c;
                // }
                // pollData.setTimeliness(timeliness);

                // calculate timeliness late
                totalSum = 0.0;
                c = 0;
                for (Long[] newItem : newItems) {

                    if (newItem[2] == null) {
                        continue;
                    }

                    totalSum += 1 / (newItem[1] / (double) newItem[2] + 1);
                    c++;
                }
                // Double timelinessLate = null;
                // if (c > 0) {
                // timelinessLate = totalSum / c;
                // }
                // pollData.setTimelinessLate(timelinessLate);

                pollData.setCumulatedDelay(cumulatedPollDelay);
            }

            pollData.setPollTimestamp(feed.getBenchmarkLookupTime());
            pollData.setNewWindowItems(numberNewItems);
            pollData.setMisses(misses);

            // pollData.setCumulatedDelay(cumulatedDelay);

            // reset cumulated delay for next new item
            if (numberNewItems > 0) {
                cumulatedEarlyDelay = 0;
            }

            pollData.setWindowSize(feed.getWindowSize());
            pollData.setDownloadSize(totalBytes);

            // remember the time the feed has been checked
            feed.setLastPollTime(new Date());

            feedChecker.updateCheckIntervals(feed, false);

            pollData.setCheckInterval(feed.getUpdateInterval());

            // add poll data object to series of poll data
            // feed.getPollDataSeries().add(pollData);

            feed.addToBenchmarkLookupTime(feed.getUpdateInterval() * DateHelper.MINUTE_MS);

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            feed.setHistoryFileCompletelyRead(true);
            feed.setBenchmarkLastLookupTime(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND);
        }

        // feed.increaseChecks();

    }

}
