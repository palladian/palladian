package ws.palladian.extraction.location.sources.importers;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.h2.jdbcx.JdbcDataSource;

import ws.palladian.persistence.BatchDataProvider;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.OneColumnRowConverter;
import ws.palladian.persistence.RowConverter;

public class H2MapIntegerString extends DatabaseManager implements Map<Integer, String> {

    private static final String ADDITIONAL_OPTIONS = ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0";

    public H2MapIntegerString(JdbcDataSource dataSource) {
        super(dataSource);
        runUpdate("CREATE TABLE IF NOT EXISTS map (key INTEGER, value VARCHAR(255), PRIMARY KEY (key))");
        runUpdate("CREATE INDEX IF NOT EXISTS index_value ON map(value)");
    }

    public static H2MapIntegerString open(File file) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:" + file.getAbsolutePath() + ADDITIONAL_OPTIONS);
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return new H2MapIntegerString(dataSource);
    }

    @Override
    public int size() {
        return runSingleQuery(OneColumnRowConverter.INTEGER, "SELECT COUNT(*) FROM map");
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object paramObject) {
        return get(paramObject) != null;
    }

    @Override
    public boolean containsValue(Object paramObject) {
        return runSingleQuery(OneColumnRowConverter.INTEGER, "SELECT COUNT(*) FROM map WHERE value = ?", paramObject) > 0;
    }

    @Override
    public String get(Object paramObject) {
        return runSingleQuery(OneColumnRowConverter.STRING, "SELECT value FROM map WHERE key = ?", paramObject);
    }

    @Override
    public String put(Integer paramK, String paramV) {
        String oldValue = get(paramK);
        runInsertReturnId("MERGE INTO map KEY(key) VALUES(?, ?)", paramK, paramV);
        return oldValue;
    }

    @Override
    public String remove(Object paramObject) {
        String oldValue = get(paramObject);
        runUpdate("DELETE FROM map WHERE key = ?", paramObject);
        return oldValue;
    }

    @Override
    public void putAll(final Map<? extends Integer, ? extends String> paramMap) {
        final Iterator<? extends Integer> keyIterator = paramMap.keySet().iterator();
        runBatchInsert("INSERT INTO map SET key = ?, value = ?", new BatchDataProvider() {
            @Override
            public void insertedItem(int number, int generatedId) {
            }

            @Override
            public List<? extends Object> getData(int number) {
                Integer key = keyIterator.next();
                String value = paramMap.get(key);
                return Arrays.<Object> asList(key, value);
            }

            @Override
            public int getCount() {
                return paramMap.size();
            }
        });
    }

    @Override
    public void clear() {
        runUpdate("TRUNCATE TABLE map");
    }

    @Override
    public Set<Integer> keySet() {
        return new HashSet<Integer>(runQuery(OneColumnRowConverter.INTEGER, "SELECT key FROM map"));
    }

    @Override
    public Collection<String> values() {
        return runQuery(OneColumnRowConverter.STRING, "SELECT value FROM map");
    }

    @Override
    public Set<Entry<Integer, String>> entrySet() {
        List<Entry<Integer, String>> list = runQuery(new RowConverter<Entry<Integer, String>>() {
            @Override
            public Entry<Integer, String> convert(ResultSet resultSet) throws SQLException {
                int key = resultSet.getInt("key");
                String value = resultSet.getString("value");
                return new EntryImpl<Integer, String>(key, value);
            }
        }, "SELECT * FROM map");
        return new HashSet<Entry<Integer, String>>(list);
    }

    private static final class EntryImpl<K, V> implements Entry<K, V> {

        private final K key;
        private final V value;

        EntryImpl(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return String.format("{%s=%s}", key, value);
        }

    }

}
