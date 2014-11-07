package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.FeedItem;

public class FeedItemRowConverter implements RowConverter<FeedItem> {

    public static final FeedItemRowConverter INSTANCE = new FeedItemRowConverter();

    private FeedItemRowConverter() {
        // singleton
    }

    @Override
    public FeedItem convert(ResultSet resultSet) throws SQLException {

        FeedItem entry = new FeedItem();

        entry.setId(resultSet.getInt("id"));
        entry.setFeedId(resultSet.getInt("feedId"));
        entry.setTitle(resultSet.getString("title"));
        entry.setLink(resultSet.getString("link"));
        entry.setIdentifier(resultSet.getString("rawId"));
        entry.setPublished(resultSet.getTimestamp("published"));
        entry.setDescription(resultSet.getString("description"));
        entry.setText(resultSet.getString("text"));
        entry.setAdded(resultSet.getTimestamp("added"));
        entry.setAuthors(resultSet.getString("authors"));
        entry.setHash(resultSet.getString("itemHash"));

        return entry;

    }

}
