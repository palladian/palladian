package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p>
 * A set of predefined {@link RowConverter}s which retrieve one column from the result of the desired type. The
 * converters are singletons, their instance can be retrieved using the constants, e.g. {@link #STRING}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class OneColumnRowConverter {

    private OneColumnRowConverter() {
        // prevent instances.
    }

    /**
     * <p>
     * A {@link RowConverter} for {@link Boolean} types.
     * </p>
     */
    public final static RowConverter<Boolean> BOOLEAN = new RowConverter<Boolean>() {
        @Override
        public Boolean convert(ResultSet resultSet) throws SQLException {
            return resultSet.getBoolean(1);
        }
    };

    /**
     * <p>
     * A {@link RowConverter} for {@link Integer} types.
     * </p>
     */
    public final static RowConverter<Integer> INTEGER = new RowConverter<Integer>() {
        @Override
        public Integer convert(ResultSet resultSet) throws SQLException {
            return resultSet.getInt(1);
        }
    };

    /**
     * <p>
     * A {@link RowConverter} for {@link Double} types.
     * </p>
     */
    public final static RowConverter<Double> DOUBLE = new RowConverter<Double>() {
        @Override
        public Double convert(ResultSet resultSet) throws SQLException {
            return resultSet.getDouble(1);
        }
    };

    /**
     * <p>
     * A {@link RowConverter} for {@link Long} types.
     * </p>
     */
    public final static RowConverter<Long> LONG = new RowConverter<Long>() {
        @Override
        public Long convert(ResultSet resultSet) throws SQLException {
            return resultSet.getLong(1);
        }
    };

    /**
     * <p>
     * A {@link RowConverter} for {@link String} types.
     * </p>
     */
    public final static RowConverter<String> STRING = new RowConverter<String>() {
        @Override
        public String convert(ResultSet resultSet) throws SQLException {
            return resultSet.getString(1);
        }
    };

}
