package ws.palladian.retrieval;

import java.io.InputStream;

import org.apache.commons.lang3.Validate;

import ws.palladian.retrieval.HttpEntity;

public final class InputStreamHttpEntity implements HttpEntity {

    private final InputStream inputStream;
    private final long length;
    private final String contentType;

    public InputStreamHttpEntity(InputStream inputStream, long length, String contentType) {
        Validate.notNull(contentType, "contentType must not be null");
        Validate.isTrue(length >= 0, "length must not be negative");
        Validate.notEmpty(contentType, "contentType must not be empty");
        this.inputStream = inputStream;
        this.length = length;
        this.contentType = contentType;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

}
