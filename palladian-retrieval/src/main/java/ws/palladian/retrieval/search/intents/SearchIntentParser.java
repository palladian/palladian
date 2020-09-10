package ws.palladian.retrieval.search.intents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * A generic intent parser. For example, query = "under 100€" + intent is "under \d+€" => action sort price < 100€.
 * Input: An intent file + query.
 * Output: A filled intent action.
 *
 * Sample Json Array:
 * [
 *  {
 *      "triggers": [
 *          {
 *              "type": "CONTAINS",
 *              "text": "cheap"
 *          }
 *      ],
 *      "context": {
 *          "categories": ["Notebook"],
 *          "userGender": "female",
 *          "whatever": 123.332
 *      },
 *      "action": {
 *          "type": "DEFINITION",
 *          "filters": [
 *              {
 *                  "key": "cost.PRICE",
 *                  "min": 0,
 *                  "max": 233
 *              }
 *          ],
 *          "sorts": [
 *              {
 *                  "key": "cost.PRICE",
 *                  "direction": "ASC"
 *              }
 *          ],
 *          "explanation": {
 *              "en": "You want the cheap stuff you penny pincher"
 *          },
 *          "metaData": {
 *              "x": "y"
 *          }
 *      }
 *  }
 * ]
 */
public class SearchIntentParser {
    List<SearchIntent> intents = new ArrayList<>();

    public SearchIntentParser(List<? extends SearchIntent> intents) {
        this.intents.addAll(intents);
    }

    public SearchIntentParser(File intentActionFile) throws JsonException {
        this(new JsonArray(FileHelper.tryReadFileToString(intentActionFile)));
    }

    public SearchIntentParser(JsonArray intentActionJson) {
        parseIntentFile(intentActionJson);
    }

    private void parseIntentFile(JsonArray intentActionsJson) {
        for (int i = 0; i < intentActionsJson.size(); i++) {
            SearchIntent intent = new SearchIntent();
            JsonObject jso = intentActionsJson.tryGetJsonObject(i);

            // parse triggers
            JsonArray triggers = jso.tryGetJsonArray("triggers");
            for (int j = 0; j < triggers.size(); j++) {
                JsonObject triggerObj = triggers.tryGetJsonObject(j);
                QueryMatchType queryMatchType = QueryMatchType.valueOf(triggerObj.tryGetString("type"));
                SearchIntentTrigger intentTrigger = new SearchIntentTrigger(queryMatchType, triggerObj.tryGetString("text"));
                intent.addIntentTrigger(intentTrigger);
            }

            // parse action
            SearchIntentAction<SearchIntentFilter> intentAction = new SearchIntentAction<>();
            JsonObject actionObj = jso.tryGetJsonObject("action");
            intentAction.setType(SearchIntentActionType.valueOf(jso.tryQueryString("action/type")));
            JsonArray filters = Optional.ofNullable(actionObj.tryGetJsonArray("filters")).orElse(new JsonArray());
            for (int j = 0; j < filters.size(); j++) {
                JsonObject filterObj = filters.tryGetJsonObject(j);
                SearchIntentFilter intentFilter = new SearchIntentFilter();
                intentFilter.setKey(filterObj.tryGetString("key"));
                String min = filterObj.tryGetString("min");
                intentFilter.setMinDefinition(min);
                String max = filterObj.tryGetString("max");
                intentFilter.setMaxDefinition(max);

                JsonArray values = filterObj.tryGetJsonArray("values");
                if (values != null) {
                    intentFilter.setValues(new ArrayList(values));
                }
                intentAction.addFilter(intentFilter);
            }
            JsonArray sorts = Optional.ofNullable(actionObj.tryGetJsonArray("sorts")).orElse(new JsonArray());
            for (int j = 0; j < sorts.size(); j++) {
                JsonObject sortObj = sorts.tryGetJsonObject(j);
                SearchIntentSort intentSort = new SearchIntentSort(sortObj.tryGetString("key"), SortDirection.valueOf(sortObj.tryGetString("direction")));
                intentAction.setSort(intentSort);
            }

            JsonObject explanation = actionObj.tryGetJsonObject("explanation");
            for (String languageCode : explanation.keySet()) {
                intentAction.getExplanation().put(Language.getByIso6391(languageCode), explanation.tryGetString(languageCode));
            }

            JsonObject metaData = actionObj.tryGetJsonObject("metaData");
            intentAction.setMetaData(metaData);

            // parse queries and redirects
            String redirect = actionObj.tryGetString("redirect");
            if (redirect != null) {
                intentAction.setRedirect(redirect);
            }
            String rewrite = actionObj.tryGetString("rewrite");
            if (rewrite != null) {
                intentAction.setRewrite(rewrite);
            }

            intent.setIntentAction(intentAction);

            this.intents.add(intent);
        }
    }

