package ws.palladian.retrieval.search;

/**
 * <p>
 * LocalIndexResult are retrieved by the WebSearcher and represent local search results from a Lucene index.
 * </p>
 * 
 * @author David Urbansky
 */
public class LocalIndexResult extends SearchResult {

    /** The id of the index result in the index. For example the WARC-TREC-ID in the ClueWeb corpus. */
    private String id = "";

    /** The full document content of the result. */
    private String content = "";

    public LocalIndexResult() {
        super();
    }

    public LocalIndexResult(int index, int rank, String id, String title, String summary, String content) {
        super(index, rank, title, summary);
        this.id = id;
        this.content = content;
    }

    public LocalIndexResult(int index, int rank, String id, String title, String summary, String content, String date) {
        super(index, rank, title, summary, date);
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}