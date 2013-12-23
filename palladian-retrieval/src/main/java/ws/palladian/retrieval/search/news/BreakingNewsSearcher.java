package ws.palladian.retrieval.search.news;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
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
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for <a href="http://www.breakingnews.com">breakingnews.com</a>. This searcher uses an unpublished,
 * reverse-engineered JSON API and is therefore not intended to be used publicly, e.g. in the KNIME nodes distribution.
 * </p>
 * 
 * @author pk
 * 
 */
public final class BreakingNewsSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BreakingNewsSearcher.class);

    /** The base URL. */
    private static final String BASE = "http://www.breakingnews.com";

    /** The query URL to start. */
    private static final String START_URL = BASE + "/api/v1/topic/search/?q=%s&order_by=name";

    /** The name of this searcher. */
    private static final String NAME = "Breaking News";

    /** The format of the provided dates. */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        String queryText = query.getText();
        int requestedItems = query.getResultCount();
        if (StringUtils.isBlank(queryText)) {
            throw new SearcherException("The query must provide a text.");
        }
        String queryUrl = String.format(START_URL, UrlHelper.encodeParameter(queryText));
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        String jsonString = null;
        try {
            List<WebContent> resultList = CollectionHelper.newArrayList();
            outer: for (;;) {
                HttpResult httpResult = httpRetriever.httpGet(queryUrl);
                jsonString = httpResult.getStringContent();
                JsonObject jsonResult = new JsonObject(jsonString);
                JsonArray objectsResult = jsonResult.getJsonArray("objects");
                for (Object object : objectsResult) {
                    JsonObject resultObject = (JsonObject)object;
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    builder.setUrl(resultObject.getString("permalink"));
                    builder.setPublished(parseDate(resultObject.getString("created_on"))); // latest_item_date
                    builder.setIdentifier(resultObject.getString("id"));
                    builder.setSummary(resultObject.getString("description"));
                    builder.setTitle(resultObject.getString("name"));
                    builder.addTag(resultObject.getString("category"));
                    Double lng = resultObject.tryGetDouble("longitude");
                    Double lat = resultObject.tryGetDouble("latitude");
                    if (lng != null && lat != null) {
                        builder.setCoordinate(lat, lng);
                    }
                    builder.setSource(NAME);
                    resultList.add(builder.create());
                    if (resultList.size() >= requestedItems) {
                        break outer;
                    }
                }
                String next = jsonResult.tryQueryString("/meta/next");
                if (StringUtils.isBlank(next)) {
                    break;
                }
                queryUrl = BASE + next;
            }
            return new SearchResults<WebContent>(resultList);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while accessing '" + queryUrl + "'", e);
        } catch (JsonException e) {
            throw new SearcherException("Error while parsing the JSON '" + jsonString + "'", e);
        }
    }

    private static Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(dateString);
        } catch (ParseException e) {
            LOGGER.warn("Error while parsing '" + dateString + "' with '" + DATE_FORMAT + "'.");
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        // List<WebContent> result = new BreakingNewsSearcher().search("pussy riot", 10);
        // List<WebContent> result = new BreakingNewsSearcher().search("harvard university", 10);
        List<WebContent> result = new BreakingNewsSearcher().search("obama", 50);
        CollectionHelper.print(result);
    }

}
