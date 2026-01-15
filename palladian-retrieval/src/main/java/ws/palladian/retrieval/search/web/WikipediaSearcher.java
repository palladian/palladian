package ws.palladian.retrieval.search.web;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * Search for <a href="https://www.wikipedia.org">Wikipedia</a> articles with fulltext search.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="https://www.mediawiki.org/wiki/API:Search">MediaWiki API:Search</a>
 * @see <a href="https://de.wikipedia.org/w/api.php">MediaWiki API</a>
 * @see <a href="https://stackoverflow.com/questions/964454/how-to-use-wikipedia-api-if-it-exists">How to use wikipedia
 * api if it exists?</a>
 */
public final class WikipediaSearcher extends AbstractMultifacetSearcher<WebContent> {

    public static final class WikipediaSearcherMetaInfo implements SearcherMetaInfo<WikipediaSearcher, WebContent> {
        @Override
        public String getSearcherName() {
            return NAME;
        }

        @Override
        public String getSearcherId() {
            return "wikipedia";
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
        public WikipediaSearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new WikipediaSearcher();
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://www.mediawiki.org/wiki/API:Search";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for <a href=\"https://www.wikipedia.org\">Wikipedia</a> articles with fulltext search.";
        }
    }

    /**
     * The name of this searcher.
     */
    private static final String NAME = "Wikipedia";

    /**
     * Pattern used for parsing the returned date strings.
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final int MAX_CHUNK_SIZE = 50;

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        List<WebContent> results = new ArrayList<>();
        Long totalCount = null;
        String baseUrl = getBaseUrl(query.getLanguage());

        // fetch in chunks of 50 items, this is maximum size
        for (int offset = 0; offset < query.getResultCount(); offset += MAX_CHUNK_SIZE) {

            JsonObject jsonResult = fetchJsonResponse(query.getText(), baseUrl, offset, MAX_CHUNK_SIZE);

            try {
                if (totalCount == null) {
                    totalCount = jsonResult.queryLong("/query/searchinfo/totalhits");
                }

                JsonArray searchResults = jsonResult.queryJsonArray("/query/search");

                if (searchResults.size() == 0) {
                    break; // no more results
                }

                for (Object result : searchResults) {
                    JsonObject resultItem = (JsonObject) result;
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    String title = resultItem.getString("title");
                    builder.setTitle(title);
                    var summary = StringEscapeUtils
                            .unescapeHtml3(HtmlHelper.stripHtmlTags(resultItem.getString("snippet")));
                    builder.setSummary(summary);
                    builder.setPublished(parseDate(resultItem.getString("timestamp")));
                    builder.setUrl(getPageUrl(baseUrl, title));
                    results.add(builder.create());

                    if (results.size() == query.getResultCount()) {
                        break;
                    }
                }

            } catch (Exception e) {
                throw new SearcherException("JSON parse error: " + e.getMessage(), e);
            }
        }

        return new SearchResults<>(results,totalCount);
    }

    private JsonObject fetchJsonResponse(String query, String baseUrl, int offset, int limit) throws SearcherException {
        String queryUrl = getQueryUrl(baseUrl, query, offset, limit);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(queryUrl);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while accessing \"" + queryUrl + "\": " + e.getMessage(), e);
        }
        String jsonString = httpResult.getStringContent();
        try {
            return new JsonObject(jsonString);
        } catch (JsonException e) {
            throw new SearcherException("JSON parse error while parsing \"" + jsonString + "\": " + e.getMessage(), e);
        }
    }

    private Date parseDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    private String getQueryUrl(String baseUrl, String query, int offset, int limit) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(baseUrl);
        queryBuilder.append("/w/api.php");
        queryBuilder.append("?action=query");
        queryBuilder.append("&list=search");
        queryBuilder.append("&format=json");
        if (offset > 0) {
            queryBuilder.append("&sroffset=").append(offset);
        }
        queryBuilder.append("&srlimit=").append(limit);
        queryBuilder.append("&srsearch=").append(UrlHelper.encodeParameter(query));
        return queryBuilder.toString();
    }

    // build an article URL from an article title; unfortunately, I cannot find any rules from Wikipedia on how to
    // create a link from a article title. I implemented the rules described here:
    // https://stackoverflow.com/a/9354004/388827
    private String getPageUrl(String baseUrl, String pageTitle) {
        String pageUrl = StringHelper.upperCaseFirstLetter(pageTitle);
        pageUrl = pageUrl.replaceAll("\\s+$", "");
        pageUrl = pageUrl.replaceAll("\\s+", "_");
        return baseUrl + "/wiki/" + pageUrl;
    }

    private String getBaseUrl(Language language) {
        switch (language) {
            case GERMAN:
                return "https://de.wikipedia.org";
            case FRENCH:
                return "https://fr.wikipedia.org";
            case ARABIC:
                return "https://ar.wikipedia.org";
            case SPANISH:
                return "https://es.wikipedia.org";
            case PORTUGUESE:
                return "https://pt.wikipedia.org";
            case RUSSIAN:
                return "https://ru.wikipedia.org";
            case PERSIAN:
                return "https://fa.wikipedia.org";
            case ITALIAN:
                return "https://it.wikipedia.org";
            case CHINESE:
                return "https://zh.wikipedia.org";
            default:
                return "https://en.wikipedia.org";
        }
    }

}
