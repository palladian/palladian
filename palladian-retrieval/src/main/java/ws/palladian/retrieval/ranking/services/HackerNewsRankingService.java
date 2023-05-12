package ws.palladian.retrieval.ranking.services;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * Ranking Service based on Hacker News via <a href="https://hn.algolia.com/api">HN Search API</a>.
 *
 * @author Philipp Katz
 */
public final class HackerNewsRankingService extends AbstractRankingService {

    private static final String SERVICE_ID = "hackernews";

    public static final RankingType POINTS = new RankingType("points", "Hacker News Points", "The summed points from the first ten hits for the URL on Hacker News.");
    public static final RankingType COMMENTS = new RankingType("comments", "Hacker News Comments",
            "The summed number of comments from the first ten hits for the URL on Hacker News.");
    public static final RankingType HITS = new RankingType("hits", "Hacker News Hits", "The number of hits for the URL on Hacker News.");

    private static final List<RankingType> RANKING_TYPES = Arrays.asList(POINTS, COMMENTS, HITS);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        String requestUrl = String.format("http://hn.algolia.com/api/v1/search?query=%s&restrictSearchableAttributes=url&hitsPerPage=10", UrlHelper.encodeParameter(url));
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new RankingServiceException("Encountered HTTP error", e);
        }
        if (httpResult.errorStatus()) {
            throw new RankingServiceException("HTTP status " + httpResult.getStatusCode() + ".");
        }
        try {
            JsonObject resultObject = new JsonObject(httpResult.getStringContent());
            JsonArray jsonHits = resultObject.getJsonArray("hits");
            long summedPoints = 0;
            long summedComments = 0;
            for (int i = 0; i < jsonHits.size(); i++) {
                JsonObject jsonHit = jsonHits.getJsonObject(i);
                summedPoints += jsonHit.getLong("points");
                summedComments = jsonHit.getLong("num_comments");
            }
            Ranking.Builder rankingBuilder = new Ranking.Builder(this, url);
            rankingBuilder.add(POINTS, summedPoints);
            rankingBuilder.add(COMMENTS, summedComments);
            rankingBuilder.add(HITS, resultObject.getLong("nbHits"));
            return rankingBuilder.create();
        } catch (JsonException e) {
            throw new RankingServiceException("Could not parse JSON response: '" + httpResult.getStringContent() + "'.", e);
        }
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] args) throws RankingServiceException {
        // Ranking for http://apple.com from hackernews: comments=151 points=10618 hits=4298
        // Ranking for https://apple.com from hackernews: comments=114 points=5581 hits=4172
        // Ranking for apple.com from hackernews: comments=492 points=11923 hits=4304
        Ranking result = new HackerNewsRankingService().getRanking("apple.com");
        System.out.println(result);
    }

}
