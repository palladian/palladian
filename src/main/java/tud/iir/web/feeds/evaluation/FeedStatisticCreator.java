package tud.iir.web.feeds.evaluation;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.helper.CountMap;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.SimpleResultCallback;
import tud.iir.web.Crawler;
import tud.iir.web.feeds.Feed;
import tud.iir.web.feeds.FeedClassifier;
import tud.iir.web.feeds.FeedPostStatistics;
import tud.iir.web.feeds.FeedReader;
import tud.iir.web.feeds.persistence.FeedDatabase;
import tud.iir.web.feeds.persistence.FeedStore;

/**
 * The FeedStatisticCreator creates a file with statistics about feeds from a feed store.
 * 
 * @author David Urbansky
 * 
 */
public class FeedStatisticCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedStatisticCreator.class);

    /**
     * Perform maxCoveragePolicyEvaluation on all evaluation tables.
     * 
     * @throws SQLException
     */
    public static void maxCoveragePolicyEvaluationAll() throws SQLException {

        Set<String> tables = new HashSet<String>();
        tables.add("feed_evaluation_polls_fixedLearned");

        for (String table : tables) {
            minDelayPolicyEvaluation(table);
        }

    }

    public static void maxCoveragePolicyEvaluation(String tableName) throws SQLException {
        maxCoveragePolicyEvaluation(1, tableName);
        maxCoveragePolicyEvaluation(2, tableName);
    }

    /**
     * <p>
     * After running the FeedChecker in {@link FeedReader.BENCHMARK_MAX} mode and importing the resulting poll data into
     * the "feed_evaluation_polls" table in the database, this method calculates the coverage score and meta
     * information.
     * </p>
     * <ul>
     * <li>Coverage = percentNew / sqrt(missedItems/windowSize), averaged over all feeds and all polls except the first
     * one.</li>
     * <li>The average percentage of new entries whenever no item was missed.</li>
     * <li>The number of missed items on average.</li>
     * <li>The percentage of missed items in relation to the window size on average.</li>
     * </ul>
     * 
     * @throws SQLException
     */
    public static void maxCoveragePolicyEvaluation(int avgStyle, String tableName) throws SQLException {

        final StringBuilder csv = new StringBuilder();

        String avgStyleExplanation = "";
        String query = "";
        String query1 = "SELECT AVG(feedGroup.coverage) AS coverage, AVG(feedGroup.percentNew) AS percentNew, AVG(feedGroup.missedItems) AS missedItems, AVG(feedGroup.missedPercent) AS missedPercent, AVG(feedGroup.traffic) AS traffic FROM (SELECT AVG(newWindowItems/(windowSize * SQRT(missedItems+1))) AS coverage, AVG(newWindowItems/windowSize) AS percentNew, AVG(missedItems) AS missedItems, AVG(missedItems/windowSize) AS missedPercent, AVG(sizeOfPoll/newWindowItems) AS traffic FROM feed_evaluation_polls WHERE numberOfPoll > 1 AND pollTimestamp <= "
            + FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000l + " GROUP BY feedID) AS feedGroup";
        String query2 = "SELECT AVG(newWindowItems/(windowSize * SQRT(missedItems+1))) AS coverage, AVG(newWindowItems/windowSize) AS percentNew, AVG(missedItems) AS missedItems, AVG(missedItems/windowSize) AS missedPercent, AVG(sizeOfPoll/newWindowItems) AS traffic FROM feed_evaluation_polls WHERE numberOfPoll > 1 AND pollTimestamp <= "
            + FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000l;

        if (avgStyle == 1) {
            avgStyleExplanation = "average over all polls per feed and then all feeds";
            query = query1;
        } else if (avgStyle == 2) {
            avgStyleExplanation = "average over all polls of all feeds";
            query = query2;
        }
        final String avgStyleExplanationF = avgStyleExplanation;

        final DecimalFormat format = new DecimalFormat("#.###################");
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        DatabaseManager dbm = new DatabaseManager();

        SimpleResultCallback callback = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {

                double coverage = (Double) object.get("coverage");
                double percentNew = (Double) object.get("percentNew");
                double missed = (Double) object.get("missedItems");
                double missedPercent = (Double) object.get("missedPercent");
                double traffic = (Double) object.get("traffic");

                // build csv
                csv.append("\"================= Average Performance (").append(avgStyleExplanationF)
                .append(", regardless of feed activity pattern) =================\"\n");
                csv.append("Coverage:;" + format.format(coverage)).append("\n");
                csv.append("Percent New:;" + format.format(100 * percentNew)).append("\n");
                csv.append("Missed:;" + format.format(missed)).append("\n");
                csv.append("Percent Missed Items / Window Size:;" + format.format(100 * missedPercent)).append("\n");
                csv.append("Traffic Per Item:;" + traffic).append("\n\n");

            }
        };
        dbm.runQuery(callback, query);


        // create statistics by activity pattern
        Integer[] activityPatternIDs = FeedClassifier.getActivityPatternIDs();

        for (Integer activityPatternID : activityPatternIDs) {

            if (activityPatternID < 5 || activityPatternID == FeedClassifier.CLASS_ON_THE_FLY) {
                continue;
            }

            csv.append("\"================= Performance for ").append(FeedClassifier.getClassName(activityPatternID))
            .append(" (").append(avgStyleExplanation)
            .append(", only feeds matching the feed activity pattern) =================\"\n");

            String queryAP = query.replaceAll("numberOfPoll > 1", "numberOfPoll > 1 AND activityPattern = "
                    + activityPatternID);

            dbm.runQuery(callback, queryAP);
        }

        System.out.println(csv);
        FileHelper.writeToFile("data/temp/feedEvaluationMaxCoverage_" + avgStyle + "_" + tableName + ".csv", csv);

        Logger.getRootLogger().info("logs written to data/temp/feedEvaluationMaxCoverage.csv");
    }

    private static double calculateMedianDelay(int avgStyle, int activityPattern, String tableName) throws SQLException {

        final List<Double> valueList = new ArrayList<Double>();
        DatabaseManager dbm = new DatabaseManager();

        String query = "";
        String countQuery = "SELECT COUNT(*) AS count FROM " + tableName;

        if (avgStyle == 1) {
            query = "SELECT AVG(cumulatedLateDelay/(newWindowItems+missedItems)) AS delay FROM " + tableName
            + " WHERE timeliness IS NOT NULL GROUP BY feedID LIMIT OFFSET,100000";
        } else if (avgStyle == 2) {
            query = "SELECT cumulatedLateDelay/(newWindowItems+missedItems) AS delay FROM " + tableName
            + " WHERE timeliness IS NOT NULL LIMIT OFFSET,100000";
        }

        if (activityPattern > -1) {
            query = query.replaceAll("timeliness IS NOT NULL", "timeliness IS NOT NULL AND activityPattern = "
                    + activityPattern);
            countQuery += " WHERE activityPattern = " + activityPattern;
        }

        SimpleResultCallback callback = new SimpleResultCallback() {
            @Override
            public void processResult(Map<String, Object> object, int number) {
                valueList.add((Double) object.get("delay"));
            }
        };

        long maxOffset = dbm.runCountQuery(countQuery);

        for (long currentOffset = 0; currentOffset < maxOffset; currentOffset += 500000) {

            String currentQuery = query.replaceAll("OFFSET", String.valueOf(currentOffset));

            Logger.getRootLogger().info(
                    "query for delay to calculate median, offset/maxOffset:" + currentOffset + "/" + maxOffset);

            dbm.runQuery(callback, currentQuery);
        }

        Collections.sort(valueList);

        return MathHelper.getMedian(valueList);
    }

    /**
     * Perform minDelayPolicyEvaluation on all evaluation tables.
     * 
     * @throws SQLException
     */
    public static void minDelayPolicyEvaluationAll() throws SQLException {

        Set<String> tables = new HashSet<String>();
        tables.add("feed_evaluation2_adaptive_min_poll");
        tables.add("feed_evaluation2_probabilistic_min_poll");
        tables.add("feed_evaluation2_fix60_max_min_poll");
        tables.add("feed_evaluation2_fix1440_max_min_poll");
        tables.add("feed_evaluation2_fix_learned_min_poll");

        for (String table : tables) {
            minDelayPolicyEvaluation(table);
        }

    }

    public static void minDelayPolicyEvaluation(String tableName) throws SQLException {
        minDelayPolicyEvaluation(1, tableName);
        minDelayPolicyEvaluation(2, tableName);
    }

    /**
     * <p>
     * After running the FeedChecker in {@link FeedReader.BENCHMARK_MIN} mode and importing the resulting poll data into
     * the "feed_evaluation_polls" table in the database, this method calculates the timeliness scores.
     * </p>
     * <ul>
     * <li>Timeliness = 1 / sqrt((cumulatedDelay/surroundingInterval + 1)), averaged over all feeds and item
     * discoveries.</li>
     * 
     * </ul>
     * 
     * @throws SQLException
     */
    public static void minDelayPolicyEvaluation(int avgStyle, String tableName) throws SQLException {

        Logger.getRootLogger().info("min evaluation for " + avgStyle + " and table " + tableName);

        final StringBuilder csv = new StringBuilder();

        // average over all polls per feed and then all feeds
        String avgStyleExplanation = "";
        String query1 = "SELECT AVG(feedGroup.timeliness) AS timeliness, AVG(feedGroup.timelinessLate) AS timelinessLate, AVG(feedGroup.delay) AS delay, AVG(feedGroup.missedItems) AS missedItems FROM (SELECT AVG(timeliness) AS timeliness, AVG(timelinessLate) AS timelinessLate, AVG(cumulatedLateDelay/(newWindowItems+missedItems)) AS delay, AVG(missedItems) AS missedItems FROM "
            + tableName + " WHERE timeliness IS NOT NULL AND timelinessLate > 0 GROUP BY feedID) AS feedGroup";
        String query1A = "SELECT AVG(feedGroup.pollsPerNewItem) AS pollsPerNewItem, AVG(feedGroup.trafficPerNewItem) AS trafficPerNewItem FROM (SELECT COUNT(*)/SUM(newWindowItems) AS pollsPerNewItem, SUM(sizeOfPoll)/SUM(newWindowItems) AS trafficPerNewItem FROM "
            + tableName + " WHERE numberOfPoll > 1 GROUP BY feedID) AS feedGroup";
        String query1B = "SELECT AVG(feedGroup.newItemsPerDiscovery) AS newItemsPerDiscovery FROM (SELECT SUM(newWindowItems) AS totalNewItems, SUM(newWindowItems)/COUNT(*) AS newItemsPerDiscovery FROM "
            + tableName + " WHERE newWindowItems > 0 GROUP BY feedID) AS feedGroup;";

        // average over all polls of all feeds
        String query2 = "SELECT AVG(timeliness) AS timeliness, AVG(timelinessLate) AS timelinessLate, AVG(cumulatedLateDelay/(newWindowItems+missedItems)) AS delay, AVG(missedItems) AS missedItems FROM "
            + tableName + " WHERE timeliness IS NOT NULL AND timelinessLate > 0";
        String query2A = "SELECT COUNT(*)/SUM(newWindowItems) AS pollsPerNewItem, SUM(sizeOfPoll)/SUM(newWindowItems) AS trafficPerNewItem FROM "
            + tableName + " WHERE numberOfPoll > 1";
        String query2B = "SELECT SUM(newWindowItems) AS totalNewItems, SUM(newWindowItems)/COUNT(*) AS newItemsPerDiscovery FROM "
            + tableName + " WHERE newWindowItems > 0";

        // TODO where newWindowItems > 0 instead of where timeliness IS NOT NULL

        String query = "";
        String queryA = "";
        String queryB = "";
        if (avgStyle == 1) {
            avgStyleExplanation = "average over all polls per feed and then all feeds";
            query = query1;
            queryA = query1A;
            queryB = query1B;
        } else if (avgStyle == 2) {
            avgStyleExplanation = "average over all polls of all feeds";
            query = query2;
            queryA = query2A;
            queryB = query2B;
        }

        final String avgStyleExplanationF = avgStyleExplanation;

        final DecimalFormat format = new DecimalFormat("#.###################");
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        DatabaseManager dbm = new DatabaseManager();

        // Double trafficPerNewItemCG = null;

        SimpleResultCallback callback = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                csv.append("\"================= Average Performance (").append(avgStyleExplanationF)
                        .append(", regardless of feed activity pattern) =================\"\n");
                csv.append("Timeliness:;" + format.format(object.get("timeliness"))).append("\n");
                csv.append("Timeliness Late:;" + format.format(object.get("timelinessLate"))).append("\n");
                csv.append(
                        "Average Delay:;"
                                + DateHelper.getTimeString(1000L * ((Double) object.get("delay")).longValue())).append(
                        "\n");
                csv.append("Avg. Missed Items:;" + format.format(object.get("missedItems"))).append("\n");
            }

        };

        dbm.runQuery(callback, query);
        csv.append(
                "Median Delay:;"
                        + DateHelper.getTimeString(1000L * ((Double) calculateMedianDelay(avgStyle, -1, tableName))
                                .longValue())).append("\n");

        SimpleResultCallback callback2 = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                csv.append("Polls Per New Item:;" + format.format(object.get("pollsPerNewItem"))).append("\n");
                csv.append("Traffic Per New Item:;" + object.get("trafficPerNewItem")).append("\n");
            }

        };

        dbm.runQuery(callback2, queryA);


        SimpleResultCallback callback3 = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                csv.append("New Items Per Discovery:;" + format.format(object.get("newItemsPerDiscovery")))
                        .append("\n");
            }

        };

        dbm.runQuery(callback3, queryB);

        
