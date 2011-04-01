package ws.palladian.retrieval.search;


/**
 * The knowledge unit web result.
 * 
 * WebResults are retrieved by the SourceRetriever and represent web search results.
 * 
 * @author Christopher Friedrich
 * @author Philipp Katz
 */
public class WebResult {

    private int index;
    private int rank;
    private String title;
    private String summary;
    private String url;
    private String date;

    public WebResult() {
        
    }
    public WebResult(int index, int rank, String url, String title, String summary) {
        super();
        this.index = index;
        this.rank = rank;
        this.url = url;
        this.title = title;
        this.summary = summary;
    }
    public WebResult(int index, int rank, String url, String title, String summary, String date) {
        super();
        this.index = index;
        this.rank = rank;
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.date = date;
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

    public String getUrl() {
        return url;
    }

    public String getDate(){
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
    public void setUrl(String url) {
        this.url = url;
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
        sb.append(":").append(getUrl());
        // sb.append(":").append(title);
        // sb.append(":").append(summary);
        return sb.toString();
    }

}