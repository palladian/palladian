package ws.palladian.retrieval.ranking.services;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * RankingService implementation to find the number of pins on <a href="http://pinterest.com">Pinterest</a> of a Web
 * page.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class PinterestPins extends AbstractRankingService implements RankingService {

    public static final class PinterestPinsMetaInfo implements RankingServiceMetaInfo<PinterestPins> {
        @Override
        public String getServiceName() {
            return "Pinterest";
        }

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public PinterestPins create(Map<ConfigurationOption<?>, ?> config) {
            return new PinterestPins();
        }

    }

    /** The id of this service. */
    private static final String SERVICE_ID = "pinterest";

    /** The ranking value types of this service **/
    public static final RankingType<Long> PINS = new RankingType<>("pinterestpins", "Pinterest Pins", "The Number of Pins on Pinterest", Long.class);

    /** All available ranking types by {@link PinterestPins}. */
    private static final List<RankingType<?>> RANKING_TYPES = Arrays.asList(PINS);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        String requestUrl = "https://api.pinterest.com/v1/urls/count.json?callback=receiveCount&url=" + UrlHelper.encodeParameter(url);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new RankingServiceException("Encountered HTTP error when accessing \"" + requestUrl + "\".", e);
        }
        if (httpResult.errorStatus()) {
            throw new RankingServiceException("Received HTTP status " + httpResult.getStatusCode());
        }
        String response = httpResult.getStringContent();
        response = response.replace("receiveCount(", "");
        response = response.replaceAll("\\)$", "");
        long pins;
        try {
            JsonObject jsonObject = new JsonObject(response);
            pins = jsonObject.getLong("count");
        } catch (JsonException e) {
            throw new RankingServiceException("Encountered JSON parse error for \"" + response + "\".", e);
        }
        return new Ranking.Builder(this, url).add(PINS, pins).create();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType<?>> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) throws RankingServiceException {
        PinterestPins gpl = new PinterestPins();
        Ranking ranking = gpl.getRanking("http://www.g4tv.com/attackoftheshow/blog/post/712294/punishing-bad-parking-jobs/");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(PinterestPins.PINS) + " pins");
    }

}
