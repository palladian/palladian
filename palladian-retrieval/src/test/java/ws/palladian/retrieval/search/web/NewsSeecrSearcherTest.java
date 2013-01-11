package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class NewsSeecrSearcherTest {

    @Test
    public void testGenerateAuthorization() throws Exception {
        String publicKey = "u3ewnlzvxvbg3gochzqcrulimgngsb";
        String privateKey = "dxkyimj8rjoyti1mqx2lqragbbg71k";
        String sha1hmac = NewsSeecrSearcher.generateMashapeHeader(publicKey, privateKey);
        assertEquals(
                "dTNld25senZ4dmJnM2dvY2h6cWNydWxpbWduZ3NiOjFmN2YzMDBhNjE1YTNhZDU0YjNmYWY3NGMzZDhlZjM0ZjBlMDhiMzU=",
                sha1hmac);
    }

    @Test
    public void testParseDate() {
        assertNotNull(NewsSeecrSearcher.parseDate("2013-01-10T00:01:00.000+0000"));
    }

}
