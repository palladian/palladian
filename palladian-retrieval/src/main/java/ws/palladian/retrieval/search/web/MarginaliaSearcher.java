package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * @author Philipp Katz
 * @since 3.0.0
 */
public class MarginaliaSearcher extends AbstractSearcher<WebContent> {

    public static final class MarginaliaSearcherMetaInfo implements SearcherMetaInfo<MarginaliaSearcher, WebContent> {
        private static final StringConfigurationOption KEY_OPTION = new StringConfigurationOption("Key", "key",
                DEFAULT_KEY);

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "marginalia";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(KEY_OPTION);
        }

        @Override
        public MarginaliaSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var key = KEY_OPTION.get(config);
            return new MarginaliaSearcher(key);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://www.marginalia.nu/marginalia-search/api/";
        }

        @Override
        public String getSearcherDescription() {
            return "<a href=\"https://www.marginalia.nu/marginalia-search/\">Marginalia Search</a> "
                    + "is an independent DIY search engine that focuses on non-commercial content, "
                    + "and attempts to show you sites you perhaps weren’t aware of in favor of the "
                    + "sort of sites you probably already knew existed. For experimentation, the key "
                    + "“public” is available. This key has a shared rate limit across all consumers. "
                    + "When this rate limit is hit a HTTP status 503 is returned.";
        }
    }

    private static final String SEARCHER_NAME = "Marginalia";

    private static final String DEFAULT_KEY = "public";

    private final String key;

    public MarginaliaSearcher() {
        this(DEFAULT_KEY);
    }

    public MarginaliaSearcher(String key) {
        this.key = key;
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebContent> results = new ArrayList<>();
        var escapedQuery = UrlHelper.encodeParameter(query);
        var endpoint = String.format("https://api.marginalia.nu/%s/search/%s?index=%s&count=%s", key, escapedQuery, 1,
                resultCount);
        try {
            var result = HttpRetrieverFactory.getHttpRetriever().httpGet(endpoint);
            if (result.errorStatus()) {
                throw new SearcherException("HTTP status " + result.getStatusCode() + ": " + result.getStringContent());
            }
            var jsonResult = new JsonObject(result.getStringContent());
            var jsonResults = jsonResult.getJsonArray("results");
            for (var idx = 0; idx < jsonResults.size(); idx++) {
                var jsonObject = jsonResults.getJsonObject(idx);
                var content = new BasicWebContent.Builder() //
                        .setUrl(jsonObject.getString("url")) //
                        .setTitle(jsonObject.getString("title")) //
                        .setSummary(jsonObject.getString("description")) //
                        .setAdditionalData("quality", jsonObject.getDouble("quality")) //
                        .create();
                results.add(content);
            }
        } catch (HttpException e) {
            throw new SearcherException(e);
        } catch (JsonException e) {
            throw new SearcherException(e);
        }

        return results;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

}
