package ws.palladian.retrieval.search.images;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private String licenses = "all-cc,commercial";

    /** If null, search all sources. */
    private String sources = null;

    public CreativeCommonsSearcher() {

    }

    public CreativeCommonsSearcher(int defaultResultCount) {
        super();
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    /**
     * @param language Supported languages are English.
     */ public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;
        resultCount = Math.min(10000, resultCount);
        int resultsPerPage = Math.min(MAX_PER_PAGE, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();
        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query, page, Math.min(MAX_PER_PAGE, resultCount - results.size()));
            try {
                JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
                if (jsonResponse == null) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse);
                JsonArray jsonArray = Optional.ofNullable(json.getJsonArray("results")).orElse(new JsonArray());
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
            } catch (Exception e) {
                throw new SearcherException(e.getMessage());
            }
        }

        return results;
    }

    private String buildRequest(String searchTerms, int page, int resultsPerPage) {
        String url = String.format("https://api.creativecommons.engineering/v1/images?q=%s&license_type=%s&page=%s&page_size=%s&mature=true",
                UrlHelper.encodeParameter(searchTerms), licenses, page, resultsPerPage);
        if (this.sources != null) {
            url += "&source=" + this.sources;
        }

        return url;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public String getLicenses() {
        return licenses;
    }

    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public static void main(String[] args) throws SearcherException {
        CreativeCommonsSearcher searcher = new CreativeCommonsSearcher();
        searcher.setSources(
                "wikimedia,thorvaldsensmuseum,thingiverse,svgsilh,sketchfab,rijksmuseum,rawpixel,phylopic,nypl,museumsvictoria,met,mccordmuseum,iha,geographorguk,floraon,eol,digitaltmuseum,deviantart,clevelandmuseum,brooklynmuseum,behance,animaldiversity,WoRMS,CAPL,500px");
        List<WebImage> results = searcher.search("brain", 1001);
        CollectionHelper.print(results);
    }
}
