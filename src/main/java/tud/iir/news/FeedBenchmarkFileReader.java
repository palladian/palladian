package tud.iir.news;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StringHelper;
import tud.iir.news.statistics.PollData;

public class FeedBenchmarkFileReader {

    protected static final Logger LOGGER = Logger.getLogger(FeedBenchmarkFileReader.class);

    private Feed feed;
    private FeedChecker feedChecker;
    private String historyFileContent = "";
    private List<String> historyFileLines;
    private String historyFilePath = "";
    private int totalEntries = 0;
    private Map<Integer,FeedEntry> feedEntryMap = new HashMap<Integer, FeedEntry>();

    public FeedBenchmarkFileReader(Feed feed, FeedChecker feedChecker) {
        this.feed = feed;
        this.feedChecker = feedChecker;

        String safeFeedName = feed.getId()
                + "_"
                + StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst("www.", ""),
                        30);

        this.historyFilePath = feedChecker.findHistoryFile(safeFeedName);

        // if file doesn't exist skip the feed
        if (!new File(historyFilePath).exists()) {
        	feed.setHistoryFileCompletelyRead(true);
        } else {
        	this.historyFileContent = FileHelper.readFileToString(historyFilePath);
        	this.historyFileLines = FileHelper.readFileToArray(historyFilePath);
            this.totalEntries = historyFileLines.size();
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
     * For benchmarking purposes, we created a dataset of feed post histories and stored it on disk. We can now run the
     * feed reader on this dataset and evaluate the updateInterval techniques.
     */
    public void updateEntriesFromDisk() {

        List<FeedEntry> entries = new ArrayList<FeedEntry>();

        long lastEntryInWindowTimestamp = Long.MIN_VALUE;
        int misses = 0;
        long totalBytes = 0;
        boolean footHeadAdded = false;
        
        // the cumulated delay of the lookup times in milliseconds (in benchmark min mode), this can happen when we read
        // too early or too late
        long cumulatedDelay = 0l;
        
        // get hold of the post entry just before the window starts, this way we can determine the delay in case
        // there are no new entries between lookup time and last lookup time
        long postEntryBeforeWindowTime = 0l;
        
        // count new entries, they must be between lookup time and last lookup time
        int newEntries = 0;
   
        for (int i = 1; i <= totalEntries; i++) {
        	
        	String line = historyFileLines.get(i-1);
        	
            String[] parts = line.split(";");

            // skip MISS lines
//            if (parts[0].equalsIgnoreCase("miss")) {
//                return;
//            }

            feed.setWindowSize(Integer.valueOf(parts[5]));

            long entryTimestamp = Long.valueOf(parts[0]);

            // get hold of the post entry just before the window starts
            if (entryTimestamp > feed.getBenchmarkLookupTime()) {
                postEntryBeforeWindowTime = entryTimestamp;
            }

            // process post entries that are in the current window
            //if ((entryTimestamp < feed.getBenchmarkLookupTime() && entries.size() < feed.getWindowSize()) || totalEntries - i < feed.getWindowSize()) {
            if ((entryTimestamp < feed.getBenchmarkLookupTime() && entries.size() < feed.getWindowSize()) || totalEntries - i < feed.getWindowSize()) {

                // find the first lookup date if the file has not been read yet
                if (feed.getBenchmarkLookupTime() == Long.MIN_VALUE && totalEntries - i + 1 == feed.getWindowSize()) {
                    feed.setBenchmarkLookupTime(entryTimestamp);
                }

                // add up download size (head and foot if necessary and post size itself)
                if (!footHeadAdded) {
                    totalBytes = totalBytes + Integer.valueOf(parts[4]);
                    footHeadAdded = true;
                }

                totalBytes += Integer.valueOf(parts[3]);

//                FeedEntry fe = feedEntryMap.get(i); 
//                if (fe == null) {
                
	                // create feed entry
	                FeedEntry feedEntry = new FeedEntry();
	                feedEntry.setPublished(new Date(entryTimestamp));
	                feedEntry.setTitle(parts[1]);
	                feedEntry.setLink(parts[2]);
	                
//	                fe = feedEntry;
	                
//	                feedEntryMap.put(i, fe);
//                }

                entries.add(feedEntry);

                // for all post entries in the window that are newer than the last lookup time we need to sum up the
                // delay to the current lookup time (and weight it)
                if (entryTimestamp > feed.getBenchmarkLookupTime() && feed.getChecks() > 0) {
                    cumulatedDelay += (entryTimestamp - feed.getBenchmarkLookupTime());

                    // count new entry
                    newEntries++;
                }

                // if top of the file is reached, we read the file completely and can stop scheduling reading this
                // feed
                if (i == 1) {
                    LOGGER.debug("complete history has been read for feed " + feed.getId() + " (" + feed.getFeedUrl() + ")");
                    feed.setHistoryFileCompletelyRead(true);
                }
                
                // check whether current post entry is the last one in the window
                if (entries.size() == feed.getWindowSize()) {
                    lastEntryInWindowTimestamp = entryTimestamp;
                }
            }

            // process post entries between the end of the current window and the last lookup time
            else if (entryTimestamp < lastEntryInWindowTimestamp && entryTimestamp > feed.getBenchmarkLastLookupTime()) {
                // count post entry as miss
                misses = misses + 1;
                //System.out.println("miss");                
            }

            // process post entries older than last lookup time
            // if no new entry was found, we add the delay to the next new post entry
            else if (entryTimestamp < feed.getBenchmarkLastLookupTime() && newEntries == 0
                    && postEntryBeforeWindowTime != 0l) {
                cumulatedDelay += (postEntryBeforeWindowTime - feed.getBenchmarkLookupTime());
                //System.out.println("break here 1");
                break;
            } else if (entryTimestamp <= feed.getBenchmarkLastLookupTime()) {
            	//System.out.println("break here 2");
            	break;
            }
        
//            if (entryTimestamp < feed.getBenchmarkLastLookupTime()) {
//            	System.out.println(feed.getId());
//            	System.out.println(entries.size());
//            	System.out.println(totalEntries - i < feed.getWindowSize());
//            	System.out.println(totalEntries + "," + i +"," + feed.getWindowSize());
//	            System.out.println("entry  " + entryTimestamp);
//	            System.out.println("current" + feed.getBenchmarkLookupTime());
//	            System.out.println("last   " + feed.getBenchmarkLastLookupTime());
//            }
            
        }
        
        feed.setEntries(entries);

        // now that we set the entries we can add information about the poll to the poll series
        PollData pollData = new PollData();

        pollData.setBenchmarkType(FeedChecker.benchmark);
        pollData.setTimestamp(feed.getBenchmarkLookupTime());
        pollData.setPercentNew(feed.getTargetPercentageOfNewEntries());
        pollData.setMisses(misses);

        if (FeedChecker.benchmark == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
            pollData.setCheckInterval(feed.getMaxCheckInterval());
        } else {
            pollData.setCheckInterval(feed.getMinCheckInterval());
            pollData.setNewPostDelay(cumulatedDelay);
        }

        pollData.setWindowSize(feed.getWindowSize());
        pollData.setDownloadSize(totalBytes);

        // add poll data object to series of poll data
        feed.getPollDataSeries().add(pollData);

        // remember the time the feed has been checked
        feed.setLastChecked(new Date());

        feedChecker.updateCheckIntervals(feed);

        if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MIN_CHECK_TIME) {
            feed.addToBenchmarkLookupTime(feed.getMinCheckInterval() * DateHelper.MINUTE_MS);
        } else if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
            feed.addToBenchmarkLookupTime(feed.getMaxCheckInterval() * DateHelper.MINUTE_MS);
        }
        
