package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * @author Philipp Katz
 * @since 3.0.0
 */
public final class WikipediaExternalLinks extends AbstractRankingService {

    public static final class WikipediaExternalLinksMetaInfo implements RankingServiceMetaInfo<WikipediaExternalLinks> {

        @Override
        public List<RankingType<?>> getRankingTypes() {
            return RANKING_TYPES;
        }

        @Override
        public String getServiceName() {
            return "Wikipedia";
        }

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public List<ConfigurationOption> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public WikipediaExternalLinks create(Map<ConfigurationOption, ?> config) {
            // TODO - make configurable
            return new WikipediaExternalLinks();
        }

    }

    /** The id of this service. */
    private static final String SERVICE_ID = "wikipedia";

    /** The ranking value types of this service **/
    public static final RankingType<Integer> WIKIPEDIA_LINKS_PAGE = new RankingType<>("wikipedia_exturl_page",
            "Wikipedia External page URLs", "The Number of External URLs on the English Wikipedia", Integer.class);
    public static final RankingType<Integer> WIKIPEDIA_LINKS_DOMAIN = new RankingType<>("wikipedia_exturl_domain",
            "Wikipedia External domain URLs", "The Number of External URLs to the domain on the English Wikipedia",
            Integer.class);

    /** All available ranking types by {@link PinterestPins}. */
    private static final List<RankingType<?>> RANKING_TYPES = Arrays.asList(//
            WIKIPEDIA_LINKS_PAGE, //
            WIKIPEDIA_LINKS_DOMAIN //
    );

    private final Language lang;

    public WikipediaExternalLinks() {
        this(Language.ENGLISH);
    }

    public WikipediaExternalLinks(Language lang) {
        this.lang = Objects.requireNonNull(lang);
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        // https://en.wikipedia.org/wiki/Help:Linksearch#:~:text=Special%3ALinksearch%20is%20a%20special,the%20link%20on%20that%20page.
        // TODO - allow to query other media wiki platforms
        // TODO - support other languages
        try {
            var builder = new Ranking.Builder(this, url);
            var numMatchesPage = retrieveRanking(url);
            builder.add(WIKIPEDIA_LINKS_PAGE, numMatchesPage);
            var numMatchesDomain = retrieveRanking(UrlHelper.getDomain(url));
            builder.add(WIKIPEDIA_LINKS_DOMAIN, numMatchesDomain);
            return builder.create();
        } catch (HttpException | JsonException e) {
            throw new RankingServiceException(e);
        }
    }

    private int retrieveRanking(String url) throws HttpException, RankingServiceException, JsonException {
        var urlWithoutProtocol = url.replaceAll("https?:\\/\\/", "");
        var apiUrl = "https://" + lang.getIso6391() + ".wikipedia.org/w/api.php?action=query&list=exturlusage&euquery="
                + urlWithoutProtocol + "&eulimit=500&eunamespace=0&format=json";
        var result = retriever.httpGet(apiUrl);
        if (result.errorStatus()) {
            throw new RankingServiceException("Encountered status " + result.getStatusCode());
        }
        var jsonResult = new JsonObject(result.getStringContent());
        // we can query for max 500 results (see eulimit),
        // thus this is in range 0 ... 500
        var numMatches = jsonResult.queryJsonArray("query/exturlusage").size();
        return numMatches;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType<?>> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] args) throws RankingServiceException {
        var ranking = new WikipediaExternalLinks().getRanking(Arrays.asList(
                "https://www.sueddeutsche.de/wirtschaft/entwurf-fuer-ceta-verhandlungen-ueber-freihandelsabkommen-mit-kanada-abgeschlossen-1.2078844", //
                "https://www.sueddeutsche.de/", //
                "http://palladian.ws", //
                "https://lineupr.com", //
                "https://lineupr.com/en/workflow", //
                "https://nodepit.com" //
        ));
        CollectionHelper.print(ranking.values());
    }

}
