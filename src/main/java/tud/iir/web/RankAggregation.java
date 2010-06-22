package tud.iir.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import tud.iir.helper.CollectionHelper;

/**
 * RankAggregation combines multiple ranked lists of WebResults into one.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class RankAggregation {

    public static final int RANK_AVERAGE = 0;

    // public static final int BORDA = 1;
    // public static final int FOOTRULE = 2;
    // public static final int MC1 = 3;
    // public static final int MC2 = 4;
    // public static final int MC3 = 5;
    // public static final int MC4 = 6;

    /**
     * The interface to access different rank aggregation techniques.
     * 
     * @param lists - List of ranked lists of WebResults
     * @param method - The technique to use for rank aggregation. Currently implemented is RANK_AVERAGE.
     * @param maxResults - The maximum number of results returned in the resulting, aggregated list.
     * @return A list of AggregatedResult's
     */
    public static List<AggregatedResult> aggregate(List<List<WebResult>> lists, int method, int maxResults) {

        List<AggregatedResult> aggregatedList = null;

        switch (method) {
            case RANK_AVERAGE:
                aggregatedList = rankAverage(lists);
            default:
                break;
        }

        if (aggregatedList.size() > maxResults) {
            return aggregatedList.subList(0, maxResults);
        } else {
            return aggregatedList;
        }
    }

    /**
     * Combine multiple ranked lists of WebResults to one ranked list, using the "Rank Average" algorithm described in "Search in Context", R.Kraft et al., WWW
     * 2006.
     * 
     * @param lists k ranked AggregatedResult lists.
     * @return A ranked list of AggregatedResults.
     */
    private static List<AggregatedResult> rankAverage(List<List<WebResult>> lists) {

        // HashMap<URL, WEBRESULTS+>>
        HashMap<String, List<WebResult>> webresults = new HashMap<String, List<WebResult>>();

        // HashMap<URL, SCORE>
        HashMap<String, Integer> scoreMap = new HashMap<String, Integer>();

        // collect all webresults from all lists, grouped by URL
        for (List<WebResult> list : lists) {
            for (WebResult wr : list) {
                // normalize url
                String url = Crawler.getCleanURL(wr.getUrl());

                // initialize
                if (!webresults.containsKey(url)) {
                    webresults.put(url, new ArrayList<WebResult>());
                    scoreMap.put(url, 0);
                }

                // remember the WebResult
                webresults.get(url).add(wr);
            }
        }

        // score each url in webresults

        // for each list
        for (List<WebResult> list : lists) {

            // for each url
            for (Entry<String, List<WebResult>> wrs : webresults.entrySet()) {

                String url = wrs.getKey();
                int score_for_this_list = 0;
                boolean found_in_list = false;

                // remember the WebResult
                for (WebResult wr : wrs.getValue()) {
                    if (list.contains(wr)) {
                        score_for_this_list = wr.getRank();
                        found_in_list = true;
                    }
                }

                if (!found_in_list) {
                    score_for_this_list = list.size() + 1;
                }

                scoreMap.put(url, scoreMap.get(url) + score_for_this_list);
            }
        }

        // for (String w : webresults.keySet()) {
        // System.out.println("\n\t" + w);
        // for (WebResult wr : webresults.get(w)) {
        // System.out.println(wr);
        // }
        // }

        // sort URLs by score
        scoreMap = CollectionHelper.sortByValue(scoreMap.entrySet(), true);

        // get first webresult from webresults, ordered by value
        ArrayList<AggregatedResult> results = new ArrayList<AggregatedResult>();

        int i = 1;
        for (Entry<String, Integer> w : scoreMap.entrySet()) {
            AggregatedResult ar = new AggregatedResult(webresults.get(w.getKey()), i);
            // System.out.println(i + " (aggRank) : " + w.getValue() + " (score) : " + webresults.get(w.getKey()).size() + " (webresults) : " +
            // w.getKey() + " (url)");
            results.add(ar);
            i++;
        }

        return results;
    }
}
