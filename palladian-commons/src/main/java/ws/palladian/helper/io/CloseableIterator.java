package ws.palladian.helper.io;

import java.io.Closeable;
import java.util.Iterator;

/**
 * <p>
 * Iterator with a {@link #close()} method, for closing wrapped streams etc. Implementations should act as following: In
 * case the data has been iterated completely (i.e. {@link #hasNext()} returns <code>false</code>), the {@link #close()}
 * method should be invoked automatically. The manual {@link #close()} is intended for cases, where the iteration is
 * stopped in between. <b>Important:</b> Closing is a must in order to avoid resources leaking!
 * </p>
 * 
 * @author pk
 * @param <T>
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

}
