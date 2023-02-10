package ws.palladian.retrieval.search.intents;

import ws.palladian.retrieval.parser.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SearchIntent {
    private String id;
    protected List<SearchIntentTrigger> triggers = new ArrayList<>();
    protected JsonObject context = new JsonObject();
    protected SearchIntentAction<SearchIntentFilter> action;

    public SearchIntent() {
        this.id = UUID.randomUUID().toString();
    }
    public List<SearchIntentTrigger> getIntentTriggers() {
        return triggers;
    }

    public void setIntentTriggers(List<SearchIntentTrigger> intentTriggers) {
        this.triggers = intentTriggers;
        sortTriggers();
    }

    public void addIntentTrigger(SearchIntentTrigger intentTrigger) {
        this.triggers.add(intentTrigger);
        sortTriggers();
    }

    private void sortTriggers() {
        triggers.sort((o1, o2) -> Integer.compare(o2.getText().length(), o1.getText().length()));
    }

    public SearchIntentAction<SearchIntentFilter> getIntentAction() {
        return action;
    }

    public void setIntentAction(SearchIntentAction<SearchIntentFilter> intentAction) {
        this.action = intentAction;
    }

    public JsonObject getContext() {
        return context;
    }

    public void setContext(JsonObject context) {
        this.context = context;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "SearchIntent{" + "triggers=" + triggers + ", context=" + context + ", action=" + action + '}';
    }
}
