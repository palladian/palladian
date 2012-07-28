package ws.palladian.retrieval;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpRetrieverTest {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(HttpRetrieverTest.class);
    
    private static final int PORT = 8888;
    private HttpServer httpServer;

    @Before
    public void setUp() {
        InetSocketAddress address = new InetSocketAddress(PORT);
        try {
            httpServer = HttpServer.create(address, 0);
        } catch (IOException e) {
            LOGGER.warn("Could not start HTTP test server: " + e.getMessage() + ", skipping test");
            Assume.assumeNoException(e);
        }
        httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
            }
        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();
    }
    
    @After
    public void tearDown() {
        httpServer.stop(0);
    }
    
    @Test(timeout = 2000, expected = HttpException.class)
    public void testGetTimeout() throws HttpException {
        HttpRetriever httpRetriever = new HttpRetriever(1000, 1000, 0, 1);
        httpRetriever.httpGet("http://localhost:8080/");
    }
    
    // TODO add further tests.

}
