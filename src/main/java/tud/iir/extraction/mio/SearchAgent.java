/**
 * The SearchAgent uses given queries to initiate a search at a searchEngine.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.web.SourceRetriever;

public class SearchAgent {

    /** The resultCount determines how many sources (URLs) should be retrieved. */
    private final transient int resultCount;

    /** The search engine. */
    private final transient int searchEngine;

    /**
     * Instantiates a new search agent.
     */
    public SearchAgent() {
        this.searchEngine = InCoFiConfiguration.getInstance().searchEngine;
        this.resultCount = InCoFiConfiguration.getInstance().resultCount;
    }

    /**
     * Initiate search.
     * 
     * @param searchQueries the search queries
     * @return the list
     */
    public List<String> initiateSearch(List<String> searchQueries) {

        List<String> mioPageCandidateList = querySearchEngine(searchEngine, searchQueries);
        mioPageCandidateList = removeDuplicates(mioPageCandidateList);

        return mioPageCandidateList;
    }

    /**
     * Query search engine.
     * 
     * @param searchEngine the search engine
     * @param searchQueries the search queries
     * @return the list
     */
    private List<String> querySearchEngine(int searchEngine, List<String> searchQueries) {
        SourceRetriever sRetriever = new SourceRetriever();
        sRetriever.setResultCount(resultCount);

        // set focus on english content
        sRetriever.setLanguage(SourceRetriever.LANGUAGE_ENGLISH);
        sRetriever.setSource(searchEngine);

        Logger.getRootLogger().info(
                "MIO SearchAgent query search engine " + searchEngine + " with " + searchQueries.size()
                + " queries expecting " + resultCount + " answers for each query");

        List<String> resultList = new ArrayList<String>();

        for (String searchQuery : searchQueries) {
            List<String> resultURLList = sRetriever.getURLs(searchQuery, false);
            resultList.addAll(resultURLList);
        }

        return resultList;
    }

    /**
     * remove duplicates.
     * 
     * @param resultList the result list
     * @return the array list
     */
    private List<String> removeDuplicates(List<String> resultList) {
        Set<String> hashSet = new HashSet<String>(resultList);
        resultList.clear();
        resultList.addAll(hashSet);

        return resultList;
    }
}
