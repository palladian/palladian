package ws.palladian.retrieval.search.images;

import java.util.List;
import java.util.Optional;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractOpenverseSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

/**
 * Search for free images on <a href="https://openverse.org">Openverse</a>.
 *
 * @author David Urbansky
 * @see <a href="https://api.openverse.engineering/v1/">Openverse API</a>
 */
public class OpenverseImageSearcher extends AbstractOpenverseSearcher<WebImage> {
    public static final class OpenverseImageSearcherMetaInfo extends AbstractOpenverseSearcherMetaInfo<WebImage> {
        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "openverse_image";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        protected AbstractOpenverseSearcher<WebImage> create(String clientId, String clientSecret) {
            return new OpenverseImageSearcher(clientId, clientSecret);
        }
    }

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Openverse Image";

    public OpenverseImageSearcher() {
        this(null, null);
    }

    public OpenverseImageSearcher(String clientId, String clientSecret) {
        super(clientId, clientSecret);
    }

    @Override
    protected WebImage parseResult(JsonObject resultHit) {
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
        return builder.create();
    }

    @Override
    protected String buildRequest(String searchTerms, int page, int resultsPerPage) {
        String url = String.format(
                "https://api.openverse.engineering/v1/images/?q=%s&license_type=%s&page=%s&page_size=%s&mature=true",
                UrlHelper.encodeParameter(searchTerms), getLicenses(), page, resultsPerPage);
        if (this.getSources() != null) {
            url += "&source=" + this.getSources();
        }
        return url;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        var searcher = new OpenverseImageSearcher();
//		var searcher = new OpenverseSearcher("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        searcher.setSources(
                "wikimedia,thorvaldsensmuseum,thingiverse,svgsilh,sketchfab,rijksmuseum,rawpixel,phylopic,nypl,museumsvictoria,met,mccordmuseum,iha,geographorguk,floraon,eol,digitaltmuseum,deviantart,clevelandmuseum,brooklynmuseum,behance,animaldiversity,WoRMS,CAPL,500px");
        List<WebImage> results = searcher.search("brain", 100);
        CollectionHelper.print(results);
    }

}
