package ws.palladian.retrieval;

import org.junit.Ignore;
import org.junit.Test;
import ws.palladian.helper.UrlHelper;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test for HTTP Retriever running against httpbin.org
 *
 * @author Philipp Katz
 */
public class HttpRetrieverHttpBinTest {
    /**
     * Test for “Invalid cookie header”, “Invalid 'expires' attribute”.
     *
     * See here:
     *
     * https://gitlab.com/palladian/palladian-knime/-/issues/57
     * https://forum.knime.com/t/using-cookie-retrieved-with-http-retriever-node-doesnt-work-in-workflow-however-copy-pasting-exact-same-cookie-from-web-console-does/27646
     * https://issues.apache.org/jira/browse/HTTPCLIENT-1763
     */
    @Test
    public void testCookiesExpiresDate() throws HttpException {
        String cookieValue = "foo=bar; Expires=Mon, 05 Oct 2021 01:48:58 GMT";
        HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, "https://httpbin.org/response-headers?set-cookie=" + UrlHelper.encodeParameter(cookieValue)).create();
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        CookieStore cookieStore = new DefaultCookieStore();
        httpRetriever.setCookieStore(cookieStore);
        HttpResult result = httpRetriever.execute(request);
        assertEquals(200, result.getStatusCode());
        assertEquals(1, cookieStore.getCookies().size());
        Cookie cookie = cookieStore.getCookies().iterator().next();
        assertEquals("foo", cookie.getName());
        assertEquals("bar", cookie.getValue());
        assertEquals("httpbin.org", cookie.getDomain());
        assertEquals("/", cookie.getPath());
    }

    /**
     * Test that the given limit is followed. (also, when it is below the internal
     * buffer size).
     *
     * https://gitlab.com/palladian/palladian-knime/-/issues/41
     */
    @Test
    @Ignore // FIXME https://httpbin.org/ wasn't accessible April 2023 and stopping the build
    public void testFileSizeLimitZero() throws HttpException {
        HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, "https://httpbin.org/bytes/8192").create();
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult result;

        httpRetriever.setMaxFileSize(0);
        result = httpRetriever.execute(request);
        assertEquals(0, result.getContent().length);
        assertTrue(result.getTransferedBytes() < 512);

        httpRetriever.setMaxFileSize(512);
        result = httpRetriever.execute(request);
        assertEquals(512, result.getContent().length);

        httpRetriever.setMaxFileSize(1536);
        result = httpRetriever.execute(request);
        assertEquals(1536, result.getContent().length);

        httpRetriever.setMaxFileSize(-1);
        result = httpRetriever.execute(request);
        assertEquals(8192, result.getContent().length);
    }

    /**
     * In case of redirects, the final HTTP Result must not contain a `Location`
     * headers (this would mean, that an additional redirect should take place).
     * Instead, store all locations which were traversed in a
     * {@link HttpResult#getLocations()} property.
     *
     * https://gitlab.com/palladian/palladian-knime/-/issues/5
     * https://gitlab.com/palladian/palladian-knime/-/issues/6
     */
    @Test
    @Ignore // FIXME https://httpbin.org/ wasn't accessible April 2023 and stopping the build
    public void testRedirects() throws HttpException {
        HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, "https://httpbin.org/status/301").create();
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult result = httpRetriever.execute(request);
        assertEquals(Arrays.asList("https://httpbin.org/status/301", "https://httpbin.org/redirect/1", "https://httpbin.org/get"), result.getLocations());
        assertNull(result.getHeader("location"));
    }
}
