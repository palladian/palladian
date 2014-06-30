package ws.palladian.retrieval.search.socialmedia;

import static ws.palladian.helper.constants.Language.CHINESE;
import static ws.palladian.helper.constants.Language.ENGLISH;
import static ws.palladian.helper.constants.Language.JAPANESE;
import static ws.palladian.helper.constants.Language.KOREAN;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for Tweets on <a href="http://topsy.com">Topsy</a>. Topsy has a better archive, so we can search older Tweets.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://code.google.com/p/otterapi/">Topsy REST API</a>
 * @see <a href="http://manage.topsy.com/">Topsy API registration</a>
 */
public final class MultifacetTopsySearcher extends AbstractMultifacetSearcher<WebContent> {

    public static enum ContentType implements Facet {
        IMAGE, TWEET, VIDEO;
        private static final String TOPSY_CONTENT_TYPE = "topsy.contentType";

        @Override
        public String getIdentifier() {
            return TOPSY_CONTENT_TYPE;
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultifacetTopsySearcher.class);

    /** The identifier for the API key when provided via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.topsy.key";

    public static final String SEARCHER_NAME = "Topsy";

    private static final Set<Language> SUPPORTED_LANGUAGES = EnumSet.of(ENGLISH, JAPANESE, CHINESE, KOREAN);

    /** Pattern for extracting the status ID from a twitter URL. */
    private static final Pattern URL_STATUS_PATTERN = Pattern
            .compile("https?://twitter.com/[A-Za-z0-9_]*/status/(\\d+)");

    /** Pattern to determine from the text content, whether the tweet is likely a retweet. */
    private static final Pattern CONTENT_RETWEET_PATTERN = Pattern.compile("(?:^|\\s)RT @[A-Za-z0-9_]+");

    /** Pattern to extract hashtags. */
    private static final Pattern CONTENT_HASHTAG_PATTERN = Pattern.compile("#([A-Za-z0-9]+)");

    private final String apiKey;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new Topsy searcher with the specified API key.
     * </p>
     * 
     * @param apiKey The API key, not <code>null</code> or empty.
     */
    public MultifacetTopsySearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new Topsy searcher with an API key provided by a {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The Configuration providing the required API key via key {@value #CONFIG_API_KEY}, not
     *            <code>null</code>.
     */
    public MultifacetTopsySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }


    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        List<WebContent> webContent = CollectionHelper.newArrayList();
        Long totalResults = null;
        int skippedRetweets = 0;
        out: for (int page = 1;; page++) {
            String queryUrl = buildQueryUrl(query, page, apiKey);
            LOGGER.debug("Request URL = {}", queryUrl);
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(queryUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching with URL \"" + query + "\": " + e.getMessage(),
                        e);
            }
            String jsonString = httpResult.getStringContent();
            LOGGER.debug("JSON = {}", jsonString);
            try {
                JsonObject jsonResult = new JsonObject(jsonString);
                JsonObject responseJson = jsonResult.getJsonObject("response");
                if (totalResults == null) {
                    totalResults = responseJson.getLong("total");
                }
                JsonArray listJson = responseJson.getJsonArray("list");
                if (listJson.size() == 0) {
                    break;
                }
                for (int i = 0; i < listJson.size(); i++) {
                    JsonObject item = listJson.getJsonObject(i);
                    if (isRetweet(item)) {
                        skippedRetweets++;
                        continue;
                    }
                    WebContent webResult = parse(item);
                    webContent.add(webResult);
                    if (webContent.size() == query.getResultCount()) {
                        break out;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the JSON response " + e.getMessage() + ", JSON was \""
                        + jsonString + "\"", e);
            }
        }
        LOGGER.debug("Skipped {} retweets", skippedRetweets);
        return new SearchResults<WebContent>(webContent, totalResults);
    }

