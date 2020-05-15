package ws.palladian.retrieval.search.intents;

import java.util.ArrayList;
import java.util.List;

public class SearchIntentAction<T> {
    private List<T> filters = new ArrayList<>();
    private SearchIntentSort sort;

    /** What type of action to take. */
    private SearchIntentActionType type = null;

    /** If true, the trigger phrase will be removed (not for redirect or rewrite matches). */
    private boolean removeTrigger = true;

    /** If the trigger leads to a redirect. */
    private String redirect = null;

    /** If the trigger leads to a rewrite. */
    private String rewrite = null;

    /** A list of queries that should be executed and appended to the search result. */
    private List<String> appendQueries = new ArrayList<>();

    /** A list of queries that should be mixed into the search result. */
    private List<String> mixInQueries = new ArrayList<>();

//    public SearchIntentAction(SearchIntentActionType type) {
//        this.type = type;
//    }
//
    public List<T> getFilters() {
        return filters;
    }

    public void setFilters(List<T> filters) {
        this.filters = filters;
    }

    public SearchIntentSort getSort() {
        return sort;
    }

    public void setSort(SearchIntentSort sort) {
        this.sort = sort;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getRewrite() {
        return rewrite;
    }

    public void setRewrite(String rewrite) {
        this.rewrite = rewrite;
    }

    public List<String> getAppendQueries() {
        return appendQueries;
    }

    public void setAppendQueries(List<String> appendQueries) {
        this.appendQueries = appendQueries;
    }

    public List<String> getMixInQueries() {
        return mixInQueries;
    }

    public void setMixInQueries(List<String> mixInQueries) {
        this.mixInQueries = mixInQueries;
    }

    public void addFilter(T intentFilter) {
        this.filters.add(intentFilter);
    }

    public boolean isRemoveTrigger() {
        return removeTrigger;
    }

    public void setRemoveTrigger(boolean removeTrigger) {
        this.removeTrigger = removeTrigger;
    }

    public SearchIntentActionType getType() {
        return type;
    }

    public void setType(SearchIntentActionType type) {
        this.type = type;
    }
}
