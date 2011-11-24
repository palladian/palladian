package ws.palladian.retrieval.search;

/**
 * <p>
 * A search result is a document that resulted from a search query.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public abstract class SearchResult {

    protected int index;
    protected int rank;
    protected String title;
    protected String summary;
    protected String date;

    public SearchResult() {

    }

    public SearchResult(int index, int rank, String title, String summary) {
        super();
        this.index = index;
        this.rank = rank;
        this.title = title;
        this.summary = summary;
    }

    public SearchResult(int index, int rank, String title, String summary, String date) {
        super();
        this.index = index;
        this.rank = rank;
        this.title = title;
        this.summary = summary;
        this.date = date;
    }

    public SearchResult(String title, String summary) {
        this.title = title;
        this.summary = summary;
    }

    public int getIndex() {
        return index;
    }

    public int getRank() {
        return rank;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDate() {
        return date;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        // return index + ":" + rank + ":" + url;
        StringBuilder sb = new StringBuilder();
        sb.append(index);
        sb.append(":").append(rank);
        // sb.append(":").append(title);
        // sb.append(":").append(summary);
        return sb.toString();
    }

}
