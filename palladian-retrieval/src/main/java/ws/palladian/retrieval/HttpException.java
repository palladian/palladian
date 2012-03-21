package ws.palladian.retrieval;

import java.io.IOException;

public class HttpException extends IOException {

    private static final long serialVersionUID = 1L;

    public HttpException() {
        super();
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpException(String message) {
        super(message);
    }

    public HttpException(Throwable cause) {
        super(cause);
    }

}
