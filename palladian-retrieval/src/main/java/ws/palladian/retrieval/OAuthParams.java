package ws.palladian.retrieval;

import org.apache.commons.lang3.Validate;

public class OAuthParams {

    public final String consumerKey;
    public final String consumerSecret;
    public final String token;
    public final String tokenSecret;

    public OAuthParams(String consumerKey, String consumerSecret, String token, String tokenSecret) {
        Validate.notNull(consumerKey, "consumerKey must not be null");
        Validate.notNull(tokenSecret, "tokenSecret must not be null");
        Validate.notNull(token, "token must not be null");
        Validate.notNull(tokenSecret, "tokenSecret must not be null");

        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.token = token;
        this.tokenSecret = tokenSecret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OAuthParams [consumerKey=");
        builder.append(consumerKey);
        builder.append(", consumerSecret=");
        builder.append(consumerSecret);
        builder.append(", token=");
        builder.append(token);
        builder.append(", tokenSecret=");
        builder.append(tokenSecret);
        builder.append("]");
        return builder.toString();
    }

}