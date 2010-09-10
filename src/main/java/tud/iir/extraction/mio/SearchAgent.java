/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tud.iir.web.SourceRetriever;

/**
 * The SearchAgent uses given queries to initiate a search at a searchEngine.
 * 
 * @author Martin Werner
 */
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
    public List<String> initiateSearch(final List<String> searchQueries) {

        List<String> mioPageCandidateList = querySearchEngine(searchEngine, searchQueries);

//        mioPageCandidateList.addAll(querySearchEngine(6, searchQueries));

        mioPageCandidateList = removeDuplicates(mioPageCandidateList);
        System.out.println("Anzahl der mioPageCandidates: " + mioPageCandidateList.size());
//        System.exit(1);

        return mioPageCandidateList;
    }

    private List<String> querySearchEngine(int searchEngine, List<String> searchQueries) {
        final SourceRetriever sRetriever = new SourceRetriever();
        sRetriever.setResultCount(resultCount);

        // set focus on english content
        sRetriever.setLanguage(0);
        sRetriever.setSource(searchEngine);

        // List<String> MIOPageCandidateList = null;

        final List<String> resultList = new ArrayList<String>();

        for (String searchQuery : searchQueries) {
            // System.out.println(searchQuery);
            final List<String> resultURLList = sRetriever.getURLs(searchQuery, false);

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
    private List<String> removeDuplicates(final List<String> resultList) {
        final Set<String> hashSet = new HashSet<String>(resultList);
        resultList.clear();
        resultList.addAll(hashSet);

        return resultList;

    }

}
