package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A {@link RowConverter} is responsible for transforming {@link ResultSet}s to specific types. Implementations perform
 * the conversion by implementing {@link #convert(ResultSet)}. A simple converter is available as
 * {@link SimpleRowConverter}, but feel free to create your own, more sophisticated mapping implementations.
 * 
 * @param <T> Type of the objects to be processed.
 * @author Philipp Katz
 */
public interface RowConverter<T> {

    /**
     * Convert one row to the specified type. <b>Attention:</b> Only perform conversion operations here, do <b>not</b>
     * use methods like {@link ResultSet#next()}, or {@link ResultSet#close()}.
     * 
     * @param resultSet
     * @return
     * @throws SQLException
     */
    T convert(ResultSet resultSet) throws SQLException;

}
