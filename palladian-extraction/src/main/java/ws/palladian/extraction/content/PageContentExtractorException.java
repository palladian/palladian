package ws.palladian.extraction.content;

public class PageContentExtractorException extends Exception {

    private static final long serialVersionUID = 1L;

    public PageContentExtractorException() {
        super();
    }

    public PageContentExtractorException(Throwable throwable) {
        super(throwable);
    }

    public PageContentExtractorException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public PageContentExtractorException(String message) {
        super(message);
    }

}