package ws.palladian.retrieval.search.intents;

import ws.palladian.helper.constants.Language;

public class SearchIntentTrigger {
    private QueryMatchType matchType;
    private String text;
    private Language language;

    public SearchIntentTrigger(QueryMatchType queryMatchType, String text) {
        this.matchType = queryMatchType;
        this.text = text;
        this.language = Language.ENGLISH;
    }

    public SearchIntentTrigger(QueryMatchType queryMatchType, String text, Language language) {
        this.matchType = queryMatchType;
        this.text = text;
        this.language = language;
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

    public Language getLanguage() {return language;}

    public void setLanguage(Language language) {this.language = language;}
}
