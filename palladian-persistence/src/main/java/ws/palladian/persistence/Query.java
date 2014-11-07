package ws.palladian.persistence;

/**
 * <p>
 * A database query consisting of a statement and bound arguments.
 * </p>
 * 
 * @author katz
 */
public interface Query {

    /**
     * @return The SQL query. The query may contain SQL place holders.
     */
    String getSql();

    /**
     * @return Arguments for all place holders in the query. In case, no place holders are set, return an empty array.
     */
    Object[] getArgs();

}
