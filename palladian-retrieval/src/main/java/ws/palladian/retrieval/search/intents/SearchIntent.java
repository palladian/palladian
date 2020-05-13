package ws.palladian.retrieval.search.intents;

import java.util.ArrayList;
import java.util.List;

public class SearchIntent {
    protected List<SearchIntentTrigger> intentTriggers = new ArrayList<>();
    protected SearchIntentAction<SearchIntentFilter> intentAction;

    public List<SearchIntentTrigger> getIntentTriggers() {
        return intentTriggers;
    }

    public void setIntentTriggers(List<SearchIntentTrigger> intentTriggers) {
        this.intentTriggers = intentTriggers;
    }

    public void addIntentTrigger(SearchIntentTrigger intentTrigger) {
        this.intentTriggers.add(intentTrigger);
    }

    public SearchIntentAction<SearchIntentFilter> getIntentAction() {
        return intentAction;
    }

    public void setIntentAction(SearchIntentAction<SearchIntentFilter> intentAction) {
        this.intentAction = intentAction;
    }
}
