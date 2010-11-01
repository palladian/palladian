package tud.iir.news;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.news.evaluation.FeedReaderEvaluator;
import tud.iir.news.statistics.PollData;

public class FeedBenchmarkFileReader {

    /** The timestamp we started the dataset gathering. 28/10/2010 */
    public static final long BENCHMARK_START_TIME = 1285689600000l;

    /** The timestamp we stopped the dataset gathering. 26/10/2010 */
    public static final long BENCHMARK_STOP_TIME = 1288108800000l;

    protected static final Logger LOGGER = Logger.getLogger(FeedBenchmarkFileReader.class);

    private Feed feed;
    private FeedChecker feedChecker;
    private List<String> historyFileLines;
    private String historyFilePath = "";
    private int totalEntries = 0;

    /** The cumulated delay of early lookups + the last cumulatedPoll delay when at least one new item was found. */
    private long cumulatedDelay = 0l;

    /**
     * We need to loop through the file many times, to expedite the process we save the last index position where the
     * window started (actually one entry before the window so we can calculate a delay to the next entry).
     */
    private int lastStartIndex = 1;

    public FeedBenchmarkFileReader(Feed feed, FeedChecker feedChecker) {
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

    public FeedChecker getFeedChecker() {
        return feedChecker;
    }

    public void setFeedChecker(FeedChecker feedChecker) {
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
     * 
     * <p>
     * We need to handle "On-The-Fly" feeds special (we disregard them here), because their timestamps should be at
     * simulation time.
     * </p>
     */
    public void updateEntriesFromDisk() {

        try {
            List<FeedEntry> entries = new ArrayList<FeedEntry>();

            // the timestamp of the first item in the window
            long firstItemInWindowTimestamp = -1l;
            
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
            int newEntries = 0;

            boolean windowStartIndexFound = false;

            for (int i = lastStartIndex; i <= totalEntries; i++) {

                String line = historyFileLines.get(i - 1);

                String[] parts = line.split(";");

                // skip MISS lines
                // if (parts[0].equalsIgnoreCase("miss")) {
                // return;
                // }

                if (feed.getWindowSize() == -1) {
                    int windowSize = Integer.valueOf(parts[5]);
                    if (windowSize > totalEntries) {
                        windowSize = totalEntries;
                    }
                    if (windowSize > 1000) {
                        LOGGER.info("feed has a window size of " + windowSize + " and will be discarded");
                        feed.setHistoryFileCompletelyRead(true);
                        feed.setBenchmarkLastLookupTime(BENCHMARK_STOP_TIME);
                        return;
                    }
                    feed.setWindowSize(windowSize);
                }

                long entryTimestamp = Long.valueOf(parts[0]);

                // FIXME remove
                // if (entryTimestamp < 1000000000000l) {
                // feed.setHistoryFileCompletelyRead(true);
                // feed.setBenchmarkLookupTime(BENCHMARK_STOP_TIME);
                // return;
                // }

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
                            && entryTimestamp > BENCHMARK_START_TIME && totalEntries - i + 1 == feed.getWindowSize()) {
                        LOGGER.error("we disregard this feed (" + feed.getId()
                                + ") since it does not comply with our start date "
                                + entryTimestamp);
                        feed.setHistoryFileCompletelyRead(true);
                        feed.setBenchmarkLastLookupTime(BENCHMARK_STOP_TIME);
                        return;
                    }

                    // add up download size (head and foot if necessary and post size itself)
                    if (!footHeadAdded) {
                        totalBytes = totalBytes + Math.max(100, Integer.valueOf(parts[4]));
                        footHeadAdded = true;
                    }

                    totalBytes += Math.max(0, Integer.valueOf(parts[3]));

                    // create feed entry
                    FeedEntry feedEntry = new FeedEntry();
                    feedEntry.setPublished(new Date(entryTimestamp));
                    feedEntry.setTitle(parts[1]);
                    feedEntry.setLink(parts[2]);

                    entries.add(feedEntry);

                    // for all post entries in the window that are newer than the last lookup time we need to sum up the
                    // delay to the current lookup time (and weight it)
                    if (entryTimestamp > feed.getBenchmarkLastLookupTime() && feed.getChecks() > 0) {

                        cumulatedPollDelay += feed.getBenchmarkLookupTime() - entryTimestamp;

                        // count new entry
                        newEntries++;
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

                // process post entries between the end of the current window and the last lookup time
                else if (entryTimestamp <= lastItemInWindowTimestamp
                        && entryTimestamp > feed.getBenchmarkLastLookupTime()) {

                    cumulatedPollDelay += feed.getBenchmarkLookupTime() - entryTimestamp;

                    // count post entry as miss
                    misses = misses + 1;
                    // System.out.println("miss");
                } else if (entryTimestamp <= feed.getBenchmarkLastLookupTime()) {
                    break;
                }

            }

            // if no new entry was found, we add the delay to the next new post entry
            if (newEntries == 0 && nextItemBeforeWindowTimestamp != 0l && feed.getChecks() > 0) {
                cumulatedPollDelay = feed.getBenchmarkLookupTime() - nextItemBeforeWindowTimestamp;
            }

            cumulatedDelay += Math.abs(cumulatedPollDelay);

            feed.setEntries(entries);

            // now that we set the entries we can add information about the poll to the poll series
            PollData pollData = new PollData();

            pollData.setBenchmarkType(FeedReaderEvaluator.benchmarkPolicy);

            // only checks after the first one get a min score
            if (feed.getChecks() > 0 && newEntries > 0 && nextItemBeforeWindowTimestamp != 0l) {

                if (feed.getChecks() > 0 && secondItemInWindowTimestamp < 0) {
                    secondItemInWindowTimestamp = feed.getLastFeedEntry().getTime();
                }

                long currentInterval = nextItemBeforeWindowTimestamp - firstItemInWindowTimestamp;
                long lastInterval = firstItemInWindowTimestamp - secondItemInWindowTimestamp;
                long surroundingIntervalsLength = currentInterval + lastInterval;
                pollData.setSurroundingIntervalsLength(surroundingIntervalsLength);
                pollData.setCurrentIntervalLength(currentInterval);
                pollData.setCumulatedLateDelay(cumulatedPollDelay);
            }

            pollData.setTimestamp(feed.getBenchmarkLookupTime());
            pollData.setNewWindowItems(newEntries);
            pollData.setMisses(misses);

            pollData.setCumulatedDelay(cumulatedDelay);
            
            // reset cumulated delay for next new item
            if (newEntries > 0) {
                cumulatedDelay = 0;
            }
            
            pollData.setWindowSize(feed.getWindowSize());
            pollData.setDownloadSize(totalBytes);

            pollData.getScore(FeedReaderEvaluator.BENCHMARK_MIN_DELAY);

            // remember the time the feed has been checked
            feed.setLastPollTime(new Date());

            feedChecker.updateCheckIntervals(feed);

            if (FeedReaderEvaluator.benchmarkPolicy == FeedReaderEvaluator.BENCHMARK_MAX_COVERAGE) {
                pollData.setCheckInterval(feed.getMaxCheckInterval());
            } else {
                pollData.setCheckInterval(feed.getMinCheckInterval());
            }

            // add poll data object to series of poll data
            feed.getPollDataSeries().add(pollData);

            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_MIN_DELAY) {
                feed.addToBenchmarkLookupTime((long) feed.getMinCheckInterval() * (long) DateHelper.MINUTE_MS);
            } else if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_MAX_COVERAGE) {
                feed.addToBenchmarkLookupTime((long) feed.getMaxCheckInterval() * (long) DateHelper.MINUTE_MS);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            feed.setHistoryFileCompletelyRead(true);
            feed.setBenchmarkLastLookupTime(BENCHMARK_STOP_TIME);
        }

        // feed.increaseChecks();

    }

}
