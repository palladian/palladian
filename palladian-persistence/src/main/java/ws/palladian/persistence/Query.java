package ws.palladian.persistence;

/**
 * <p>
 * A database query consisting of a statement and bound arguments.
 * </p>
 * 
 * @author katz
 */
public interface Query {

    String getSql();

    Object[] getArgs();

}
