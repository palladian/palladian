package tud.iir.news.evaluation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;

/**
 * @author Sandro Reichert
 * 
 */
public class ChartCreator {

    private static final Logger LOGGER = Logger.getLogger(ChartCreator.class);
    
    private final String FEED_SIZE_HISTOGRAM_FILE_PATH = "data/temp/feedSizeHistogrammData.csv";
    private final String FEED_AGE_FILE_PATH = "data/temp/feedAgeData.csv";
    private final String TIMELINESS2_FILE_PATH = "data/temp/timeliness2Data.csv";
    private final String PERCENTAGE_NEW_MAX_POLL_FILE_PATH = "data/temp/percentNewMaxPollData.csv";
    private final String SUM_VOLUME_MAX_MIN_TIME_FILE_PATH = "data/temp/sumVolumeTimeData";

    private final int MAX_NUMBER_OF_POLLS_SCORE_MIN;
    private final int MAX_NUMBER_OF_POLLS_SCORE_MAX;
    
    private final EvaluationDatabase ED;
    
    private final int TOTAL_EXPERIMENT_HOURS;

    /**
     * all available polling strategies
     * 
     * @author Sandro Reichert
     */
    public enum PollingStrategy {
        ADAPTIVE, PROBABILISTIC, FIX_LEARNED, FIX60, FIX1440
    }

    /**
     * Our policies:<br />
     * MIN: get every item as early as possible but not before it is available and <br />
     * MAX: only poll feed if all items in the window are new, but do not miss any item inbetween
     * 
     * @author Sandro Reichert
     */
    public enum Policy {
        MIN, MAX
    }

    /**
     * @param MAX_NUMBER_OF_POLLS_SCORE_MIN The maximum number of polls to be used by methods creating data for the
     *            MIN-policy.
     * @param MAX_NUMBER_OF_POLLS_SCORE_MAX The maximum number of polls to be used by methods creating data for the
     *            MAX-policy.
     */
    public ChartCreator(final int MAX_NUMBER_OF_POLLS_SCORE_MIN, final int MAX_NUMBER_OF_POLLS_SCORE_MAX) {
        this.ED = EvaluationDatabase.getInstance();
        this.MAX_NUMBER_OF_POLLS_SCORE_MIN = MAX_NUMBER_OF_POLLS_SCORE_MIN;
        this.MAX_NUMBER_OF_POLLS_SCORE_MAX = MAX_NUMBER_OF_POLLS_SCORE_MAX;
        this.TOTAL_EXPERIMENT_HOURS = (int) ((FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND - FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND) / DateHelper.HOUR_MS);
    }


