package ws.palladian.retrieval.search.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;

import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.services.WebSearcherLanguage;

public final class ClueWebSearcher implements Searcher<LocalIndexResult> {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClueWebSearcher.class);
    
    private final String indexPath;
    
    public ClueWebSearcher(String indexPath) {
        super();
        this.indexPath = indexPath;
    }

    @Override
    public List<LocalIndexResult> search(String query) {
        return getLocalIndexResultsFromClueWeb(query);
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
    public List<LocalIndexResult> getLocalIndexResultsFromClueWeb(String searchQuery) {

        List<LocalIndexResult> indexResults = new ArrayList<LocalIndexResult>();

        QueryProcessor queryProcessor = new QueryProcessor(indexPath);

        List<ScoredDocument> indexAnswers = new ArrayList<ScoredDocument>();
        try {
            // FIXME remove exact parameter?
            indexAnswers = queryProcessor.queryIndex(searchQuery, getResultCount(), false);
        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        for (ScoredDocument scoredDocument : indexAnswers) {

            String warcId = scoredDocument.getWarcId();
            String content = scoredDocument.getContent();
            
            LocalIndexResult indexResult = new LocalIndexResult(warcId, content);
            
//            indexResult.setIndex(WebSearcherManager.CLUEWEB);
//            indexResult.setId(warcId);
//            indexResult.setRank(scoredDocument.getRank());
//            indexResult.setContent(content);

            LOGGER.debug("clueweb retrieved url " + warcId);
            indexResults.add(indexResult);

        }
        return indexResults;
    }

    @Override
    public List<String> searchUrls(String query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTotalResultCount(String query) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLanguage(WebSearcherLanguage language) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public WebSearcherLanguage getLanguage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setResultCount(int resultCount) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getResultCount() {
        // TODO Auto-generated method stub
        return 0;
    }

 
}
