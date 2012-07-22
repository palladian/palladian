package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p>
 * A {@link RowConverter} is responsible for transforming {@link ResultSet}s to specific types. Implementations perform
 * the conversion by implementing {@link #convert(ResultSet)}.
 * </p>
 * 
 * @param <T> Type of the objects to be processed.
 * @author Philipp Katz
 */
public interface RowConverter<T> {

    /**
     * <p>
     * Convert one row to the specified type. <b>Attention:</b> Only perform conversion operations here, do <b>not</b>
     * use methods like {@link ResultSet#next()}, or {@link ResultSet#close()}.
     * </p>
     * 
     * @param resultSet The {@link ResultSet} to process.
     * @return The mapped object.
     * @throws SQLException
     */
    T convert(ResultSet resultSet) throws SQLException;

}
