package ws.palladian.extraction.location.geocoder;

public class GeocoderException extends Exception {

    private static final long serialVersionUID = 1L;

    public GeocoderException() {
    }

    public GeocoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeocoderException(String message) {
        super(message);
    }

    public GeocoderException(Throwable cause) {
        super(cause);
    }

}
