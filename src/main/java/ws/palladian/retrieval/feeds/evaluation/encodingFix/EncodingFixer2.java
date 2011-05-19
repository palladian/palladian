/**
 * 
 */
package ws.palladian.retrieval.feeds.evaluation.encodingFix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;

/**
 * Quick'n'dirty
 * 
 * Detect real and encoding duplicates within one window
 * 
 * Required for CIKM feed dataset paper using TUDCS2 dataset. On one machine, the encoding has temporarily been changed,
 * so non-ASCII characters have been written as "?" to *.csv and *.gz files.
 * Detects duplicate items like
 * orig: "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹"
 * bad_: "?????hk????????????? ???? nputSemicolonHere??l?"
 * 
 * 
 * @author Sandro Reichert
 * @author Philipp Katz
 * 
 */
public class EncodingFixer2 extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EncodingFixer2.class);

    private Feed feed;

    /** all line numbers containing a MISS-line */
    private List<Integer> linesContainingMISS = new ArrayList<Integer>();

    private int windowSize = 0;

    private BoundedFifoBuffer windowBuffer = null;

    private List<String[]> deduplicatedItems = new ArrayList<String[]>();

    public static final String BACKUP_FILE_EXTENSION = ".original";

    /**
     * @param feed the Feed to process
     */
    public EncodingFixer2(Feed feed) {
        this.feed = feed;
    }

    /**
     * For usage in JUnit Test only!
     */
    /* package */ EncodingFixer2() {

    }

    @Override
    public void run() {
        try {
            // get path to csv, taken from DatasetCreator
            String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "")
                    .replaceFirst("www.", ""), 30);

            int slice = (int) Math.floor(feed.getId() / 1000.0);

            String folderPath = DatasetCreator.DATASET_PATH + slice + "/" + feed.getId() + "/";
            String csvPath = folderPath + feed.getId() + "_" + safeFeedName + ".csv";

            // read csv
            LOGGER.debug("processing: " + csvPath);
            if (!FileHelper.fileExists(csvPath)) {
                LOGGER.fatal("No csv file found for feed id " + feed.getId() + ", tried to get file " + csvPath
                        + ". Nothing to do for this feed.");
                return;
            }
            File originalCSV = new File(csvPath);
            List<String> items = readCsv(csvPath);

            // do the actual de-duplication magic
            List<String> finalItems = deduplicate(items, csvPath);

            // store new list if we found at least one duplicate (the new list another size than the original)
            if (finalItems.size() != items.size()) {

                boolean backupOriginal = originalCSV.renameTo(new File(csvPath + BACKUP_FILE_EXTENSION));
                boolean newFileWritten = false;
                if (backupOriginal) {
                    newFileWritten = FileHelper.writeToFile(csvPath, finalItems);
                }
                if (backupOriginal && newFileWritten) {
                    LOGGER.info("Found misses for feed id " + feed.getId() + ", new file written to " + csvPath);
                } else {
                    LOGGER.fatal("could not write output file, dumping to log:\n" + finalItems);
                }
            } else {
                LOGGER.debug("No dupliates found in  to do for file " + csvPath);
            }

            // } else {
            // LOGGER.debug("Nothing to do for file " + csvPath);
            // }
            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
        }

    }

    /* package */ static List<String> readCsv(String csvPath) {
        //List<String> items = FileHelper.readFileToArray(csvPath);
        //return items;
        
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

    /* package */ List<String> deduplicate(List<String> items, String csvPath) {
        String[] missLine = null;
        List<String[]> splitItems = new ArrayList<String[]>();

        int lineCount = 0;
        for (String item : items) {
            lineCount++;
            String[] split = item.split(";");
            if (split[0].startsWith("MISS")) {
                missLine = split;
                linesContainingMISS.add(lineCount);
            } else if (windowSize <= Integer.parseInt(split[5])) {
                windowSize = Integer.parseInt(split[5]);
            }
            splitItems.add(split);
        }

        // buffer size = window size + 1 to store 1 MISS in buffer
        windowBuffer = new BoundedFifoBuffer(windowSize + 1);

        boolean recentLineWasMiss = false;

        for (int currentLineNr = splitItems.size(); currentLineNr >= 1; currentLineNr--) {

            String[] currentItem = splitItems.get(currentLineNr - 1);

            // ignore MISS in last line of csv
            if (currentItem[0].equals("MISS") && windowBuffer.isEmpty()) {
                LOGGER.debug("last line in csv was MISS - removed it.");
                // we do not need to store this miss to recentLineWasMiss!
                continue;
            }

            // keep MISS in mind, if its between items
            if (currentItem[0].equals("MISS")) {
                recentLineWasMiss = true;
                continue;
            }

            // does buffer contain currentItem?
            boolean encodingDuplicate = false;
            Iterator<?> iterator = windowBuffer.iterator();
            while (iterator.hasNext()) {
                String[] bufferdItem = (String[]) (iterator.next());
                if (currentItem[0].equals(bufferdItem[0]) && currentItem[1].length() == bufferdItem[1].length()
                        && isDuplicate(currentItem[1], bufferdItem[1]) && currentItem[2].equals(bufferdItem[2])) {
                    encodingDuplicate = true;
                    // we know, that the feed has been checked before, so the item in the buffer is the
                    // original and the current line is the encoding duplicate. Therefore, we do not
                    // put the duplicate to buffer but discard it.
                    LOGGER.debug("found duplicate lines in file " + csvPath + "\n" + "original:  "
                            + restoreCSVString(bufferdItem) + " -> added\n" + "duplicate: "
                            + restoreCSVString(currentItem));
                    break;
                }
            }
            if (!encodingDuplicate) {
                // remember the miss - since we did not found an encoding duplicate, it is a real miss
                // and we need to write it to the buffer
                if (recentLineWasMiss) {
                    addToBuffer(missLine);
                    recentLineWasMiss = false;
                }
                addToBuffer(currentItem);
            } else if (recentLineWasMiss) {
                recentLineWasMiss = false;
            }
        }

        // get remaining Elements from buffer.
        Iterator<?> iterator = windowBuffer.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            String[] currentItem = (String[]) (windowBuffer.remove());
            deduplicatedItems.add(currentItem);
            LOGGER.trace("adding to deduplicated items: " + restoreCSVString(currentItem));
        }

        List<String> finalItems = new ArrayList<String>();

        LOGGER.trace("final List:");
        for (String[] currentItem : deduplicatedItems) {
            finalItems.add(restoreCSVString(currentItem));
            LOGGER.trace(restoreCSVString(currentItem));
        }
        
        // reverse to get original order of csv
        List<String> reversedItems = CollectionHelper.reverse(finalItems);

        return reversedItems;
    }

    private void addToBuffer(String[] itemToAdd) {
        // if full, store last element
        if (windowBuffer.maxSize() == windowBuffer.size()) {
            String[] leastRecentItem = (String[]) (windowBuffer.remove());
            deduplicatedItems.add(leastRecentItem);
            LOGGER.trace("adding to deduplicated items: " + restoreCSVString(leastRecentItem));
        }
        windowBuffer.add(itemToAdd);
        LOGGER.trace("adding to windowbuffer: " + restoreCSVString(itemToAdd));
    }

    private boolean isOriginal(String title) {
        // TODO Auto-generated method stub
        return !title.equals(StringHelper.removeNonAsciiCharacters(title));
    }

    /**
     * Checks whether there is a MISS-line between the two given lines
     * 
     * @param itemA
     * @param itemB
     * @return true if there is a MISS-line between the two given lines
     */
    private boolean missBetweenLines(int itemA, int itemB) {
        boolean missBetween = false;
        if (itemA != itemB) {
            int low = 0;
            int high = 0;
            if (itemA < itemB) {
                low = itemA;
                high = itemB;
            } else {
                low = itemB;
                high = itemA;
            }
            for (Integer missline : linesContainingMISS) {
                if (low < missline && high > missline) {
                    missBetween = true;
                    break;
                }
            }
        }
        return missBetween;
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

    // detect encoding and normal duplicates
    /* package */ static boolean isDuplicate(String stringA, String stringB) {
        String aStriped = StringHelper.removeNonAsciiCharacters(stringA.replace("?", "").replace("–", ""));
        String bStriped = StringHelper.removeNonAsciiCharacters(stringB.replace("?", "").replace("–", ""));
        return aStriped.equals(bStriped);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        /*
        EncodingFixer2 fixer = new EncodingFixer2();
        List<String> input = EncodingFixer2.readCsv("src/test/resources/feedDataset/EncodingBug_Goldstandard/90_http___0_tqn_com_6_g_golftrave.encodingBug.csv");
        List<String> gold = EncodingFixer2.readCsv("src/test/resources/feedDataset/EncodingBug_Goldstandard/90_http___0_tqn_com_6_g_golftrave.goldstandard.csv");
        List<String> deduplicate = fixer.deduplicate(input, null);
        System.out.println("#input:" + input.size());
        System.out.println("#dedup:" + deduplicate.size());
        System.out.println("#gold:" + gold.size());
        System.out.println("dedup=gold:" + deduplicate.equals(gold));
        
        CollectionHelper.print(gold);
        CollectionHelper.print(deduplicate);
        */

        // fixer.run();
        // String boese = "?????hk????????????? ???? nputSemicolonHere??l?";
        // String gut = "æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹";
        // boolean equal = isTitleEqual(gut, boese);
        // System.out.println("bös: " + boese);
        // System.out.println("gut: " + gut);
        // System.out.println("gleich?: " + equal);

    }

}
