package ws.palladian.retrieval.search.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;

import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;
import ws.palladian.retrieval.search.WebSearcherManager;
import ws.palladian.retrieval.search.services.BaseWebSearcher;
import ws.palladian.retrieval.search.services.WebSearcherLanguage;

public class ClueWebSearcher extends BaseWebSearcher implements WebSearcher {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClueWebSearcher.class);

    @Override
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {
        
        List<WebResult> webResults = new ArrayList<WebResult>();
        List<LocalIndexResult> localIndexResults = getLocalIndexResultsFromClueWeb(query, exact);

        for (LocalIndexResult localIndexResult : localIndexResults) {
            WebResult webResult = new WebResult();
            webResult.setIndex(localIndexResult.getIndex());
            webResult.setUrl(localIndexResult.getId());
            webResult.setRank(localIndexResult.getRank());
            webResults.add(webResult);
        }

        return webResults;
    }

    @Override
    public String getName() {
        return "ClueWeb09";
    }
    
    /**
     * <p>
     * Query the ClueWeb09 (English) corpus. The index path must be set in the {@link WebSearcherManager} in order for
     * this to return web results.
     * </p>
     * 
     * @param searchQuery The search query.
     * @return A list of web results.
     */
    public List<LocalIndexResult> getLocalIndexResultsFromClueWeb(String searchQuery, boolean exact) {

        List<LocalIndexResult> indexResults = new ArrayList<LocalIndexResult>();

        QueryProcessor queryProcessor = new QueryProcessor(WebSearcherManager.getInstance().getIndexPath());

        List<ScoredDocument> indexAnswers = new ArrayList<ScoredDocument>();
        try {
            indexAnswers = queryProcessor.queryIndex(searchQuery, getResultCount(), exact);
        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        for (ScoredDocument scoredDocument : indexAnswers) {

            LocalIndexResult indexResult = new LocalIndexResult();
            indexResult.setIndex(WebSearcherManager.CLUEWEB);
            indexResult.setId(scoredDocument.getWarcId());
            indexResult.setRank(scoredDocument.getRank());
            indexResult.setContent(scoredDocument.getContent());

            LOGGER.debug("clueweb retrieved url " + scoredDocument.getWarcId());
            indexResults.add(indexResult);

        }
        return indexResults;
    }

 
}