    // TODO option to rewrite only matching part or entire query
    public ActivatedSearchIntentAction parse(String query) {
        return parse(query, null);
    }
    public ActivatedSearchIntentAction parse(String query, JsonObject context) {
        // XXX this could be slightly faster if we index actions by their match type so we don't have to iterate through all intents all the time
        for (SearchIntent intent : intents) {
            if (!contextMatch(intent.getContext(), context)) {
                continue;
            }
            for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                if (intentTrigger.getMatchType() == QueryMatchType.MATCH && intentTrigger.getText().equals(query)) {
                    return processMatch(QueryMatchType.MATCH, intent, query, null, intentTrigger);
                }
            }
        }

        for (SearchIntent intent : intents) {
            if (!contextMatch(intent.getContext(), context)) {
                continue;
            }
            for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                if (intentTrigger.getMatchType() == QueryMatchType.PHRASE_MATCH && StringHelper.containsWordCaseSensitive(intentTrigger.getText(), query)) {
                    return processMatch(QueryMatchType.PHRASE_MATCH, intent, query, null, intentTrigger);
                }
            }
        }

        for (SearchIntent intent : intents) {
            if (!contextMatch(intent.getContext(), context)) {
                continue;
            }
            for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                if (intentTrigger.getMatchType() == QueryMatchType.CONTAINS && query.contains(intentTrigger.getText())) {
                    return processMatch(QueryMatchType.CONTAINS, intent, query, null, intentTrigger);
                }
            }
        }

        for (SearchIntent intent : intents) {
            if (!contextMatch(intent.getContext(), context)) {
                continue;
            }
            for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                String regex = intentTrigger.getText();

                // for URL replacements we want to replace the entire query, not just the matching part
                if (intent.getIntentAction().getRedirect() != null) {
                    regex = ".*" + regex + ".*";
                }
                Matcher matcher = PatternHelper.compileOrGet(regex, Pattern.CASE_INSENSITIVE).matcher(query);
                if (intentTrigger.getMatchType() == QueryMatchType.REGEX && matcher.find()) {
                    return processMatch(QueryMatchType.REGEX, intent, query, matcher, intentTrigger);
                }
            }
        }

        return null;
    }

    private boolean contextMatch(JsonObject intentContext, JsonObject queryContext) {
        if (intentContext.isEmpty() || queryContext == null) {
            return true;
        }
        // if a key exists in both intent and query context:
        // if key is string the intent and query context values must match
        // if key is numeric the intent and query context values must match
        // if key is array the intent context must contain the query context
        for (Map.Entry<String, Object> queryContextMap : queryContext.entrySet()) {
            Object intentContextValue = intentContext.get(queryContextMap.getKey());
            if (intentContextValue == null) {
                continue;
            }
            queryContextMap.getValue().getClass().equals(intentContextValue.getClass());
        }

        return true;
    }

    private ActivatedSearchIntentAction processMatch(QueryMatchType qmt, SearchIntent intent, String query, Matcher matcher, SearchIntentTrigger intentTrigger) {
        ActivatedSearchIntentAction intentAction = new ActivatedSearchIntentAction(intent.getIntentAction(), query);

        switch (intentAction.getType()) {
            case REWRITE:
                String rewrite = intent.getIntentAction().getRewrite();
                if (qmt == QueryMatchType.REGEX) {
                    rewrite = matcher.replaceAll(intentAction.getRewrite()).toLowerCase();
                }
                intentAction.setRewrite(rewrite);
                intentAction.setModifiedQuery(rewrite);
                return intentAction;
            case REDIRECT:
                String redirect = intentAction.getRedirect();
                if (qmt == QueryMatchType.REGEX) {
                    redirect = matcher.replaceAll(intentAction.getRedirect());
                }
                intentAction.setRedirect(redirect);
                return intentAction;
            case DEFINITION:
            default:
                List<ActivatedSearchIntentFilter> filledFilters = intentAction.getFilters();

                for (ActivatedSearchIntentFilter filledFilter : filledFilters) {
                    String minDefinition = filledFilter.getMinDefinition();
                    if (minDefinition != null) {
                        if (minDefinition.contains("$")) {
                            int position = Integer.parseInt(minDefinition.replace("$", ""));
                            filledFilter.setMin(Double.valueOf(matcher.group(position)));
                        } else if (!minDefinition.isEmpty()) {
                            try {
                                filledFilter.setMin(Double.valueOf(minDefinition));
                            } catch (Exception e) {
                            }
                        }
                    }
                    String maxDefinition = filledFilter.getMaxDefinition();
                    if (maxDefinition != null) {
                        if (maxDefinition.contains("$")) {
                            int position = Integer.parseInt(maxDefinition.replace("$", ""));
                            filledFilter.setMax(Double.valueOf(matcher.group(position)));
                        } else if (!maxDefinition.isEmpty()) {
                            try {
                                filledFilter.setMax(Double.valueOf(maxDefinition));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                intentAction.setFilters(filledFilters);
                if (intentAction.isRemoveTrigger()) {
                    intentAction.setModifiedQuery(query.replaceAll("[^ ]*" + intentTrigger.getText() + "[^ ]*", ""));
                }
                return intentAction;
        }
    }

    @Override
    public String toString() {
        return "SearchIntentParser{" +
                "intents=" + intents +
                '}';
    }
}
