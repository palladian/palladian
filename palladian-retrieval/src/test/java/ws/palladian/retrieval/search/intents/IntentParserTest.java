package ws.palladian.retrieval.search.intents;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;

public class IntentParserTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testParse() throws JsonException {
        String intentJson = "[{\n" +
                "    \"triggers\": [\n" +
                "      {type: \"REGEX\", text: \"under \\\\$(\\\\d+)\"}" +
                "    ],\n" +
                "    \"action\": {\n" +
                "      \"filters\": [\n" +
                "        {\n" +
                "          \"key\": \"price\",\n" +
                "          \"max\": \"$1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"sorts\": [\n" +
                "        {\n" +
                "          \"key\": \"price\",\n" +
                "          \"direction\": \"ASC\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "  }]";
        SearchIntentParser intentParser = new SearchIntentParser(new JsonArray(intentJson));
        SearchIntentAction<ActivatedSearchIntentFilter> intentAction = intentParser.parse("shoes under $101");

        collector.checkThat(intentAction.getFilters().get(0).getKey(), Matchers.is("price"));
        collector.checkThat(intentAction.getFilters().get(0).getMinDefinition(), Matchers.nullValue());
        collector.checkThat(intentAction.getFilters().get(0).getMaxDefinition(), Matchers.is("$1"));
        collector.checkThat(intentAction.getFilters().get(0).getMax(), Matchers.is(101.0));
        collector.checkThat(intentAction.getSort().getKey(), Matchers.is("price"));
        collector.checkThat(intentAction.getSort().getDirection(), Matchers.is(SortDirection.ASC));
        
        // test redirect
        intentJson = "[{\n" +
                "    \"triggers\": [\n" +
                "      {type: \"MATCH\", text: \"delivery\"}," +
                "      {type: \"PHRASE_MATCH\", text: \"ups\"}," + 
                "    ],\n" +
                "    \"action\": {\n" +
                "      \"redirect\": \"https://delivery.com\"" +
                "    },\n" +
                "  }]";
        intentParser = new SearchIntentParser(new JsonArray(intentJson));
        intentAction = intentParser.parse("what about delivery?");
        collector.checkThat(intentAction, Matchers.nullValue());
        
        intentAction = intentParser.parse("what about ups?");
        collector.checkThat(intentAction.getRedirect(), Matchers.is("https://delivery.com"));

        // test rewrite
        intentJson = "[{\n" +
                "    \"triggers\": [\n" +
                "      {type: \"REGEX\", text: \"gta (\\\\d+)\"}," +
                "    ],\n" +
                "    \"action\": {\n" +
                "      \"rewrite\": \"grand theft auto $1\"" +
                "    },\n" +
                "  }]";
        intentParser = new SearchIntentParser(new JsonArray(intentJson));
        intentAction = intentParser.parse("ps4 gta 6");
        collector.checkThat(intentAction.getRewrite(), Matchers.is("ps4 grand theft auto 6"));

        // test regex redirects
        intentJson = "[{\n" +
                "    \"triggers\": [\n" +
                "      {type: \"REGEX\", text: \"ticket ([a-z]\\\\d+)\"}," +
                "    ],\n" +
                "    \"action\": {\n" +
                "      \"redirect\": \"https://helpcenter.com/tickets/$1\"" +
                "    },\n" +
                "  }]";
        intentParser = new SearchIntentParser(new JsonArray(intentJson));
        intentAction = intentParser.parse("need help with ticket C8788 fast please!!!");
        collector.checkThat(intentAction.getRedirect(), Matchers.is("https://helpcenter.com/tickets/C8788"));
    }
}