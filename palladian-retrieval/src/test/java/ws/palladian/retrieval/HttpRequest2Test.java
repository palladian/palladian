package ws.palladian.retrieval;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    /**
     * URL which contains a ? but not being used as a query param.
     *
     * See:
     *
     * https://forum.knime.com/t/http-retriever-fails-to-fetch-urls-containing-a-outside-of-the-context-of-query-parameters/28018
     * https://gitlab.com/palladian/palladian-knime/-/issues/61
     */
    @Test
    public void testUrlWithQuestionParsing() {
        String url = "https://www.dutyfree.co.il/בשמים-וקוסמטיקה/Skincare/yves-saint-laurent-top-secrets-cream-4?ml";
        HttpRequest2 request = new HttpRequest2Builder(HttpMethod.GET, url).create();
        assertEquals(url, request.getUrl());
    }

}
