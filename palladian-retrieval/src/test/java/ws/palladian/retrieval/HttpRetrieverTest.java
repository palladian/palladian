package ws.palladian.retrieval;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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

@SuppressWarnings("restriction")
public class HttpRetrieverTest {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRetrieverTest.class);

    private HttpServer httpServer;

    private int port;

    @Before
    public void setUp() {
        port = pickPort();
        InetSocketAddress address = new InetSocketAddress(port);
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

    private int pickPort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @After
    public void tearDown() {
        httpServer.stop(0);
    }

    @Test(timeout = 5000, expected = HttpException.class)
    public void testGetTimeout() throws HttpException {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        httpRetriever.setConnectionTimeout(1000);
        httpRetriever.setSocketTimeout(1000);
        httpRetriever.setNumRetries(0);
        httpRetriever.httpGet("http://localhost:" + port + "/");
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

    /** See: https://bitbucket.org/palladian/palladian/issue/286/possibility-to-accept-cookies-in */
    @Test
    @Ignore
    public void testCookies_issue286() throws HttpException {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        String url = "http://arc.aiaa.org/action/showPublications?pubType=meetingProc";
        HttpResult httpResult = httpRetriever.httpGet(url);
        System.out.println(httpResult.getStatusCode()); // is 200 :/
        System.out.println(httpResult.getStringContent()); // gives a description, that we need to enable cookies
    }

}
