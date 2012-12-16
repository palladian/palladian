package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpRequest.HttpMethod;

/**
 * @author Philipp Katz
 */
public class OAuthUtilTest {

    @Test
    public void testOAuthHelper() {
        // Example data from https://dev.twitter.com/docs/auth/creating-signature#note-lexigraphically

        String baseUrl = "https://api.twitter.com/1/statuses/update.json";
        HttpMethod httpMethod = HttpMethod.POST;
        String consumerKey = "xvz1evFS4wEEPTGEFPHBog";
        String token = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb";
        String consumerSecret = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
        String tokenSecret = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
        OAuthParams oAuthParams = new OAuthParams(consumerKey, consumerSecret, token, tokenSecret);
        HttpRequest httpRequest = new HttpRequest(httpMethod, baseUrl);

        Map<String, String> params = CollectionHelper.newHashMap();

        params.put("status", "Hello Ladies + Gentlemen, a signed OAuth request!");
        params.put("include_entities", "true");
        params.put("oauth_consumer_key", "xvz1evFS4wEEPTGEFPHBog");
        params.put("oauth_nonce", "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg");
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", "1318622958");
        params.put("oauth_token", "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb");
        params.put("oauth_version", "1.0");

        String parameterString = OAuthUtil.createParameterString(params);
        assertEquals(
                "include_entities=true&oauth_consumer_key=xvz1evFS4wEEPTGEFPHBog&oauth_nonce=kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1318622958&oauth_token=370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb&oauth_version=1.0&status=Hello%20Ladies%20%2B%20Gentlemen%2C%20a%20signed%20OAuth%20request%21",
                parameterString);

        String signatureBaseString = OAuthUtil.createSignatureBaseString(httpRequest, params);
        assertEquals(
                "POST&https%3A%2F%2Fapi.twitter.com%2F1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0%26status%3DHello%2520Ladies%2520%252B%2520Gentlemen%252C%2520a%2520signed%2520OAuth%2520request%2521",
                signatureBaseString);

        String signingKey = OAuthUtil.createSigningKey(consumerSecret, tokenSecret);
        assertEquals("kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw&LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE",
                signingKey);

        String signature = OAuthUtil.createSignature(signatureBaseString, signingKey);
        assertEquals("tnnArxj06cWHq44gCs1OSKk/jLY=", signature);

        Map<String, String> params2 = CollectionHelper.newHashMap();
        params2.put("include_entities", "true");
        params2.put("status", "Hello Ladies + Gentlemen, a signed OAuth request!");
        HttpRequest signedHttpRequest = OAuthUtil.createSignedRequest(httpRequest, oAuthParams);
        System.out.println(signedHttpRequest.getHeaders().get("Authorization"));
    }

    @Test
    public void testUrlEncode() {
        assertEquals("Ladies%20%2B%20Gentlemen", OAuthUtil.urlEncode("Ladies + Gentlemen"));
        assertEquals("An%20encoded%20string%21", OAuthUtil.urlEncode("An encoded string!"));
        assertEquals("Dogs%2C%20Cats%20%26%20Mice", OAuthUtil.urlEncode("Dogs, Cats & Mice"));
        assertEquals("%E2%98%83", OAuthUtil.urlEncode("☃"));
    }

}
