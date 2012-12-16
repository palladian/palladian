package ws.palladian.retrieval;

import org.apache.commons.lang3.Validate;

public final class OAuthParams {

    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String accessTokenSecret;

    public OAuthParams(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        Validate.notEmpty(consumerKey, "consumerKey must not be empty");
        Validate.notEmpty(consumerSecret, "consumerSecret must not be empty");
        Validate.notEmpty(accessToken, "accessToken must not be empty");
        Validate.notEmpty(accessTokenSecret, "accessTokenSecret must not be empty");

        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OAuthParams [consumerKey=");
        builder.append(getConsumerKey());
        builder.append(", consumerSecret=");
        builder.append(getConsumerSecret());
        builder.append(", accessToken=");
        builder.append(getAccessToken());
        builder.append(", accessTokenSecret=");
        builder.append(getAccessTokenSecret());
        builder.append("]");
        return builder.toString();
    }

}