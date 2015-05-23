package ws.palladian.retrieval;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.functional.Factory;

public final class FormEncodedHttpEntity implements HttpEntity {

    public static final class Builder implements Factory<FormEncodedHttpEntity> {

        private final Map<String, String> data = new HashMap<>();

        public Builder addData(String key, String value) {
            data.put(key, value);
            return this;
        }

        @Override
        public FormEncodedHttpEntity create() {
            return new FormEncodedHttpEntity(UrlHelper.createParameterString(data));
        }

    }
    
    /** The content type of this entity. */
    private static final String FORM_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final String content;

    private FormEncodedHttpEntity(String content) {
        this.content = content;
    }

    @Override
    public long length() {
        return content.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public String getContentType() {
        return FORM_ENCODED_CONTENT_TYPE;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FormEncodedHttpEntity [content=");
        builder.append(content);
        builder.append("]");
        return builder.toString();
    }

}
