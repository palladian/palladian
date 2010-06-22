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

    public SearchAgent(int resultCount) {
        this.resultCount = resultCount;
    }

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
     * remove duplicates
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
