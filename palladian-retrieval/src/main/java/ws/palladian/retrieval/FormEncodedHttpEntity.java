package ws.palladian.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.functional.Factory;

public final class FormEncodedHttpEntity {
    
    // TODO remove useless wrapper class (legacy)

    public static final class Builder implements Factory<HttpEntity> {

        private final List<Pair<String, String>> data = new ArrayList<>();

        public Builder addData(String key, String value) {
            Validate.notNull(key, "key must not be null");
            data.add(Pair.of(key, value));
            return this;
        }

        public Builder addData(Map<String, String> data) {
            Validate.notNull(data, "data must not be null");
            for (Entry<String, String> entry : data.entrySet()) {
                addData(entry.getKey(), entry.getValue());
            }
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
