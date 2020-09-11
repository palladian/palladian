package ws.palladian.retrieval.search.intents;

import ws.palladian.retrieval.parser.json.JsonObject;

public interface SearchIntentContextMatcher {
    boolean match(JsonObject searchIntentContext);
}
