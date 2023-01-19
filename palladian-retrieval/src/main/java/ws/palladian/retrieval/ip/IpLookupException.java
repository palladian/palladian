package ws.palladian.retrieval.ip;

@SuppressWarnings("serial")
public class IpLookupException extends Exception {

    public IpLookupException() {
        super();
    }

    public IpLookupException(String message, Throwable cause) {
        super(message, cause);
    }

    public IpLookupException(String message) {
        super(message);
    }

    public IpLookupException(Throwable cause) {
        super(cause);
    }

}
