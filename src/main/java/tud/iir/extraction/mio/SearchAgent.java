/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import tud.iir.web.SourceRetriever;
import tud.iir.web.WebResult;

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

        ArrayList<String> MIOPageCandidateList;

        ArrayList<WebResult> webResultList = new ArrayList<WebResult>();

        for (String searchQuery : searchQueries) {

            ArrayList<WebResult> ResultList = sRetriever.getWebResults(searchQuery, searchEngine, false);
            webResultList.addAll(ResultList);
        }

        MIOPageCandidateList = removeDuplicates(webResultList);

        return MIOPageCandidateList;
    }

    /**
     * remove duplicates.
     *
     * @param webResultList the web result list
     * @return the array list
     */
    private ArrayList<String> removeDuplicates(List<WebResult> webResultList) {
        ArrayList<String> urlList = new ArrayList<String>();
        for (WebResult webrsl : webResultList) {

            if (!urlList.contains(webrsl.getUrl())) {
                urlList.add(webrsl.getUrl());
            }
        }
        return urlList;

    }

}
