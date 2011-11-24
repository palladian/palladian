package ws.palladian.retrieval.search;

/**
 * <p>
 * WebResults are retrieved by the WebSearcher and represent web search results.
 * </p>
 * 
 * @author David Urbansky
 */
public class WebResult extends SearchResult {

    /** The URL of the web result. */
    private String url = "";

    public WebResult() {
        super();
    }
    
    public WebResult(String url, String title, String summary) {
        super(title, summary);
        this.url = url;
    }
    
    public WebResult(String url, String title, String summary, String date) {
        super(title, summary);
        this.url = url;
        this.date = date;
    }

    public WebResult(int index, int rank, String url, String title, String summary) {
        super(index, rank, title, summary);
        this.url = url;
    }

    public WebResult(int index, int rank, String url, String title, String summary, String date) {
        super(index, rank, title, summary, date);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebResult [url=");
        builder.append(url);
        builder.append(", title=");
        builder.append(title);
        builder.append(", summary=");
        builder.append(summary);
        builder.append("]");
        return builder.toString();
    }
    
    
    
}