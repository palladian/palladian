package ws.palladian.retrieval.search.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;

import ws.palladian.retrieval.search.Searcher;

/**
 * <p>
 * Allows to query the ClueWeb09 (English) corpus. The index path must be supplied via constructor in order for this to
 * return results.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ClueWebSearcher implements Searcher<LocalIndexResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClueWebSearcher.class);

    private final String indexPath;

    public ClueWebSearcher(String indexPath) {
        super();
        this.indexPath = indexPath;
    }

    @Override
    public List<LocalIndexResult> search(String query, int resultCount) {
        List<LocalIndexResult> indexResults = new ArrayList<LocalIndexResult>();

        QueryProcessor queryProcessor = new QueryProcessor(indexPath);

        List<ScoredDocument> indexAnswers = new ArrayList<ScoredDocument>();
        try {
            indexAnswers = queryProcessor.queryIndex(query, resultCount, false);
        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        for (ScoredDocument scoredDocument : indexAnswers) {
            String warcId = scoredDocument.getWarcId();
            String content = scoredDocument.getContent();
            LocalIndexResult indexResult = new LocalIndexResult(warcId, content);
            LOGGER.debug("clueweb retrieved id " + warcId);
            indexResults.add(indexResult);
        }
        return indexResults;
    }

    @Override
    public String getName() {
        return "ClueWeb09";
    }

    @Override
    public int getTotalResultCount(String query) {
        throw new UnsupportedOperationException();
    }

}
