package ws.palladian.retrieval;

/**
 * <p>
 * A callback interface which can be hooked into the {@link HttpRetriever}. The methods are executed just before/after
 * the request. If you want to interrupt a request, you may throw an {@link HttpException}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public interface HttpHook {

    Proxy getProxy(String url) throws HttpException;

    // void beforeRequest(String url, HttpRetriever downloader) throws HttpException;

    // void afterRequest(HttpResult result, HttpRetriever downloader) throws HttpException;

    public static class DefaultHttpHook implements HttpHook {

//        @Override
//        public void beforeRequest(String url, HttpRetriever downloader) throws HttpException {
//            // no op.
//        }

//        @Override
//        public void afterRequest(HttpResult result, HttpRetriever downloader) throws HttpException {
//            // no op.
//        }

        @Override
        public Proxy getProxy(String url) throws HttpException {
            return null;
        }

    }

}