        // save the feed back to the database
        //NewsAggregator fa = new NewsAggregator();
        //fa.updateFeed(feed);
    }
    
    /**
     * For benchmarking purposes, we created a dataset of feed post histories and stored it on disk. We can now run the
     * feed reader on this dataset and evaluate the updateInterval techniques.
     */
    public void updateEntriesFromDisk2() {

        List<FeedEntry> entries = new ArrayList<FeedEntry>();

        // create feed entries with: pubdate, title, link
        final Object[] obj = new Object[9];

        // the post entries
        obj[0] = entries;

        // the total number of entries in the file
        obj[1] = totalEntries;

        // timestamp of the last post entry in the window
        obj[2] = Long.MIN_VALUE;

        // number of misses
        obj[3] = 0;

        // total number of bytes of posts in the window (once head+foot and each time size of post)
        obj[4] = 0;

        // whether head and foot download size has been added to current poll
        obj[5] = false;

        // the cumulated delay of the lookup times in milliseconds (in benchmark min mode), this can happen when we read
        // too early or too late
        obj[6] = 0l;

        // get hold of the post entry just before the window starts, this way we can determine the delay in case
        // there are no new entries between lookup time and last lookup time
        obj[7] = 0l;

        // count new entries, they must be between lookup time and last lookup time
        obj[8] = 0;

        LineAction la = new LineAction(obj) {

            @Override
            public void performAction(final String line, final int lineNumber) {

                int totalEntries = (Integer) obj[1];

                String[] parts = line.split(";");

                // skip MISS lines
                if (parts[0].equalsIgnoreCase("miss")) {
                    return;
                }

                feed.setWindowSize(Integer.valueOf(parts[5]));

                long entryTimestamp = Long.valueOf(parts[0]);

                // get hold of the post entry just before the window starts
                if (entryTimestamp > feed.getBenchmarkLookupTime()) {
                    obj[7] = entryTimestamp;
                }

                // process post entries that are in the current window
                if (entryTimestamp < feed.getBenchmarkLookupTime() || totalEntries - lineNumber < feed.getWindowSize()) {

                    // find the first lookup date if the file has not been read yet
                    if (totalEntries - lineNumber + 1 == feed.getWindowSize()
                            && feed.getBenchmarkLookupTime() == Long.MIN_VALUE) {
                        feed.setBenchmarkLookupTime(entryTimestamp);
                    }

                    // add up download size (head and foot if necessary and post size itself)
                    if ((Boolean) obj[5] == false) {
                        obj[4] = (Integer) obj[4] + Integer.valueOf(parts[4]);
                        obj[5] = true;
                    }

                    obj[4] = (Integer) obj[4] + Integer.valueOf(parts[3]);

                    // create feed entry
                    FeedEntry feedEntry = new FeedEntry();
                    feedEntry.setPublished(new Date(entryTimestamp));
                    feedEntry.setTitle(parts[1]);
                    feedEntry.setLink(parts[2]);

                    ((ArrayList<FeedEntry>) obj[0]).add(feedEntry);

                    // check whether current post entry is the last one in the window
                    if (((ArrayList<FeedEntry>) obj[0]).size() == feed.getWindowSize()) {
                        obj[2] = entryTimestamp;
                    }

                    // for all post entries in the window that are newer than the last lookup time we need to sum up the
                    // delay to the current lookup time (and weight it)
                    if (entryTimestamp > feed.getBenchmarkLookupTime() && feed.getChecks() > 0) {
                        obj[6] = (Long) obj[6] + (entryTimestamp - feed.getBenchmarkLookupTime());

                        // count new entry
                        obj[8] = (Integer) obj[8] + 1;
                    }

                    // if top of the file is reached, we read the file completely and can stop scheduling reading this
                    // feed
                    if (lineNumber == 1) {
                        LOGGER.debug("complete history has been read for feed " + feed.getFeedUrl());
                        feed.setHistoryFileCompletelyRead(true);
                    }
                }

                // process post entries between the end of the current window and the last lookup time
                else if (entryTimestamp < (Long) obj[2] && entryTimestamp > feed.getBenchmarkLastLookupTime()) {
                    // count post entry as miss
                    obj[3] = (Integer) obj[3] + 1;
                }

                // process post entries older than last lookup time
                // if no new entry was found, we add the delay to the next new post entry
                else if (entryTimestamp < feed.getBenchmarkLastLookupTime() && (Integer) obj[8] == 0
                        && (Long) obj[7] != 0l) {
                    obj[6] = (Long) obj[6] + ((Long) obj[7] - feed.getBenchmarkLookupTime());
                }

            }

        };

        //FileHelper.performActionOnEveryLine(historyFilePath, la);
        FileHelper.performActionOnEveryLineText(historyFileContent, la);
        feed.setEntries(entries);

        // now that we set the entries we can add information about the poll to the poll series
        PollData pollData = new PollData();

        pollData.setBenchmarkType(FeedChecker.benchmark);
        pollData.setTimestamp(feed.getBenchmarkLookupTime());
        pollData.setPercentNew(feed.getTargetPercentageOfNewEntries());
        pollData.setMisses((Integer) obj[3]);

        if (FeedChecker.benchmark == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
            pollData.setCheckInterval(feed.getMaxCheckInterval());
        } else {
            pollData.setCheckInterval(feed.getMinCheckInterval());
            pollData.setNewPostDelay((Long) obj[6]);
        }

        pollData.setWindowSize(feed.getWindowSize());
        pollData.setDownloadSize((Integer) obj[4]);

        // add poll data object to series of poll data
        feed.getPollDataSeries().add(pollData);

        // remember the time the feed has been checked
        feed.setLastChecked(new Date());

        feedChecker.updateCheckIntervals(feed);

        if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MIN_CHECK_TIME) {
            feed.addToBenchmarkLookupTime(feed.getMinCheckInterval() * DateHelper.MINUTE_MS);
        } else if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
            feed.addToBenchmarkLookupTime(feed.getMaxCheckInterval() * DateHelper.MINUTE_MS);
        }

        // save the feed back to the database
        NewsAggregator fa = new NewsAggregator();
        fa.updateFeed(feed);
    }

}
