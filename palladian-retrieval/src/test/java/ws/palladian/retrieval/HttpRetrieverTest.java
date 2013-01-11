package ws.palladian.retrieval;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpRetrieverTest {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRetrieverTest.class);
    
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
    
    @Test(timeout = 5000, expected = HttpException.class)
    @Ignore // for now.
    public void testGetTimeout() throws HttpException {
        HttpRetriever httpRetriever = new HttpRetriever(1000, 1000, 0, 1);
        httpRetriever.httpGet("http://localhost:" + PORT + "/");
    }

    /** See : https://bitbucket.org/palladian/palladian/issue/133/redirecting-throws-unexpected-error */
    @Test
    @Ignore
    public void testRedirects_issue133() throws Exception {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        String url = "http://feedproxy.google.com/~r/pwcooks/~3/iqsvvvykjpo/";
        List<String> redirectUrls = httpRetriever.getRedirectUrls(url);
        CollectionHelper.print(redirectUrls);
    }

    /** See: https://dev.twitter.com/docs/tco-redirection-behavior */
    @Test
    @Ignore
    public void testRedirects_tco() throws Exception {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        List<String> urls = httpRetriever.getRedirectUrls("http://t.co/RGJg5LjK");
        CollectionHelper.print(urls);
    }
    
    // TODO add further tests.

}
