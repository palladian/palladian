package ws.palladian.retrieval.helper;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;

import ws.palladian.retrieval.HttpRequest;

/**
 * <p>
 * Utility class to sign requests for <a href="https://www.mashape.com">Mashape</a>.
 * </p>
 * 
 * @see <a href="https://www.mashape.com/docs/consume/rest">Consume an API in Plain REST</a>
 * @author Philipp Katz
 */
public final class MashapeUtil {

    /** Key of the Mashape Authorization header. */
    private static final String MASHAPE_AUTHORIZATION_HEADER = "X-Mashape-Authorization";

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * <p>
     * Sign the given {@link HttpRequest} with the specified public/private key combination and put the authorization in
     * the HTTP header {@value #MASHAPE_AUTHORIZATION_HEADER}.
     * </p>
     * 
     * @param request The request to sign, not <code>null</code>.
     * @param publicKey The public key for signing, not empty or <code>null</code>.
     * @param privateKey The private key for signing, not empty or <code>null</code>.
     */
    public static void signRequest(HttpRequest request, String publicKey, String privateKey) {
        Validate.notNull(request, "request must not be null");
        Validate.notEmpty(publicKey, "publicKey must not be empty");
        Validate.notEmpty(privateKey, "privateKey must not be empty");
        String mashapeHeader = MashapeUtil.generateMashapeHeader(publicKey, privateKey);
        request.addHeader(MASHAPE_AUTHORIZATION_HEADER, mashapeHeader);
    }

    // https://www.mashape.com/docs/consume/rest
    static String generateMashapeHeader(String publicKey, String privateKey) {
        try {
            return new String(Base64.encodeBase64(String.format("%s:%s", publicKey, sha1hmac(publicKey, privateKey))
                    .getBytes()));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Code taken from:
    // https://github.com/Mashape/mashape-java-client-library/blob/master/src/main/java/com/mashape/client/http/utils/CryptUtils.java
    static String sha1hmac(String publicKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey key = new SecretKeySpec(privateKey.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(key);
        byte[] rawHmac = mac.doFinal(publicKey.getBytes());
        BigInteger hash = new BigInteger(1, rawHmac);
        String hmac = hash.toString(16);
        if (hmac.length() % 2 != 0) {
            hmac = "0" + hmac;
        }
        return hmac;
    }

    private MashapeUtil() {
        // do not instantiate.
    }


}
