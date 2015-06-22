package ws.palladian.retrieval;

import java.util.HashMap;
import java.util.Map;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.functional.Factory;

public final class FormEncodedHttpEntity {
    
    // TODO remove useless wrapper class (legacy)

    public static final class Builder implements Factory<HttpEntity> {

        private final Map<String, String> data = new HashMap<>();

        public Builder addData(String key, String value) {
            data.put(key, value);
            return this;
        }

        @Override
        public HttpEntity create() {
            return new StringHttpEntity(UrlHelper.createParameterString(data), FORM_ENCODED_CONTENT_TYPE);
        }

    }

    /** The content type of this entity. */
    private static final String FORM_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

}
