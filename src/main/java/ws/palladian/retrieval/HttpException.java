package ws.palladian.retrieval;

public class HttpException extends Exception {

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
