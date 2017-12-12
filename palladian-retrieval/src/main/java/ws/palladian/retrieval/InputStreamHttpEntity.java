package ws.palladian.retrieval;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.lang3.Validate;

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
    
    public InputStreamHttpEntity(File file, String contentType) throws FileNotFoundException {
    	this(new FileInputStream(file), file.length(), contentType);
    }
    
	public InputStreamHttpEntity(byte[] buffer, String contentType) {
		this(new ByteArrayInputStream(buffer), buffer.length, contentType);
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
