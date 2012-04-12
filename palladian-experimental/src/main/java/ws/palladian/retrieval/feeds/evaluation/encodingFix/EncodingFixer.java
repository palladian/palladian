/**
 * 
 */
package ws.palladian.retrieval.feeds.evaluation.encodingFix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;

/**
 * <p>Quick'n'dirty</p>
 * 
 * <p>Required for CIKM feed dataset paper using TUDCS2 dataset. On one machine, the encoding has temporarily been changed,
 * so non-ASCII characters have been written as "?" to *.csv and *.gz files.</p>
 * 
 * <p>Detects duplicate items like
 * orig: "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
 * bad_: "?????hk????????????? ???? nputSemicolonHere??l?"</p>
 * 
 * 
 * @author Sandro Reichert
 * 
 */
public class EncodingFixer extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EncodingFixer.class);

    private final Feed feed;

    /** all line numbers containing a MISS-line */
    private List<Integer> linesContainingMISS = new ArrayList<Integer>();

    public EncodingFixer(Feed feed) {
        this.feed = feed;
    }

    // FIXME: debug hack
    public EncodingFixer() {
        feed = null;
    }

    @Override
    public void run() {
        try {
            // get path to csv, taken from DatasetCreator
            // FIXME: debug hack
            String csvPath = "";
            if (feed == null) {
                csvPath = "data/datasets/feedPosts/344_http___159_35_251_13_index_php.csv";
            } else {
                String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "")
                        .replaceFirst("www.", ""), 30);

                int slice = (int) Math.floor(feed.getId() / 1000.0);

                String folderPath = DatasetCreator.DATASET_PATH + slice + "/" + feed.getId() + "/";
                csvPath = folderPath + feed.getId() + "_" + safeFeedName + ".csv";
            }
            // read csv
            LOGGER.debug("processing: " + csvPath);
            if (!FileHelper.fileExists(csvPath)) {
                LOGGER.fatal("No csv file found for feed id " + feed.getId() + ", tried to get file " + csvPath
                        + ". Nothing to do for this feed.");
                return;
            }
            File originalCSV = new File(csvPath);
            List<String> items = FileHelper.readFileToArray(originalCSV);

            boolean feedContainsMiss = false;
            List<String[]> splitItems = new ArrayList<String[]>();

            int lineCount = 0;
            for (String item : items) {
                lineCount++;
                String[] split = item.split(";");
                if (split[0].startsWith("MISS")) {
                    feedContainsMiss = true;
                    linesContainingMISS.add(lineCount);
                }
                splitItems.add(split);
            }

            List<String[]> deduplicatedItems = new ArrayList<String[]>();

            if (feedContainsMiss) {

                int currentLine = 0;

                for (String[] currentItem : splitItems) {
                    currentLine++;

                    // search for first MISS-Line
                    // if (!duplicateSearch) {
                    // deduplicatedItems.add(currentItem);
                    // LOGGER.trace("adding to deduplicatedItems: " + restoreCSVString(currentItem));
                    // if (currentItem[0].startsWith("MISS")) {
                    // LOGGER.trace("found first MISS at line: " + currentLine);
                    // duplicateSearch = true;
                    // }
                    // } else {
                    if (currentItem[0].startsWith("MISS")) {
                        deduplicatedItems.add(currentItem);
                        LOGGER.trace("adding to deduplicatedItems: " + restoreCSVString(currentItem));
                    } else {

                            // now search for duplicate of this item
                        int duplicateCandidateLine = 0;
                        boolean duplicateFound = false;
                        for (String[] duplicateCandidate : splitItems) {
                            duplicateCandidateLine++;
                            if (currentLine != duplicateCandidateLine
                                    && currentItem[0].equals(duplicateCandidate[0])
                                    && currentItem[1].length() == duplicateCandidate[1].length()
                                    && isEncodingDuplicate(currentItem[1], duplicateCandidate[1])
                                    && currentItem[2].equals(duplicateCandidate[2])) {
                                if (isOriginal(currentItem[1])) {
                                    deduplicatedItems.add(currentItem);
                                    LOGGER.info("found duplicate lines " + currentLine + " and "
                                            + duplicateCandidateLine + " in file " + csvPath + "\n" + "original:  "
                                            + restoreCSVString(currentItem) + " -> added\n" + "duplicate: "
                                            + restoreCSVString(duplicateCandidate));
                                } else {
                                    LOGGER.info("found duplicate lines " + currentLine + " and "
                                            + duplicateCandidateLine + " in file " + csvPath + "\n" + "original:  "
                                            + restoreCSVString(duplicateCandidate) + "\n" + "duplicate: "
                                            + restoreCSVString(currentItem));
                                }
                                duplicateFound = true;
                                break;
                                }
                            }
                        if (!duplicateFound) {
                            deduplicatedItems.add(currentItem);
                            LOGGER.trace("adding to deduplicatedItems: " + restoreCSVString(currentItem));
                        }
                        }
                    // }
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("intermediate List:");
                    for (String[] currentItem : deduplicatedItems) {
                        LOGGER.trace(restoreCSVString(currentItem));
                    }
                }

                // find and remove consecutive MISS lines (they result if csv contained a block of encoding-misses and
                // we eliminated all of them and csv was like this (see 142924_d3p_co_jp_rss_mobile_rdf.csv):
                // normal item 1
                // normal item 2
                // MISS-Line
                // item 1 with encoding bug
                // item 2 with encoding bug
                // MISS-Line
                // normal item 1
                // normal item 2
                List<String> finalItems = new ArrayList<String>();
                boolean lastLineMISS = false;
                for (String[] currentItem : deduplicatedItems) {
                    boolean currentLineMISS = currentItem[0].startsWith("MISS");
                    if (currentLineMISS) {
                        if (lastLineMISS) {
                            continue;
                        } else {
                            lastLineMISS = true;
                            finalItems.add(restoreCSVString(currentItem));
                        }

                    } else {
                        finalItems.add(restoreCSVString(currentItem));
                        lastLineMISS = false;
                    }
                }

                LOGGER.trace("final List:");
                for (String currentItem : finalItems) {
                    LOGGER.trace(currentItem);
                }

                boolean backupOriginal = originalCSV.renameTo(new File(csvPath + ".bak"));
                boolean newFileWritten = false;
                if (backupOriginal) {
                    newFileWritten = FileHelper.writeToFile(csvPath, finalItems);
                }
                if (!backupOriginal || !newFileWritten) {
                    LOGGER.fatal("could not write output file, dumping to log:\n" + finalItems);
                }

            } else {
                LOGGER.info("Nothing to do for file " + csvPath);
            }
            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
        }

    }

    private boolean isOriginal(String title) {
        // TODO Auto-generated method stub
        return !title.equals(StringHelper.removeNonAsciiCharacters(title));
    }
    
    private String restoreCSVString(String[] toPrint) {
        String result = "";
        for (String part : toPrint) {
            result += part + ";";
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * Two titles are encoding duplicates if the strings are not equal but equal after removing non-ASCII chars. eg:
     * stringA = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
     * stringB = "?????hk????????????? ???? nputSemicolonHere??l?"
     * strings are encoding duplicates
     * 
     * stringC = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
     * stringD = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
     * strings are *not* encoding duplicates since they are real duplicates
     * 
     * @param stringA
     * @param stringB
     * @return
     */
    private static boolean isEncodingDuplicate(String stringA, String stringB) {
        boolean encodingDuplicate = false;
        String aStriped = StringHelper.removeNonAsciiCharacters(stringA.replace("?", "").replace("–", ""));
        String bStriped = StringHelper.removeNonAsciiCharacters(stringB.replace("?", "").replace("–", ""));
        encodingDuplicate = (!stringA.equals(stringB) && aStriped.equals(bStriped));
        return encodingDuplicate;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        EncodingFixer fixer = new EncodingFixer();
        fixer.run();

        // String boese = "?????hk????????????? ???? nputSemicolonHere??l?";
        // String gut = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹";
        // boolean equal = isTitleEqual(gut, boese);
        // System.out.println("bös: " + boese);
        // System.out.println("gut: " + gut);
        // System.out.println("gleich?: " + equal);

    }

}
