package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import ws.palladian.retrieval.OAuthParams.SignatureMethod;

/**
 * @author Philipp Katz
 */
public class OAuthUtilTest {
    // Example data from https://dev.twitter.com/docs/auth/creating-signature#note-lexigraphically
    static final private String BASE_URL = "https://api.twitter.com/1/statuses/update.json";
    static final private HttpMethod HTTP_METHOD = HttpMethod.POST;
    static final private String CONSUMER_KEY = "xvz1evFS4wEEPTGEFPHBog";
    static final private String TOKEN = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb";
    static final private String CONSUMER_SECRET = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
    static final private String TOKEN_SECRET = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
    static final private List<Pair<String, String>> PARAMS = new ArrayList<>();
    static {
        PARAMS.add(Pair.of("status", "Hello Ladies + Gentlemen, a signed OAuth request!"));
        PARAMS.add(Pair.of("include_entities", "true"));
        PARAMS.add(Pair.of("oauth_consumer_key", CONSUMER_KEY));
        PARAMS.add(Pair.of("oauth_nonce", "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg"));
        PARAMS.add(Pair.of("oauth_signature_method", "HMAC-SHA1"));
        PARAMS.add(Pair.of("oauth_timestamp", "1318622958"));
        PARAMS.add(Pair.of("oauth_token", TOKEN));
        PARAMS.add(Pair.of("oauth_version", "1.0"));
    }
    static final private HttpRequest2 HTTP_REQUEST = new HttpRequest2Builder(HTTP_METHOD, BASE_URL).create();
    static final private OAuthParams OAUTH_PARAMS = new OAuthParams(CONSUMER_KEY, CONSUMER_SECRET, TOKEN, TOKEN_SECRET);
    static final private OAuthUtil OATH_UTIL_UNDER_TEST = new OAuthUtil(OAUTH_PARAMS) {
        @Override
        String createRandomString() {
            return "e49dc9f83c9dfdded86e691e2cbff366923e6721";
        }

        @Override
        String createTimestamp() {
            return "1436207905";
        }
    };

    private static final String EXPECTED_PARAMETER_STRING = "include_entities=true&oauth_consumer_key=xvz1evFS4wEEPTGEFPHBog&oauth_nonce=kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1318622958&oauth_token=370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb&oauth_version=1.0&status=Hello%20Ladies%20%2B%20Gentlemen%2C%20a%20signed%20OAuth%20request%21";
    private static final String EXPECTED_SIGNATURE_BASE_STRING = "POST&https%3A%2F%2Fapi.twitter.com%2F1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0%26status%3DHello%2520Ladies%2520%252B%2520Gentlemen%252C%2520a%2520signed%2520OAuth%2520request%2521";
    private static final String EXPECTED_SIGNING_KEY = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw&LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
    private static final String EXPECTED_AUTHORIZATION_PARAM = "OAuth oauth_consumer_key=\"xvz1evFS4wEEPTGEFPHBog\", oauth_nonce=\"e49dc9f83c9dfdded86e691e2cbff366923e6721\", oauth_signature=\"Ua5kSGsxloL%2B4yyslqibhcDsY5Y%3D\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"1436207905\", oauth_token=\"370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb\", oauth_version=\"1.0\"";

    @Test
    public void testCreateParameterString() {
        String parameterString = OAuthUtil.createParameterString(PARAMS);
        assertEquals(EXPECTED_PARAMETER_STRING, parameterString);
    }

    @Test
    public void testCreateSignatureBaseString() {
        String signatureBaseString = OAuthUtil.createSignatureBaseString(HTTP_METHOD, BASE_URL, PARAMS);
        assertEquals(EXPECTED_SIGNATURE_BASE_STRING, signatureBaseString);
    }

    @Test
    public void testCreateSigningKey() {
        String signingKey = OAuthUtil.createSigningKey(CONSUMER_SECRET, TOKEN_SECRET);
        assertEquals(EXPECTED_SIGNING_KEY, signingKey);
    }

    @Test
    public void testCreateSignature_HMAC_SHA1() {
        String signatureBaseString = OAuthUtil.createSignatureBaseString(HTTP_METHOD, BASE_URL, PARAMS);
        String signingKey = OAuthUtil.createSigningKey(CONSUMER_SECRET, TOKEN_SECRET);
        String signature = OAuthUtil.createSignature(signatureBaseString, signingKey, SignatureMethod.HMAC_SHA1);
        assertEquals("tnnArxj06cWHq44gCs1OSKk/jLY=", signature);
    }

    @Test
    public void testCreateAuthorization() {
        String authorizationParam = OATH_UTIL_UNDER_TEST.createAuthorization(HTTP_METHOD, BASE_URL, null);
        assertEquals(EXPECTED_AUTHORIZATION_PARAM, authorizationParam);
    }

    @Test
    public void testCreateSignedRequest() {
        HttpRequest2 signedRequest = OATH_UTIL_UNDER_TEST.createSignedRequest(HTTP_REQUEST);
        assertEquals(BASE_URL, signedRequest.getUrl());
        assertEquals(HTTP_METHOD, signedRequest.getMethod());
        assertEquals(1, signedRequest.getHeaders().size());
        assertEquals(EXPECTED_AUTHORIZATION_PARAM, signedRequest.getHeaders().get("Authorization"));
    }

    @Test
    public void testUrlEncode() {
        assertEquals("Ladies%20%2B%20Gentlemen", OAuthUtil.urlEncode("Ladies + Gentlemen"));
        assertEquals("An%20encoded%20string%21", OAuthUtil.urlEncode("An encoded string!"));
        assertEquals("Dogs%2C%20Cats%20%26%20Mice", OAuthUtil.urlEncode("Dogs, Cats & Mice"));
        assertEquals("%E2%98%83", OAuthUtil.urlEncode("☃"));
    }

}
