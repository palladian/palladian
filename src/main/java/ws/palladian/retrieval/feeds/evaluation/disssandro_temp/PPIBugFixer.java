package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;

/**
 * Temp class to recalculate PPI in evaluation result tables that ave been created with bug: all polls with
 * windowSize=0 were ignored
 * 
 * @author Sandro Reichert
 * 
 */
public class PPIBugFixer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PPIBugFixer.class);

    public static void main(String[] args) {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();


        final EvaluationFeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, config);

        StringBuilder logMsg = new StringBuilder();
        logMsg.append("Recalculating ");

        String[] feed11 = { "eval_fixLearnedP_1_10080_2011-11-18_00-41-27",
                "eval_fixLearnedP_1_1440_2011-11-17_22-54-47", 
                "eval_fixLearnedP_5_10080_2011-11-18_06-37-54",
                "eval_fixLearnedP_5_1440_2011-11-18_02-04-36",
                "z_eval_AdaptiveTTL_0.2_min_time_100_2011-11-08_20-08-39",
                "z_eval_AdaptiveTTL_0.3_min_time_100_2011-11-09_14-11-50",
                "z_eval_AdaptiveTTL_0.7_min_time_100_2011-11-10_10-53-49",
                "z_eval_fix1440_min_time_100_2011-11-07_20-51-27",
                "z_eval_fixLearnedP_min_time_100_2011-11-07_22-18-21",
                "z_eval_IndHist_0.3_min_time_100_2011-11-15_21-32-30",
                "z_eval_IndHist_0.6_min_time_100_2011-11-16_17-35-44", 
                "z_eval_LRU2_min_time_100_2011-11-13_00-07-21" };

        String[] hp = { "gold_eval_fixlearnedp_min_time_100_2011-11-07_19-25-09",
                "gold_eval_fixlearnedw_min_time_100_2011-11-11_16-27-10",
                "eval_indhist_0.5_min_time_100_2011-11-14_23-15-19",
                "eval_indhist_0.5_min_time_100_2011-11-14_23-15-19", 
                "eval_mav_min_time_100_2011-11-12_18-59-01",
                "eval_mav_old_min_time_100_2011-11-12_18-00-05" };
        
        String[] feed104 = { "z_eval_AdaptiveTTL_0.1_min_time_100_2011-11-10_21-14-37",
                "z_eval_AdaptiveTTL_0.1_min_time_100_2011-11-10_21-14-37",
                "z_eval_AdaptiveTTL_0.5_min_time_100_2011-11-09_13-25-56",
                "z_eval_AdaptiveTTL_0.6_min_time_100_2011-11-09_22-51-03",
                "z_eval_AdaptiveTTL_0.8_min_time_100_2011-11-10_11-04-55",
                "z_eval_AdaptiveTTL_0.9_min_time_100_2011-11-11_16-13-54",
                "z_eval_AdaptiveTTL_1.0_min_time_100_2011-11-12_16-54-36",
                "z_eval_fix10080_min_time_100_2011-11-07_21-10-40",
                "z_eval_fix60_min_time_100_2011-11-07_00-56-55",
                "z_eval_fixLearnedW_min_time_100_2011-11-11_23-15-53",
                "z_eval_IndHist_0.15_min_time_100_2011-11-15_14-13-07",
                "z_eval_IndHist_0.4_min_time_100_2011-11-16_17-31-50",
                "z_eval_IndHist_0.5_min_time_100_2011-11-14_23-22-52",
                "z_eval_MAVSync_min_time_100_2011-11-13_00-56-23",
                "eval_MAVSync_1_10080_2011-11-17_23-32-53",
                "eval_MAVSync_1_1440_2011-11-17_22-29-45",
                "eval_MAVSync_15_10080_2011-11-18_16-24-14",
                "eval_MAVSync_15_1440_2011-11-18_13-38-16",
                "eval_MAVSync_15_43200_2011-11-18_18-52-22",
                "eval_MAVSync_5_10080_2011-11-18_04-59-41",
                "eval_MAVSync_5_1440_2011-11-18_00-13-13",
                "eval_MAVSync_5_43200_2011-11-18_09-20-34",
                "eval_MAVSync_60_1440_2011-11-18_21-24-06",
                "eval_MAVSync_60_10080_2011-11-18_23-11-36",
                "eval_MAVSync_60_43200_2011-11-19_00-39-09" };

        String[] tables = feed104;
        
        for (String table : tables) {
            LOGGER.info("Processing table " + table);
            feedStore.setPPIModeItems(table);
            feedStore.setPPIModeFeeds(table);
            feedStore.removeEvaluationSummaryFeeds(table);
            feedStore.createPerStrategyAveragesModeFeeds(table);
        }


    }

}
