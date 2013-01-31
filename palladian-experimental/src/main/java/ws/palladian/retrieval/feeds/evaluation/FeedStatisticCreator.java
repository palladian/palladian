package ws.palladian.retrieval.feeds.evaluation;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedActivityPattern;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.icwsm2011.FeedBenchmarkFileReader;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * The FeedStatisticCreator creates a file with statistics about feeds from a feed store.
 * 
 * @author David Urbansky
 * 
 */
/**
 * @author Sandro Reichert
 * 
 */
public class FeedStatisticCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedStatisticCreator.class);

    /**
     * A feed must have this many polls with new items to be used for the delay
     * chart.
     */
    private static int pollsWithNewItems = 300;

    // _50 -> ~9600 Feeds ohne 1440 zu betrachten - "temptable50"
    // 100 -> ~5800 Feeds ohne 1440 zu betrachten
    // 150 -> ~3700 Feeds ohne 1440 zu betrachten - "temptable150"
    // 200 -> ~2740 Feeds ohne 1440 zu betrachten - "temptable200"
    // 300 -> ~1500 Feeds ohne 1440 zu betrachten - "temptable300"

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

        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {

                double coverage = resultSet.getDouble("coverage");
                double percentNew = resultSet.getDouble("percentNew");
                double missed = resultSet.getDouble("missedItems");
                double missedPercent = resultSet.getDouble("missedPercent");
                double traffic = resultSet.getDouble("traffic");

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
        // Integer[] activityPatternIDs = FeedClassifier.getActivityPatternIDs();
        FeedActivityPattern[] activityPatterns = FeedActivityPattern.values();

        for (FeedActivityPattern activityPattern : activityPatterns) {

            if (activityPattern.getIdentifier() < 5 || activityPattern == FeedActivityPattern.CLASS_ON_THE_FLY) {
                continue;
            }

            csv.append("\"================= Performance for ").append(activityPattern.getClassName())
            .append(" (").append(avgStyleExplanation)
            .append(", only feeds matching the feed activity pattern) =================\"\n");

            String queryAP = query.replaceAll("numberOfPoll > 1", "numberOfPoll > 1 AND activityPattern = "
                    + activityPattern);

            dbm.runQuery(callback, queryAP);
        }

        System.out.println(csv);
        FileHelper.writeToFile("data/temp/feedEvaluationMaxCoverage_" + avgStyle + "_" + tableName + ".csv", csv);

        LOGGER.info("logs written to data/temp/feedEvaluationMaxCoverage.csv");
    }

    // TODO Sandro: für Evaluation Diss benötigt, missedItems sollte aber nicht in Berechnung eingehen
    private static double calculateMedianDelay(int avgStyle, int activityPattern, String tableName) throws SQLException {

        List<Double> valueList = new ArrayList<Double>();
        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

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

        RowConverter<Double> converter = new RowConverter<Double>() {

            @Override
            public Double convert(ResultSet resultSet) throws SQLException {
                return resultSet.getDouble("delay");
            }
        };

        Integer maxOffset = dbm.runAggregateQuery(countQuery);

        if (maxOffset != null) {

            for (int currentOffset = 0; currentOffset < maxOffset; currentOffset += 500000) {

                String currentQuery = query.replaceAll("OFFSET", String.valueOf(currentOffset));

                LOGGER.info(
                        "query for delay to calculate median, offset/maxOffset:" + currentOffset + "/" + maxOffset);

                List<Double> currentValues = dbm.runQuery(converter, currentQuery);
                valueList.addAll(currentValues);
            }

        }

        Collections.sort(valueList);
        CollectionHelper.removeNulls(valueList);

        long[] valueArray = ArrayUtils.toPrimitive(valueList.toArray(new Long[0]));
        return MathHelper.getMedian(valueArray);
    }

    /**
     * Perform minDelayPolicyEvaluation on all evaluation tables.
     * 
     * @throws SQLException
     */
    public static void minDelayPolicyEvaluationAll() throws SQLException {

        Set<String> tables = new HashSet<String>();
        tables.add("feed_evaluation2_adaptive_min_time");
        tables.add("feed_evaluation2_probabilistic_min_time");
        tables.add("feed_evaluation2_fix60_max_min_time");
        tables.add("feed_evaluation2_fix1440_max_min_time");
        tables.add("feed_evaluation2_fix_learned_min_time");

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

        LOGGER.info("min evaluation for " + avgStyle + " and table " + tableName);

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

        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

        // Double trafficPerNewItemCG = null;

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                csv.append("\"================= Average Performance (").append(avgStyleExplanationF)
                .append(", regardless of feed activity pattern) =================\"\n");
                csv.append("Timeliness:;" + format.format(resultSet.getDouble("timeliness"))).append("\n");
                csv.append("Timeliness Late:;" + format.format(resultSet.getDouble("timelinessLate"))).append("\n");
                csv.append(
                        "Average Delay:;"
                                + DateHelper.getTimeString(1000L * ((Double) resultSet.getDouble("delay")).longValue()))
                                .append("\n");
                csv.append("Avg. Missed Items:;" + format.format(resultSet.getDouble("missedItems"))).append("\n");
            }
        };

        dbm.runQuery(callback, query);
        csv.append(
                "Median Delay:;"
                        + DateHelper.getTimeString(1000L * ((Double) calculateMedianDelay(avgStyle, -1, tableName))
                                .longValue())).append("\n");

        ResultSetCallback callback2 = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                csv.append("Polls Per New Item:;" + format.format(resultSet.getDouble("pollsPerNewItem"))).append("\n");
                csv.append("Traffic Per New Item:;" + resultSet.getDouble("trafficPerNewItem")).append("\n");
            }

        };

        dbm.runQuery(callback2, queryA);

        ResultSetCallback callback3 = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                csv.append("New Items Per Discovery:;" + format.format(resultSet.getDouble("newItemsPerDiscovery")))
                .append("\n");
            }

        };

        dbm.runQuery(callback3, queryB);

        // score = avgDelay / 1000 * pollsPerNewItem;
        // csv.append("Score:;" + format.format(score)).append("\n");
        // score *= 1 + missedItems;
        // csv.append("Score (with misses):;" +
        // format.format(score)).append("\n\n");

        // csv.append("Traffic Per New Item (conditional get):;" +
        // trafficPerNewItemCG).append("\n\n");

        // create statistics by activity pattern
        FeedActivityPattern[] activityPatterns = FeedActivityPattern.values();

        for (FeedActivityPattern activityPattern : activityPatterns) {

            if (activityPattern.getIdentifier() < 5 || activityPattern == FeedActivityPattern.CLASS_ON_THE_FLY) {
                continue;
            }

            csv.append("\"================= Performance for ").append(activityPattern.getClassName())
            .append(" (").append(avgStyleExplanation)
            .append(", only feeds matching the activity pattern) =================\"\n");

            String queryAP = query.replaceAll("timeliness IS NOT NULL", "timeliness IS NOT NULL AND activityPattern = "
                    + activityPattern.getIdentifier());

            dbm.runQuery(callback, queryAP);

            csv.append(
                    "Median Delay:;"
                            + DateHelper.getTimeString(1000L * ((Double) calculateMedianDelay(avgStyle, -1, tableName))
                                    .longValue())).append("\n");

            String queryAAP = queryA.replaceAll("numberOfPoll > 1", "numberOfPoll > 1 AND activityPattern = "
                    + activityPattern.getIdentifier());

            dbm.runQuery(callback2, queryAAP);

            String queryBAP = queryB.replaceAll("newWindowItems > 0", "newWindowItems > 0 AND activityPattern = "
                    + activityPattern.getIdentifier());

            dbm.runQuery(callback3, queryBAP);
        }

        System.out.println(csv);
        FileHelper.writeToFile("data/temp/feedEvaluationMinDelay_" + avgStyle + "_" + tableName + ".csv", csv);

        LOGGER.info("logs written to data/temp/feedEvaluationMinDelay.csv");
    }

    public static void delayChart() throws SQLException {

        final String tempTable = "tempTable" + pollsWithNewItems;

        Set<String> tables = new HashSet<String>();
        tables.add("feed_evaluation2_adaptive_min_poll");
        tables.add("feed_evaluation2_probabilistic_min_poll");
        tables.add("feed_evaluation2_fix60_max_min_poll");
        tables.add("feed_evaluation2_fix1440_max_min_poll");
        tables.add("feed_evaluation2_fix_learned_min_poll");

        createTempTablePollsX(tempTable);

        for (String table : tables) {
            delayChart(table, tempTable);
        }

        dropTempTable(tempTable);
    }

    /**
     * Creates a temporary table required to generate the delay charts, uses
     * FeedStatisticCreator.pollsWithNewItems to restrict the feeds to feeds
     * that have at least that many polls with at least one new item.
     * 
     * @param tempTableName
     *            The table's name.
     */
    private static void createTempTablePollsX(final String tempTableName) {
        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

        // with fix1440
        // final String sql = "CREATE TABLE "
        // + tempTableName
        // + " AS SELECT a.feedID, a.activityPattern FROM "
        // +
        // "(SELECT feedID, activityPattern FROM feed_evaluation2_adaptive_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
        // + POLLS_WITH_NEW_ITEMS
        // + ") a, "
        // +
        // "(SELECT feedID FROM feed_evaluation2_probabilistic_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
        // + POLLS_WITH_NEW_ITEMS
        // + ") b, "
        // +
        // "(SELECT feedID FROM feed_evaluation2_fix60_max_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
        // + POLLS_WITH_NEW_ITEMS
        // + ") c, "
        // +
        // "(SELECT feedID FROM feed_evaluation2_fix1440_max_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
        // + POLLS_WITH_NEW_ITEMS
        // + ") d, "
        // +
        // "(SELECT feedID FROM feed_evaluation2_fix_learned_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
        // + POLLS_WITH_NEW_ITEMS
        // +
        // ") e WHERE a.feedID = b.feedID AND b.feedID = c.feedID AND c.feedID = d.feedID AND d.feedID = e.feedID";

        // ignore table 1440
        final String sql = "CREATE TABLE "
                + tempTableName
                + " AS SELECT a.feedID, a.activityPattern FROM "
                + "(SELECT feedID, activityPattern FROM feed_evaluation2_adaptive_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
                + pollsWithNewItems
                + ") a, "
                + "(SELECT feedID FROM feed_evaluation2_probabilistic_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
                + pollsWithNewItems
                + ") b, "
                + "(SELECT feedID FROM feed_evaluation2_fix60_max_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
                + pollsWithNewItems
                + ") c, "
                + "(SELECT feedID FROM feed_evaluation2_fix_learned_min_poll WHERE newWindowItems > 0 GROUP BY feedID HAVING COUNT(feedID) >= "
                + pollsWithNewItems + ") d WHERE a.feedID = b.feedID AND b.feedID = c.feedID AND c.feedID = d.feedID";

        LOGGER.info(sql);
        dbm.runUpdate(sql);
        dbm.runUpdate("ALTER TABLE " + tempTableName + " ADD PRIMARY KEY (`feedID`)");
    }

    /**
     * Drops the given table.
     * 
     * @param tempTableName
     *            Table to drop.
     */
    private static void dropTempTable(final String tempTableName) {
        final String sql = "DROP TABLE " + tempTableName;
        LOGGER.info(sql);
        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());
        dbm.runUpdate(sql);
    }

    public static void delayChart(final String tableName, final String tempTableName) throws SQLException {

        StringBuilder csv = new StringBuilder();

        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

        // new item number, [total delay, number of feeds]
        final Map<Integer, Double[]> delayChartData = new TreeMap<Integer, Double[]>();

        // 0: previousFeedID, 1: newItemNumberDiscovery
        final Integer[] ints = new Integer[2];
        ints[0] = -1;
        ints[1] = 1;

        ResultSetCallback callback3 = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                Integer feedID = resultSet.getInt("feedID");

                if (feedID != ints[0]) {
                    ints[1] = 1;
                }

                Double[] data = delayChartData.get(ints[1]);
                if (data == null) {
                    data = new Double[2];
                    data[0] = 0.0;
                    data[1] = 0.0;
                }
                data[0] += resultSet.getDouble("delay");
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
                        + " fep,"
                        + tempTableName
                        + " temp WHERE fep.feedID = temp.feedID AND newWindowItems > 0 ORDER BY fep.feedID ASC, numberOfPoll ASC");

        csv.append("new item number discovery;average delday;number of feeds\n");
        for (Entry<Integer, Double[]> dataEntry : delayChartData.entrySet()) {
            double avgTimeliness = dataEntry.getValue()[0] / dataEntry.getValue()[1];
            csv.append(dataEntry.getKey()).append(";").append(avgTimeliness).append(";")
            .append(dataEntry.getValue()[1]).append("\n");
        }

        String path = "data/temp/feedEvaluationDelayChart_" + pollsWithNewItems + "polls_" + tableName + ".csv";
        FileHelper.writeToFile(path, csv);

        LOGGER.info(path);

    }

    public static void timelinessChart() throws SQLException {

        StringBuilder csv = new StringBuilder();

        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());

        // new item number, [total timeliness, number of feeds]
        final Map<Integer, Double[]> timelinessChartData = new TreeMap<Integer, Double[]>();

        // 0: previousFeedID, 1: newItemNumber
        final Integer[] ints = new Integer[2];
        ints[0] = -1;
        ints[1] = 1;

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                Integer feedID = resultSet.getInt("feedID");

                if (feedID != ints[0]) {
                    ints[1] = 1;
                }

                Double[] data = timelinessChartData.get(ints[1]);
                if (data == null) {
                    data = new Double[2];
                    data[0] = 0.0;
                    data[1] = 0.0;
                }
                data[0] += resultSet.getDouble("timeliness");
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

        LOGGER.info("logs written to data/temp/feedEvaluationTimelinessChart.csv");

    }

    /**
     * Generate a csv containing the following information:<br>
     * feedID;activityPattern;avgEntriesPerDay;medianPostGap;averagePostGap
     * 
     * @param feedStore
     *            The store where the feeds are held.
     * @param statisticOutputPath
     *            The path where the output should be written to.
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
            feed.setMeticulousPostDistribution(null);
            feed = null;

            LOGGER.info("percent done: " + MathHelper.round(100 * c / (double) totalSize, 2));
        }

        csv.close();
    }

    /**
     * Creates the temporary table "tempTableMin" that contains all feedIDs that
     * are contained in all five "_min_time" tables.
     */
    @SuppressWarnings("unused")
    private static void createTempTableMin() {
        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());
        String sql = "CREATE TABLE tempTableMin AS SELECT DISTINCT a.feedID FROM "
                + "feed_evaluation2_adaptive_min_time a, feed_evaluation2_fix1440_max_min_time b "
                + "WHERE a.feedID = b.feedID " + "AND a.pollTimestamp BETWEEN "
                + FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND / 1000 + " AND "
                + FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000
                + " AND b.pollTimestamp BETWEEN " + FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND
                / 1000 + " AND "
                + FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000;

        LOGGER.info(sql);
        dbm.runUpdate(sql);

        sql = "ALTER TABLE tempTableMin ADD PRIMARY KEY (`feedID`)";
        LOGGER.info(sql);
        dbm.runUpdate(sql);
    }

    /**
     * Checks whether a feed is contained in tempTableMin.
     * 
     * @param feed
     *            The feed to check
     * @return true if feed is contained in tempTableMin, otherwise {@code false}.
     * @see #createTempTableMin()
     */
    private static boolean isInTempTable(Feed feed) {
        String sql = "SELECT COUNT(*) AS count FROM tempTableMin WHERE feedID = " + feed.getId();
        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());
        Integer c = dbm.runAggregateQuery(sql);
        if (c != null && c > 0) {
            return true;
        }
        return false;
    }

    /**
     * Generate a csv file containing the following information:<br>
     * feedID;realAverageUpdateInterval;averageIntervalFix1d;
     * averageIntervalFix1h;averageIntervalFixLearned;
     * averageIntervalPostRate;averageIntervalMAV
     * 
     * @param feedStore
     *            The store where the feeds are held.
     * @param statisticOutputPath
     *            The path where the output should be written to.
     * @throws IOException
     * @throws SQLException
     */
    public static void createFeedUpdateIntervals(FeedStore feedStore, String statisticOutputPath) throws IOException,
    SQLException {

        // String psFix1d =
        // "SELECT AVG(checkInterval) FROM feed_evaluation2_fix1440_max_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        // String psFix1h =
        // "SELECT AVG(checkInterval) FROM feed_evaluation2_fix60_max_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        // String psFixLearned =
        // "SELECT AVG(checkInterval) FROM feed_evaluation2_fix_learned_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        // String psFixPostRate =
        // "SELECT AVG(checkInterval) FROM feed_evaluation2_probabilistic_min_poll WHERE feedID = ? AND numberOfPoll > 1";
        // String psFixMAV =
        // "SELECT AVG(checkInterval) FROM feed_evaluation2_adaptive_min_poll WHERE feedID = ? AND numberOfPoll > 1";

        String psFixLearned = "SELECT AVG(checkInterval) AS checkInterval FROM feed_evaluation2_fix_learned_min_time WHERE feedID = ?";
        String psFixPostRate = "SELECT AVG(checkInterval) AS checkInterval FROM feed_evaluation2_probabilistic_min_time WHERE feedID = ?";
        String psFixMAV = "SELECT AVG(checkInterval) AS checkInterval FROM feed_evaluation2_adaptive_min_time WHERE feedID = ?";

        // create the file we want to write to
        FileWriter csv = new FileWriter(statisticOutputPath);

        // create header
        csv.write("feedID;realUpdateInterval;realItemInterval;Fix 1d;Fix 1h;Fix Learned;Post Rate;MAV;\n");

        int feedCounter = 0;
        int totalSize = feedStore.getFeeds().size();
        for (Feed feed : feedStore.getFeeds()) {
            feedCounter++;

            if (!isInTempTable(feed)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("not in temptable, that is, not in our time frame...skipping");
                }
                continue;
            }

            String safeFeedName = feed.getId() + "_";
            String historyFilePath = FeedReaderEvaluator.findHistoryFile(safeFeedName);

            List<String> items = FileHelper.readFileToArray(historyFilePath);

            if (items.size() <= 1) {
                continue;
            }

            long stopTime = FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND;
            int numberOfUpdatesDuringExperiment = 0;
            // long newestItemTime = 0;
            long oldestItemTime = 0;
            long itemTimeLastStep = 0;
            int numberOfItemsDuringExperiment = 0;
            for (String item : items) {
                String[] itemParts = item.split(";");
                // if (newestItemTime == 0) {
                // newestItemTime = Long.valueOf(itemParts[0]);
                // }
                oldestItemTime = Long.valueOf(itemParts[0]);

                // skip items with same timestamp to count updates
                if (oldestItemTime == itemTimeLastStep) {
                    // LOGGER.info("Feed " + feed.getId() +
                    // " skip item with timestamp " + oldestItemTime);
                    numberOfItemsDuringExperiment++;
                    continue;
                }

                // count only items that are in the time frame of our experiment
                if (Long.valueOf(itemParts[0]) < stopTime) {
                    numberOfUpdatesDuringExperiment++;
                    numberOfItemsDuringExperiment++;
                }
                itemTimeLastStep = oldestItemTime;
            }

            long totalTime = stopTime - oldestItemTime;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("feedID " + feed.getId() + "totalTime: " + totalTime + ", stopTime: " + stopTime
                        + ", oldestItemTime: " + oldestItemTime + ", numberOfUpdatesDuringExperiment: "
                        + numberOfUpdatesDuringExperiment + ", numberOfItemsDuringExperiment: "
                        + numberOfItemsDuringExperiment);
            }

            double realAverageUpdateInterval = MathHelper.round(
                    (totalTime / ((double)numberOfUpdatesDuringExperiment - 1)), 2) / TimeUnit.MINUTES.toMillis(1);

            double realAverageItemInterval = MathHelper.round(
                    (totalTime / ((double)numberOfItemsDuringExperiment - 1)), 2) / TimeUnit.MINUTES.toMillis(1);

            // limit feeds to a maximum real average update interval of 31 days
            // = 44640 minutes
            if (realAverageUpdateInterval > 44640) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("feed had real update interval of "
                            + DateHelper.formatDuration(0L, (long) realAverageUpdateInterval)
                            + ", that is too few updates, we should skip, but don't :)");
                }
                // continue;
            }

            StringBuilder temp = new StringBuilder();

            temp.append(String.valueOf(feed.getId() + ";"));
            temp.append(String.valueOf(realAverageUpdateInterval) + ";");
            temp.append(String.valueOf(realAverageItemInterval) + ";");

            // get the number of polls for the feed from the database for the
            // update strategies
            temp.append("1440;");
            temp.append("60;");

            String sql = psFixLearned.replace("?", String.valueOf(feed.getId()));
            double updateInterval = getUpdateInterval(sql);
            if (updateInterval < 1) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("feed " + feed.getId() + " had " + items.size()
                            + " items and the number of polls was too small to calculate a meaningful value");
                }
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            sql = psFixPostRate.replace("?", String.valueOf(feed.getId()));
            updateInterval = getUpdateInterval(sql);
            if (updateInterval < 1) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("feed had " + items.size()
                            + " items and the number of polls was too small to calculate a meaningful value");
                }
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            sql = psFixMAV.replace("?", String.valueOf(feed.getId()));
            updateInterval = getUpdateInterval(sql);
            if (updateInterval < 1) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("feed had " + items.size()
                            + " items and the number of polls was too small to calculate a meaningful value");
                }
                continue;
            }
            temp.append(String.valueOf(updateInterval) + ";");

            csv.write(temp.toString());
            csv.write("\n");

            csv.flush();

            if (feedCounter % 1000 == 0) {
                LOGGER.info("percent done: " + MathHelper.round(100 * feedCounter / (double) totalSize, 2));
            }
        }
        csv.close();
        LOGGER.info("feedUpdateIntervals written to " + statisticOutputPath);
    }

    private static double getUpdateInterval(String query) throws SQLException {

        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance().getConfig());
        RowConverter<Double> converter = new RowConverter<Double>() {

            @Override
            public Double convert(ResultSet resultSet) throws SQLException {
                return resultSet.getDouble("checkInterval");
            }
        };

        return dbm.runSingleQuery(converter, query);
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
        CountMap<FeedActivityPattern> updateClassCounts = CountMap.create();

        // number of unique domain names
        Set<String> uniqueDomains = new HashSet<String>();

        List<Feed> feeds = feedStore.getFeeds();
        for (Feed feed : feeds) {

            // int updateClassCount = (Integer)
            // updateClassCounts.get(feed.getUpdateClass());
            // updateClassCount++;
            // updateClassCounts.put(feed.getUpdateClass(), updateClassCount);

            updateClassCounts.add(feed.getActivityPattern());

            uniqueDomains.add(UrlHelper.getDomain(feed.getFeedUrl()));
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Number of feeds:").append(feeds.size()).append("\n");
        stats.append("Number of unique domains:").append(uniqueDomains.size()).append("\n");

        String chartData = "";
        String chartDataLabels = "";
        String chartColors = "";
        for (FeedActivityPattern o : updateClassCounts.uniqueItems()) {
            stats.append("Number of feeds in update class ").append(o).append(":").append(updateClassCounts.getCount(o))
            .append("\n");
            chartData += updateClassCounts.getCount(o) + ",";
            chartDataLabels += o + "|";
            chartColors += colors.get(o.getIdentifier()) + "|";
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
        // FeedStatisticCreator.createGeneralStatistics(FeedDatabase.getInstance(),
        // "data/temp/feedstats_combined.txt");
        FeedStatisticCreator.createFeedUpdateIntervalDistribution(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig()),
                "data/temp/feedUpdateIntervals.csv");
        // FeedStatisticCreator.maxCoveragePolicyEvaluation("feed_evaluation_polls");
        // FeedStatisticCreator.minDelayPolicyEvaluation("feed_evaluation_polls");
        // FeedStatisticCreator.minDelayPolicyEvaluation("feed_evaluation2_fix60_max_min_poll");
        // FeedStatisticCreator.minDelayPolicyEvaluationAll();
        // FeedStatisticCreator.timelinessChart();
        // FeedStatisticCreator.delayChart();
        // FeedStatisticCreator.createTempTableMin();
        // FeedStatisticCreator.createFeedUpdateIntervals(new FeedDatabase(),
        // "data/temp/feedUpdateIntervals.csv");
        // FeedStatisticCreator.pollsWithNewItems = 500;
        // delayChart();

    }

}
