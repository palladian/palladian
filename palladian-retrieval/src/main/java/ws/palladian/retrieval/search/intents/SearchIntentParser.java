package ws.palladian.retrieval.search.intents;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.normalization.UnitNormalizer;
import ws.palladian.helper.normalization.UnitTranslator;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A generic intent parser. For example, query = "under 100€" + intent is "under \d+€" => action sort price < 100€.
 * Input: An intent file + query.
 * Output: A filled intent action.
 * <p>
 * Sample Json Array:
 * [
 * {
 * "triggers": [
 * {
 * "type": "CONTAINS",
 * "text": "cheap"
 * }
 * ],
 * "context": {
 * "categories": ["Notebook"],
 * "userGender": "female",
 * "whatever": 123.332
 * },
 * "action": {
 * "type": "DEFINITION",
 * "filters": [
 * {
 * "key": "cost.PRICE",
 * "min": 0,
 * "max": 233
 * }
 * ],
 * "sorts": [
 * {
 * "key": "cost.PRICE",
 * "direction": "ASC"
 * }
 * ],
 * "explanation": {
 * "en": "You want the cheap stuff you penny pincher"
 * },
 * "metaData": {
 * "x": "y"
 * }
 * }
 * }
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

            JsonObject explanation = Optional.ofNullable(actionObj.tryGetJsonObject("explanation")).orElse(new JsonObject());
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
    public List<ActivatedSearchIntentAction> parse(String query) {
        return parse(query, null);
    }

    public List<ActivatedSearchIntentAction> parse(String query, SearchIntentContextMatcher contextMatcher) {
        List<ActivatedSearchIntentAction> intentActions = new ArrayList<>();

        // if something goes wrong (e.g. circular redirects), we break out of the cycle
        int numTries = 0;
        boolean intentMatchFound = false;
        ol:
        do {
            // XXX this could be slightly faster if we index actions by their match type so we don't have to iterate through all intents all the time
            for (SearchIntent intent : intents) {
                if (contextMatcher != null && !contextMatcher.match(intent.getContext())) {
                    continue;
                }
                for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                    if (intentTrigger.getMatchType() == QueryMatchType.MATCH && intentTrigger.getText().equals(query)) {
                        intentMatchFound = true;
                        ActivatedSearchIntentAction im = processMatch(QueryMatchType.MATCH, intent, query, null, intentTrigger);
                        intentActions.add(im);
                        query = im.getModifiedQuery();
                        if (im.getRedirect() != null) {
                            return intentActions;
                        }
                        continue ol;
                    }
                }
            }

            for (SearchIntent intent : intents) {
                if (contextMatcher != null && !contextMatcher.match(intent.getContext())) {
                    continue;
                }
                for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                    if (intentTrigger.getMatchType() == QueryMatchType.PHRASE_MATCH && StringHelper.containsWordCaseSensitive(intentTrigger.getText(), query)) {
                        intentMatchFound = true;
                        ActivatedSearchIntentAction im = processMatch(QueryMatchType.PHRASE_MATCH, intent, query, null, intentTrigger);
                        intentActions.add(im);
                        query = im.getModifiedQuery();
                        if (im.getRedirect() != null) {
                            return intentActions;
                        }
                        continue ol;
                    }
                }
            }

            for (SearchIntent intent : intents) {
                if (contextMatcher != null && !contextMatcher.match(intent.getContext())) {
                    continue;
                }
                for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                    if (intentTrigger.getMatchType() == QueryMatchType.CONTAINS && query.contains(intentTrigger.getText())) {
                        intentMatchFound = true;
                        ActivatedSearchIntentAction im = processMatch(QueryMatchType.CONTAINS, intent, query, null, intentTrigger);
                        intentActions.add(im);
                        query = im.getModifiedQuery();
                        if (im.getRedirect() != null) {
                            return intentActions;
                        }
                        continue ol;
                    }
                }
            }

            for (SearchIntent intent : intents) {
                if (contextMatcher != null && !contextMatcher.match(intent.getContext())) {
                    continue;
                }
                for (SearchIntentTrigger intentTrigger : intent.getIntentTriggers()) {
                    String regex = intentTrigger.getText();

                    // for URL replacements we want to replace the entire query, not just the matching part
                    if (intent.getIntentAction().getRedirect() != null) {
                        regex = ".*" + regex + ".*";
                    }
                    try {
                        Matcher matcher = PatternHelper.compileOrGet(regex, Pattern.CASE_INSENSITIVE).matcher(query);
                        if (intentTrigger.getMatchType() == QueryMatchType.REGEX && matcher.find()) {
                            intentMatchFound = true;
                            ActivatedSearchIntentAction im = processMatch(QueryMatchType.REGEX, intent, query, matcher, intentTrigger);
                            intentActions.add(im);
                            query = im.getModifiedQuery();
                            if (im.getRedirect() != null) {
                                return intentActions;
                            }
                            continue ol;
                        }
                    } catch (PatternSyntaxException e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }
            intentMatchFound = false;
        } while (intentMatchFound && numTries++ < 10);

        return intentActions;
    }

    private ActivatedSearchIntentAction processMatch(QueryMatchType qmt, SearchIntent intent, String query, Matcher matcher, SearchIntentTrigger intentTrigger) {
        ActivatedSearchIntentAction intentAction = new ActivatedSearchIntentAction(intent.getIntentAction(), query);

        switch (intentAction.getType()) {
            case REWRITE:
                String rewrite = intent.getIntentAction().getRewrite();
                if (qmt == QueryMatchType.REGEX) {
                    rewrite = matcher.replaceAll(intentAction.getRewrite()).toLowerCase();
                } else {
                    rewrite = query.replace(intentTrigger.getText(), intentAction.getRewrite());
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
                    if (qmt.equals(QueryMatchType.REGEX)) {
                        Collection<String> values = filledFilter.getValues();
                        List<String> replacedValues = new ArrayList<>();
                        for (String value : values) {
                            if (value.contains("$")) {
                                int position = MathHelper.parseStringNumber(value).intValue();
                                String group = matcher.group(position);

                                // is the group match a number? could also be text such as "XXL"
                                if (StringHelper.isNumber(group)) {
                                    Double aDouble = Double.valueOf(group);
                                    Double margin = filledFilter.getMargin();
                                    String unit = filledFilter.getUnit();
                                    if (margin == null) {
                                        margin = 0.05;
                                    }
                                    if (unit != null && unit.contains("$")) {
                                        int unitPosition = Integer.parseInt(unit.replace("$", ""));
                                        String unitGroup = matcher.group(unitPosition);
                                        if (unitGroup != null) {
                                            String translatedUnit = UnitTranslator.translate(unitGroup, intentTrigger.getLanguage());
                                            aDouble = UnitNormalizer.getNormalizedNumber(aDouble, translatedUnit);
                                        }
                                    }
                                    Double min = aDouble - (aDouble * margin);
                                    Double max = aDouble + (aDouble * margin);
                                    filledFilter.setMin(min);
                                    filledFilter.setMax(max);
                                } else {
                                    replacedValues.add(value.replace("$" + position, group));
                                }
                            } else {
                                replacedValues.add(value);
                            }
                        }
                        filledFilter.setValues(replacedValues);
                    }
                }
                intentAction.setFilters(filledFilters);
                if (intentAction.isRemoveTrigger()) {
                    String replace = "[^ ]*" + intentTrigger.getText() + "[^ ]*";
                    if (matcher != null) {
                        replace = "[^ ]*" + Pattern.quote(matcher.group()) + "[^ ]*";
                    }
                    intentAction.setModifiedQuery(query.replaceAll(replace, ""));
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