    /**
     * Generates a *.csv file to generate a feed size histogram and stores it at
     * {@link ChartCreator#FEED_SIZE_HISTOGRAM_FILE_PATH}.
     * csv file has pattern (feed size in KB; number of feeds having this size;percentage of all feeds;)
     * 
     * @param chartInterval The size of the interval in KB, e.g. 10: 10KB, 20KB
     * @param chartNumberOfIntervals The number of intervals to display in detail, one additional interval 'more' is
     *            added automatically.
     *            e.g. 20 generates 20 intervals of size {@link #chartInterval} plus one containing all feeds that are
     *            larger
     */
    @SuppressWarnings("unused")
    private void createFeedSizeHistogrammFile(final int chartInterval, final int chartNumberOfIntervals) {
        List<EvaluationFeedPoll> polls = ED.getFeedSizes();
        int[] feedSizeDistribution = new int[chartNumberOfIntervals + 1];
        int totalNumberOfFeeds = 0;
        
        for (EvaluationFeedPoll poll : polls) {
//            int feedID = poll.getFeedID();
            float pollSize = poll.getSizeOfPoll();
            int i =  new Double(Math.floor(pollSize/1024/chartInterval)).intValue() ;
            i = i > chartNumberOfIntervals ? chartNumberOfIntervals : i;
            feedSizeDistribution[i]++;
            totalNumberOfFeeds++;
        }        
        
        StringBuilder feedSizeDistributionSB = new StringBuilder();
        feedSizeDistributionSB.append("Feed size in KB;number of feeds;percentage of the feeds;\n");
        int currentIntervalSize = 0;
        String intervalSizeToWrite = "";
        final int chartMax = chartInterval * chartNumberOfIntervals;

        for (int number : feedSizeDistribution) {
            currentIntervalSize += chartInterval;
            intervalSizeToWrite = currentIntervalSize > chartMax ? "more" : String.valueOf(currentIntervalSize);
            feedSizeDistributionSB.append(intervalSizeToWrite).append(";").append(number).append(";")
                    .append((float) number / (float) totalNumberOfFeeds * 100).append(";\n");
        }
        
        boolean outputWritten = FileHelper.writeToFile(FEED_SIZE_HISTOGRAM_FILE_PATH, feedSizeDistributionSB);
        if (outputWritten) {
            LOGGER.info("feedSizeHistogrammFile written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
        } else {
            LOGGER.fatal("feedSizeHistogrammFile has not been written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
        }
    }


    /**
     * Generates a *.csv file to generate a feed age histogram and stores it at {@link ChartCreator#FEED_AGE_FILE_PATH}.
     * csv file has pattern (feed file age;number of feeds;percentage of the feeds;)
     */
    @SuppressWarnings("unused")
    private void createFeedAgeFile(){        
        List<EvaluationItemIntervalItem> polls = ED.getAverageUpdateIntervals();
        int[] feedAgeDistribution = new int[34];
        int totalNumberOfFeeds = 0;
        
        for (EvaluationItemIntervalItem intervalItem : polls) {
            int averageUpdateIntervalHours = new Double(Math.floor(intervalItem.getAverageUpdateInterval()/3600000)).intValue();
            int i = -1;
            if (averageUpdateIntervalHours <= 24) {
                i = averageUpdateIntervalHours;
            } else if (averageUpdateIntervalHours <= 24 * 2) {
                i = 24; // 2 days
            } else if (averageUpdateIntervalHours <= 24 * 3) {
                i = 25; // 3 days
            } else if (averageUpdateIntervalHours <= 24 * 4) {
                i = 26; // 4 days
            } else if (averageUpdateIntervalHours <= 24 * 5) {
                i = 27; // 5 days
            } else if (averageUpdateIntervalHours <= 24 * 6) {
                i = 28; // 6 days
            } else if (averageUpdateIntervalHours <= 24 * 7) {
                i = 29; // 7 days
            } else if (averageUpdateIntervalHours <= 24 * 7 * 2) {
                i = 30; // 2 week
            } else if (averageUpdateIntervalHours <= 24 * 7 * 3) {
                i = 31; // 3 weeks
            } else if (averageUpdateIntervalHours <= 24 * 7 * 4) {
                i = 32; // 4 weeks
            } else {
                i = 33; // more
            }
            feedAgeDistribution[i]++;
            totalNumberOfFeeds++;
        }        

        StringBuilder feedAgeSB = new StringBuilder();
        feedAgeSB.append("feed file age;number of feeds;percentage of the feeds;\n");
        int i = 0;
        String[] caption = {"1 hour","2 hours","3 hours","4 hours","5 hours","6 hours","7 hours","8 hours","9 hours","10 hours","11 hours","12 hours","13 hours","14 hours","15 hours","16 hours","17 hours","18 hours","19 hours","20 hours","21 hours","22 hours","23 hours","24 hours","2 days","3 days","4 days","5 days","6 days","7 days","2 weeks","3 weeks","4 weeks","more"};
        
        for (int number : feedAgeDistribution) {
            feedAgeSB.append(caption[i]).append(";").append(number).append(";")
                    .append((float) number / (float) totalNumberOfFeeds * 100).append(";\n");
            i++;
        }
        
        boolean outputWritten = FileHelper.writeToFile(FEED_AGE_FILE_PATH, feedAgeSB);
        if (outputWritten) {
            LOGGER.info("feedAgeFile written to: " + FEED_AGE_FILE_PATH);
        } else {
            LOGGER.fatal("feedAgeFile has not been written to: " + FEED_AGE_FILE_PATH);
        }
    }

    /**
     * Helper function to process data aggregated (e.g. averaged) by numberOfPolls (for each data base table)
     * 
     * @param resultMap the map to write the output to
     * @param polls contains the preaggregated average values from one table
     * @param columnToWrite the position in Double[] to write the data to. This is the position in the *.csv file that
     *            is written.
     * @param numberOfColumns the total number of columns that are written to the *.csv file, used to create the
     *            Double[] in {@link resultMap}
     * @return the number of lines that has been processed
     */
    private int processDataAggregatedByPoll(List<EvaluationFeedPoll> polls, Map<Integer, Double[]> resultMap,
            final int columnToWrite, final int numberOfColumns) {
        int lineCount = 0;
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] aggregatedDataAtCurrentPoll = new Double[numberOfColumns];
            if (resultMap.containsKey(pollToProcess)) {
                aggregatedDataAtCurrentPoll = resultMap.get(pollToProcess);
            }
            aggregatedDataAtCurrentPoll[columnToWrite] = poll.getAverageValue();
            resultMap.put(poll.getNumberOfPoll(), aggregatedDataAtCurrentPoll);
            lineCount++;
        }
        return lineCount;
    }


