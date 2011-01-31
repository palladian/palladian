package tud.iir.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A no-operation {@link RowConverter}, just providing the pure {@link ResultSet} from the database query.
 * 
 * @author Philipp Katz
 */
public class NopRowConverter implements RowConverter<ResultSet> {

    @Override
    public ResultSet convert(ResultSet resultSet) throws SQLException {
        return resultSet;
    }

}
