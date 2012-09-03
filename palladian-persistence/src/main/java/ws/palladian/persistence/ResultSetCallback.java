package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p>
 * A {@link ResultCallback}, just providing the pure {@link ResultSet} from the database query.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class ResultSetCallback extends ResultCallback<ResultSet> {

    @Override
    public abstract void processResult(ResultSet resultSet, int number) throws SQLException;
}