    /**
     * Generates a *.csv file to generate the timeliness2 chart and stores it at
     * {@link ChartCreator#TIMELINESS2_FILE_PATH}. The file contains the average scoreMin per numberOfPoll for each
     * polling strategy separately. The csv file has the pattern (number of poll; adaptive; probabilistic; fix learned;
     * fix1h; fix1d)
     */
    @SuppressWarnings("unused")
    private void createAverageScoreMinByPollFile() {
        LOGGER.info("starting to create timeliness2File...");
        StringBuilder timeliness2SB = new StringBuilder();        
        Map<Integer, Double[]> timeliness2Map = new TreeMap<Integer, Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int NUMBER_OF_COLUMNS = 5;
        int columnToWrite = 0;

        timeliness2SB.append("numberOfPoll;");
        for (PollingStrategy pollingStrategy : PollingStrategy.values()) {
            LOGGER.info("starting to create data for " + pollingStrategy.toString());
            timeliness2SB.append(pollingStrategy.toString().toLowerCase()).append(";");
            polls = ED.getAverageScoreMinPerPollFromMinPoll(pollingStrategy, MAX_NUMBER_OF_POLLS_SCORE_MIN);
            processDataAggregatedByPoll(polls, timeliness2Map, columnToWrite, NUMBER_OF_COLUMNS);
            LOGGER.info("finished creating data for " + pollingStrategy.toString());
            columnToWrite++;
        }
        timeliness2SB.append("\n");

        writeMapToCSV(timeliness2Map, timeliness2SB, TIMELINESS2_FILE_PATH);
        LOGGER.info("finished creating timeliness2File.");
    }

    /**
     * Generates a *.csv file containing the average percentage of percentageNewEntries by numberOfPoll for each
     * strategy separately. File is written to {@link ChartCreator#PERCENTAGE_NEW_MAX_POLL_FILE_PATH}, file has structure
     * (numberOfPoll; adaptive; probabilistic; fix learned; fix1h; fix1d)
     */
    @SuppressWarnings("unused")
    private void createPercentageNewMaxPollFile() {
        LOGGER.info("starting to create percentageNewMax...");
        StringBuilder percentageNewSB = new StringBuilder();
        Map<Integer, Double[]> percentageNewMap = new TreeMap<Integer, Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int NUMBER_OF_COLUMNS = 5;
        int columnToWrite = 0;

        percentageNewSB.append("numberOfPoll;");
        for (PollingStrategy pollingStrategy : PollingStrategy.values()) {
            LOGGER.info("starting to create data for " + pollingStrategy.toString());
            percentageNewSB.append(pollingStrategy.toString().toLowerCase()).append(";");
            polls = ED.getAveragePercentageNewEntriesPerPollFromMaxPoll(pollingStrategy, MAX_NUMBER_OF_POLLS_SCORE_MAX);
            processDataAggregatedByPoll(polls, percentageNewMap, columnToWrite, NUMBER_OF_COLUMNS);
            LOGGER.info("finished creating data for " + pollingStrategy.toString());
            columnToWrite++;
        }
        percentageNewSB.append("\n");

        writeMapToCSV(percentageNewMap, percentageNewSB, PERCENTAGE_NEW_MAX_POLL_FILE_PATH);
        LOGGER.info("finished creating percentageNewFile.");
    }