    /**
     * Very simple check, to determine, whether the current tweet is a retweet. This will not catch all acutal retweets,
     * as the "RT @bla" is not compulsive, but still will filter out lots of duplicates.
     * 
     * @param item The JSON representing the tweet.
     * @return <code>true</code> in case the tweet is very likely a retweet.
     */
    private boolean isRetweet(JsonObject item) {
        String content = item.tryGetString("content");
        if (content == null) {
            throw new IllegalStateException("content from JSON was null");
        }
        return CONTENT_RETWEET_PATTERN.matcher(content).find();
    }

    private WebContent parse(JsonObject item) throws JsonException {
        System.out.println(item);
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        String permalink = item.tryGetString("trackback_permalink");
        builder.setUrl(permalink);
        String title = item.tryGetString("title");
        if (title != null) {
            builder.setTitle(StringEscapeUtils.unescapeHtml4(title));
        }
        Integer firstpostDate = item.tryGetInt("firstpost_date");
        if (firstpostDate != null && firstpostDate != 0) {
            builder.setPublished(new Date(firstpostDate * 1000l));
        }
        Integer date = item.tryGetInt("date");
        if (date != null && date != 0) {
            builder.setPublished(new Date(date * 1000l));
        }
        String content = item.tryGetString("content");
        if (content != null) {
            builder.setSummary(StringEscapeUtils.unescapeHtml4(content));
        }
        builder.setTags(extractTags(content));
        builder.setIdentifier(extractIdentifier(permalink));
        builder.setSource(SEARCHER_NAME);
        return builder.create();
    }

    /**
     * Extract hashtags from the Tweet's text.
     * 
     * @param content The text.
     * @return {@link Set} with hashtags in the order they occur in the text, or an empty Set.
     */
    static Set<String> extractTags(String content) {
        if (StringUtils.isNotEmpty(content)) {
            Matcher matcher = CONTENT_HASHTAG_PATTERN.matcher(content);
            LinkedHashSet<String> tags = CollectionHelper.newLinkedHashSet();
            while (matcher.find()) {
                tags.add(matcher.group(1));
            }
            return tags;
        }
        return Collections.emptySet();
    }

    /**
     * Extract the Tweet ID from the permalink URL.
     * 
     * @param permalink The parmalink.
     * @return The Tweet ID, or <code>null</code> in case the ID could not be extracted.
     */
    static String extractIdentifier(String permalink) {
        if (StringUtils.isNotEmpty(permalink)) {
            Matcher matcher = URL_STATUS_PATTERN.matcher(permalink);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String buildQueryUrl(MultifacetQuery query, int page, String apiKey) throws SearcherException {
        StringBuilder queryUrl = new StringBuilder();
        if (StringUtils.isNotBlank(query.getText())) {
            queryUrl.append("http://otter.topsy.com/search.json");
            queryUrl.append("?q=").append(UrlHelper.encodeParameter(query.getText()));
            Facet facet = query.getFacet(ContentType.TOPSY_CONTENT_TYPE);
            if (facet != null) {
                ContentType contentTypeFacet = (ContentType)facet;
                queryUrl.append("&type=").append(contentTypeFacet.toString().toLowerCase());
            }
        } else if (StringUtils.isNotBlank(query.getUrl())){
            validateUrl(query.getUrl());
            queryUrl.append("http://otter.topsy.com/trackbacks.json");
            queryUrl.append("?url=").append(query.getUrl());
        }else{
            throw new SearcherException("Either text or URL must be provided for the query.");
        }
        queryUrl.append("&apikey=").append(apiKey);
        queryUrl.append("&page=").append(page);
        queryUrl.append("&perpage=100");
        Language language = query.getLanguage();
        if (language != null && SUPPORTED_LANGUAGES.contains(language)) {
            queryUrl.append("&allow_lang=").append(language.getIso6391());
        }
        if (query.getStartDate() != null) {
            queryUrl.append("&mintime=").append(query.getStartDate().getTime() / 1000);
        }
        if (query.getEndDate() != null) {
            queryUrl.append("&maxtime=").append(query.getEndDate().getTime() / 1000);
        }
        return queryUrl.toString();
    }

    private static void validateUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid parameter, only URLs are supported (was: \"" + url + "\")");
        }
    }

    @Override
    public boolean isDeprecated() {
        return true;
    }

}
