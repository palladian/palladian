package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.UrlHelper;

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
		HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET,
				"https://httpbin.org/response-headers?set-cookie=" + UrlHelper.encodeParameter(cookieValue)).create();
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

}
