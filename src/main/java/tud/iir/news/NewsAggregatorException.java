package tud.iir.news;

public class NewsAggregatorException extends Exception {

    private static final long serialVersionUID = -8787100315945118852L;

    public NewsAggregatorException(Throwable t) {
        super(t);
    }

    public NewsAggregatorException(String string) {
        super(string);
    }

}
