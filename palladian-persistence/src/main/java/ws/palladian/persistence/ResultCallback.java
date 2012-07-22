package ws.palladian.persistence;

import java.sql.SQLException;

/**
 * <p>
 * This is a callback interface to be used for handling database results.
 * </p>
 * 
 * @param <T> Type of the objects to be processed, see {@link RowConverter}.
 * @author Philipp Katz
 */
public abstract class ResultCallback<T> {

    /** Whether to continue looping. */
    private boolean looping = true;

    /**
     * <p>
     * Process one row from the result. Call {@link #breakLoop()} if you want to stop the processing loop.
     * </p>
     * 
     * @param object Current object to be processed.
     * @param number Number in the result set, starting with 1.
     * @throws SQLException
     */
    public abstract void processResult(T object, int number) throws SQLException;

    /**
     * <p>
     * Cancel the callback loop. This closes all open resources.
     * </p>
     */
    public void breakLoop() {
        looping = false;
    }

    /**
     * <p>
     * Determine, if the loop should continue.
     * </p>
     * 
     * @return Indicate, whether to continue processing results. <code>false</code> if loop should be ended.
     */
    public boolean isLooping() {
        return looping;
    }

}
