package ws.palladian.retrieval.search.intents;

import ws.palladian.persistence.json.JsonObject;

public interface SearchIntentContextMatcher {
    boolean match(JsonObject searchIntentContext);
}
