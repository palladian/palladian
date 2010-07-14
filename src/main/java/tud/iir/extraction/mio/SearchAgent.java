/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import tud.iir.web.SourceRetriever;

/**
 * The SearchAgent uses given queries to initiate a search at a searchEngine.
 * 
 * @author Martin Werner
 */
public class SearchAgent {

    int resultCount = 20;
    int searchEngine = 2;

    /**
     * Instantiates a new search agent.
     * 
     * @param resultCount the result count
     */
    public SearchAgent(int resultCount) {
        this.resultCount = resultCount;
    }

    /**
     * Initiate search.
     * 
     * @param searchQueries the search queries
     * @return the list
     */
    public List<String> initiateSearch(List<String> searchQueries) {
        SourceRetriever sRetriever = new SourceRetriever();
        sRetriever.setResultCount(resultCount);

        // set focus on english content
        sRetriever.setLanguage(0);
        sRetriever.setSource(searchEngine);

        ArrayList<String> MIOPageCandidateList;

        ArrayList<String> resultList = new ArrayList<String>();

        for (String searchQuery : searchQueries) {
            // System.out.println(searchQuery);
            ArrayList<String> resultURLList = sRetriever.getURLs(searchQuery, false);
            // System.out.println("URLLIST: " + ResultURLList.size() + "____");
            // resultList.add("TEST");
            // ArrayList<WebResult> ResultList = sRetriever.getWebResults(searchQuery, searchEngine, false);
            // System.out.println("Webresults: " + ResultList.size());
            // webResultList.addAll(ResultList);
            resultList.addAll(resultURLList);
            // break;
        }
        // System.out.println(resultList.size());

        MIOPageCandidateList = removeDuplicates(resultList);
        // System.out.println(MIOPageCandidateList.toString());
        // System.out.println(MIOPageCandidateList.size());
        // System.exit(1);
        return MIOPageCandidateList;
    }

    /**
     * remove duplicates.
     * 
     * @param webResultList the web result list
     * @return the array list
     */
    private ArrayList<String> removeDuplicates(List<String> resultList) {
        ArrayList<String> urlList = new ArrayList<String>();
        for (String webrsl : resultList) {

            if (!urlList.contains(webrsl)) {
                urlList.add(webrsl);
            }
        }
        return urlList;

    }

}
