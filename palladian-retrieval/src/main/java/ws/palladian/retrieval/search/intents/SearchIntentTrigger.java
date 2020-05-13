package ws.palladian.retrieval.search.intents;

public class SearchIntentTrigger {
    private QueryMatchType matchType;
    private String text;

    public SearchIntentTrigger(QueryMatchType queryMatchType, String text) {
        this.matchType = queryMatchType;
        this.text = text;
    }

    public QueryMatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(QueryMatchType matchType) {
        this.matchType = matchType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
