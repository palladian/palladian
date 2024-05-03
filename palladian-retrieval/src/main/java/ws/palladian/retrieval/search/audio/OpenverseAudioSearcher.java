package ws.palladian.retrieval.search.audio;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebAudio;
import ws.palladian.retrieval.resources.WebAudio;
import ws.palladian.retrieval.search.AbstractOpenverseSearcher;

/**
 * Search for free audio files on <a href="https://openverse.org">Openverse</a>.
 *
 * @author Philipp Katz
 * @see <a href="https://api.openverse.engineering/v1/">Openverse API</a>
 */
public class OpenverseAudioSearcher extends AbstractOpenverseSearcher<WebAudio> {
    public static final class OpenverseAudioSearcherMetaInfo extends AbstractOpenverseSearcherMetaInfo<WebAudio> {
        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "openverse_audio";
        }

        @Override
        public Class<WebAudio> getResultType() {
            return WebAudio.class;
        }

        @Override
        protected AbstractOpenverseSearcher<WebAudio> create(String clientId, String clientSecret) {
            return new OpenverseAudioSearcher(clientId, clientSecret);
        }
    }

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Openverse Audio";

    public OpenverseAudioSearcher() {
        this(null, null);
    }

    public OpenverseAudioSearcher(String clientId, String clientSecret) {
        super(clientId, clientSecret);
    }

    @Override
    protected WebAudio parseResult(JsonObject resultHit) {
        BasicWebAudio.Builder builder = new BasicWebAudio.Builder();
        builder.setAdditionalData("id", resultHit.tryQueryString("id"));
        builder.setUrl(resultHit.tryQueryString("url"));
        builder.setAudioUrl(resultHit.tryQueryString("url"));
        builder.setTitle(resultHit.tryQueryString("title"));
        return builder.create();
    }

    @Override
    protected String buildRequest(String searchTerms, int page, int resultsPerPage) {
        String url = String.format(
                "https://api.openverse.engineering/v1/audio/?q=%s&license_type=%s&page=%s&page_size=%s&mature=true",
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

}
