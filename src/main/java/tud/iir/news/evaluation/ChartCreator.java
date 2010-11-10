package tud.iir.news.evaluation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;

/**
 * @author Sandro Reichert
 * 
 */
public class ChartCreator {

    private static final Logger LOGGER = Logger.getLogger(ChartCreator.class);
    
    private final String FEED_SIZE_HISTOGRAM_FILE_PATH = "data/evaluation/feedPaper/feedSizeHistogrammData.csv";
    private final String feedAgeFilePath = "data/evaluation/feedPaper/feedAgeData.csv";
    private final String timeliness2FilePath = "data/evaluation/feedPaper/timeliness2Data.csv";
    private final String percentageNewFilePath = "data/evaluation/feedPaper/percentNewData.csv";
    private final String sumVolumeMaxFilePath = "data/evaluation/feedPaper/sumVolumeMaxData.csv";
    private final String sumVolumeMinFilePath = "data/evaluation/feedPaper/sumVolumeMinData.csv";
    private final String sumVolumeMinEtag304FilePath = "data/evaluation/feedPaper/sumVolumeMinEtag304Data.csv";
    
    private final int MAX_NUMBER_OF_POLLS_SCORE_MIN;
    private final int MAX_NUMBER_OF_POLLS_SCORE_MAX;
    
    private final EvaluationDatabase ed ; 
    
    private final int TOTAL_EXPERIMENT_HOURS;

