package ws.palladian.retrieval;

/**
 * A callback interface which can be hooked into the {@link DocumentRetriever}. The methods are executed just
 * before/after the request. If you want to interrupt a request, you may throw an {@link HttpException}.
 * 
 * @author Philipp Katz
 * 
 */
public interface HttpHook {
    
    void beforeRequest(String url, HttpRetriever downloader) throws HttpException;
    void afterRequest(HttpResult result, HttpRetriever downloader) throws HttpException;
    
    public static class DefaultHttpHook implements HttpHook {
        
        @Override
        public void beforeRequest(String url, HttpRetriever downloader) throws HttpException {
            // no op.
        }
        
        @Override
        public void afterRequest(HttpResult result, HttpRetriever downloader) throws HttpException {
            // no op.
        }
        
    }

}
