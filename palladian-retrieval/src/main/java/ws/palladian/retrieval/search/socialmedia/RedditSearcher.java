package ws.palladian.retrieval.search.socialmedia;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for <a href="http://www.reddit.com">reddit.com</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://www.reddit.com/dev/api">reddit.com API documentation</a>
 */
public final class RedditSearcher extends AbstractMultifacetSearcher<WebContent> {

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
    private static final String SEARCHER_NAME = "reddit.com";
    
    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        List<WebContent> result = CollectionHelper.newArrayList();
        String pagingAfter = null;
        int limit = Math.min(100, query.getResultCount());

        paging: for (;;) {
            String queryUrl = makeQueryUrl(query, pagingAfter, limit);
            LOGGER.debug("Retrieve from {}", queryUrl);

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
                    builder.setUrl(jsonChildData.getString("url"));
                    builder.setPublished(new Date((long)jsonChildData.getInt("created_utc") * 1000));
                    result.add(builder.create());
                    if (result.size() == query.getResultCount()) {
                        break paging;
                    }
                }
                pagingAfter = jsonData.getString("after");
                if (pagingAfter == null) {
                    break;
                }
            } catch (JsonException e) {
                throw new SearcherException(
                        "Error while parsing JSON response '" + stringResult + "': " + e.toString(), e);
            }
        }
        return new SearchResults<WebContent>(result);
    }

    private static String makeQueryUrl(MultifacetQuery query, String after, int limit) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://www.reddit.com/search.json");
        urlBuilder.append("?limit=").append(limit);
        urlBuilder.append("&q=").append(query.getText());
        Facet temp = query.getFacet(Sorting.IDENTIFIER);
        if (temp != null) {
            Sorting sorting = (Sorting)temp;
            urlBuilder.append("&sort=").append(sorting.getValue());
        }
        urlBuilder.append("&t=").append("all"); // XXX hard coded for now
        if (after != null) {
            urlBuilder.append("&after=").append(after);
        }
        return urlBuilder.toString();
    }

    public static void main(String[] args) throws SearcherException {
        SearchResults<WebContent> result = new RedditSearcher().search(new MultifacetQuery.Builder().setText("snowden")
                .setResultCount(500).addFacet(Sorting.COMMENTS).create());
        CollectionHelper.print(result);
    }

}