    /**
     * @param MAX_NUMBER_OF_POLLS_SCORE_MIN The maximum number of polls to be used by methods creating data for the
     *            MIN-policy.
     * @param MAX_NUMBER_OF_POLLS_SCORE_MAX The maximum number of polls to be used by methods creating data for the
     *            MAX-policy.
     * @param TOTAL_EXPERIMENT_HOURS The number of hours the experiment took place
     */
    public ChartCreator(final int MAX_NUMBER_OF_POLLS_SCORE_MIN, final int MAX_NUMBER_OF_POLLS_SCORE_MAX,
            final int TOTAL_EXPERIMENT_HOURS) {
        this.ed = EvaluationDatabase.getInstance();
        this.MAX_NUMBER_OF_POLLS_SCORE_MIN = MAX_NUMBER_OF_POLLS_SCORE_MIN;
        this.MAX_NUMBER_OF_POLLS_SCORE_MAX = MAX_NUMBER_OF_POLLS_SCORE_MAX;
        this.TOTAL_EXPERIMENT_HOURS = TOTAL_EXPERIMENT_HOURS;
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
    private void createFeedSizeHistogrammFile(final int chartInterval, final int chartNumberOfIntervals) {
        List<EvaluationFeedPoll> polls = ed.getFeedSizes();
        int[] feedSizeDistribution = new int[chartNumberOfIntervals + 1];
        int totalNumberOfFeeds = 0;
        
        for (EvaluationFeedPoll poll : polls) {
//            int feedID = poll.getFeedID();
            float pollSize = poll.getSizeOfPoll();
            int i =  new Double(Math.floor(pollSize/1024/chartInterval)).intValue() ;
            i = (i > chartNumberOfIntervals) ? chartNumberOfIntervals : i;
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
            intervalSizeToWrite = (currentIntervalSize > chartMax) ? "more" : String.valueOf(currentIntervalSize);
            feedSizeDistributionSB.append(intervalSizeToWrite).append(";").append(number).append(";")
                    .append((float) number / (float) totalNumberOfFeeds * 100).append(";\n");
        }
        
        boolean outputWritten = FileHelper.writeToFile(FEED_SIZE_HISTOGRAM_FILE_PATH, feedSizeDistributionSB);
        if (outputWritten)
            LOGGER.info("feedSizeHistogrammFile written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
        else
            LOGGER.fatal("feedSizeHistogrammFile has not been written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
    }


    /**
     * Generates a *.csv file to generate a feed age histogram and stores it at {@link ChartCreator#feedAgeFilePath}.
     * csv file has pattern (feed file age;number of feeds;percentage of the feeds;)
     */
    private void createFeedAgeFile(){        
        List<EvaluationItemIntervalItem> polls = ed.getAverageUpdateIntervals();
        int[] feedAgeDistribution = new int[34];
        int totalNumberOfFeeds = 0;
        
        for (EvaluationItemIntervalItem intervalItem : polls) {
            int averageUpdateIntervalHours = new Double(Math.floor(intervalItem.getAverageUpdateInterval()/3600000)).intValue();
            int i = -1;
            if(averageUpdateIntervalHours <= 24) i = averageUpdateIntervalHours;
            else if(averageUpdateIntervalHours <= 24*2) i = 24;  //2 days
            else if(averageUpdateIntervalHours <= 24*3) i = 25;  //3 days
            else if(averageUpdateIntervalHours <= 24*4) i = 26;  //4 days
            else if(averageUpdateIntervalHours <= 24*5) i = 27; //5 days
            else if(averageUpdateIntervalHours <= 24*6) i = 28; //6 days
            else if(averageUpdateIntervalHours <= 24*7) i = 29; //7 days
            else if(averageUpdateIntervalHours <= 24*7*2) i = 30; //2 weeks
            else if(averageUpdateIntervalHours <= 24*7*3) i = 31; //3 weeks
            else if(averageUpdateIntervalHours <= 24*7*4) i = 32; //4 weeks
            else i = 33; //more
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
        
        boolean outputWritten = FileHelper.writeToFile(feedAgeFilePath, feedAgeSB);
        if (outputWritten)
            LOGGER.info("feedAgeFile written to: " + feedAgeFilePath);
        else
            LOGGER.fatal("feedAgeFile has not been written to: " + feedAgeFilePath);
    }

    /**
     * Helper function to process data aggregated (e.g. averaged) by numberOfPolls (for each data base table)
     * 
     * @param resultMap the map to write the output to
     * @param polls contains the preaggregated average values from one table
     * @param rowToWrite the position in Double[] to write the data to. This is the position in the *.csv file that is
     *            written .
     * @param numberOfRows the total number of rows that are written to the *.csv file, used to create the Double[] in
     *            {@link resultMap}
     * @return the number of lines that has been processed
     */
    private int processDataAggregatedByPoll(List<EvaluationFeedPoll> polls, Map<Integer, Double[]> resultMap,
            final int rowToWrite, final int numberOfRows) {
        int lineCount = 0;
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] aggregatedDataAtCurrentPoll = new Double[numberOfRows];
            if (resultMap.containsKey(pollToProcess)) {
                aggregatedDataAtCurrentPoll = resultMap.get(pollToProcess);
            }
            aggregatedDataAtCurrentPoll[rowToWrite] = poll.getAverageValue();
            resultMap.put(poll.getNumberOfPoll(), aggregatedDataAtCurrentPoll);
            lineCount++;
        }
        return lineCount;
    }


    /**
     * Generates a *.csv file to generate the timeliness2 chart and stores it at
     * {@link ChartCreator#timeliness2FilePath}. The file contains the average scoreMin per numberOfPoll for each
     * polling strategy separately. The csv file has the pattern (number of poll; adaptive; probabilistic; fix learned;
     * fix1h; fix1d)
     */
    private void createAverageScoreMinByPollFile() {
        LOGGER.info("starting to create timeliness2File...");
        StringBuilder timeliness2SB = new StringBuilder();        
        Map<Integer, Double[]> timeliness2Map = new TreeMap<Integer, Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int numberOfRows = 5;
        int linesProcessed = 0;
        timeliness2SB.append("numberOfPoll;");
        
        LOGGER.info("starting to process table adaptive...");
        timeliness2SB.append("adaptive;");
        polls = ed.getAverageScoreMinAdaptive(MAX_NUMBER_OF_POLLS_SCORE_MIN);
        linesProcessed = processDataAggregatedByPoll(polls, timeliness2Map, 0, numberOfRows);
        LOGGER.info("finished processing table adaptive, processed " + linesProcessed + " lines in the result set.");

        LOGGER.info("starting to process table probabilistic...");
        timeliness2SB.append("probabilistic;");
        polls = ed.getAverageScoreMinProbabilistic(MAX_NUMBER_OF_POLLS_SCORE_MIN);
        linesProcessed = processDataAggregatedByPoll(polls, timeliness2Map, 1, numberOfRows);
        LOGGER.info("finished processing table probabilistic, processed " + linesProcessed
                + " lines in the result set.");

        LOGGER.info("starting to process table fix learned...");
        timeliness2SB.append("fix learned;");
        polls = ed.getAverageScoreMinFIXlearned(MAX_NUMBER_OF_POLLS_SCORE_MIN);
        linesProcessed = processDataAggregatedByPoll(polls, timeliness2Map, 2, numberOfRows);
        LOGGER.info("finished processing table fix learned, processed " + linesProcessed + " lines in the result set.");

        LOGGER.info("starting to process table fix1h...");
        timeliness2SB.append("fix1h;");
        polls = ed.getAverageScoreMinFIX60(MAX_NUMBER_OF_POLLS_SCORE_MIN);
        linesProcessed = processDataAggregatedByPoll(polls, timeliness2Map, 3, numberOfRows);
        LOGGER.info("finished processing table fix1h, processed " + linesProcessed + " lines in the result set.");

        LOGGER.info("starting to process table fix1d...");
        timeliness2SB.append("fix1d;\n");
        polls = ed.getAverageScoreMinFIX1440(MAX_NUMBER_OF_POLLS_SCORE_MIN);
        linesProcessed = processDataAggregatedByPoll(polls, timeliness2Map, 4, numberOfRows);
        LOGGER.info("finished processing table fix1d, processed " + linesProcessed + " lines in the result set.");

        writeMapToCSV(timeliness2Map, timeliness2SB, timeliness2FilePath);
        LOGGER.info("finished creating timeliness2File.");
    } 
    
    
    
    /**
     * Generates a *.csv file containing the average percentage of percentageNewEntries by numberOfPoll for each
     * strategy separately. File is written to {@link ChartCreator#percentageNewFilePath}, file has structure
     * (numberOfPoll; adaptive; probabilistic; fix learned; fix1h; fix1d)
     */
    private void createPercentageNewFile() {
        LOGGER.info("starting to create timeliness2File...");
        StringBuilder percentageNewSB = new StringBuilder();
        Map<Integer, Double[]> percentageNewMap = new TreeMap<Integer, Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int numberOfRows = 5;
        int linesProcessed = 0;
        percentageNewSB.append("numberOfPoll;");

        LOGGER.info("starting to process table adaptive...");
        percentageNewSB.append("adaptive;");
        polls = ed.getAveragePercentageNewEntriesByPollFromAdaptiveMaxPoll(MAX_NUMBER_OF_POLLS_SCORE_MAX);
        linesProcessed = processDataAggregatedByPoll(polls, percentageNewMap, 0, numberOfRows);
        LOGGER.info("finished processing table adaptive, processed " + linesProcessed + " lines in the result set.");

        LOGGER.info("starting to process table probabilistic...");
        percentageNewSB.append("probabilistic;");
        polls = ed.getAveragePercentageNewEntriesByPollFromProbabilisticMaxPoll(MAX_NUMBER_OF_POLLS_SCORE_MAX);
        linesProcessed = processDataAggregatedByPoll(polls, percentageNewMap, 1, numberOfRows);
        LOGGER.info("finished processing table probabilistic, processed " + linesProcessed
                + " lines in the result set.");

        LOGGER.info("starting to process table fix learned...");
        percentageNewSB.append("fix learned;");
        polls = ed.getAveragePercentageNewEntriesByPollFromFIXlearnedMaxPoll(MAX_NUMBER_OF_POLLS_SCORE_MAX);
        linesProcessed = processDataAggregatedByPoll(polls, percentageNewMap, 2, numberOfRows);
        LOGGER.info("finished processing table fix learned, processed " + linesProcessed + " lines in the result set.");

        LOGGER.info("starting to process table fix1h...");
        percentageNewSB.append("fix1h;");
        polls = ed.getAveragePercentageNewEntriesByPollFromFIX60MaxMinPoll(MAX_NUMBER_OF_POLLS_SCORE_MAX);
        linesProcessed = processDataAggregatedByPoll(polls, percentageNewMap, 3, numberOfRows);
        LOGGER.info("finished processing table fix1h, processed " + linesProcessed + " lines in the result set.");

        LOGGER.info("starting to process table fix1d...");
        percentageNewSB.append("fix1d;\n");
        polls = ed.getAveragePercentageNewEntriesByPollFromFix1440MaxMinPoll(MAX_NUMBER_OF_POLLS_SCORE_MAX);
        linesProcessed = processDataAggregatedByPoll(polls, percentageNewMap, 4, numberOfRows);
        LOGGER.info("finished processing table fix1d, processed " + linesProcessed + " lines in the result set.");

        writeMapToCSV(percentageNewMap, percentageNewSB, percentageNewFilePath);
        LOGGER.info("finished creating percentageNewFile.");
    }

    /**
     * Helper to traverse the result map {@link outputMap}, append its items to given StringBuilder {@link outputSB} and
     * write SB into the *.csv file {@link filePath}.
     * For every pair (K,V) in the map, the Intger value is written into the first row (e.g. number of poll), followed
     * by the values for each strategy, e.g. adaptive, probabilistic, fix learned, fix1h, fix1d
     * 
     * @param outputMap the map to traverse with <numberOfPoll, {adaptive, probabilistic, fix learned, fix1h, fix1d}>
     * @param outputSB the StringBuilder that already contains the header information (column heads)
     * @param filePath the file to write the output to
     */
    private void writeMapToCSV(Map<Integer, Double[]> outputMap, StringBuilder outputSB, String filePath) {
        Iterator<Integer> it = outputMap.keySet().iterator();
        while (it.hasNext()) {
            int currentPoll = (int) it.next();
            Double[] scoresAtCurrentPoll = outputMap.get(currentPoll);
            outputSB.append(currentPoll).append(";").append(scoresAtCurrentPoll[0]).append(";")
                    .append(scoresAtCurrentPoll[1]).append(";").append(scoresAtCurrentPoll[2]).append(";")
                    .append(scoresAtCurrentPoll[3]).append(";").append(scoresAtCurrentPoll[4]).append(";\n");
        }

        boolean outputWritten = FileHelper.writeToFile(filePath, outputSB);
        if (outputWritten)
            LOGGER.info(filePath + " has been written");
        else
            LOGGER.fatal(filePath + "has NOT been written!");
    }


    /**
     * Helper function to cumulate the sizeOfPoll values per hour.<br />
     * 
     * The function gets a list of {@link polls} which represents all polls one algorithm (fix, adaptive, etc.) would
     * have done within our experiment time (specified by {@link ChartCreator#TOTAL_EXPERIMENT_HOURS}. Missing polls are
     * simulated by adding the sizeOfPoll of the last poll that has been done by the algorithm.
     * 
     * @param polls contains the polls done by one algorithm
     * @param totalResultMapMax the map to write the output to. The map may contain values of other algorithms.
     * @param ROW_TO_WRITE the position in Long[] to write the data to.
     * @param NUMBER_OF_ROWS the total number of rows (algorithms)
     */
    private void volumeHelper(List<EvaluationFeedPoll> polls, Map<Integer, Long[]> totalResultMapMax,
            final int ROW_TO_WRITE, final int NUMBER_OF_ROWS, final boolean SIMULATE_ETAG_USAGE) {

        int feedIDLastStep = -1;
        int sizeOfPollLast = -1;
        int minuteLastStep = 1;

        for (EvaluationFeedPoll poll : polls) {
            int feedIDCurrent = poll.getFeedID();
            
            // in Davids DB nicht vorhandene Polls simulieren
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                while ((minuteLastStep + poll.getCheckInterval()) < (TOTAL_EXPERIMENT_HOURS * 60)) {

                    final int MINUTE_TO_PROCESS = (int) (minuteLastStep + poll.getCheckInterval());
                    // x/60 + 1: add 1 hour to result to start with hour 1 instead of 0 (minute 1 means hour 1)
                    final int HOUR_TO_PROCESS = MINUTE_TO_PROCESS / 60 + 1;

                    addSizeOfPollToMap(totalResultMapMax, NUMBER_OF_ROWS, ROW_TO_WRITE, HOUR_TO_PROCESS, sizeOfPollLast);

                    minuteLastStep = MINUTE_TO_PROCESS;
                }
                minuteLastStep = 1;
            }
                   
            // aktuellen Poll behandeln
            final int HOUR_TO_PROCESS = poll.getHourOfExperiment();

            int sizeOfPoll = poll.getSizeOfPoll();
            if (SIMULATE_ETAG_USAGE && poll.getPercentageNewEntries() == 0f) {
                if (poll.getSupportsConditionalGet() == true)
                    sizeOfPoll = poll.getConditionalGetResponseSize();
                else if (poll.getSupportsETag() == true)
                    sizeOfPoll = poll.geteTagResponseSize();
            }

            addSizeOfPollToMap(totalResultMapMax, NUMBER_OF_ROWS, ROW_TO_WRITE, HOUR_TO_PROCESS, sizeOfPoll);

            if (poll.getNumberOfPoll() >= 2) {
                minuteLastStep += (int) poll.getCheckInterval();
            }
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
        }
    }


    /**
     * Helper's helper to add a sizeOfPoll ({@link SIZE_TO_ADD}) to the given virtual row ({@link ROW_TO_WRITE}) and
     * column ({@link HOUR_TO_PROCESS}) of a *.csv file, represented by the {@link totalResultMapMax}. If
     * {@link totalResultMapMax} already contains an entry for ({@link HOUR_TO_PROCESS}), ({@link SIZE_TO_ADD}) is added
     * to the specified ({@link ROW_TO_WRITE}), otherwise a new Long[] with {@link NUMBER_OF_ROWS} is generated and the
     * value is written to it. Finally, the virtual row is written back into {@link totalResultMapMax}
     * 
     * @param totalResultMapMax the map to write the result to, schema: <hourOfExperiment,
     *            culmulatedVolumePerAlgorithm{fix1440,fix60,ficLearned,adaptive,probabilistic} in Bytes>
     * @param NUMBER_OF_ROWS the number of rows = number of algorithms (5)
     * @param ROW_TO_WRITE the row to which {@link SIZE_TO_ADD} is written
     * @param HOUR_TO_PROCESS the column to which {@link SIZE_TO_ADD} is written
     * @param SIZE_TO_ADD the sizeOfPoll in bytes that is added to the specified field
     */
    private void addSizeOfPollToMap(Map<Integer, Long[]> totalResultMapMax, final int NUMBER_OF_ROWS,
            final int ROW_TO_WRITE, final int HOUR_TO_PROCESS, final int SIZE_TO_ADD) {

        Long[] transferredDataArray = new Long[NUMBER_OF_ROWS];
        if (totalResultMapMax.containsKey(HOUR_TO_PROCESS)) {
            transferredDataArray = totalResultMapMax.get(HOUR_TO_PROCESS);
        } else {
            Arrays.fill(transferredDataArray, 0l);
        }
        transferredDataArray[ROW_TO_WRITE] += SIZE_TO_ADD;
        totalResultMapMax.put(HOUR_TO_PROCESS, transferredDataArray);
    }

    /**
     * @param FEED_ID_MAX the highest FeedID in the dataset
     */
    private void cumulatedVolumeMaxTimeFile(final int FEED_ID_MAX, final boolean SIMULATE_ETAG_USAGE) {
        LOGGER.info("starting to create sumVolumeMaxFile...");        
        StringBuilder culmulatedVolumeSB = new StringBuilder();
        // <hourOfExperiment, culmulatedVolumePerAlgorithm{fix1440,fix60,ficLearned,adaptive,probabilistic} in Bytes>
        Map<Integer,Long[]> totalResultMapMax = new TreeMap<Integer,Long[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int NUMBER_OF_ROWS = 5;
        int feedIDStart = 1;
        int feedIDEnd = 10000;
        final int FEED_ID_STEP = 10000;
        
        culmulatedVolumeSB.append("hour of experiment;");
    

        // /////////// get data from adaptiveMaxTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create adaptive data...");
        culmulatedVolumeSB.append("adaptive;");
        feedIDStart = 1;
        feedIDEnd = 10000;
        while (feedIDEnd < FEED_ID_MAX) {
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getTransferVolumeByHourFromAdaptiveMaxTime(feedIDStart, feedIDEnd);
            volumeHelper(polls, totalResultMapMax, 0, NUMBER_OF_ROWS, SIMULATE_ETAG_USAGE);
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating adaptive data...");


        // /////////// get data from probabilisticMaxTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create probabilistic data...");
        culmulatedVolumeSB.append("probabilistic;");
        feedIDStart = 1;
        feedIDEnd = 10000;
        while (feedIDEnd < FEED_ID_MAX) {
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getTransferVolumeByHourFromProbabilisticMaxTime(feedIDStart, feedIDEnd);
            volumeHelper(polls, totalResultMapMax, 1, NUMBER_OF_ROWS, SIMULATE_ETAG_USAGE);
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating probabilistic data...");


        // /////////// get data from fixLearnedTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fixLearned data...");
        culmulatedVolumeSB.append("fixLearned;");
        feedIDStart = 1;
        feedIDEnd = 10000;
        while (feedIDEnd < FEED_ID_MAX) {
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getTransferVolumeByHourFromFixLearnedMaxTime(feedIDStart, feedIDEnd);
            volumeHelper(polls, totalResultMapMax, 2, NUMBER_OF_ROWS, SIMULATE_ETAG_USAGE);
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fixLearned data...");


        // /////////// get data from fix60Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix60 data...");
        culmulatedVolumeSB.append("fix60;");
        feedIDStart = 1;
        feedIDEnd = 10000;
        while (feedIDEnd < FEED_ID_MAX) {
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getTransferVolumeByHourFromFix60MaxMinTime(feedIDStart, feedIDEnd);
            volumeHelper(polls, totalResultMapMax, 3, NUMBER_OF_ROWS, SIMULATE_ETAG_USAGE);

            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fix60 data...");


        // /////////// get data from fix1440Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix1440 data...");
        culmulatedVolumeSB.append("fix1440;\n");
        feedIDStart = 1;
        feedIDEnd = 10000;
        while (feedIDEnd < FEED_ID_MAX) {
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getTransferVolumeByHourFromFix1440MaxMinTime(feedIDStart, feedIDEnd);
            volumeHelper(polls, totalResultMapMax, 4, NUMBER_OF_ROWS, SIMULATE_ETAG_USAGE);

            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fix1440 data...");


        // //////////// write final output to file \\\\\\\\\\\\\\\\\
        final long BYTE_TO_MB = 1048576;
        Long[] volumesCumulated = new Long[NUMBER_OF_ROWS];
        Arrays.fill(volumesCumulated, 0l);
        Iterator<Integer> it = totalResultMapMax.keySet().iterator();
        while (it.hasNext()) {
            int currentHour = (int) it.next();
            Long[] volumes = totalResultMapMax.get(currentHour);

            volumesCumulated[0] += volumes[0];
            volumesCumulated[1] += volumes[1];
            volumesCumulated[2] += volumes[2];
            volumesCumulated[3] += volumes[3];
            volumesCumulated[4] += volumes[4];

            culmulatedVolumeSB.append(currentHour).append(";").append(volumesCumulated[0] / BYTE_TO_MB).append(";")
                    .append(volumesCumulated[1] / BYTE_TO_MB).append(";").append(volumesCumulated[2] / BYTE_TO_MB)
                    .append(";").append(volumesCumulated[3] / BYTE_TO_MB).append(";")
                    .append(volumesCumulated[4] / BYTE_TO_MB).append(";\n");
        }

        boolean outputWritten = FileHelper.writeToFile(sumVolumeMaxFilePath, culmulatedVolumeSB);
        if (outputWritten)
            LOGGER.info("sumVolumeMaxFilePath written to: " + sumVolumeMaxFilePath);
        else
            LOGGER.fatal("sumVolumeMaxFilePath has not been written to: " + sumVolumeMaxFilePath);
    } 
    



    //
    // private void culmulatedVolumeMinTimeFile(final long TIMESTAMP_MAX) {
    // LOGGER.info("starting to create sumVolumeMinFile...");
    // StringBuilder culmulatedVolumeSB = new StringBuilder();
    // /* <hourOfExperiment, culmulatedVolumePerAlgorithm in Bytes> */
    // Map<Integer,Long[]> totalResultMapMin = new TreeMap<Integer,Long[]>();
    // List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
    // int totalExperimentHours = 672;
    // long culmulatedVolumePerTechnique = 0;
    // int feedIDLastStep = -1;
    // int requiredNumberOfPolls = -1;
    // float sizeOfPollLast = -1;
    // int hourLastStep = -1;
    // int numberOfPollsToProcess = -1;
    // int pollingInterval = -1;
    //
    // culmulatedVolumeSB.append("hour of experiment;");
    //
    //
    //
    // ///////////// get data from fix1440Time \\\\\\\\\\\\\\\\\
    // LOGGER.info("starting to create fix1440 data...");
    // culmulatedVolumeSB.append("fix1440;");
    // culmulatedVolumePerTechnique = 0;
    // feedIDLastStep = -1;
    // pollingInterval = 24;
    // requiredNumberOfPolls = totalExperimentHours/pollingInterval;
    // polls = ed.getSumTransferVolumeByHourFromFix1440MaxMinTime(TIMESTAMP_MAX);
    // for (EvaluationFeedPoll poll : polls) {
    // Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // int feedIDCurrent = poll.getFeedID();
    //
    // // in der DB nicht vorhandene Polls generieren
    // if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
    // while (numberOfPollsToProcess > 0){
    // numberOfPollsToProcess--;
    // int hourToProcess = hourLastStep + pollingInterval;
    // long culmulatedVolumePerHour = 0;
    // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    //
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // if (transferredDataArray[0] != null) {
    // culmulatedVolumePerHour = transferredDataArray[0];
    // }
    // }
    // culmulatedVolumePerHour += sizeOfPollLast;
    // transferredDataArray[0] = culmulatedVolumePerHour;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // hourLastStep = hourToProcess;
    // }
    // // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
    // numberOfPollsToProcess = requiredNumberOfPolls;
    // }
    //
    // // aktuellen Poll behandeln
    // int hourToProcess = poll.getHourOfExperiment();
    // long culmulatedVolumePerHour = 0;
    // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    //
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // if (transferredDataArray[0] != null) {
    // culmulatedVolumePerHour = transferredDataArray[0];
    // }
    // }
    // float sizeOfPoll = poll.getSizeOfPoll();
    // culmulatedVolumePerHour += sizeOfPoll;
    // transferredDataArray[0] = culmulatedVolumePerHour;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    //
    // numberOfPollsToProcess--;
    // hourLastStep = hourToProcess;
    // feedIDLastStep = feedIDCurrent;
    // sizeOfPollLast = sizeOfPoll;
    // }
    // LOGGER.info("finished creating fix1440 data...");
    //
    //
    //
    //
    // // ///////////// get data from fix720Time \\\\\\\\\\\\\\\\\
    // // LOGGER.info("starting to create fix720 data...");
    // // culmulatedVolumeSB.append("fix720;");
    // // culmulatedVolumePerTechnique = 0;
    // // feedIDLastStep = -1;
    // // pollingInterval = 12;
    // // requiredNumberOfPolls = totalExperimentHours/pollingInterval;
    // //
    // // polls = ed.getSumTransferVolumeByHourFromFix720MaxMinTime();
    // //
    // // for (EvaluationFeedPoll poll : polls) {
    // // Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // // int feedIDCurrent = poll.getFeedID();
    // //
    // // // in der DB nicht vorhandene Polls generieren
    // // if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
    // // while (numberOfPollsToProcess > 0){
    // // numberOfPollsToProcess--;
    // // int hourToProcess = hourLastStep + pollingInterval;
    // // long culmulatedVolumePerHour = 0;
    // // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // //
    // // if( totalResultMapMin.containsKey(hourToProcess)){
    // // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // // if (transferredDataArray[1] != null) {
    // // culmulatedVolumePerHour = transferredDataArray[1];
    // // }
    // // }
    // // culmulatedVolumePerHour += sizeOfPollLast;
    // // transferredDataArray[1] = culmulatedVolumePerHour;
    // // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // // hourLastStep = hourToProcess;
    // // }
    // // // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
    // // numberOfPollsToProcess = requiredNumberOfPolls;
    // // }
    // //
    // // // aktuellen Poll behandeln
    // // int hourToProcess = poll.getHourOfExperiment();
    // // long culmulatedVolumePerHour = 0;
    // // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // //
    // // if( totalResultMapMin.containsKey(hourToProcess)){
    // // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // // if (transferredDataArray[1] != null) {
    // // culmulatedVolumePerHour = transferredDataArray[1];
    // // }
    // // }
    // // float sizeOfPoll = poll.getSizeOfPoll();
    // // culmulatedVolumePerHour += sizeOfPoll;
    // // transferredDataArray[1] = culmulatedVolumePerHour;
    // // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // //
    // // numberOfPollsToProcess--;
    // // hourLastStep = hourToProcess;
    // // feedIDLastStep = feedIDCurrent;
    // // sizeOfPollLast = sizeOfPoll;
    // // }
    // // LOGGER.info("finished creating fix720 data...");
    //
    //
    // ///////////// get data from fix60Time \\\\\\\\\\\\\\\\\
    // LOGGER.info("starting to create fix60 data...");
    // culmulatedVolumeSB.append("fix60;");
    // culmulatedVolumePerTechnique = 0;
    // feedIDLastStep = -1;
    // pollingInterval = 1;
    // requiredNumberOfPolls = totalExperimentHours/pollingInterval;
    //
    // int feedIDStart = 1;
    // int feedIDEnd = 10000;
    // final int FEED_ID_STEP = 10000;
    // final int FEED_ID_MAX = 210000;
    //
    // while(feedIDEnd < FEED_ID_MAX){
    // LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
    // polls = ed.getSumTransferVolumeByHourFromFix60MaxMinTime(TIMESTAMP_MAX, feedIDStart, feedIDEnd);
    // numberOfPollsToProcess = requiredNumberOfPolls;
    //
    // for (EvaluationFeedPoll poll : polls) {
    // Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // int feedIDCurrent = poll.getFeedID();
    //
    // // in der DB nicht vorhandene Polls generieren
    // if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
    // while (numberOfPollsToProcess > 0){
    // int hourToProcess = hourLastStep + pollingInterval;
    // long culmulatedVolumePerHour = 0;
    // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    //
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // if (transferredDataArray[2] != null) {
    // culmulatedVolumePerHour = transferredDataArray[2];
    // }
    // }
    // culmulatedVolumePerHour += sizeOfPollLast;
    // transferredDataArray[2] = culmulatedVolumePerHour;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // hourLastStep = hourToProcess;
    // numberOfPollsToProcess--;
    // }
    // // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
    // numberOfPollsToProcess = requiredNumberOfPolls;
    // }
    //
    // // aktuellen Poll behandeln
    // int hourToProcess = poll.getHourOfExperiment();
    // long culmulatedVolumePerHour = 0;
    // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    //
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // if (transferredDataArray[2] != null) {
    // culmulatedVolumePerHour = transferredDataArray[2];
    // }
    // }
    // float sizeOfPoll = poll.getSizeOfPoll();
    // culmulatedVolumePerHour += sizeOfPoll;
    // transferredDataArray[2] = culmulatedVolumePerHour;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    //
    // numberOfPollsToProcess--;
    // hourLastStep = hourToProcess;
    // feedIDLastStep = feedIDCurrent;
    // sizeOfPollLast = sizeOfPoll;
    // }
    // feedIDStart += FEED_ID_STEP;
    // feedIDEnd += FEED_ID_STEP;
    // }
    // LOGGER.info("finished creating fix60 data...");
    //
    //
    //
    // ///////////// get data from fixLearnedTime \\\\\\\\\\\\\\\\\
    // LOGGER.info("starting to create fixLearned data...");
    // culmulatedVolumeSB.append("fixLearned;");
    // culmulatedVolumePerTechnique = 0;
    // feedIDLastStep = -1;
    // numberOfPollsToProcess = -1;
    // feedIDStart = 1;
    // feedIDEnd = 10000;
    //
    // while(feedIDEnd < FEED_ID_MAX){
    // LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
    // int minuteLastStep = 0;
    //
    // polls = ed.getSumTransferVolumeByHourFromFixLearnedMinTime(feedIDStart, feedIDEnd);
    //
    // for (EvaluationFeedPoll poll : polls) {
    // Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // int feedIDCurrent = poll.getFeedID();
    //
    // // in der DB nicht vorhandene Polls generieren
    // if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
    // while (minuteLastStep < totalExperimentHours*60){
    //
    // int minuteToProcess = (int)(minuteLastStep + poll.getCheckInterval());
    // int hourToProcess = minuteToProcess/60;
    // long culmulatedVolumePerHour = 0;
    // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    //
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // if (transferredDataArray[3] != null) {
    // culmulatedVolumePerHour = transferredDataArray[3];
    // }
    // }
    // culmulatedVolumePerHour += sizeOfPollLast;
    // transferredDataArray[3] = culmulatedVolumePerHour;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // minuteLastStep = minuteToProcess;
    // numberOfPollsToProcess--;
    // }
    // }
    //
    // // aktuellen Poll behandeln
    // int hourToProcess = poll.getHourOfExperiment();
    // long culmulatedVolumePerHour = 0;
    // transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    //
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // if (transferredDataArray[3] != null) {
    // culmulatedVolumePerHour = transferredDataArray[3];
    // }
    // }
    // float sizeOfPoll = poll.getSizeOfPoll();
    // culmulatedVolumePerHour += sizeOfPoll;
    // transferredDataArray[3] = culmulatedVolumePerHour;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    //
    // if(poll.getNumberOfPoll() > 2) {
    // minuteLastStep += (int)poll.getCheckInterval();
    // }
    // feedIDLastStep = feedIDCurrent;
    // sizeOfPollLast = sizeOfPoll;
    // }
    // feedIDStart += FEED_ID_STEP;
    // feedIDEnd += FEED_ID_STEP;
    // }
    // LOGGER.info("finished creating fixLearned data...");
    //
    //
    //
    // ///////////// get data from adaptiveMaxTime \\\\\\\\\\\\\\\\\
    // LOGGER.info("starting to create adaptive data...");
    // culmulatedVolumeSB.append("adaptive;");
    // culmulatedVolumePerTechnique = 0;
    // polls = ed.getSumTransferVolumeByHourFromAdaptiveMinTime();
    // for (EvaluationFeedPoll poll : polls) {
    // Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // int hourToProcess = poll.getHourOfExperiment();
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // }
    //
    // culmulatedVolumePerTechnique += poll.getCulmulatedSizeofPolls();
    // transferredDataArray[4] = culmulatedVolumePerTechnique;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // }
    // LOGGER.info("finished creating adaptive data...");
    //
    //
    // ///////////// get data from probabilisticMaxTime \\\\\\\\\\\\\\\\\
    // LOGGER.info("starting to create probabilistic data...");
    // culmulatedVolumeSB.append("probabilistic;\n");
    // culmulatedVolumePerTechnique = 0;
    // polls = ed.getSumTransferVolumeByHourFromProbabilisticMinTime();
    // for (EvaluationFeedPoll poll : polls) {
    // Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
    // int hourToProcess = poll.getHourOfExperiment();
    // if( totalResultMapMin.containsKey(hourToProcess)){
    // transferredDataArray = totalResultMapMin.get(hourToProcess);
    // }
    //
    // culmulatedVolumePerTechnique += poll.getCulmulatedSizeofPolls();
    // transferredDataArray[5] = culmulatedVolumePerTechnique;
    // totalResultMapMin.put(hourToProcess, transferredDataArray);
    // }
    // LOGGER.info("finished creating probabilistic data...");
    //
    //
    // int i = 1;
    // final long BYTE_TO_MB = 1048576;
    // Long[] volumesCumulated = new Long[]{0l,0l,0l,0l,0l,0l};
    // for(Long[] volumes : totalResultMapMin.values()){
    // volumesCumulated[0] += volumes[0];
    // volumesCumulated[1] += volumes[1];
    // volumesCumulated[2] += volumes[2];
    // volumesCumulated[3] += volumes[3];
    // culmulatedVolumeSB.append(i).append(";")
    // .append(volumesCumulated[0]/BYTE_TO_MB).append(";")
    // .append(volumesCumulated[1]/BYTE_TO_MB).append(";")
    // .append(volumesCumulated[2]/BYTE_TO_MB).append(";")
    // .append(volumesCumulated[3]/BYTE_TO_MB).append(";")
    // .append(volumes[4]/BYTE_TO_MB).append(";")
    // .append(volumes[5]/BYTE_TO_MB).append(";\n");
    // i++;
    // }
    //
    // boolean outputWritten = FileHelper.writeToFile(sumVolumeMinFilePath, culmulatedVolumeSB);
    // if (outputWritten)
    // LOGGER.info("sumVolumeMinFile written to: " + sumVolumeMaxFilePath);
    // else
    // LOGGER.fatal("sumVolumeMinFile has not been written to: " + sumVolumeMinFilePath);
    // }
    //
    


    /**
     * Only a test
     */
    private void printFeedPolls() {
        List<EvaluationFeedPoll> polls = ed.getFeedPolls();

        for (EvaluationFeedPoll poll : polls) {
            // int feedID = poll.getFeedID();
            System.out.println(poll.getFeedID() + " " + poll.getSizeOfPoll());
        }

    }

    /**
     * @param args ...
     */
	public static void main(String[] args) {

        /**
         * 200 polls for scoreMin
         * 200 polls for scoreMax
         * dataset crawled for 4 weeks = 672 hours - HAS TO BE ADJUSTED!!!!
         * EvaluationDatabase#TIMESTAMP_MAX HAS ALSO TO BE ADJUSTED!!!!
         */
        ChartCreator cc = new ChartCreator(200, 200, 672);

        // cc.printFeedPolls();
        // cc.createFeedSizeHistogrammFile(10, 20); // letzter Test 10.11.
        // cc.createFeedAgeFile(); // letzter Test 10.11.
        cc.createAverageScoreMinByPollFile();
        cc.createPercentageNewFile();

// cc.cumulatedVolumeMaxTimeFile(210000, false);
//      cc.culmulatedVolumeMinTimeFile();
//	    cc.culmulatedVolumeMinTimeEtag304File();
	    	    

	}

}
