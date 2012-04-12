package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p>
 * A no-operation {@link RowConverter}, just providing the pure {@link ResultSet} from the database query.
 * </p>
 * 
 * @author Philipp Katz
 */
class NopRowConverter implements RowConverter<ResultSet> {

    @Override
    public ResultSet convert(ResultSet resultSet) throws SQLException {
        return resultSet;
    }

}
