package ws.palladian.persistence;

import java.sql.SQLException;

/**
 * This is a callback interface to be used for handling database results.
 * 
 * @param <T> Type of the objects to be processed, see {@link RowConverter}.
 * @author Philipp Katz
 */
public abstract class ResultCallback<T> {

    /** Whether to contiue looping. */
    private boolean looping = true;

    /**
     * Process one row from the result. Call {@link #breakLoop()} if you want to stop the processing loop.
     * 
     * @param object Current object to be processed.
     * @param number Number in the result set, starting with 1.
     * @throws SQLException
     */
    public abstract void processResult(T object, int number) throws SQLException;

    /**
     * Cancel the callback loop.
     */
    public void breakLoop() {
        looping = false;
    }

    /**
     * Determine, if the loop should continue.
     * 
     * @return
     */
    public boolean isLooping() {
        return looping;
    }

}
