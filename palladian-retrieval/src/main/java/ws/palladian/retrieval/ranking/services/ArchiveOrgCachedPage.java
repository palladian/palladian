package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ws.palladian.helper.UrlHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

public class ArchiveOrgCachedPage extends AbstractRankingService {

    public static final class ArchiveOrgCachedPageMetaInfo implements RankingServiceMetaInfo<ArchiveOrgCachedPage> {
        @Override
        public String getServiceName() {
            return "Archive.org Wayback Machine";
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
        public ArchiveOrgCachedPage create(Map<ConfigurationOption<?>, ?> config) {
            return new ArchiveOrgCachedPage();
        }

        @Override
        public String getServiceDocumentationUrl() {
            return "https://archive.org/help/wayback_api.php";
        }

        @Override
        public String getServiceDescription() {
            return "Find whether a certain URL has been cached by Archive.org’s Wayback Machine. "
                    + "Note that API is not highly reliable at times and might then return false negative results.";
        }
    }

    private static final String SERVICE_ID = "archive.org";

    public static final RankingType<Short> ARCHIVE_ORG_CACHED = new RankingType<>("wayback_machine_cached",
            "Wayback Machine Cached", "Whether the page is cached in Archive.org’s Wayback Machine", Short.class);
    public static final RankingType<Long> ARCHIVE_ORG_TIMESTAMP = new RankingType<>("wayback_machine_timestamp",
            "Wayback Machine Timestamp", "Timestamp of the closest snapshot on Archive.org’s Wayback Machine",
            Long.class);

    private static final List<RankingType<?>> RANKING_TYPES = Arrays.asList(ARCHIVE_ORG_CACHED, ARCHIVE_ORG_TIMESTAMP);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        try {
            var requestUrl = String.format("http://archive.org/wayback/available?url=%s",
                    UrlHelper.encodeParameter(url));
            var httpResult = retriever.httpGet(requestUrl);
            if (httpResult.errorStatus()) {
                throw new RankingServiceException("Encountered HTTP status " + httpResult.getStatusCode());
            }
            var jsonObject = new JsonObject(httpResult.getStringContent());
            var archivedSnapshots = jsonObject.getJsonObject("archived_snapshots");
            var closest = archivedSnapshots.getJsonObject("closest");
            boolean available = false;
            Long timestamp = null;
            if (closest != null) {
                available = closest.getBoolean("available");
                timestamp = closest.getLong("timestamp");
            }
            var builder = new Ranking.Builder(this, url);
            builder.add(ARCHIVE_ORG_CACHED, (short) (available ? 1 : 0));
            builder.add(ARCHIVE_ORG_TIMESTAMP, timestamp);
            return builder.create();
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        } catch (JsonException e) {
            throw new RankingServiceException(e);
        }
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType<?>> getRankingTypes() {
        return RANKING_TYPES;
    }

}
