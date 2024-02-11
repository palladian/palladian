package ws.palladian.retrieval.search.web;

import com.squareup.okhttp.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.*;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Using the unofficial Qwant API without limits.
 *
 * See https://api.qwant.com/api/suggest/?q=shre&client=opensearch
 *
 * @author David Urbansky
 * @since 18.05.2019
 */
public final class QwantSearcher extends AbstractMultifacetSearcher<WebContent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(QwantSearcher.class);

    /**
     * The name of this WebSearcher.
     */
    private static final String SEARCHER_NAME = "Qwant";

    private final HttpRetriever retriever;

    /**
     * <p>
     * Creates a new Qwant Searcher.
     * </p>
     */
    public QwantSearcher() {
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        List<WebContent> results = new ArrayList<>();
        Long resultCount = null;

        // Qwant gives chunks of max. 10 items, and allows 10 chunks, i.e. max. 100 results.
        double numChunks = Math.min(10, Math.ceil((double) query.getResultCount() / 10));

        for (int start = 0; start <= numChunks; start += 10) {
            String searchUrl = createRequestUrl(query.getText(), start, Math.min(10, query.getResultCount() - results.size()), query.getLanguage());
            LOGGER.debug("Search with URL " + searchUrl);

            OkHttpClient okHttpClient = new OkHttpClient();

            // set headers
            okHttpClient.interceptors().add(chain -> chain.proceed(chain.request().newBuilder().addHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").addHeader("Accept",
                    "application/json, text/plain, */*").addHeader("Accept-Language", "en-US,en;q=0.9").addHeader("Origin",
                    "https://www.qwant.com").build()));

            String jsonString;
            try {
                jsonString = okHttpClient.newCall(new com.squareup.okhttp.Request.Builder().url(searchUrl).build()).execute().body().string();
            } catch (IOException e) {
                throw new SearcherException("Search request failed.");
            }

            if (StringUtils.isBlank(jsonString)) {
                throw new SearcherException("JSON response is empty.");
            }
            JsonObject responseJson;
            try {
                responseJson = JsonObject.tryParse(jsonString);
                checkError(responseJson);
                List<WebContent> current = parse(responseJson);
                if (current.isEmpty()) {
                    break;
                }
                results.addAll(current);
                if (resultCount == null) {
                    resultCount = parseResultCount(responseJson);
                }
            } catch (Exception e) {
                throw new SearcherException("Error parsing the response from URL \"" + searchUrl + "\" (JSON was: \"" + jsonString + "\"): " + e.getMessage(), e);
            }
        }

        return new SearchResults<>(results, resultCount);
    }

    private String createRequestUrl(String query, int offset, int num, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://api.qwant.com/v3/search/web");
        urlBuilder.append("?count=").append(num);
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&tgp=1");
        urlBuilder.append("&safesearch=0");
        urlBuilder.append("&uiv=4");

        if (language != null) {
            urlBuilder.append("&locale=").append(getLanguageCode(language));
        }
        return urlBuilder.toString();
    }

    /**
     * @param language Palladian language.
     * @return Language identifier
     * @see https://github.com/NLDev/qwant-api/blob/master/lib/langs.js
     */
    private String getLanguageCode(Language language) {
        switch (language) {
            case BULGARIAN:
                return "bg_bg";
            case CATALAN:
                return "ca_ca";
            case CZECH:
                return "cs_cs";
            case DANISH:
                return "da_da";
            case GERMAN:
                return "de_de";
            case GREEK:
                return "el_el";
            case ENGLISH:
                return "en_us";
            case SPANISH:
                return "es_es";
            case ESTONIAN:
                return "et_et";
            case FINNISH:
                return "fi_fi";
            case FRENCH:
                return "fr_fr";
            case HUNGARIAN:
                return "hu_hu";
            case ITALIAN:
                return "it_it";
            case JAPANESE:
                return "ja_ja";
            case MALAY:
                return "ms_ms";
            case HEBREW:
                return "he_he";
            case KOREAN:
                return "ko_ko";
            case THAI:
                return "th_th";
            case DUTCH:
                return "nl_nl";
            case NORWEGIAN:
                return "no_no";
            case POLISH:
                return "pl_pl";
            case PORTUGUESE:
                return "pt_pt";
            case ROMANIAN:
                return "ro_ro";
            case RUSSIAN:
                return "ru_ru";
            case SWEDISH:
                return "sv_sv";
            case TURKISH:
                return "tr_tr";
            case WELSH:
                return "cy_cy";
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    static void checkError(JsonObject jsonObject) throws SearcherException {
        Integer errorCode = jsonObject.tryQueryInt("data/error_code");
        if (errorCode != null) {
            throw new SearcherException("Error from Qwant API: " + errorCode);
        }
    }

    /**
     * default visibility for unit testing.
     */
    static List<WebContent> parse(JsonObject jsonObject) {
        JsonArray mainlines = jsonObject.tryQueryJsonArray("data/result/items/mainline");
        // get the one with type == web
        Optional<Object> first = mainlines.stream().filter(o -> ((JsonObject) o).tryGetString("type").equals("web")).findFirst();
        if (!first.isPresent()) {
            return Collections.emptyList();
        }
        JsonObject webMainline = (JsonObject) first.get();
        JsonArray jsonItems = webMainline.tryQueryJsonArray("items");
        if (jsonItems == null) {
            LOGGER.warn("JSON result did not contain an 'items' property. (JSON = '" + jsonObject.toString() + "'.");
            return Collections.emptyList();
        }
        List<WebContent> result = new ArrayList<>();
        for (int i = 0; i < jsonItems.size(); i++) {
            JsonObject jsonItem = jsonItems.tryGetJsonObject(i);
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setTitle(jsonItem.tryGetString("title"));
            builder.setUrl(jsonItem.tryGetString("url"));
            builder.setSummary(jsonItem.tryGetString("desc"));
            builder.setSource(SEARCHER_NAME);
            result.add(builder.create());
        }
        return result;
    }

    static long parseResultCount(JsonObject jsonObject) {
        return jsonObject.tryQueryInt("data/result/total");
    }
}
