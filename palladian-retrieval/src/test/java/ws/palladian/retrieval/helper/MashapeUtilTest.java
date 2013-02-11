package ws.palladian.retrieval.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MashapeUtilTest {

    @Test
    public void testGenerateAuthorization() throws Exception {
        String publicKey = "u3ewnlzvxvbg3gochzqcrulimgngsb";
        String privateKey = "dxkyimj8rjoyti1mqx2lqragbbg71k";
        String sha1hmac = MashapeUtil.generateMashapeHeader(publicKey, privateKey);
        assertEquals(
                "dTNld25senZ4dmJnM2dvY2h6cWNydWxpbWduZ3NiOjFmN2YzMDBhNjE1YTNhZDU0YjNmYWY3NGMzZDhlZjM0ZjBlMDhiMzU=",
                sha1hmac);
    }

}
