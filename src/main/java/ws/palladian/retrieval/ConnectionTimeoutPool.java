package ws.palladian.retrieval;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * The {@link ConnectionTimeoutPool} is responsible for timing out (e. g. disconnecting) {@link HttpURLConnection}s
 * after a specified time interval. This mechanism is maintained by a separate daemon thread. The timeout is necessary,
 * because Sun's {@link HttpURLConnection} implementation does not offer general timeouts for the case, that a server
 * starts sending data, but stops without sending an end signal.
 * 
 * For further information concerning this issue, see the attached links.
 * 
 * @see <a href="http://www.twmacinta.com/myjava/ucon_timeout.php">Sun's URLConnection Cannot Be Reliably Timed Out</a>
 *      (note the box on the right!)
 * 
 * @author Philipp Katz
 * 
 */
public class ConnectionTimeoutPool implements Runnable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ConnectionTimeoutPool.class);

    /** The singleton instance of this class, eager initialization. */
    private static final ConnectionTimeoutPool INSTANCE = new ConnectionTimeoutPool();

    /** Wrapper for {@link HttpURLConnection}s which will be timed out. */
    private class DelayedConnectionTimeout implements Delayed {

        /** The wrapped HttpURLConnection. */
        private HttpURLConnection httpUrlConnection;

        /** Time stamp, when this timeout is due. */
        private long timeout;

        /**
         * Create a new wrapper for the specified {@link HttpURLConnection} with the specified timeout in milliseconds.
         * 
         * @param httpUrlConnection
         * @param timeout
         */
        public DelayedConnectionTimeout(HttpURLConnection httpUrlConnection, long timeout) {
            this.httpUrlConnection = httpUrlConnection;
            this.timeout = System.currentTimeMillis() + timeout;
        }

        public HttpURLConnection getHttpUrlConnection() {
            return httpUrlConnection;
        }

        @Override
        public int compareTo(Delayed o) {
            DelayedConnectionTimeout that = (DelayedConnectionTimeout) o;
            return Long.valueOf(this.getDelay(TimeUnit.MILLISECONDS)).compareTo(that.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long result = timeout - System.currentTimeMillis();
            return unit.convert(result, TimeUnit.MILLISECONDS);
        }

    }

    /** The queue which stores all connections which will be timed out using the specified delay. */
    private DelayQueue<DelayedConnectionTimeout> queue = new DelayQueue<DelayedConnectionTimeout>();

    /** Private constructor. */
    private ConnectionTimeoutPool() {
        Thread thread = new Thread(this, "ConnectionTimeoutPool");
        thread.setDaemon(true);
        thread.start();
    }

    /** Obtain the singleton instance, package access. */
    public static ConnectionTimeoutPool getInstance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        while (true) {
            try {
                DelayedConnectionTimeout delayedConnectionTimeout = queue.take();
                HttpURLConnection httpUrlConnection = delayedConnectionTimeout.getHttpUrlConnection();
                httpUrlConnection.disconnect();
                LOGGER.warn("disconnected : " + httpUrlConnection.getURL() + " ; # left in queue : " + queue.size());
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException : " + e.getMessage());
            }
        }
    }

    /**
     * Add a {@link HttpURLConnection} for connection timeout.
     * 
     * @param httpUrlConnection
     * @param timeout Time interval in milliseconds, after the connection is disconnected.
     */
    public void add(HttpURLConnection httpUrlConnection, long timeout) {
        LOGGER.debug("add : " + httpUrlConnection.getURL());
        queue.add(new DelayedConnectionTimeout(httpUrlConnection, timeout));
    }

    public static void main(String[] args) throws MalformedURLException, IOException {

        ConnectionTimeoutPool ct = ConnectionTimeoutPool.INSTANCE;

        for (int i = 0; i < 25; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            URLConnection urlConnection = new URL("http://example.com/" + i).openConnection();
            ct.add((HttpURLConnection) urlConnection, 10000);
        }

    }

}
