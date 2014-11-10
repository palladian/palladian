package ws.palladian.retrieval.search.web;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
 * Searcher for the <a href="http://stackexchange.com">Stack Exchange network</a>.
 * 
 * @author pk
 * @see <a href="http://api.stackexchange.com">Stack Exchange API</a>
 */
public final class StackExchangeSearcher extends AbstractMultifacetSearcher<WebContent> {

    // TODO provide authentication mechanisms

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(StackExchangeSearcher.class);

    private static final String NAME = "Stack Exchange";

    private final String site;

    public StackExchangeSearcher(String site) {
        Validate.notEmpty(site, "site must not be empty");
        this.site = site;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        String queryUrl = createQueryUrl(query);
        LOGGER.debug("request url = {}", queryUrl);
        System.out.println(queryUrl);
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(queryUrl);
        } catch (HttpException e) {
            throw new SearcherException("Error while accessing \"" + queryUrl + "\"", e);
        }
        if (httpResult.errorStatus()) {
            throw new SearcherException("Encountered HTTP status " + httpResult.getStatusCode() + " while accessing \""
                    + queryUrl + "\"");
        }
        try {
            List<WebContent> resultList = CollectionHelper.newArrayList();
            JsonObject resultJson = new JsonObject(httpResult.getStringContent());
            JsonArray jsonItems = resultJson.getJsonArray("items");
            for (int i = 0; i < jsonItems.size(); i++) {
                JsonObject jsonItem = jsonItems.getJsonObject(i);
                BasicWebContent.Builder builder = new BasicWebContent.Builder();
                JsonArray tagArray = jsonItem.getJsonArray("tags");
                for (int j = 0; j < tagArray.size(); j++) {
                    builder.addTag(tagArray.getString(j));
                }
                builder.setIdentifier(jsonItem.getString("question_id"));
                builder.setPublished(new Date(jsonItem.getInt("creation_date") * 1000l));
                builder.setUrl(jsonItem.getString("link"));
                builder.setTitle(jsonItem.getString("title"));
                builder.setSource(NAME);
                resultList.add(builder.create());
            }
            return new SearchResults<WebContent>(resultList);
        } catch (JsonException e) {
            throw new SearcherException("Error while parsing JSON \"" + httpResult.getStringContent() + "\"", e);
        }
    }

    private String createQueryUrl(MultifacetQuery query) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://api.stackexchange.com/2.2/search/advanced");
        // urlBuilder.append("http://api.stackexchange.com/2.2/search/excerpts");
        urlBuilder.append("?site=").append(site);
        if (StringUtils.isNotBlank(query.getText())) {
            urlBuilder.append("&q=").append(UrlHelper.encodeParameter(query.getText()));
        }
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            urlBuilder.append("&tags=").append(StringUtils.join(query.getTags(), ';'));
        }
        if (StringUtils.isNotBlank(query.getUrl())) {
            urlBuilder.append("&url=").append(UrlHelper.encodeParameter(query.getUrl()));
        }
        return urlBuilder.toString();
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static void main(String[] args) throws SearcherException {
        StackExchangeSearcher searcher = new StackExchangeSearcher("stackoverflow");
        // MultifacetQuery query = new MultifacetQuery.Builder().setUrl("http://tech.knime.org/community/palladian").create();
        // MultifacetQuery query = new MultifacetQuery.Builder().setUrl("http://palladian.ws").create();
        MultifacetQuery query = new MultifacetQuery.Builder().setText("palladian").create();
        SearchResults<WebContent> result = searcher.search(query);
        CollectionHelper.print(result);
    }

}