    /**
     * Helper to traverse the result map {@link outputMap}, append its items to given StringBuilder {@link outputSB} and
     * write SB into the *.csv file {@link filePath}.
     * For every pair (K,V) in the map, the Intger value (key) is written into the first column (e.g. number of poll),
     * followed by the values for each strategy, e.g. adaptive, probabilistic, fix learned, fix1h, fix1d
     * 
     * @param outputMap the map to traverse with <numberOfPoll, {adaptive, probabilistic, fix learned, fix1h, fix1d}>
     * @param outputSB the StringBuilder that already contains the header information (column heads)
     * @param filePath the file to write the output to
     */
    private void writeMapToCSV(Map<Integer, Double[]> outputMap, StringBuilder outputSB, String filePath) {
        Iterator<Integer> it = outputMap.keySet().iterator();
        while (it.hasNext()) {
            int currentPoll = it.next();
            Double[] scoresAtCurrentPoll = outputMap.get(currentPoll);
            outputSB.append(currentPoll).append(";").append(scoresAtCurrentPoll[0]).append(";")
                    .append(scoresAtCurrentPoll[1]).append(";").append(scoresAtCurrentPoll[2]).append(";")
                    .append(scoresAtCurrentPoll[3]).append(";").append(scoresAtCurrentPoll[4]).append(";\n");
        }

        boolean outputWritten = FileHelper.writeToFile(filePath, outputSB);
        if (outputWritten) {
            LOGGER.info(filePath + " has been written");
        } else {
            LOGGER.fatal(filePath + " has NOT been written!");
        }
    }

    /**
     * Helper function to cumulate the sizeOfPoll values per hour.<br />
     * 
     * The function gets a list of {@link polls} which represents all polls one {@link PollingStrategy} (fix, adaptive,
     * etc.) would have done within our experiment time (specified by {@link ChartCreator#TOTAL_EXPERIMENT_HOURS}.
     * Missing polls are simulated by adding the sizeOfPoll of the last poll that has been done by the
     * {@link PollingStrategy}.
     * 
     * @param polls contains the polls done by one {@link PollingStrategy}
     * @param totalResultMapMax the map to write the output to. The map may contain values of other
     *            {@link PollingStrategy}s.
     * @param COLUMN_TO_WRITE the position in Long[] to write the data to.
     * @param NUMBER_OF_COLUMNS the total number of columns ({@link PollingStrategy}s)
     * @param SIMULATE_ETAG_USAGE Use to simulate the usage of eTags and HTTP304. If true, and if the poll contains no
     *            new item, only the header is added, otherwise the transfer volume of the poll (or the last poll if
     *            polls are simulated)
     */
    private void volumeHelper(List<EvaluationFeedPoll> polls, Map<Integer, Long[]> totalResultMapMax,
            final int COLUMN_TO_WRITE, final int NUMBER_OF_COLUMNS, final boolean SIMULATE_ETAG_USAGE) {

        int feedIDLastStep = -1;
        int sizeOfPollLast = -1;
        int minuteLastStep = 0;
        int checkIntervalLast = -1;

        for (EvaluationFeedPoll poll : polls) {
            int feedIDCurrent = poll.getFeedID();
            
            // in Davids DB nicht vorhandene Polls simulieren
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                
                int sizeToAdd = (SIMULATE_ETAG_USAGE && poll.getSupportsConditionalGet()) ? poll
                        .getConditionalGetResponseSize() : sizeOfPollLast;
                
                while (minuteLastStep + checkIntervalLast < TOTAL_EXPERIMENT_HOURS * 60) {

                    final int MINUTE_TO_PROCESS = (minuteLastStep + checkIntervalLast);
                    // x/60 + 1: add 1 hour to result to start with hour 1 instead of 0 (minute 1 means hour 1)
                    final int HOUR_TO_PROCESS = MINUTE_TO_PROCESS / 60 + 1;

                    addSizeOfPollToMap(totalResultMapMax, NUMBER_OF_COLUMNS, COLUMN_TO_WRITE, HOUR_TO_PROCESS,
                            sizeToAdd);
                    minuteLastStep = MINUTE_TO_PROCESS;

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("simmuliere für FeedID " + feedIDLastStep + ", aktuelle Stunde: "
                                + HOUR_TO_PROCESS + " aktuelle Minute " + MINUTE_TO_PROCESS + " checkInterval "
                                + checkIntervalLast + " addiere " + sizeToAdd + "bytes" + " totalResultMapMax Feld: "
                                + totalResultMapMax.get(HOUR_TO_PROCESS)[COLUMN_TO_WRITE]);
                    }
                }
                minuteLastStep = 0;
            }
                   
            // aktuellen Poll behandeln
            final int HOUR_TO_PROCESS = poll.getHourOfExperiment();

