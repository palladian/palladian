package ws.palladian.retrieval.search.images;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.List;

/**
 * Search for free gifs from <a href="https://klipy.com">Klipy</a>.
 *
 * @author David Urbansky
 * @see <a href="https://klipy.com/developers">Klipy API Docs</a>
 */
public class KlipySearcher extends AbstractSearcher<WebImage> {
    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Klipy";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.klipy.key";

    private final String apiKey;

    /**
     * Creates a new Klipy searcher.
     *
     * @param apiKey The API key for accessing Klipy, not <code>null</code> or empty.
     */
    public KlipySearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new Klipy searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Klipy, which must be
     *                      provided as string via key {@value KlipySearcher#CONFIG_API_KEY} in the configuration.
     */
    public KlipySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    /**
     * @param language Supported languages are English.
     */ public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;
        int resultsPerPage = Math.min(50, resultCount);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        String requestUrl = buildRequest(query, resultsPerPage);
        try {
            JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
            if (jsonResponse == null) {
                throw new SearcherException("Failed to get JSON from " + requestUrl);
            }
            JsonArray jsonArray = jsonResponse.queryJsonArray("data/data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject resultHit = jsonArray.getJsonObject(i);

                BasicWebImage.Builder builder = new BasicWebImage.Builder();
                builder.setUrl(resultHit.tryQueryString("file/hd/gif/url"));
                builder.setImageUrl(resultHit.tryQueryString("file/hd/gif/url"));
                builder.setTitle(resultHit.tryGetString("title"));
                builder.setWidth(resultHit.tryQueryInt("file/hd/gif/width", 0));
                builder.setHeight(resultHit.tryQueryInt("file/hd/gif/height", 0));
                builder.setImageType(ImageType.GIF);
                builder.setThumbnailUrl(resultHit.tryQueryString("file/hd/gif/url"));
                builder.setLicense(License.FREE);
                results.add(builder.create());
                if (results.size() >= resultCount) {
                    break;
                }
            }
        } catch (JsonException e) {
            throw new SearcherException(e.getMessage());
        }

        return results;
    }

    private String buildRequest(String searchTerms, int limit) {
        return String.format("https://api.klipy.com/api/v1/%s/gifs/search?q=%s&per_page=%s", apiKey, UrlHelper.encodeParameter(searchTerms), limit);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        KlipySearcher searcher = new KlipySearcher("KEY");
        List<WebImage> results = searcher.search("pizza", 101);
        CollectionHelper.print(results);
    }
}
