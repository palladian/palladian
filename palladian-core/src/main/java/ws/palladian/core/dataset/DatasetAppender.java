package ws.palladian.core.dataset;

import ws.palladian.core.Instance;

import java.io.Closeable;

/**
 * An object which allows appendable writing to a dataset. Implements
 * {@link Closeable}; {@link #close()} must be called when all instances have
 * been written to properly close resources.
 *
 * @author Philipp Katz
 */
public interface DatasetAppender extends Closeable {

    void append(Instance instance);

}
