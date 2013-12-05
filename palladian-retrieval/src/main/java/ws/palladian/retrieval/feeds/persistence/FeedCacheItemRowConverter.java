package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;

/**
 * @author Sandro Reichert
 * 
 */
public class FeedCacheItemRowConverter implements RowConverter<CachedItem> {

    public static final FeedCacheItemRowConverter INSTANCE = new FeedCacheItemRowConverter();

    private FeedCacheItemRowConverter() {
        // singleton
    }

    @Override
    public CachedItem convert(ResultSet resultSet) throws SQLException {
        CachedItem cachedItem = new CachedItem();
        cachedItem.setId(resultSet.getInt("id"));
        cachedItem.setHash(resultSet.getString("itemHash"));
        cachedItem.setCorrectedPublishDate(resultSet.getTimestamp("correctedPollTime"));
        return cachedItem;
    }

}
