package ws.palladian.retrieval.search.socialmedia;

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
import ws.palladian.retrieval.search.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Searcher for <a href="https://www.reddit.com">reddit.com</a>.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="https://www.reddit.com/dev/api">reddit.com API documentation</a>
 * @see <a href="https://github.com/reddit/reddit/wiki/API">GitHub: reddit API</a>
 */
public final class RedditSearcher extends AbstractMultifacetSearcher<WebContent> {

    public static final class RedditSearcherMetaInfo implements SearcherMetaInfo<RedditSearcher, WebContent> {

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "reddit";
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
        public RedditSearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new RedditSearcher();
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://www.reddit.com/dev/api";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for content on <a href=\"https://www.reddit.com\">Reddit</a>.";
        }
    }

    public static enum Sorting implements Facet {

        RELEVANCE, NEW, HOT, TOP, COMMENTS;

        private static final String IDENTIFIER = "reddit.sorting";

        @Override
        public String getIdentifier() {
            return IDENTIFIER;
        }

        public String getValue() {
            return toString().toLowerCase();
        }

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RedditSearcher.class);

    /* The name of this searcher. */
    private static final String SEARCHER_NAME = "Reddit";

    /** Reddit API allows 30 requests/minute. */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 28);

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        List<WebContent> result = new ArrayList<>();
        String pagingAfter = null;
        int limit = Math.min(100, query.getResultCount());

        paging:
        for (; ; ) {
            String queryUrl = makeQueryUrl(query, pagingAfter, limit);
            LOGGER.debug("Retrieve from {}", queryUrl);
            THROTTLE.hold();

            String stringResult;
            try {
                HttpResult httpResult = retriever.httpGet(queryUrl);
                stringResult = httpResult.getStringContent();
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while accessing '" + queryUrl + "': " + e, e);
            }

            try {
                JsonObject jsonData = new JsonObject(stringResult).getJsonObject("data");
                JsonArray jsonChildren = jsonData.getJsonArray("children");
                for (int i = 0; i < jsonChildren.size(); i++) {
                    JsonObject jsonChild = jsonChildren.getJsonObject(i);
                    JsonObject jsonChildData = jsonChild.getJsonObject("data");
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    builder.setTitle(jsonChildData.getString("title"));
                    builder.setSummary(jsonChildData.getString("selftext"));
                    var url = jsonChildData.getString("url");
                    if (!url.matches("^https?://.*")) {
                        url = "https://www.reddit.com" + url;
                    }
                    builder.setUrl(url);
                    builder.setPublished(new Date((long) jsonChildData.getInt("created_utc") * 1000));
                    builder.setSource(SEARCHER_NAME);

                    // create "fullname"; see https://www.reddit.com/dev/api
                    String fullName = jsonChild.getString("kind") + "_" + jsonChildData.getString("id");
                    builder.setIdentifier(fullName);

                    result.add(builder.create());
                    if (result.size() == query.getResultCount()) {
                        break paging;
                    }
                }
                pagingAfter = jsonData.tryGetString("after");
                if (pagingAfter == null) {
                    break;
                }
            } catch (JsonException e) {
                throw new SearcherException("Error while parsing JSON response '" + stringResult + "': " + e.toString(), e);
            }
        }
        return new SearchResults<WebContent>(result);
    }

    private static String makeQueryUrl(MultifacetQuery query, String after, int limit) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://www.reddit.com/search.json");
        urlBuilder.append("?limit=").append(limit);
        urlBuilder.append("&q=").append(query.getText());
        Facet temp = query.getFacet(Sorting.IDENTIFIER);
        if (temp != null) {
            Sorting sorting = (Sorting) temp;
            urlBuilder.append("&sort=").append(sorting.getValue());
        }
        urlBuilder.append("&t=").append("all"); // XXX hard coded for now
        if (after != null) {
            urlBuilder.append("&after=").append(after);
        }
        return urlBuilder.toString();
    }

    public static void main(String[] args) throws SearcherException {
        SearchResults<WebContent> result = new RedditSearcher().search(new MultifacetQuery.Builder().setText("snowden").setResultCount(500).addFacet(Sorting.COMMENTS).create());
        CollectionHelper.print(result);
    }

}
