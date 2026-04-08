package ws.palladian.retrieval.search.socialmedia;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * Searcher for Bluesky.
 *
 * @author Philipp Katz
 */
public final class BlueskySearcher extends AbstractMultifacetSearcher<WebContent> {

    public static final class BlueskySearcherMetaInfo implements SearcherMetaInfo<BlueskySearcher, WebContent> {

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "bluesky";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public BlueskySearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new BlueskySearcher();
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://docs.bsky.app/docs/api/app-bsky-feed-search-posts";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for posts on <a href=\"https://bsky.app\">Bluesky</a>.";
        }

    }

    private static final String SEARCHER_NAME = "Bluesky";

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        if (query.getResultCount() > 100) {
            // https://github.com/bluesky-social/atproto/issues/2838
            throw new SearcherException("Currently maximum 100 results are allowed. "
                    + "(see https://github.com/bluesky-social/atproto/issues/2838)");
        }

        var result = new ArrayList<WebContent>();

        var limit = Math.min(query.getResultCount(), 100);
        String cursor = null;
        var retriever = HttpRetrieverFactory.getHttpRetriever();

        outer: for (;;) {
            var url = String.format("https://public.api.bsky.app/xrpc/app.bsky.feed.searchPosts");
            url += String.format("?q=%s", UrlHelper.encodeParameter(query.getText()));
            if (cursor != null) {
                url += "&cursor=" + cursor;
            }
            url += "&limit=" + limit;
            if (query.getLanguage() != null) {
                url += "&lang=" + query.getLanguage().getIso6391();
            }

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(url);
            } catch (HttpException e) {
                throw new SearcherException(e);
            }

            var stringResult = httpResult.getStringContent();

            if (httpResult.errorStatus()) {
                throw new SearcherException(
                        String.format("HTTP status %s (%s)", httpResult.getStatusCode(), stringResult));
            }

            try {
                var jsonResult = new JsonObject(stringResult);
                var jsonPosts = jsonResult.getJsonArray("posts");
                cursor = jsonResult.getString("cursor");

                for (var post : jsonPosts) {
                    var jsonPost = (JsonObject) post;
                    var uri = jsonPost.getString("uri");
                    var cid = jsonPost.getString("cid");
                    var record = jsonPost.getJsonObject("record");
                    var createdAt = record.getString("createdAt");
                    var text = record.getString("text");
                    // TODO tags

                    var builder = new BasicWebContent.Builder();
                    builder.setUrl(convertAtUriToUrl(uri));
                    builder.setIdentifier(cid);
                    builder.setSource(SEARCHER_NAME);
                    builder.setTitle(text);

                    var createdAtDate = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(createdAt)));
                    builder.setPublished(createdAtDate);

                    result.add(builder.create());

                    if (result.size() >= query.getResultCount()) {
                        break outer;
                    }
                }

                if (jsonPosts.size() < limit) {
                    break outer;
                }

            } catch (JsonException e) {
                throw new SearcherException(e);
            }

        }

        return new SearchResults<>(result);
    }

    /**
     * Convert at:// URI to a web URL.
     *
     * See https://github.com/bluesky-social/atproto/discussions/2523
     *
     * The schema is:
     *
     * <pre>
     * at://[DID]/[COLLECTION[/[RKEY]
     * https://bsky.app/profile/[DID]/post/[RKEY]
     * </pre>
     *
     * Example:
     *
     * <pre>
     * at://did:plc:mnnxdrxw3wncocae53mmdsxi/app.bsky.feed.post/3latlue4glc26
     * https://bsky.app/profile/did:plc:mnnxdrxw3wncocae53mmdsxi/post/3latlue4glc26
     * </pre>
     *
     * @param uri
     * @return
     */
    static String convertAtUriToUrl(String uri) {
        return uri.replace("at://", "https://bsky.app/profile/").replace("/app.bsky.feed.post/", "/post/");

    }

}
