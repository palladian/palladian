package ws.palladian.retrieval.search.intents;

public class SearchIntentSort {
    private String key;
    private SortDirection direction;

    public SearchIntentSort(String key, SortDirection direction) {
        this.key = key;
        this.direction = direction;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "SearchIntentSort{" + "key='" + key + '\'' + ", direction=" + direction + '}';
    }
}
