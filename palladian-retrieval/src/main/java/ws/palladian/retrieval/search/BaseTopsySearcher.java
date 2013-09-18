package ws.palladian.retrieval.search;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.resources.WebContent;

public abstract class BaseTopsySearcher extends AbstractSearcher<WebContent> {

    /** The identifier for the API key when provided via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.topsy.key";

    private final String apiKey;
    
    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new Topsy searcher with the specified API key.
     * </p>
     * 
     * @param apiKey The API key, not <code>null</code> or empty.
     */
    protected BaseTopsySearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new Topsy searcher with an API key provided by a {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The Configuration providing the required API key via key {@value #CONFIG_API_KEY}, not
     *            <code>null</code>.
     */
    protected BaseTopsySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> result = CollectionHelper.newArrayList();

        // # of necessary requests, we fetch in chunks of 100
        int numRequests = (int)Math.ceil(resultCount / 100.);

        for (int page = 1; page <= Math.min(numRequests, 10); page++) {

            String queryUrl = buildQueryUrl(query, page, apiKey);
            // System.out.println(queryUrl);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(queryUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching with URL \"" + query + "\": " + e.getMessage(),
                        e);
            }

            String jsonString = httpResult.getStringContent();
            try {
                JSONObject jsonResult = new JSONObject(jsonString);
                JSONObject responseJson = jsonResult.getJSONObject("response");
                JSONArray listJson = responseJson.getJSONArray("list");

                for (int i = 0; i < listJson.length(); i++) {

                    JSONObject item = listJson.getJSONObject(i);
                    WebContent webResult = parse(item);
                    result.add(webResult);

                    if (result.size() == resultCount) {
                        break;
                    }

                }

            } catch (JSONException e) {
                throw new SearcherException("Error parsing the JSON response " + e.getMessage() + ", JSON was \""
                        + jsonString + "\"", e);
            }

        }

        return result;
    }

    /**
     * Subclass provides the URL for the query.
     */
    protected abstract String buildQueryUrl(String query, int page, String apiKey);

    /**
     * Subclass performs the parsing for each item in the JSON list.
     */
    protected abstract WebContent parse(JSONObject item) throws JSONException;

}