// score = avgDelay / 1000 * pollsPerNewItem;
        // csv.append("Score:;" + format.format(score)).append("\n");
        // score *= 1 + missedItems;
        // csv.append("Score (with misses):;" + format.format(score)).append("\n\n");

        // csv.append("Traffic Per New Item (conditional get):;" + trafficPerNewItemCG).append("\n\n");

        // create statistics by activity pattern
        Integer[] activityPatternIDs = FeedClassifier.getActivityPatternIDs();

        for (Integer activityPatternID : activityPatternIDs) {

            if (activityPatternID < 5 || activityPatternID == FeedClassifier.CLASS_ON_THE_FLY) {
                continue;
            }

            csv.append("\"================= Performance for ").append(FeedClassifier.getClassName(activityPatternID))
            .append(" (").append(avgStyleExplanation)
            .append(", only feeds matching the activity pattern) =================\"\n");

            String queryAP = query.replaceAll("timeliness IS NOT NULL", "timeliness IS NOT NULL AND activityPattern = "
                    + activityPatternID);

            dbm.runQuery(callback, queryAP);

            csv.append(
                    "Median Delay:;"
                            + DateHelper.getTimeString(1000L * ((Double) calculateMedianDelay(avgStyle, -1, tableName))
                                    .longValue())).append("\n");

           
            String queryAAP = queryA.replaceAll("numberOfPoll > 1", "numberOfPoll > 1 AND activityPattern = "
                    + activityPatternID);

            dbm.runQuery(callback2, queryAAP);

            String queryBAP = queryB.replaceAll("newWindowItems > 0", "newWindowItems > 0 AND activityPattern = "
                    + activityPatternID);

            dbm.runQuery(callback3, queryBAP);
        }

        System.out.println(csv);
        FileHelper.writeToFile("data/temp/feedEvaluationMinDelay_" + avgStyle + "_" + tableName + ".csv", csv);

        Logger.getRootLogger().info("logs written to data/temp/feedEvaluationMinDelay.csv");
    }

    public static void delayChart() throws SQLException {

        Set<String> tables = new HashSet<String>();
        tables.add("feed_evaluation2_adaptive_min_poll");
        tables.add("feed_evaluation2_probabilistic_min_poll");
        tables.add("feed_evaluation2_fix60_max_min_poll");
        tables.add("feed_evaluation2_fix1440_max_min_poll");
        tables.add("feed_evaluation2_fix_learned_min_poll");

        for (String table : tables) {
            delayChart(table);
        }

    }

    public static void delayChart(String tableName) throws SQLException {

        StringBuilder csv = new StringBuilder();

        DatabaseManager dbm = new DatabaseManager();

        dbm.runUpdate("CREATE TABLE tempTableA AS SELECT a.feedID FROM (SELECT feedID FROM feed_evaluation2_adaptive_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= 40) a, (SELECT feedID FROM feed_evaluation2_probabilistic_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= 40) b, (SELECT feedID FROM feed_evaluation2_fix60_max_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= 40) c, (SELECT feedID FROM feed_evaluation2_fix1440_max_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= 20) d, (SELECT feedID FROM feed_evaluation2_fix_learned_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= 40) e WHERE a.feedID = b.feedID AND b.feedID = c.feedID AND c.feedID = d.feedID AND d.feedID = e.feedID");
        dbm.runUpdate("ALTER TABLE tempTableA ADD PRIMARY KEY (`feedID`)");

        // new item number, [total delay, number of feeds]
        final Map<Integer, Double[]> delayChartData = new TreeMap<Integer, Double[]>();

        // 0: previousFeedID, 1: newItemNumberDiscovery
        final Integer[] ints = new Integer[2];
        ints[0] = -1;
        ints[1] = 1;

        SimpleResultCallback callback3 = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                Integer feedID = (Integer) object.get("feedID");

                if (feedID != ints[0]) {
                    ints[1] = 1;
                }

                Double[] data = delayChartData.get(ints[1]);
                if (data == null) {
                    data = new Double[2];
                    data[0] = 0.0;
                    data[1] = 0.0;
                }
                data[0] += (Double) object.get("delay");
                data[1]++;
                delayChartData.put(ints[1], data);
                ints[0] = feedID;
                ints[1]++;
            }

        };

        dbm.runQuery(
                callback3,
                "SELECT fep.feedID, numberOfPoll, cumulatedLateDelay/(newWindowItems+missedItems) AS delay FROM "
                        + tableName
                        + " fep,tempTableA WHERE fep.feedID = tempTableA.feedID AND newWindowItems > 0 ORDER BY fep.feedID ASC, numberOfPoll ASC");

        
        dbm.runUpdate("DROP TABLE tempTableA");

        csv.append("new item number discovery;average delday;number of feeds\n");
        for (Entry<Integer, Double[]> dataEntry : delayChartData.entrySet()) {
            double avgTimeliness = dataEntry.getValue()[0] / dataEntry.getValue()[1];
            csv.append(dataEntry.getKey()).append(";").append(avgTimeliness).append(";")
            .append(dataEntry.getValue()[1]).append("\n");
        }

        String path = "data/temp/feedEvaluationDelayChart_" + tableName + ".csv";
        FileHelper.writeToFile(path, csv);

        Logger.getRootLogger().info(path);

    }

    public static void timelinessChart() throws SQLException {

        StringBuilder csv = new StringBuilder();

        DatabaseManager dbm = new DatabaseManager();

        // new item number, [total timeliness, number of feeds]
        final Map<Integer, Double[]> timelinessChartData = new TreeMap<Integer, Double[]>();

        // 0: previousFeedID, 1: newItemNumber
        final Integer[] ints = new Integer[2];
        ints[0] = -1;
        ints[1] = 1;

        SimpleResultCallback callback = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                Integer feedID = (Integer) object.get("feedID");

                if (feedID != ints[0]) {
                    ints[1] = 1;
                }

                Double[] data = timelinessChartData.get(ints[1]);
                if (data == null) {
                    data = new Double[2];
                    data[0] = 0.0;
                    data[1] = 0.0;
                }
                data[0] += (Double) object.get("timeliness");
                data[1]++;
                timelinessChartData.put(ints[1], data);
                ints[0] = feedID;
                ints[1]++;
            }
        };

        dbm.runQuery(callback, "SELECT feedID, timeliness FROM feed_evaluation_polls WHERE timeliness IS NOT NULL");

        csv.append("new item number;average timeliness;number of feeds\n");
        for (Entry<Integer, Double[]> dataEntry : timelinessChartData.entrySet()) {
            double avgTimeliness = dataEntry.getValue()[0] / dataEntry.getValue()[1];
            csv.append(dataEntry.getKey()).append(";").append(avgTimeliness).append(";")
            .append(dataEntry.getValue()[1]).append("\n");
        }

        FileHelper.writeToFile("data/temp/feedEvaluationTimelinessChart.csv", csv);

        Logger.getRootLogger().info("logs written to data/temp/feedEvaluationTimelinessChart.csv");

    }

    /**
     * Generate a csv containing the following information:<br>
     * feedID;activityPattern;avgEntriesPerDay;medianPostGap;averagePostGap
     * 
     * @param feedStore The store where the feeds are held.
     * @param statisticOutputPath The path where the output should be written to.
     * @throws IOException
     */
    public static void createFeedUpdateIntervalDistribution(FeedStore feedStore, String statisticOutputPath)
    throws IOException {

        FeedReader fc = new FeedReader(feedStore);
        FeedReaderEvaluator.setBenchmarkPolicy(FeedReaderEvaluator.BENCHMARK_MAX_COVERAGE);

        FileWriter csv = new FileWriter(statisticOutputPath);

        int c = 0;
        int totalSize = feedStore.getFeeds().size();
        for (Feed feed : feedStore.getFeeds()) {

            // if (feed.getId() != 27) {
            // continue;
            // }

            FeedBenchmarkFileReader fbfr = new FeedBenchmarkFileReader(feed, fc);
            fbfr.updateEntriesFromDisk();
            if (feed.getItems() == null || feed.getItems().size() < 1) {
                continue;
            }
            FeedPostStatistics fps = new FeedPostStatistics(feed);
            csv.write(String.valueOf(feed.getId() + ";"));
            csv.write(String.valueOf(feed.getActivityPattern()) + ";");
            csv.write(String.valueOf(fps.getAvgEntriesPerDay()) + ";");
            csv.write(String.valueOf(fps.getMedianPostGap()) + ";");
            csv.write(String.valueOf((long) fps.getAveragePostGap()) + ";");
            csv.write("\n");

            csv.flush();

            c++;

            feed.freeMemory();
            feed.setLastHeadlines("");
            feed.setMeticulousPostDistribution(null);
            feed = null;

            Logger.getRootLogger().info("percent done: " + MathHelper.round(100 * c / (double) totalSize, 2));
        }

        csv.close();
    }

    /**
     * Generate a csv file containing the following information:<br>
     * feedID;realAverageUpdateInterval;averageIntervalFix1d;averageIntervalFix1h;averageIntervalFixLearned;
     * averageIntervalPostRate;averageIntervalMAV
     * 
     * @param feedStore The store where the feeds are held.
     * @param statisticOutputPath The path where the output should be written to.
     * @throws IOException
     * @throws SQLException
     */
    public static void createFeedUpdateIntervals(FeedStore feedStore, String statisticOutputPath) throws IOException,
    SQLException {

        String psFix1d = "SELECT AVG(checkInterval) FROM feed_evaluation2_fix1440_max_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        String psFix1h = "SELECT AVG(checkInterval) FROM feed_evaluation2_fix60_max_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        String psFixLearned = "SELECT AVG(checkInterval) FROM feed_evaluation2_fix_learned_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        String psFixPostRate = "SELECT AVG(checkInterval) FROM feed_evaluation2_probabilistic_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        String psFixMAV = "SELECT AVG(checkInterval) FROM feed_evaluation2_adaptive_min_poll WHERE feedID = ? AND numberOfPoll > 1";

        // create the file we want to write to
        FileWriter csv = new FileWriter(statisticOutputPath);

        int c = 0;
        int totalSize = feedStore.getFeeds().size();
        for (Feed feed : feedStore.getFeeds()) {

            String safeFeedName = feed.getId() + "_";
            String historyFilePath = FeedReaderEvaluator.findHistoryFile(safeFeedName);

            // determine the real average update interval of the feeds by looking at the first and last poll timestamp
            // and the number of items
            long newestItemTime = 0;
            long oldestItemTime = 0;
            List<String> items = FileHelper.readFileToArray(historyFilePath);

            if (items.size() <= 1) {
                continue;
            }

            for (String item : items) {
                String[] itemParts = item.split(";");
                if (newestItemTime == 0) {
                    newestItemTime = Long.valueOf(itemParts[0]);
                }
                oldestItemTime = Long.valueOf(itemParts[0]);
            }

            long totalTime = (newestItemTime - oldestItemTime) / DateHelper.MINUTE_MS;

            double realAverageUpdateInterval = MathHelper.round(totalTime / ((double) items.size() - 1), 2);

            // limit feeds to a maximum real average update interval of 31 days = 44640 minutes
            if (realAverageUpdateInterval > 44640) {
                LOGGER.warn("feed had real update interval of" + realAverageUpdateInterval
                        + ", that is too few updates, we skip");
                continue;
            }

            StringBuilder temp = new StringBuilder();

            temp.append(String.valueOf(feed.getId() + ";"));
            temp.append(String.valueOf(realAverageUpdateInterval) + ";");

            // get the number of polls for the feed from the database for the update strategies
            psFix1d = psFix1d.replace("?", String.valueOf(feed.getId()));
            double updateInterval = getUpdateInterval(psFix1d);
            if (updateInterval < 1) {
                LOGGER.warn("feed had " + items.size()
                        + " items and the number of polls was too small to calculate a meaningful value");
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            psFix1h = psFix1h.replace("?", String.valueOf(feed.getId()));
            updateInterval = getUpdateInterval(psFix1h);
            if (updateInterval < 1) {
                LOGGER.warn("feed had " + items.size()
                        + " items and the number of polls was too small to calculate a meaningful value");
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            psFixLearned = psFixLearned.replace("?", String.valueOf(feed.getId()));
            updateInterval = getUpdateInterval(psFixLearned);
            if (updateInterval < 1) {
                LOGGER.warn("feed had " + items.size()
                        + " items and the number of polls was too small to calculate a meaningful value");
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            psFixPostRate = psFixPostRate.replace("?", String.valueOf(feed.getId()));
            updateInterval = getUpdateInterval(psFixPostRate);
            if (updateInterval < 1) {
                LOGGER.warn("feed had " + items.size()
                        + " items and the number of polls was too small to calculate a meaningful value");
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            psFixMAV = psFixMAV.replace("?", String.valueOf(feed.getId()));
            updateInterval = getUpdateInterval(psFixMAV);
            if (updateInterval < 1) {
                LOGGER.warn("feed had " + items.size()
                        + " items and the number of polls was too small to calculate a meaningful value");
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            csv.write(temp.toString());
            csv.write("\n");

            csv.flush();

            c++;

            Logger.getRootLogger().info("percent done: " + MathHelper.round(100 * c / (double) totalSize, 2));
        }

        csv.close();
    }

    private static double getUpdateInterval(String query) throws SQLException {
        DatabaseManager dbm = new DatabaseManager();

        final Double[] updateInterval = new Double[1];

        SimpleResultCallback callback = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                updateInterval[0] = (Double) object.get("updateInterval");
            }

        };

        dbm.runQuery(callback, query);

        return updateInterval[0];
    }

    public static void createGeneralStatistics(FeedStore feedStore, String statisticOutputPath) {

        // colors for the google chart
        List<String> colors = new ArrayList<String>();
        colors.add("E72727");
        colors.add("34B434");
        colors.add("3072F3");
        colors.add("FF9900");
        colors.add("7777CC");
        colors.add("AA0033");
        colors.add("626262");
        colors.add("9ABE9A");
        colors.add("E6FF00");
        colors.add("F626B1");
        colors.add("FF9900");

        // count number of feeds in each updateClass
        CountMap updateClassCounts = new CountMap();

        // number of unique domain names
        Set<String> uniqueDomains = new HashSet<String>();

        List<Feed> feeds = feedStore.getFeeds();
        for (Feed feed : feeds) {

            // int updateClassCount = (Integer) updateClassCounts.get(feed.getUpdateClass());
            // updateClassCount++;
            // updateClassCounts.put(feed.getUpdateClass(), updateClassCount);

            updateClassCounts.increment(feed.getActivityPattern());

            uniqueDomains.add(Crawler.getDomain(feed.getFeedUrl()));
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Number of feeds:").append(feeds.size()).append("\n");
        stats.append("Number of unique domains:").append(uniqueDomains.size()).append("\n");

        String chartData = "";
        String chartDataLabels = "";
        String chartColors = "";
        for (Entry<Object, Integer> o : updateClassCounts.entrySet()) {
            stats.append("Number of feeds in update class ").append(o.getKey()).append(":").append(o.getValue())
            .append("\n");
            chartData += o.getValue().intValue() + ",";
            chartDataLabels += o.getKey() + "|";
            chartColors += colors.get((Integer) o.getKey()) + "|";
        }
        chartData = chartData.substring(0, chartData.length() - 1);
        chartDataLabels = chartDataLabels.substring(0, chartDataLabels.length() - 1);
        chartColors = chartColors.substring(0, chartColors.length() - 1);

        stats.append("Google pie chart:").append("http://chart.apis.google.com/chart?chs=600x425&chco=")
        .append(chartColors).append("&chdl=").append(chartDataLabels).append("&chds=0,").append(feeds.size())
        .append("&cht=p&chd=t:").append(chartData).append("\n");

        FileHelper.writeToFile(statisticOutputPath, stats);

    }

    /**
     * @param args
     * @throws IOException
     * @throws SQLException
     */
    public static void main(String[] args) throws IOException, SQLException {
        // FeedStatisticCreator.createGeneralStatistics(FeedDatabase.getInstance(), "data/temp/feedstats_combined.txt");
        // FeedStatisticCreator.createFeedUpdateIntervalDistribution(FeedDatabase.getInstance(),"data/temp/feedUpdateIntervals.csv");
        // FeedStatisticCreator.maxCoveragePolicyEvaluation("feed_evaluation_polls");
        // FeedStatisticCreator.minDelayPolicyEvaluation("feed_evaluation_polls");
        // FeedStatisticCreator.minDelayPolicyEvaluation("feed_evaluation2_fix60_max_min_poll");
        // FeedStatisticCreator.minDelayPolicyEvaluationAll();
        // FeedStatisticCreator.timelinessChart();
        // FeedStatisticCreator.delayChart();
        FeedStatisticCreator.createFeedUpdateIntervals(new FeedDatabase(), "data/temp/feedUpdateIntervals.csv");

    }

}
