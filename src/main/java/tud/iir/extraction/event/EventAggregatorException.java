package tud.iir.extraction.event;

/**
 * @author Martin Wunderwald
 */
public class EventAggregatorException extends Exception {

    private static final long serialVersionUID = -8787100315945118852L;

    public EventAggregatorException(Throwable throwable) {
        super(throwable);
    }

    public EventAggregatorException(String string) {
        super(string);
    }

}
