package ws.palladian.retrieval.search.intents;

import java.util.ArrayList;
import java.util.List;

public class SearchIntent {
    protected List<SearchIntentTrigger> triggers = new ArrayList<>();
    protected SearchIntentAction<SearchIntentFilter> action;

    public List<SearchIntentTrigger> getIntentTriggers() {
        return triggers;
    }

    public void setIntentTriggers(List<SearchIntentTrigger> intentTriggers) {
        this.triggers = intentTriggers;
    }

    public void addIntentTrigger(SearchIntentTrigger intentTrigger) {
        this.triggers.add(intentTrigger);
    }

    public SearchIntentAction<SearchIntentFilter> getIntentAction() {
        return action;
    }

    public void setIntentAction(SearchIntentAction<SearchIntentFilter> intentAction) {
        this.action = intentAction;
    }
}
