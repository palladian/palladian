package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;

/**
 * Temp class to recalculate recall in evaluation result tables that have been created with 2 bugs: <br />
 * 1) items received in first poll were included in calculation but shouldn't. <br />
 * 2) average recall in mode feeds must not exclude feeds that had PPI = null.
 * 
 * @author Sandro Reichert
 * 
 */
public class RecallBugFixer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PPIBugFixer.class);

    public static void main(String[] args) {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        final EvaluationFeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, config);

        StringBuilder logMsg = new StringBuilder();
        logMsg.append("Recalculating ");

         String[] feed11 = { "eval_IndHistTTL_1.0_1.0_2.0_1_1_40320_2011-11-27_14-10-58",
                 "eval_fixLearnedP_15_10080_2011-11-19_02-00-41",
                 "eval_fixLearnedP_15_1440_2011-11-18_23-22-29",
                 "eval_fixLearnedP_15_43200_2011-11-19_04-17-04",
                 "eval_fixLearnedP_1_10080_2011-11-18_00-41-27",
                 "eval_fixLearnedP_1_1440_2011-11-17_22-54-47",
                 "eval_fixLearnedP_5_10080_2011-11-18_06-37-54",
                 "eval_fixLearnedP_5_1440_2011-11-18_02-04-36",
                 "eval_fixLearnedP_5_43200_2011-11-18_10-52-30",
                 "eval_fixLearnedP_60_10080_2011-11-19_08-23-49",
                 "eval_fixLearnedP_60_1440_2011-11-19_06-29-58",
                 "eval_fixLearnedP_60_43200_2011-11-19_09-53-14",
                 "z_eval_AdaptiveTTL_0.2_min_time_100_2011-11-08_20-08-39",
                 "z_eval_AdaptiveTTL_0.3_min_time_100_2011-11-09_14-11-50",
                 "z_eval_AdaptiveTTL_0.7_min_time_100_2011-11-10_10-53-49",
                 "z_eval_IndHist_0.1_1_44640_2011-11-22_15-02-57",
                 "z_eval_IndHist_0.6_1_40320_2011-11-24_20-53-30",
                 "z_eval_IndHist_0.9_1_40320_2011-11-25_16-38-32",
                 "z_eval_IndHist_1.0_1_40320_2011-11-26_13-55-20",
                 "z_eval_LIHZ_0.5_1_40320_2011-11-24_16-01-41",
                 "z_eval_LIHZ_0.9_1_40320_2011-11-24_18-01-28",
                 "z_eval_LRU2_min_time_100_2011-11-13_00-07-21",
                 "z_eval_fix1440_min_time_100_2011-11-07_20-51-27",
                 "z_eval_fixLearnedP_min_time_100_2011-11-07_22-18-21" };

        // String[] hp = { "gold_eval_fix1440_min_time_100_2011-11-07_14-06-25",
        // "gold_eval_fixlearnedp_min_time_100_2011-11-07_19-25-09",
        // "gold_eval_fixlearnedw_min_time_100_2011-11-11_16-27-10",
        // "eval_indhist_0.5_min_time_100_2011-11-14_23-15-19",
        // };

        String[] feed104 = { "eval_IndHistTTL_0.1_0.1_2.0_1_1_40320_2011-11-26_14-02-07",
                "eval_LIHZ_0.2_1_40320_2011-11-28_02-06-56",
                "eval_LIHZ_0.4_1_40320_2011-11-28_03-36-39",
                "eval_LIHZ_0.6_1_40320_2011-11-28_05-06-01",
                "eval_LIHZ_0.8_1_40320_2011-11-28_06-35-37",
                "eval_LIHZ_0.9_1_40320_2011-11-28_08-04-55",
                "eval_LIHZ_1.0_1_40320_2011-11-28_09-34-41",
                "eval_LIHZ_1.5_1_40320_2011-11-28_11-02-22",
                "eval_LIHZ_2.0_1_40320_2011-11-28_12-25-38",
                "eval_LIHZ_2.5_1_40320_2011-11-28_13-46-43",
                "z_eval_AdaptiveTTL_0.1_min_time_100_2011-11-10_21-14-37",
                "z_eval_AdaptiveTTL_0.4_min_time_100_2011-11-08_20-52-34",
                "z_eval_AdaptiveTTL_0.5_min_time_100_2011-11-09_13-25-56",
                "z_eval_AdaptiveTTL_0.6_min_time_100_2011-11-09_22-51-03",
                "z_eval_AdaptiveTTL_0.8_min_time_100_2011-11-10_11-04-55",
                "z_eval_AdaptiveTTL_0.9_min_time_100_2011-11-11_16-13-54",
                "z_eval_AdaptiveTTL_1.0_min_time_100_2011-11-12_16-54-36",
                "z_eval_AdaptiveTTL_4.0_1_40320_2011-11-27_21-31-14",
                "z_eval_IndHist_0.2_1_40320_2011-11-22_17-11-39",
                "z_eval_IndHist_0.3_1_40320_2011-11-20_17-05-22",
                "z_eval_IndHist_0.4_1_40320_2011-11-24_22-51-14",
                "z_eval_IndHist_0.5_1_40320_2011-11-21_11-05-17",
                "z_eval_IndHist_0.7_1_40320_2011-11-22_01-55-14",
                "z_eval_IndHist_0.8_1_40320_2011-11-25_16-37-51",
                "z_eval_LIHZ_0.1_1_40320_2011-11-24_15-56-21",
                "z_eval_LIHZ_0.3_1_40320_2011-11-24_17-26-57",
                "z_eval_LIHZ_0.5_1_40320_2011-11-24_20-31-58",
                "z_eval_LIHZ_0.7_1_40320_2011-11-24_18-55-26",
                "z_eval_MAVSync_15_10080_2011-11-18_16-24-14",
                "z_eval_MAVSync_15_1440_2011-11-18_13-38-16",
                "z_eval_MAVSync_15_43200_2011-11-18_18-52-22",
                "z_eval_MAVSync_1_10080_2011-11-17_23-32-53",
                "z_eval_MAVSync_1_1440_2011-11-17_22-29-45",
                "z_eval_MAVSync_5_10080_2011-11-18_04-59-41",
                "z_eval_MAVSync_5_1440_2011-11-18_00-13-13",
                "z_eval_MAVSync_5_43200_2011-11-18_09-20-34",
                "z_eval_MAVSync_60_10080_2011-11-18_23-11-36",
                "z_eval_MAVSync_60_1440_2011-11-18_21-24-06",
                "z_eval_MAVSync_60_43200_2011-11-19_00-39-09",
                "z_eval_MAVSync_min_time_100_2011-11-13_00-56-23",
                "z_eval_fix10080_min_time_100_2011-11-07_21-10-40",
                "z_eval_fix60_min_time_100_2011-11-07_00-56-55",
                "z_eval_fixLearnedW_min_time_100_2011-11-11_23-15-53" };

        String[] tables = feed11;

        for (String table : tables) {
            LOGGER.info("Processing table " + table);
            feedStore.createInitialItemsTempTable(table);
            feedStore.setRecallModeFeeds(table);
            feedStore.setRecallModeItems(table);
            feedStore.removeEvaluationSummaryFeeds(table);
            feedStore.createPerStrategyAveragesModeFeeds(table);
        }

    }

}
