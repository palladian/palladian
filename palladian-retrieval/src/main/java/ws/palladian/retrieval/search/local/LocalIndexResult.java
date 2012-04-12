package ws.palladian.retrieval.search.local;

import java.util.Date;

import ws.palladian.retrieval.search.SearchResult;

/**
 * <p>
 * LocalIndexResult are retrieved by the WebSearcher and represent local search results from a Lucene index.
 * </p>
 * 
 * @author David Urbansky
 */
public class LocalIndexResult extends SearchResult {

    /** The id of the index result in the index. For example the WARC-TREC-ID in the ClueWeb corpus. */
    private final String id;

    /** The full document content of the result. */
    private final String content;

    public LocalIndexResult(String id, String title, String content, String summary, Date date) {
        super(title, summary, date);
        this.id = id;
        this.content = content;
    }

    public LocalIndexResult(String id, String content) {
        this(id, null, content, null, null);
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

}