            int sizeOfPoll;
            if (SIMULATE_ETAG_USAGE && poll.getNewWindowItems() == 0 && poll.getSupportsConditionalGet() == true) {
                    sizeOfPoll = poll.getConditionalGetResponseSize();
            } else {
                sizeOfPoll = poll.getSizeOfPoll();
            }

            addSizeOfPollToMap(totalResultMapMax, NUMBER_OF_COLUMNS, COLUMN_TO_WRITE, HOUR_TO_PROCESS, sizeOfPoll);

            if (poll.getNumberOfPoll() >= 2) {
                minuteLastStep += checkIntervalLast;
            }
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
            checkIntervalLast = poll.getCheckInterval();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bearbeite      FeedID " + feedIDCurrent + ", aktuelle Stunde: " + HOUR_TO_PROCESS
                        + " aktuelle Minute " + minuteLastStep + " checkInterval " + poll.getCheckInterval()
                        + " addiere " + sizeOfPoll + "bytes" + " totalResultMapMax Feld: "
                        + totalResultMapMax.get(HOUR_TO_PROCESS)[COLUMN_TO_WRITE]);
            }
        }
    }

    /**
     * Helper's helper to add a sizeOfPoll ({@link SIZE_TO_ADD}) to the given virtual column ({@link COLUMN_TO_WRITE})
     * in the row ({@link HOUR_TO_PROCESS}) of a *.csv file, represented by the {@link totalResultMapMax}. If
     * {@link totalResultMapMax} already contains an entry for ({@link HOUR_TO_PROCESS}), ({@link SIZE_TO_ADD}) is added
     * to the specified ({@link COLUMN_TO_WRITE}), otherwise a new Long[] with {@link NUMBER_OF_COLUMNS} is generated
     * and the value is written to it. Finally, the virtual row is written back into {@link totalResultMapMax}
     * 
     * @param totalResultMapMax the map to write the result to, schema: <hourOfExperiment,
     *            cumulatedVolumePerAlgorithm{fix1440,fix60,ficLearned,adaptive,probabilistic} in Bytes>
     * @param NUMBER_OF_COLUMNS the number of columns = number of algorithms (5)
     * @param COLUMN_TO_WRITE the column to which {@link SIZE_TO_ADD} is written
     * @param HOUR_TO_PROCESS the row to which {@link SIZE_TO_ADD} is written
     * @param SIZE_TO_ADD the sizeOfPoll in bytes that is added to the specified field
     */
    private void addSizeOfPollToMap(Map<Integer, Long[]> totalResultMapMax, final int NUMBER_OF_COLUMNS,
            final int COLUMN_TO_WRITE, final int HOUR_TO_PROCESS, final int SIZE_TO_ADD) {

        Long[] transferredDataArray = new Long[NUMBER_OF_COLUMNS];
        if (totalResultMapMax.containsKey(HOUR_TO_PROCESS)) {
            transferredDataArray = totalResultMapMax.get(HOUR_TO_PROCESS);
        } else {
            Arrays.fill(transferredDataArray, 0L);
        }
        transferredDataArray[COLUMN_TO_WRITE] += SIZE_TO_ADD;
        totalResultMapMax.put(HOUR_TO_PROCESS, transferredDataArray);
    }

    /**
     * Calculates the cumulated transfer volume per poll per {@link PollingStrategy} to a *.csv file.
     * Every row represents one poll, every column is one {@link PollingStrategy}.
     * 
     * @param POLICY The {@link Policy} to generate the file for.
     * @param SIMULATE_ETAG_USAGE If true, for each poll that has no new item, the size of the conditional header is
     *            added to the transfer volume (instead of the sizeOfPoll).
     * @param FEED_ID_MAX the highest FeedID in the data set.
     */
    @SuppressWarnings("unused")
    private void cumulatedVolumePerTimeFile(final Policy POLICY, final boolean SIMULATE_ETAG_USAGE,
            final int FEED_ID_MAX) {
        LOGGER.info("starting to create sumVolumeFile for policy " + POLICY);
        StringBuilder cumulatedVolumeSB = new StringBuilder();
        // <hourOfExperiment, cumulatedVolumePerAlgorithm[fix1440,fix60,ficLearned,adaptive,probabilistic] in Bytes>
        Map<Integer, Long[]> totalResultMap = new TreeMap<Integer, Long[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int NUMBER_OF_COLUMNS = PollingStrategy.values().length;
        int feedIDStart = 1;
        int feedIDEnd = 10000;
        final int FEED_ID_STEP = 10000;
        int columnToWrite = 0;
    
        cumulatedVolumeSB.append("hour of experiment;");
        for (PollingStrategy pollingStrategy : PollingStrategy.values()) {

            LOGGER.info("starting to create data for " + pollingStrategy.toString());
            cumulatedVolumeSB.append(pollingStrategy.toString().toLowerCase()).append(";");
            feedIDStart = 1;
            feedIDEnd = 10000;

            while (feedIDEnd < FEED_ID_MAX) {
                LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
                polls = ED.getTransferVolumeByHourFromTime(POLICY, pollingStrategy, feedIDStart, feedIDEnd);
                volumeHelper(polls, totalResultMap, columnToWrite, NUMBER_OF_COLUMNS, SIMULATE_ETAG_USAGE);
                feedIDStart += FEED_ID_STEP;
                feedIDEnd += FEED_ID_STEP;
            }
            columnToWrite++;
            LOGGER.info("finished creating data for " + pollingStrategy.toString());
        }
        cumulatedVolumeSB.append("\n");

        // //////////// write totalResultMapMax to StringBuilder, cumulating the values row-wise \\\\\\\\\\\\\\\\\
        final long BYTE_TO_MB = 1048576;
        Long[] volumesCumulated = new Long[NUMBER_OF_COLUMNS];
        Arrays.fill(volumesCumulated, 0l);
        Iterator<Integer> it = totalResultMap.keySet().iterator();
        while (it.hasNext()) {
            int currentHour = it.next();
            Long[] volumes = totalResultMap.get(currentHour);

            for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
                volumesCumulated[i] += volumes[i];
            }
            cumulatedVolumeSB.append(currentHour).append(";").append(volumesCumulated[0] / BYTE_TO_MB).append(";")
                    .append(volumesCumulated[1] / BYTE_TO_MB).append(";").append(volumesCumulated[2] / BYTE_TO_MB)
                    .append(";").append(volumesCumulated[3] / BYTE_TO_MB).append(";")
                    .append(volumesCumulated[4] / BYTE_TO_MB).append(";\n");
        }

        // //////////// write final output to file \\\\\\\\\\\\\\\\\
        String filePathToWrite = "";
        String eTag = SIMULATE_ETAG_USAGE ? "ETag" : "NoETag";
        switch (POLICY) {
            case MAX:
                filePathToWrite = SUM_VOLUME_MAX_MIN_TIME_FILE_PATH + "_Max" + eTag + ".csv";
                break;
            case MIN:
                filePathToWrite = SUM_VOLUME_MAX_MIN_TIME_FILE_PATH + "_Min" + eTag + ".csv";
                break;
            default:
                throw new IllegalStateException("unknown Policy: " + POLICY.toString());
        }
        boolean outputWritten = FileHelper.writeToFile(filePathToWrite, cumulatedVolumeSB);
        if (outputWritten) {
            LOGGER.info("sumVolumeFile for policy " + POLICY + " written to: " + filePathToWrite);
        } else {
            LOGGER.fatal("sumVolumeFile for policy " + POLICY + " has not been written to: " + filePathToWrite);
        }
    } 
    




    // /**
    // * Only a test
    // */
    // private void printFeedPolls() {
    // List<EvaluationFeedPoll> polls = ed.getAllFeedPollsFromAdaptiveMaxTime();
    //
    // for (EvaluationFeedPoll poll : polls) {
    // // int feedID = poll.getFeedID();
    // LOGGER.info(poll.getFeedID() + " " + poll.getSizeOfPoll());
    // }
    //
    // }

    /**
     * @param args ...
     */
	public static void main(String[] args) {

        /**
         * 200 polls for scoreMin
         * 200 polls for scoreMax
         */
        ChartCreator cc = new ChartCreator(200, 200);

        // cc.printFeedPolls();
        // cc.createFeedSizeHistogrammFile(10, 20); // letzter Test 12.11. DB Schema v2
        // cc.createFeedAgeFile(); // letzter Test 12.11. DB Schema v2
        // cc.createAverageScoreMinByPollFile(); // letzter Test 12.11. DB Schema v2
        // cc.createPercentageNewMaxPollFile(); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MAX, false, 210000); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MAX, true, 210000); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MIN, false, 210000); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MIN, true, 210000); // letzter Test 12.11. DB Schema v2
	}

}
