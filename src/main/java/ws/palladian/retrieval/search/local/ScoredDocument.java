package ws.palladian.retrieval.search.local;

import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * A scored document that is returned from a Lucene index after querying the index with the {@link QueryProcessor} for
 * example.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class ScoredDocument {

    private int rank = -1;
    private double score = -1.0;
    private String warcId = "";
    private String content = "";

    public ScoredDocument(int rank, double score, String warcId, String content) {
        super();
        this.rank = rank;
        this.score = score;
        this.warcId = warcId;
        this.content = content;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getWarcId() {
        return warcId;
    }

    public void setWarcId(String warcId) {
        this.warcId = warcId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ScoredDocument [rank=" + rank + ", score=" + score + ", warcId=" + warcId + ", content abstract="
                + StringHelper.clean(content.substring(0, Math.min(content.length(), 300))) + "]";
    }

}