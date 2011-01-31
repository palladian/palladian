package tud.iir.persistence;

import java.sql.ResultSet;

/**
 * A {@link ResultCallback}, just providing the pure {@link ResultSet} from the database query.
 * 
 * @author Philipp Katz
 */
public abstract class RawResultCallback extends ResultCallback<ResultSet> {

}
