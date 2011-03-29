package ws.palladian.retrieval.search;

import ws.palladian.retrieval.resources.Source;

/**
 * The knowledge unit web result.
 * 
 * WebResults are retrieved by the SourceRetriever and represent web search results.
 * 
 * @author Christopher Friedrich
 */
public class WebResult {

    private int index;
    private int rank;
    private String title;
    private String summary;
    private Source source;
    private String date;

    public WebResult(int index, int rank, Source source, String title, String summary) {
        super();
        this.index = index;
        this.rank = rank;
        this.source = source;
        this.title = title;
        this.summary = summary;
    }
    public WebResult(int index, int rank, Source source, String title, String summary, String date) {
        super();
        this.index = index;
        this.rank = rank;
        this.source = source;
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

    public Source getSource() {
        return source;
    }

    public String getUrl() {
        return source.getUrl();
    }

    public String getDate(){
    	return date;
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