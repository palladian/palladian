package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        private static final StringListConfigurationOption LANGUAGE_OPTION = new StringListConfigurationOption(
                "Language Codes", "language_codes");

        @Override
        public String getServiceName() {
            return "Wikipedia";
        }

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(LANGUAGE_OPTION);
        }

        @Override
        public WikipediaExternalLinks create(Map<ConfigurationOption<?>, ?> config) {
            var languages = LANGUAGE_OPTION.get(config);
            var langObjs = languages //
                    .stream() //
                    .map(Language::getByIso6391) //
                    .filter(v -> v != null) // remove languages which do not exist
                    .collect(Collectors.toList());
            return new WikipediaExternalLinks(langObjs);
        }

    }

    /** The id of this service. */
    private static final String SERVICE_ID = "wikipedia";

    private final List<Language> langs;

    public WikipediaExternalLinks() {
        this(Collections.singletonList(Language.ENGLISH));
    }

    public WikipediaExternalLinks(List<Language> langs) {
        this.langs = Objects.requireNonNull(langs);
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        // https://en.wikipedia.org/wiki/Help:Linksearch#:~:text=Special%3ALinksearch%20is%20a%20special,the%20link%20on%20that%20page.
        // TODO - allow to query other media wiki platforms
        try {
            var builder = new Ranking.Builder(this, url);
            for (var lang : langs) {
                var numMatchesPage = retrieveRanking(url, lang);
                builder.add(rankingTypePage(lang), numMatchesPage);
                var numMatchesDomain = retrieveRanking(UrlHelper.getDomain(url), lang);
                builder.add(rankingTypeDomain(lang), numMatchesDomain);
            }
            return builder.create();
        } catch (HttpException | JsonException e) {
            throw new RankingServiceException(e);
        }
    }

    private int retrieveRanking(String url, Language lang)
            throws HttpException, RankingServiceException, JsonException {
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
        List<RankingType<?>> rankingTypes = new ArrayList<>();
        for (var lang : langs) {
            rankingTypes.add(rankingTypePage(lang));
            rankingTypes.add(rankingTypeDomain(lang));
        }
        return rankingTypes;
    }

    private static RankingType<Integer> rankingTypeDomain(Language lang) {
        return new RankingType<>("wikipedia_exturl_domain_" + lang.getIso6391(),
                "Wikipedia External domain URLs " + lang.getName(),
                "The Number of External URLs to the domain on the " + lang.getName() + " Wikipedia", Integer.class);
    }

    private static RankingType<Integer> rankingTypePage(Language lang) {
        return new RankingType<>("wikipedia_exturl_page_" + lang.getIso6391(),
                "Wikipedia External page URLs for " + lang.getName(),
                "The Number of External URLs on the " + lang.getName() + " Wikipedia", Integer.class);
    }

    public static void main(String[] args) throws RankingServiceException {
        var langs = Arrays.asList(Language.ENGLISH, Language.GERMAN);
        var ranking = new WikipediaExternalLinks(langs).getRanking(Arrays.asList(
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
