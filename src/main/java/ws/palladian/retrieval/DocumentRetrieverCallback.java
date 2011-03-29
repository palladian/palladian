package ws.palladian.retrieval;

import java.io.InputStream;

/**
 * Callback interface to be used with the URLDownloader. For each downloaded URL, the
 * {@link URLDownloaderCallback#finished(String, InputStream)} method is called.
 * 
 * @author Philipp Katz
 * 
 */
public interface DocumentRetrieverCallback {

    void finished(String url, InputStream inputStream);

}