package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * @author Philipp Katz
 * @since 3.0.0
 */
public final class OpenPageRank extends AbstractRankingService {

    private static final String SERVICE_ID = "openpagerank";

    public static final RankingType RANKING_TYPE_PAGE_RANK_INTEGER = new RankingType("page_rank_integer",
            "OPR integer value", "");

    public static final RankingType RANKING_TYPE_PAGE_RANK_DECIMAL = new RankingType("page_rank_decimal",
            "OPR decimal value", "");

    public  static final RankingType RANKING_TYPE_RANK = new RankingType("rank", "OPR rank", "");

    private final String apiKey;

    public OpenPageRank(String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    // TODO - implement batch requests - API supports up to 100 domains / request

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        try {
            var domain = UrlHelper.getDomain(url, false);
            // https://www.domcop.com/openpagerank/documentation
            var apiUrl = "https://openpagerank.com/api/v1.0/getPageRank?domains[]=" + domain;
            var request = new HttpRequest2Builder(HttpMethod.GET, apiUrl).addHeader("API-OPR", apiKey).create();
            var response = retriever.execute(request);
            var responseJson = new JsonObject(response.getStringContent());
            var responseArray = responseJson.getJsonArray("response");
            var responseObject = responseArray.getJsonObject(0);
            var pageRankInteger = responseObject.getInt("page_rank_integer");
            var pageRankDecimal = responseObject.getDouble("page_rank_decimal");
            var rank = responseObject.getInt("rank");
            return new Ranking.Builder(this, url) //
                    .add(RANKING_TYPE_PAGE_RANK_INTEGER, pageRankInteger) //
                    .add(RANKING_TYPE_PAGE_RANK_DECIMAL, pageRankDecimal) //
                    .add(RANKING_TYPE_RANK, rank) //
                    .create();
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        } catch (JsonException e) {
            throw new RankingServiceException(e);
        }
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return Arrays.asList( //
                RANKING_TYPE_PAGE_RANK_INTEGER, //
                RANKING_TYPE_PAGE_RANK_DECIMAL, //
                RANKING_TYPE_RANK //
        );
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

}
