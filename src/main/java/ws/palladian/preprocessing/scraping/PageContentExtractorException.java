package ws.palladian.preprocessing.scraping;

public class PageContentExtractorException extends Exception {

    private static final long serialVersionUID = -5723919183701989944L;

    public PageContentExtractorException() {
        super();
    }

    public PageContentExtractorException(Throwable t) {
        super(t);
    }

    public PageContentExtractorException(String message, Throwable cause) {
        super(message, cause);
    }

    public PageContentExtractorException(String message) {
        super(message);
    }

}