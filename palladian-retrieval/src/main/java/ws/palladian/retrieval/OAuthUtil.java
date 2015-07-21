package ws.palladian.retrieval;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Utility for creating OAuth signed {@link HttpRequest}s. Implemented according to Twitter's <a
 * href="https://dev.twitter.com/docs/auth/authorizing-request">instructions</a>, but can be used for general OAuth 1.0
 * signing purposes.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://hueniverse.com/oauth/guide/authentication/">The OAuth 1.0 Guide</a>
 * @see <a href="https://dev.twitter.com/docs/auth/authorizing-request">Twitter: Authorizing a request</a>
 */
public class OAuthUtil {

    private final OAuthParams params;

    public OAuthUtil(OAuthParams params) {
        Validate.notNull(params, "oAuthParams must not be null");
        this.params = params;
    }

    /**
     * <p>
     * Sign the given {@link HttpRequest2} using the specified {@link OAuthParams}. The signed request is returned as
     * new instance. After the request has been signed, no changes must be made to the request, or the authentication is
     * void.
     * </p>
     * 
     * @param httpRequest The HttpRequest2 to sign, not <code>null</code>.
     * @return The signed HttpRequest2.
     */
    public HttpRequest2 createSignedRequest(HttpRequest2 httpRequest) {
        Validate.notNull(httpRequest, "httpRequest must not be null");
        HttpRequest2Builder builder = new HttpRequest2Builder(httpRequest.getMethod(), httpRequest.getUrl());
        builder.addHeaders(httpRequest.getHeaders());
        builder.addHeader("Authorization", createAuthorization(httpRequest, params));
        builder.setEntity(httpRequest.getEntity());
        return builder.create();
    }

    String createAuthorization(HttpRequest2 httpRequest, OAuthParams oAuthParams) {
        List<Pair<String, String>> oAuthHeader = new ArrayList<>();
        oAuthHeader.add(Pair.of("oauth_consumer_key", oAuthParams.getConsumerKey()));
        oAuthHeader.add(Pair.of("oauth_nonce", createRandomString()));
        oAuthHeader.add(Pair.of("oauth_signature_method", "HMAC-SHA1"));
        oAuthHeader.add(Pair.of("oauth_timestamp", createTimestamp()));
        oAuthHeader.add(Pair.of("oauth_token", oAuthParams.getAccessToken()));
        oAuthHeader.add(Pair.of("oauth_version", "1.0"));

        List<Pair<String, String>> allParams = new ArrayList<>();
        allParams.addAll(UrlHelper.parseParams(httpRequest.getUrl()));
        allParams.addAll(oAuthHeader);

        String sigBaseString = createSignatureBaseString(httpRequest, allParams);
        String sigKey = createSigningKey(oAuthParams.getConsumerSecret(), oAuthParams.getAccessTokenSecret());
        oAuthHeader.add(Pair.of("oauth_signature", createSignature(sigBaseString, sigKey)));
        Collections.sort(oAuthHeader);

        StringBuilder authorization = new StringBuilder();
        authorization.append("OAuth ");
        boolean first = true;
        for (Pair<String, String> pair : oAuthHeader) {
            if (first) {
                first = false;
            } else {
                authorization.append(", ");
            }
            authorization.append(String.format("%s=\"%s\"", urlEncode(pair.getKey()), urlEncode(pair.getValue())));
        }
        return authorization.toString();
    }

    static String createParameterString(List<Pair<String, String>> allParameters) {
        Collections.sort(allParameters);
        StringBuilder parameterString = new StringBuilder();
        boolean first = true;
        for (Pair<String, String> pair : allParameters) {
            if (first) {
                first = false;
            } else {
                parameterString.append('&');
            }
            parameterString.append(String.format("%s=%s", urlEncode(pair.getKey()), urlEncode(pair.getValue())));
        }
        return parameterString.toString();
    }

    static String createSignatureBaseString(HttpRequest2 httpRequest, List<Pair<String, String>> allParameters) {
        StringBuilder signature = new StringBuilder();
        String methodName = httpRequest.getMethod().toString().toUpperCase();
        signature.append(methodName).append('&');
        signature.append(urlEncode(UrlHelper.parseBaseUrl(httpRequest.getUrl()))).append('&');
        signature.append(urlEncode(createParameterString(allParameters)));
        return signature.toString();
    }

    /** Package private so that it can be overridden by Unit-Test. */
    String createRandomString() {
        return StringHelper.sha1(String.valueOf(System.currentTimeMillis()));
    }

    /** Package private so that it can be overridden by Unit-Test. */
    String createTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    static String urlEncode(String string) {
        // https://dev.twitter.com/docs/auth/percent-encoding-parameters
        return UrlHelper.encodeParameter(string).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    static String createSigningKey(String consumerSecret, String tokenSecret) {
        StringBuilder signingKey = new StringBuilder();
        signingKey.append(urlEncode(consumerSecret));
        signingKey.append('&');
        if (tokenSecret != null) {
            signingKey.append(tokenSecret);
        }
        return signingKey.toString();
    }

    static String createSignature(String signatureBaseString, String signingKey) {
        try {
            SecretKey key = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            byte[] hmacBytes = mac.doFinal(signatureBaseString.getBytes());
            return new String(Base64.encodeBase64(hmacBytes));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("InvalidKeyException when creating OAuth signature: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "NoSuchAlgorithmException when creating OAuth signature: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("IllegalStateException when creating OAuth signature: " + e.getMessage(), e);
        }
    }

}
