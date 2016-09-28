package ws.palladian.core.dataset;

import java.io.Closeable;

import ws.palladian.core.Instance;

/**
 * An object which allows appendable writing to a dataset. Implements
 * {@link Closeable}; {@link #close()} must be called when all instances have
 * been written to properly close resources.
 * 
 * @author pk
 */
public interface DatasetAppender extends Closeable {

	void append(Instance instance);

}
