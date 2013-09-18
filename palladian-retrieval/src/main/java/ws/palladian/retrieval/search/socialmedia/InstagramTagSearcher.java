package ws.palladian.retrieval.search.socialmedia;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.images.WebImageResult;

/**
 * <p>
 * Search for images on <a href="http://instagram.com">Instagram</a> by tags. This searcher has one peculiarity: As tags
 * are one word terms, only the first term in the query is considered, the rest is silently ignored. Authentication
 * needs to be done via OAuth 2.0 in advance, the accessToken is necessary for initializing this searcher. The necessary
 * steps are not implemented here, as they require user interaction.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://instagram.com/developer/">Instagram Developer Documentation</a>
 * @see <a href="http://instagram.com/developer/authentication/">Authentication</a>
 */
public final class InstagramTagSearcher extends AbstractSearcher<WebImageResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramTagSearcher.class);

    /** The identifier for the {@link Configuration} key with the access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.instagram.accessToken";

    private final String accessToken;
    
    private final HttpRetriever retriever;

    /**
     * <p>
     * Initialize a new {@link InstagramTagSearcher} with the specified OAuth access token.
     * </p>
     * 
     * @param accessToken The OAuth access token, not <code>null</code> or empty.
     */
    public InstagramTagSearcher(String accessToken) {
        Validate.notEmpty(accessToken, "accessToken must not be empty");
        this.accessToken = accessToken;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Initialize a new {@link InstagramTagSearcher} with the an OAuth access token provided via the
     * {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The configuration providing an OAuth access token with the identifier
     *            {@value #CONFIG_ACCESS_TOKEN}, not <code>null</code>.
     */
    public InstagramTagSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCESS_TOKEN));
    }

    @Override
    public String getName() {
        return "Instagram";
    }

    @Override
    public List<WebImageResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebImageResult> result = new ArrayList<WebImageResult>();

        String[] querySplit = query.split("\\s");
        if (querySplit.length > 1) {
            LOGGER.warn("Query \"" + query + "\" consists of multiple terms, only the first one is considered!");
            query = querySplit[0];
        }

        String queryUrl = String.format("https://api.instagram.com/v1/tags/%s/media/recent?access_token=%s",
                UrlHelper.encodeParameter(query), accessToken);

        page: for (;;) {

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(queryUrl);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP exception while accessing \"" + queryUrl + "\": "
                        + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();

            try {
                JSONObject jsonResult = new JSONObject(jsonString);
                JSONArray dataArray = jsonResult.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);

                    String pageUrl = data.getString("link");
                    Date date = new Date(data.getLong("created_time") * 1000);

                    JSONObject imageData = data.getJSONObject("images").getJSONObject("standard_resolution");
                    String imageUrl = imageData.getString("url");
                    int width = imageData.getInt("width");
                    int height = imageData.getInt("height");

                    String title = null;
                    if (data.has("caption") && !data.getString("caption").equals("null")) {
                        title = data.getJSONObject("caption").getString("text");
                    }
                    result.add(new WebImageResult(pageUrl, imageUrl, title, null, width, height, date));

                    if (result.size() == resultCount) {
                        break page;
                    }

                }

                JSONObject paginationJson = jsonResult.getJSONObject("pagination");
                if (!paginationJson.has("next_url")) {
                    break;
                }
                queryUrl = paginationJson.getString("next_url");

            } catch (JSONException e) {
                throw new SearcherException("Parse exception while parsing JSON data: \"" + jsonString + "\"", e);
            }

        }
        return result;
    }

}
