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
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;

/**
 * <p>Quick'n'dirty worker thread that remove superfluous feed size and item size columns with "-1" values.</p>
 * 
 * <p>Input format: PUBLISH_TIMESTAMP;TITLE;LINK;-1;-1;WINDOW_SIZE</p>
 * <p>Output format: PUBLISH_TIMESTAMP;TITLE;LINK;WINDOW_SIZE;</p>
 * 
 * <p>Required for CIKM feed dataset paper using TUDCS5 dataset.</p>
 * 
 * @author Sandro Reichert
 * 
 */
public class CsvCleaner extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CsvCleaner.class);

    private Feed feed;

    public static final String BACKUP_FILE_EXTENSION = ".6rows";

    /**
     * @param feed the Feed to process
     */
    public CsvCleaner(Feed feed) {
        this.feed = feed;
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

            List<String> rewrittenCSV = rewrite(items, csvPath);

            boolean backupOriginal = originalCSV.renameTo(new File(csvPath + BACKUP_FILE_EXTENSION));
            boolean newFileWritten = false;
            if (backupOriginal) {
                newFileWritten = FileHelper.writeToFile(csvPath, rewrittenCSV);
            }
            if (backupOriginal && newFileWritten) {
                LOGGER.info("New file written to " + csvPath);
            } else {
                LOGGER.fatal("could not write output file, dumping to log:\n" + rewrittenCSV);
            }

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
        }

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

    /* package */List<String> rewrite(List<String> items, String csvPath) {

        List<String> cleanedItems = new ArrayList<String>();

        int lineCount = 0;
        for (String item : items) {
            lineCount++;
            String[] splitItem = item.split(";");
            StringBuilder cleanedItem = new StringBuilder();
            cleanedItem.append(splitItem[0]).append(";").append(splitItem[1]).append(";").append(splitItem[2])
                    .append(";").append(splitItem[5]).append(";");
            cleanedItems.add(cleanedItem.toString());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(cleanedItem.toString());
            }
        }

        return cleanedItems;
    }


    /**
     * @param args
     */
    public static void main(String[] args) {

        /*
         * EncodingFixer2 fixer = new EncodingFixer2();
         * List<String> input = EncodingFixer2.readCsv(
         * "src/test/resources/feedDataset/EncodingBug_Goldstandard/90_http___0_tqn_com_6_g_golftrave.encodingBug.csv");
         * List<String> gold = EncodingFixer2.readCsv(
         * "src/test/resources/feedDataset/EncodingBug_Goldstandard/90_http___0_tqn_com_6_g_golftrave.goldstandard.csv"
         * );
         * List<String> deduplicate = fixer.deduplicate(input, null);
         * System.out.println("#input:" + input.size());
         * System.out.println("#dedup:" + deduplicate.size());
         * System.out.println("#gold:" + gold.size());
         * System.out.println("dedup=gold:" + deduplicate.equals(gold));
         * CollectionHelper.print(gold);
         * CollectionHelper.print(deduplicate);
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
