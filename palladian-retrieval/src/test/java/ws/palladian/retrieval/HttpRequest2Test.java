package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class HttpRequest2Test {
	/**
	 * Trying to request the following URL:
	 * 
	 * http://www.monetas.ch/htm/651/de/Firmen-Suchresultate.htm?Firmensuche=lindt+spr%FCngli&CompanySearchSubmit=1
	 * 
	 * However, actually this URL is effectively requested (see DEBUG log output):
	 * 
	 * >> "GET
	 * /htm/651/de/Firmen-Suchresultate.htm?Firmensuche=lindt+spr%EF%BF%BDngli&CompanySearchSubmit=1
	 * HTTP/1.1[\r][\n]"
	 * 
	 * The problem is, that Palladian's HttpRequest2Builder's constructor parses the
	 * query parameters and always assumes UTF-8 as encoding. However, the encoding
	 * above is actually ISO-blabla. It gets decoded and reencoded and ends up as a
	 * different URL.
	 */
	@Test
	@Ignore
	public void testUrlParsingIssue() {
		String url = "http://www.monetas.ch/htm/651/de/Firmen-Suchresultate.htm?Firmensuche=lindt+spr%FCngli&CompanySearchSubmit=1";
		HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, url).create();
		assertEquals(url, request.getUrl());
	}

}
