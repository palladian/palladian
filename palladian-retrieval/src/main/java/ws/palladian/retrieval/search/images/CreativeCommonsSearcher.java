package ws.palladian.retrieval.search.images;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for free images on <a href="https://creativecommons.org">Creative Commons</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="https://api.creativecommons.engineering/v1/">Creative Commons API Docs</a>
 */
public class CreativeCommonsSearcher extends AbstractSearcher<WebImage> {
    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Creative Commons";

    private static final int MAX_PER_PAGE = 500;

    @Override
    /**
     * @param language Supported languages are English.
     */
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = Math.min(10000, resultCount);
        int resultsPerPage = Math.min(MAX_PER_PAGE, resultCount);
        int pagesNeeded = (int)Math.ceil(resultCount / (double)resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();
        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query, page, Math.min(MAX_PER_PAGE, resultCount - results.size()));
            try {
                JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
                if (jsonResponse == null) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse);
                JsonArray jsonArray = json.getJsonArray("results");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resultHit = jsonArray.getJsonObject(i);

                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setAdditionalData("id", resultHit.tryQueryString("id"));
                    builder.setUrl(resultHit.tryQueryString("url"));
                    builder.setImageUrl(resultHit.tryQueryString("url"));
                    builder.setTitle(resultHit.tryQueryString("title"));
                    builder.setWidth(Optional.ofNullable(resultHit.tryGetInt("width")).orElse(0));
                    builder.setHeight(Optional.ofNullable(resultHit.tryGetInt("height")).orElse(0));
                    builder.setImageType(ImageType.UNKNOWN);
                    builder.setThumbnailUrl(resultHit.tryQueryString("thumbnail"));
                    builder.setLicense(License.FREE);
                    builder.setLicenseLink(resultHit.tryQueryString("license_url"));
                    results.add(builder.create());
                    if (results.size() >= resultCount) {
                        break;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException(e.getMessage());
            }
        }

        return results;
    }

    private String buildRequest(String searchTerms, int page, int resultsPerPage) {
        return String.format("https://api.creativecommons.engineering/v1/images?q=%s&license_type=all-cc,commercial&page=%s&page_size=%s&mature=true", UrlHelper.encodeParameter(searchTerms), page, resultsPerPage);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        CreativeCommonsSearcher searcher = new CreativeCommonsSearcher();
        List<WebImage> results = searcher.search("brain", 1001);
        CollectionHelper.print(results);
    }
}
