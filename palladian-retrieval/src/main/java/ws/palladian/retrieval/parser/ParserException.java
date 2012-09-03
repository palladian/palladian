package ws.palladian.retrieval.parser;

public class ParserException extends Exception {

    private static final long serialVersionUID = 1L;

    public ParserException() {
        super();
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParserException(String message) {
        super(message);
    }

    public ParserException(Throwable cause) {
        super(cause);
    }

}
