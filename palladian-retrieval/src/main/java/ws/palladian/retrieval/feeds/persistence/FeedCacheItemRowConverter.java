package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import ws.palladian.persistence.RowConverter;

/**
 * @author Sandro Reichert
 * @author pk
 */
public class FeedCacheItemRowConverter implements RowConverter<CachedItem> {

    public static final FeedCacheItemRowConverter INSTANCE = new FeedCacheItemRowConverter();

    private FeedCacheItemRowConverter() {
        // singleton
    }

    @Override
    public CachedItem convert(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String itemHash = resultSet.getString("itemHash");
        Date correctedPollTime = resultSet.getTimestamp("correctedPollTime");
        return new CachedItem(id, itemHash, correctedPollTime);
    }
}
