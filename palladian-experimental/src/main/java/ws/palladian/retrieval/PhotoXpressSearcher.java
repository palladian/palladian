package ws.palladian.retrieval;

import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.images.ImageType;
import ws.palladian.retrieval.search.images.WebImageResult;

/**
 * <p>
 * Search for public domain images on <a href="http://www.photoxpress.com/">PhotoXpress</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://www.photoxpress.com/Services/API/Documentation">PhotoXpress API</a>
 */
public class PhotoXpressSearcher extends AbstractSearcher<WebImageResult> {

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.photoxpress.key";

    private final String apiKey;

    /**
     * <p>
     * Creates a new PhotoXpress searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing PhotoXpress, not <code>null</code> or empty.
     */
    public PhotoXpressSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new PhotoXpress searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing PhotoXpress, which must be
     *            provided
     *            as string via key {@value PhotoXpressSearcher#CONFIG_API_KEY} in the configuration.
     */
    public PhotoXpressSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebImageResult> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImageResult> results = CollectionHelper.newArrayList();

        // FIXME pagination not done yet
        // resultCount = Math.min(1000, resultCount);
        // int resultsPerPage = Math.min(100, resultCount);
        // int pagesNeeded = (int)Math.ceil(resultCount / (double)resultsPerPage);

        String request = buildRequest(query);

        Map<String, String> content = CollectionHelper.newHashMap();
        System.out.println(request);
        content.put("content", request);
        HttpResult httpPost;
        try {
            httpPost = HttpRetrieverFactory.getHttpRetriever().httpPost("http://www.photoxpress.com/Xmlrpc", content);
            String resultContent = httpPost.getStringContent();
            System.out.println(resultContent);
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // JsonObjectWrapper json = new JsonObjectWrapper(documentRetriever.getText(request));
        // JSONArray jsonArray = json.getJSONArray("hits");
        // for (int i = 0; i < jsonArray.length(); i++) {
        // try {
        // JsonObjectWrapper resultHit = new JsonObjectWrapper(jsonArray.getJSONObject(i));
        // String url = resultHit.getString("pageURL");
        // String imageUrl = resultHit.getString("webformatURL");
        // String summary = resultHit.getString("tags");
        // String imageTypeString = resultHit.getString("type");
        // int width = resultHit.getInt("imageWidth");
        // int height = resultHit.getInt("imageHeight");
        // WebImageResult webImageResult = new WebImageResult(url, imageUrl, summary, summary, width, height,
        // null, null);
        //
        // webImageResult.setThumbImageUrl(resultHit.getString("previewURL"));
        // webImageResult.setLicense(License.PUBLIC_DOMAIN);
        // webImageResult.setLicenseLink("http://creativecommons.org/publicdomain/zero/1.0/deed.en");
        // webImageResult.setImageType(getImageType(imageTypeString));
        //
        // results.add(webImageResult);
        //
        // } catch (JSONException e) {
        // throw new SearcherException(e.getMessage());
        // }
        //
        // }

        return results;
    }

    private ImageType getImageType(String imageTypeString) {
        if (imageTypeString.equalsIgnoreCase("photo")) {
            return ImageType.PHOTO;
        } else if (imageTypeString.equalsIgnoreCase("clipart")) {
            return ImageType.CLIPART;
        } else if (imageTypeString.equalsIgnoreCase("vector")) {
            return ImageType.VECTOR;
        }

        return ImageType.UNKNOWN;
    }

    private String buildRequest(String searchTerms) {
        StringBuilder query = new StringBuilder();

        query.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        query.append("<methodCall>");
        query.append("<methodName>search.getSearchResults</methodName>");
        query.append("<params>");
        query.append("<param><value><string>").append(this.apiKey).append("</string></value></param>");
        query.append("<param>");
        query.append("<value>");
        query.append("<struct>");
        query.append("<member>");
        query.append("<name>q</name>");
        query.append("<value>");
        query.append("<string>").append(UrlHelper.encodeParameter(searchTerms)).append("</string>");
        query.append("</value>");
        query.append("</member>");
        query.append("<member>");
        query.append("<name>limit</name>");
        query.append("<value>");
        query.append("<int>64</int>");
        query.append("</value>");
        query.append("</member>");
        query.append("<member>");
        query.append("<name>limit_free</name>");
        query.append("<value>");
        query.append("<int>64</int>");
        query.append("</value>");
        query.append("</member>");
        query.append("<member>");
        query.append("<name>limit_premium</name>");
        query.append("<value>");
        query.append("<int>0</int>");
        query.append("</value>");
        query.append("</member>");
        query.append("<member>");
        query.append("<name>detail_level</name>");
        query.append("<value>");
        query.append("<int>0</int>");
        query.append("</value>");
        query.append("</member>");
        query.append("</struct>");
        query.append("</value>");
        query.append("</param>");
        query.append("</params>");
        query.append("</methodCall>");

        return query.toString();
    }

    @Override
    public String getName() {
        return "PhotoXpress";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        PhotoXpressSearcher pixabaySearcher = new PhotoXpressSearcher("qhhsJoCG92Uj9qwbooua2Xyti97mGfbx");
        List<WebImageResult> results = pixabaySearcher.search("car", 101);
        CollectionHelper.print(results);
    }
}
