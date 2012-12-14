package ws.palladian.retrieval;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpRequest.HttpMethod;

/**
 * Quickndirty OAuth implementation, implemented by Twitter's instructions. Might by used as general OAuth mechanism
 * in the future though.
 * 
 * @author pk
 * @see http://hueniverse.com/oauth/guide/authentication/
 * @see https://dev.twitter.com/docs/auth/authorizing-request
 */
public class OAuthUtil {

    /** https://dev.twitter.com/docs/auth/authorizing-request 
     * @param parameterObject TODO*/
    public static HttpRequest createSignedRequest(HttpRequest httpRequest, OAuthParams parameterObject) {

        Map<String, String> oAuthParams = CollectionHelper.newHashMap();
        oAuthParams.put("oauth_consumer_key", parameterObject.consumerKey);
        oAuthParams.put("oauth_nonce", randomString());
        oAuthParams.put("oauth_signature_method", "HMAC-SHA1");
        oAuthParams.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        oAuthParams.put("oauth_token", parameterObject.token);
        oAuthParams.put("oauth_version", "1.0");

        Map<String, String> allParameters = CollectionHelper.newHashMap();
        allParameters.putAll(httpRequest.getParameters());
        allParameters.putAll(oAuthParams);

        String signatureBaseString = createSignatureBaseString(httpRequest.getMethod(), httpRequest.getUrl(), allParameters);
        String signingKey = createSigningKey(parameterObject.consumerSecret, parameterObject.tokenSecret);
        String signature = createSignature(signatureBaseString, signingKey);
        oAuthParams.put("oauth_signature", signature);

        StringBuilder ret = new StringBuilder();
        ret.append("OAuth ");
        boolean first = true;
        for (String key : oAuthParams.keySet()) {
            if (first) {
                first = false;
            } else {
                ret.append(", ");
            }
            String value = oAuthParams.get(key);
            ret.append(urlEncode(key));
            ret.append('=').append('"');
            ret.append(urlEncode(value));
            ret.append('"');
        }
        
        HashMap<String, String> newHeaders = CollectionHelper.newHashMap();
        newHeaders.putAll(httpRequest.getHeaders());
        newHeaders.put("Authorization", ret.toString());
        return new HttpRequest(httpRequest.getMethod(), httpRequest.getUrl(), newHeaders, httpRequest.getParameters());
    }

    static String createParameterString(Map<String, String> allParameters) {
        SortedMap<String, String> alphabeticallySorted = new TreeMap<String, String>(allParameters);
        StringBuilder parameterCollection = new StringBuilder();
        boolean first = true;
        for (String key : alphabeticallySorted.keySet()) {
            if (first) {
                first = false;
            } else {
                parameterCollection.append('&');
            }
            String value = alphabeticallySorted.get(key);
            parameterCollection.append(urlEncode(key));
            parameterCollection.append('=');
            parameterCollection.append(urlEncode(value));
        }
        return parameterCollection.toString();
    }

    static String createSignatureBaseString(HttpMethod httpMethod, String url, Map<String, String> allParameters) {
        StringBuilder signature = new StringBuilder();
        signature.append(httpMethod.toString().toUpperCase()).append('&');
        signature.append(urlEncode(url)).append('&');
        signature.append(urlEncode(createParameterString(allParameters)));
        return signature.toString();
    }

    private static String randomString() {
        return StringHelper.sha1(String.valueOf(System.currentTimeMillis()));
    }

    /** https://dev.twitter.com/docs/auth/percent-encoding-parameters */
    static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException();
        }
    }

    static String createSigningKey(String consumerSecret, String tokenSecret) {
        StringBuilder ret = new StringBuilder();
        ret.append(urlEncode(consumerSecret));
        ret.append('&');
        if (tokenSecret != null) {
            ret.append(tokenSecret);
        }
        return ret.toString();
    }

    static String createSignature(String signatureBaseString, String signingKey) {
        try {
            SecretKey key = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            byte[] hmacBytes = mac.doFinal(signatureBaseString.getBytes());
            return new String(Base64.encodeBase64(hmacBytes));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        } catch (IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

}
