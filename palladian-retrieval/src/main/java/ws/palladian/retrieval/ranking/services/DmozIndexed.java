package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find whether a certain URL has been accepted to DMOZ.
 * </p>
 * 
 * @author David Urbansky
 */
public final class DmozIndexed extends AbstractRankingService implements RankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "DMOZ";

    /** The ranking value types of this service **/
    public static final RankingType DMOZ_INDEXED = new RankingType(SERVICE_ID, "DMOZ Accepted",
            "Whether the page has been accepted to DMOZ.");

    /** All available ranking types by {@link DmozIndexed}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(DMOZ_INDEXED);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);
        int indexed = 0;
        try {
            HttpResult httpGet = retriever.httpGet("http://www.dmoz.org/search?q=" + url);
            String content = new String(httpGet.getContent());
            if (content.contains("small>(1-")) {
                indexed = 1;
            }
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        }
        builder.add(DMOZ_INDEXED, indexed);
        return builder.create();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) throws RankingServiceException {
        DmozIndexed gpl = new DmozIndexed();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://google.com");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(DmozIndexed.DMOZ_INDEXED) + " -> in DMOZ");
    }

}
