package ws.palladian.retrieval.feeds.evaluation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.feeds.Feed;

/**
 * @author Sandro Reichert
 * 
 */
public class DatasetMerger extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetMerger.class);

    /** Path to data directory of base dataset for merging. */
    private final String basePath;

    /** Path to data directory of dataset to merge into base. */
    private final String mergePath;

    /** Path to data directory to write the output to. */
    private final String resultPath;

    /** The {@link Feed} to merge. */
    private final Feed feed;

    /** Items used as base for merging, split into their parts */
    private List<String[]> splitBaseItems = null;

    /** Items used to merge into base, split into their parts */
    private List<String[]> splitMergeItems = null;

    /** Contains all Items after merging */
    private Stack<String> resultItems = new Stack<String>();

    /** A MISS line, separated into rows */
    private static final String[] MISS = { "MISS", "MISS", "MISS", "MISS", "MISS", "MISS" };

    /** Remember the position in splitMergeItems to not completely scan the list each time we found a MISS */
    private int mergeLine = -1;

    /**
     * If <code>true</code>, at least one of the csv files is corrupted, that is, the items are not sorted by
     * timestamps.
     */
    private boolean corruptedMode = false;

    /** number of MISSes in base */
    private int missCounterBase = 0;

    /** number of MISSes in result */
    private int missCounterResult = 0;

    /**
     * @param feed The {@link Feed} to merge.
     * @param basePath Path to data directory of base dataset for merging.
     * @param mergePath Path to data directory of dataset to merge into base.
     * @param resultPath Path to data directory to write the output to.
     */
    public DatasetMerger(Feed feed, String basePath, String mergePath, String resultPath) {
        this.feed = feed;
        this.basePath = addSlashToPath(basePath);
        this.mergePath = addSlashToPath(mergePath);
        this.resultPath = addSlashToPath(resultPath);
    }

    @Override
    public void run() {
        try {
            LOGGER.debug("processing feed " + feed.getId());
            String dataPath = getRelativePathToFeed();
            String csvFileName = getCSVFileName();

            // read csv files
            if (!FileHelper.fileExists(basePath + dataPath + csvFileName)) {
                LOGGER.fatal("No csv base file found for feed id " + feed.getId() + ", tried to read file " + basePath
                        + dataPath + csvFileName + ". Nothing to do for this feed.");
                return;
            }
            if (!FileHelper.fileExists(mergePath + dataPath + csvFileName)) {
                LOGGER.fatal("No csv file to merge into base found for feed id " + feed.getId()
                        + ", tried to read file " + mergePath + dataPath + csvFileName
                        + ". Nothing to do for this feed.");
                return;
            }
            // get items from file
            List<String> baseItems = readCsv(basePath + dataPath + csvFileName);
            List<String> mergeItems = readCsv(mergePath + dataPath + csvFileName);

            // split items into parts
            splitBaseItems = splitItems(baseItems);
            splitMergeItems = splitItems(mergeItems);

            checkForTimestampBug(splitBaseItems, basePath + dataPath + csvFileName);
            checkForTimestampBug(splitMergeItems, mergePath + dataPath + csvFileName);

            // search bottom-up for MISS in base file
            boolean recentLineWasMiss = false;
            String[] recentItem = null;

            for (int currentLineNr = splitBaseItems.size(); currentLineNr >= 1; currentLineNr--) {

                String[] currentItem = splitBaseItems.get(currentLineNr - 1);

                // ignore MISS in bottom line of csv
                if (resultItems.isEmpty() && currentItem[0].equals("MISS")) {
                    LOGGER.debug("last line in csv was MISS - removed it.");
                    // we do not need to store this miss to recentLineWasMiss!
                    missCounterBase++;
                    continue;
                }

                // keep MISS in mind
                if (currentItem[0].equals("MISS")) {
                    recentLineWasMiss = true;
                    missCounterBase++;
                    continue;
                }

                // try to eliminate the MISS by copying items from file to merge into base
                if (recentLineWasMiss) {
                    mergeItemsIntoBase(recentItem, currentItem);
                    recentLineWasMiss = false;
                }

                resultItems.push(restoreCSVString(currentItem));
                recentItem = currentItem;
            }

            // TODO: copy all items from merge that are newer than newest item in base (and their gzs)


            // write output to file

            List<String> resultItemList = getResultAsList();

            boolean resultFileWritten = FileHelper.writeToFile(resultPath + dataPath + csvFileName, resultItemList);
            if (resultFileWritten) {
                LOGGER.info("Finished processing feed id " + feed.getId() + " base MISSes: " + missCounterBase
                        + ", result MISSes: " + missCounterResult);
            } else {
                LOGGER.fatal("Could not write output file for feed id " + feed.getId());
            }


            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
        }
    }


    /**
     * Try to fix the MISS by searching {@link #splitMergeItems} for itemBeforeMiss and itemAfterMiss
     * 
     * @param itemBeforeMiss The last item before we found a MISS in the base.
     * @param itemAfterMiss The next item after (newer) the MISS.
     */
    private void mergeItemsIntoBase(String[] itemBeforeMiss, String[] itemAfterMiss) {

        LOGGER.debug("feed id " + feed.getId() + " try to compensate MISS between itemBeforeMiss: "
                + restoreCSVString(itemBeforeMiss) + " and itemAfterMiss: "
                + restoreCSVString(itemAfterMiss));

        // if first method call or corrupted mode, start from bottom of file to merge into base
        if (corruptedMode || mergeLine == -1) {
            mergeLine = splitMergeItems.size();
        }

        boolean beforeMISS = true;
        boolean afterMISS = true;
        boolean searchMode = true;
        // set to true if we copied at least one item
        boolean copyGZs = false;

        List<String> itemBuffer = new ArrayList<String>();

        for (int currentLineNr = mergeLine; currentLineNr >= 1; currentLineNr--) {
            String[] currentMergeItem = splitMergeItems.get(currentLineNr - 1);

            // stop walking through items to merge if current item is younger than what we are looking for
            // do not increase mergeLine in this case
            if (!corruptedMode && foundNewerItem(itemAfterMiss, currentMergeItem, false)) {
                break;
            }

            if (searchMode) {
                // search items to merge for itemBeforeMiss or newer item

                if (isDuplicate(itemBeforeMiss, currentMergeItem)) {
                    searchMode = false;
                    beforeMISS = false;
                } else if (!corruptedMode && foundNewerItem(itemBeforeMiss, currentMergeItem, false)) {
                    // we found a newer one but not the one we were looking for so there still might be a miss
                    searchMode = false;
                    LOGGER.debug("feed id " + feed.getId()
                            + " has still a MISS, didn't find itemBeforeMiss in items to merge.");
                    itemBuffer.add(restoreCSVString(MISS));
                    missCounterResult++;
                }

            } else {
                // copy all subsequent items till we reach itemAfterMiss
                if (isDuplicate(itemAfterMiss, currentMergeItem)) {
                    // do not copy itemAfterMiss itself, this is done by the calling method
                    afterMISS = false;
                    break;
                }

                if (currentMergeItem[0].equals("MISS")) {
                    missCounterResult++;
                }

                itemBuffer.add(restoreCSVString(currentMergeItem));
            }

            mergeLine = currentLineNr;
        }

        // if we did not found itemAfterMiss in file to merge, we still might missed some items so we have to add a miss
        // line to the result
        if (afterMISS) {
            itemBuffer.add(restoreCSVString(MISS));
            missCounterResult++;
            LOGGER.debug("feed id " + feed.getId() + " has still a MISS, didn't find itemAfterMiss in items to merge.");
        }
        
        // write buffer to output only in case we are in normal mode (not in corruptedMode) or if we found both items have been found in file to merge 
        if (!corruptedMode || (!beforeMISS && !afterMISS)) {
            resultItems.addAll(itemBuffer);
            copyGZs = true;
        }

        // if we copied at least one item, we also have to copy the gz files
        if (copyGZs) {
            // TODO: copy files here!
        }
    }

    /**
     * Checks whether input files have correct sorting of items by timestamps. If the file is corrupted,
     * {@link #corruptedMode} is set to <code>true</code>.
     * 
     * @param list The list to check.
     * @param filePath For logging purpose, the corrupted file's path.
     */
    private void checkForTimestampBug(List<String[]> list, String filePath) {
        // do check only if we are not already in corruptedMode
        if (!corruptedMode) {
            String[] lastItem = null;
            for (String[] item : list) {
                if (lastItem == null) {
                    lastItem = item;
                    continue;
                }
                if (!foundNewerItem(item, lastItem, true)) {
                    corruptedMode = true;
                    LOGGER.error("Feed id " + feed.getId()
                            + " has corrupted sorting of items, they are not ordered by timestamp. Items: "
                            + restoreCSVString(item) + " and " + restoreCSVString(lastItem) + " in file " + filePath);
                    break;
                }
                lastItem = item;
            }
        }
    }

    /**
     * Checks whether the two items are duplicates, using parts 0,1 and 2 as comparison key (timestamp, title and link)
     * 
     * @param baseItem input 1
     * @param mergeItem input 2
     * @return true if items are duplicates, false otherwise
     */
    private boolean isDuplicate(String[] baseItem, String[] mergeItem) {
        boolean duplicate = false;
        if (baseItem[0].equals(mergeItem[0]) && baseItem[1].equals(mergeItem[1]) && baseItem[2].equals(mergeItem[2])) {
            duplicate = true;
        }
        return duplicate;
    }

    /**
     * Returns <code>true</code> if oldItem is older than newItem, or if lessOrEqual is <code>true</code> and they are
     * equal, <code>false</code> otherwise.
     * 
     * @param oldItem Item that is expected to be older.
     * @param newItem Item that is expected to be newer.
     * @param lessOrEqual If set to <code>true</code>, the other parameters are also checked whether they are true.
     * @return <code>true</code> if oldItem is older than newItem, or if lessOrEqual is <code>true</code> and they are
     *         equal, <code>false</code> otherwise.
     */
    private boolean foundNewerItem(String[] oldItem, String[] newItem, boolean lessOrEqual) {
        boolean foundNewer = false;
        if (!oldItem[0].equals("MISS") && !newItem[0].equals("MISS")) {
            long timestampOld = Long.parseLong(oldItem[0]);
            long timestampNew = Long.parseLong(newItem[0]);
            if (timestampOld < timestampNew || lessOrEqual && timestampOld == timestampNew) {
                foundNewer = true;
            }
        }
        return foundNewer;
    }

    /**
     * Splits each item into its parts, using ";" as separator.
     * 
     * @param items Items to split
     * @return List with items split into their parts.
     */
    private List<String[]> splitItems(List<String> items) {
        List<String[]> splitItems = new ArrayList<String[]>();
        for (String item : items) {
            splitItems.add(item.split(";"));
        }
        return splitItems;
    }

    /**
     * Restores splitted items into single line to write to csv, using ";" as separator
     * 
     * @param toPrint
     * @return
     */
    private String restoreCSVString(String[] toPrint) {
        StringBuilder result = new StringBuilder();
        for (String part : toPrint) {
            result.append(part).append(";");
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * Caution! TUDCS2 specific! use in last run only!
     * Removes the columns containing item and feed size, restores splitted item into single line to write to csv, using
     * ";" as separator
     * 
     * @param toPrint
     * @return
     */
    private String removeUnusedColumnsAndRestoreCSVString(String[] toPrint) {
        StringBuilder result = new StringBuilder();
        result.append(toPrint[0]).append(";"); // item timestamp
        result.append(toPrint[1]).append(";"); // title
        result.append(toPrint[2]).append(";"); // link
        result.append(toPrint[5]); // window size
        return result.toString();
    }


    /* package */static List<String> readCsv(String csvPath) {
        // List<String> items = FileHelper.readFileToArray(csvPath);
        // return items;

        // FileHelper does not set an encoding explicitly, this causes trouble, when our test cases
        // are run via maven. I suppose the problem has to do with a different default encoding,
        // although everything seems to be configured correctly at first glance. We should think whether
        // it makes sense to always explicitly set UTF-8 when reading files. -- Philipp.
        BufferedReader reader = null;
        List<String> result = new ArrayList<String>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(reader);
        }
        return result;

    }

    /**
     * Relative path to feed data, such as "data/datasets/feedPosts/0/100/" for feed id 100.
     * 
     * @return relative path to feed data.
     */
    private String getRelativePathToFeed() {

        int slice = (int) Math.floor(feed.getId() / 1000.0);
        String folderPath = DatasetCreator.DATASET_PATH + slice + "/" + feed.getId() + "/";
        LOGGER.debug("Relative Path to feed: " + folderPath);
        return folderPath;
    }

    /**
     * Add trailing slash to path if not already there
     * 
     * @param path The path to check.
     * @return path ending with / as path separator.
     */
    private String addSlashToPath(String path){
        String returnPath = path;
        if (!returnPath.endsWith("/")) {
            returnPath += "/";
        }
        return returnPath;
    }

    /**
     * @return The name of the csv file.
     */
    private String getCSVFileName() {
        String safeFeedName = StringHelper.makeSafeName(
                feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst("www.", ""), 30);
        String fileName = feed.getId() + "_" + safeFeedName + ".csv";
        LOGGER.debug("CSV filename: " + fileName);
        return fileName;
    }

    /**
     * Returns the contents of {@link #resultItems} as a {@link List<{@link String}>}.
     * 
     * @return The contents of {@link #resultItems} as a {@link List<{@link String}>}.
     */
    private List<String> getResultAsList() {
        List<String> output = new ArrayList<String>(resultItems.size());

        while (!resultItems.isEmpty()) {
            output.add(resultItems.pop());
        }
        return output;
    }

    public static void main(String[] args) {
        Feed feed = new Feed();
        feed.setId(1001);
        feed.setFeedUrl("http://511virginia.org/rss/Northern/Events.ashx");
        DatasetMerger merger = new DatasetMerger(feed, "data/test/base", "data/test/mergeInto",
                "data/test/mergedResults");
        merger.run();
    }

}
