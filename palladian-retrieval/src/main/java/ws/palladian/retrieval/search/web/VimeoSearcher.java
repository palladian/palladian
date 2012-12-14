package ws.palladian.retrieval.search.web;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.OAuthParams;
import ws.palladian.retrieval.OAuthUtil;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * WebSearcher for <a href="http://vimeo.com/">Vimeo</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://developer.vimeo.com/apis/advanced/methods/vimeo.videos.search">API documentation</a>
 */
public final class VimeoSearcher extends WebSearcher<WebVideoResult> {

    private final OAuthParams oAuthParams;

    /**
     * Create a new {@link VimeoSearcher}.
     * 
     * @param oAuthParams
     */
    public VimeoSearcher(OAuthParams oAuthParams) {
        this.oAuthParams = oAuthParams;
    }

    /**
     * <p>
     * Create a new {@link VimeoSearcher}.
     * </p>
     * 
     * @param clientId
     * @param clientSecret
     * @param accessToken
     * @param accessTokenSecret
     */
    public VimeoSearcher(String clientId, String clientSecret, String accessToken, String accessTokenSecret) {
        this(new OAuthParams(clientId, clientSecret, accessToken, accessTokenSecret));
    }

    @Override
    public String getName() {
        return "Vimeo";
    }

    private HttpRequest buildRequest(String query, int page, int resultCount, Language language) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, "http://vimeo.com/api/rest/v2");
        request.addParameter("method", "vimeo.videos.search");
        request.addParameter("query", query);
        request.addParameter("full_response", "true");
        request.addParameter("format", "json");
        request.addParameter("page", page);
        request.addParameter("per_page", resultCount);
        return OAuthUtil.createSignedRequest(request, oAuthParams);
    }

    @Override
    public List<WebVideoResult> search(String query, int resultCount, Language language) throws SearcherException {

        // TODO pagination available? Currently I get only 50 results max.

        HttpRequest request = buildRequest(query, 1, resultCount, language);

        HttpResult httpResult;
        try {
            httpResult = retriever.perform(request);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                    + " (request: \"" + request + "\"): " + e.getMessage(), e);
        }

        List<WebVideoResult> webResults = CollectionHelper.newArrayList();
        try {

            String jsonString = HttpHelper.getStringContent(httpResult);
            webResults.addAll(parse(jsonString));
            System.out.println(jsonString);

        } catch (Exception e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);

        }
        return webResults;
    }

    static List<WebVideoResult> parse(String jsonString) throws JSONException {
        List<WebVideoResult> result = CollectionHelper.newArrayList();

        JSONArray jsonVideos = JPathHelper.get(jsonString, "videos/video", JSONArray.class);
        for (int i = 0; i < jsonVideos.length(); i++) {
            JSONObject jsonVideo = jsonVideos.getJSONObject(i);

            String title = JPathHelper.get(jsonVideo, "title", String.class);
            String description = JPathHelper.get(jsonVideo, "description", String.class);
            String uploadDate = JPathHelper.get(jsonVideo, "upload_date", String.class);
            String id = JPathHelper.get(jsonVideo, "id", String.class);

            WebVideoResult videoResult = new WebVideoResult("https://vimeo.com" + id, null, title, null, null);
            System.out.println(videoResult);
            result.add(videoResult);
        }

        return result;
    }

    public static void main(String[] args) throws SearcherException {
        String clientId = "8297612f1583ce07644c3c03c6053e5575a1bd53";
        String clientSecret = "ee7af363604085f2c6f72b62e58c767dceca3893";
        String accessToken = "873505f0c25c364a58c4ce3d585680ad";
        String accessTokenSecret = "69ab925e89e0e8f240ce2afe43c9b965332f35fb";
        VimeoSearcher searcher = new VimeoSearcher(clientId, clientSecret, accessToken, accessTokenSecret);
        searcher.search("cat", 10);
    }
}
