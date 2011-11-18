package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.evaluation.ChartCreator;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;

/**
 * Temp class to recalculate PPI in evaluation result tables that cave been created with bug: all polls with
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

        String[] tables = feed11;
        

        // for (String table : tables){
        // LOGGER.info("Processing table " + table);
        // feedStore.setPPIModeItems(table);
        // feedStore.setPPIModeFeeds(table);
        // feedStore.removeEvaluationSummaryFeeds(table);
        // feedStore.createPerStrategyAveragesModeFeeds(table);
        // }

        LOGGER.info("Additionally, adding evaluation results for eval_fixLearnedP_5_43200_2011-11-18_10-52-30");
        String missingEvalTab = "eval_fixLearnedP_5_43200_2011-11-18_10-52-30";

        LOGGER.info("Start generating evaluation summary. This may take a while. Seriously!");
        boolean dataWritten = feedStore.generateEvaluationSummary(missingEvalTab);
        if (dataWritten) {
            LOGGER.info("Evaluation results have been written to database.");
        } else {
            LOGGER.fatal("Evaluation results have NOT been written to database!");
        }
        ChartCreator chartCreator = new ChartCreator(200, 200);
        String[] dbTable = { missingEvalTab };
        chartCreator.transferVolumeCreator(feedStore, dbTable);

    }

}
