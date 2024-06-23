package ws.palladian.retrieval.search.socialmedia;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Searcher for Hacker News via <a href="https://hn.algolia.com/api">HN Search API</a>.
 *
 * @author Philipp Katz
 */
public final class HackerNewsSearcher extends AbstractMultifacetSearcher<WebContent> {

    public static final class HackerNewsSearcherMetaInfo implements SearcherMetaInfo<HackerNewsSearcher, WebContent> {

        @Override
        public String getSearcherName() {
            return NAME;
        }

        @Override
        public String getSearcherId() {
            return "hackernews";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public HackerNewsSearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new HackerNewsSearcher();
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://hn.algolia.com/api";
        }

        @Override
        public String getSearcherDescription() {
            return "Search posts on <a href=\"https://news.ycombinator.com/news\">Hacker News</a> "
                    + "via <a href=\"https://www.algolia.com/\">Algolia Search’s API</a>.";
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HackerNewsSearcher.class);

    private static final String NAME = "Hackernews";

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.HOURS, 10000);

    private static final HttpRetriever HTTP_RETRIEVER = HttpRetrieverFactory.getHttpRetriever();

    /** The pattern for parsing the date. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssX";

    private static final int RESULTS_PER_PAGE = 20;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        String text = query.getText();
        Validate.notEmpty(text, "query#getText must not be empty");

        List<WebContent> resultList = new ArrayList<>();
        Long resultCount = null;

        // TODO implement different sortings
        // TODO ability to specify what to search: comment,poll,pollopt,show_hn,ask_hn

        for (int page = 0; page < Math.ceil((float) query.getResultCount() / RESULTS_PER_PAGE); page++) {

            String queryUrl = String.format("https://hn.algolia.com/api/v1/search_by_date?query=%s&tags=(story)&page=%s", text, page);
            LOGGER.debug("query URL = {}", queryUrl);

            try {
                HttpResult httpResult = HTTP_RETRIEVER.httpGet(queryUrl);
                THROTTLE.hold();
                JsonObject jsonResult = new JsonObject(httpResult.getStringContent());

                JsonArray jsonHits = jsonResult.getJsonArray("hits");
                if (jsonHits.isEmpty()) {
                    break;
                }
                for (int i = 0; i < jsonHits.size(); i++) {
                    JsonObject currentHit = jsonHits.getJsonObject(i);
                    BasicWebContent.Builder resultBuilder = new BasicWebContent.Builder();
                    resultBuilder.setTitle(currentHit.getString("title"));
                    resultBuilder.setUrl(currentHit.tryGetString("url"));
                    resultBuilder.setPublished(parseDate(currentHit.getString("created_at")));
                    resultBuilder.setIdentifier(currentHit.getString("objectID"));
                    resultBuilder.setSource(NAME);
                    var storyText = currentHit.tryGetString("story_text");
                    if (storyText != null && !storyText.isBlank()) {
                        resultBuilder.setSummary(storyText);
                    }
                    resultList.add(resultBuilder.create());
                    if (resultList.size() >= query.getResultCount()) {
                        break;
                    }
                }
                resultCount = jsonResult.getLong("nbHits");
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while querying \"" + queryUrl + "\".", e);
            } catch (JsonException e) {
                throw new SearcherException("Error while parsing JSON result", e);
            }

        }

        return new SearchResults<WebContent>(resultList, resultCount);

    }

    private static Date parseDate(String string) throws SearcherException {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(string);
        } catch (ParseException e) {
            throw new SearcherException("Could not parse date string \"" + string + "\" using pattern \"" + DATE_PATTERN + "\".");
        }
    }

    public static void main(String[] args) throws SearcherException {
        MultifacetQuery query = new MultifacetQuery.Builder().setText("github").setResultCount(50).create();
        SearchResults<WebContent> results = new HackerNewsSearcher().search(query);
        CollectionHelper.print(results);
    }

}
