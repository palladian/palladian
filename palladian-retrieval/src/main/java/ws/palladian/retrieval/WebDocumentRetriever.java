package ws.palladian.retrieval;

import org.w3c.dom.Document;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.functional.Predicates;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.helper.NoThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.search.DocumentRetrievalTrial;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by David Urbansky on 07.10.2017.
 */
public abstract class WebDocumentRetriever {
    /**
     * The filter for the retriever.
     */
    private Predicate<? super String> downloadFilter = Predicates.ALL;

    /**
     * The number of threads for downloading in parallel.
     */
    public static final int DEFAULT_NUM_THREADS = 10;

    /**
     * The maximum number of threads to use.
     */
    private int numThreads = DEFAULT_NUM_THREADS;

    /**
     * The number of milliseconds each host gets between two requests.
     */
    private RequestThrottle requestThrottle = NoThrottle.INSTANCE;

    /**
     * The callbacks that are called after each parsed page.
     */
    private final List<Consumer<Document>> retrieverCallbacks = new ArrayList<>();

    /**
     * The callback that are called if a URL could not successfully be parsed to a document.
     */
    private Consumer<DocumentRetrievalTrial> errorCallback = null;

    /** Special consumers for file types other than HTML */
    private Map<String, Consumer<String>> fileTypeConsumers = new HashMap<>();

    /**
     * Some APIs require to send headers such as the accept header, so we can specify that globally for all calls with
     * this retriever.
     */
    Map<String, String> globalHeaders = null;

    public abstract Document getWebDocument(String url);

    public String getText(String url) {
        Document webDocument = getWebDocument(url);
        if (webDocument == null) {
            return null;
        }
        return HtmlHelper.getInnerXml(webDocument);
    }

    public Map<String, String> getGlobalHeaders() {
        return globalHeaders;
    }

    public void setGlobalHeaders(Map<String, String> globalHeaders) {
        this.globalHeaders = globalHeaders;
    }

    public void setDownloadFilter(Predicate<String> downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public Predicate<? super String> getDownloadFilter() {
        return downloadFilter;
    }

    /**
     * <p>
     * Set the maximum number of simultaneous threads for downloading.
     * </p>
     *
     * @param numThreads the number of threads to use.
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getNumThreads() {
        return this.numThreads;
    }

    public RequestThrottle getRequestThrottle() {
        return requestThrottle;
    }

    public void setRequestThrottle(RequestThrottle requestThrottle) {
        this.requestThrottle = requestThrottle;
    }


    /**
     * <p>
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked. The number of
     * simultaneous threads for downloading and parsing can be defined using {@link #setNumThreads(int)}.
     * </p>
     *
     * @param urls the URLs to download.
     * @param callback the callback to be called for each finished download.
     */
    public void getWebDocuments(Collection<String> urls, final Consumer<Document> callback) {
        getWebDocuments(urls, callback,  new ProgressMonitor(urls.size(), 1., "DocumentRetriever"));
    }

    public abstract void getWebDocuments(Collection<String> urls, final Consumer<Document> callback,final ProgressMonitor progressMonitor);

    /**
     * <p>
     * Get multiple URLs in parallel. The number of simultaneous threads for downloading and parsing can be defined
     * using {@link #setNumThreads(int)}.
     * </p>
     *
     * @param urls the URLs to download.
     * @return Set with the downloaded documents, documents which could not be downloaded or parsed successfully, are
     *         not included.
     */
    public abstract Set<Document> getWebDocuments(Collection<String> urls);

    boolean reactToFileTypeConsumer(String url, Map<String, Consumer<String>> fileTypeConsumers) {
        if (fileTypeConsumers != null) {
            String fileType = FileHelper.getFileType(url);
            Consumer<String> stringConsumer = fileTypeConsumers.get(fileType);
            if (stringConsumer != null) {
                stringConsumer.accept(url);
                return true;
            }
        }

        return false;
    }

    public void close() {
        // nothing to close by default
    }

    public Map<String, Consumer<String>> getFileTypeConsumers() {
        return fileTypeConsumers;
    }

    public void setFileTypeConsumers(Map<String, Consumer<String>> fileTypeConsumers) {
        this.fileTypeConsumers = fileTypeConsumers;
    }

    // ////////////////////////////////////////////////////////////////
    // Callbacks
    // ////////////////////////////////////////////////////////////////
    void callRetrieverCallback(Document document) {
        for (Consumer<Document> retrieverCallback : retrieverCallbacks) {
            retrieverCallback.accept(document);
        }
    }

    public List<Consumer<Document>> getRetrieverCallbacks() {
        return retrieverCallbacks;
    }

    public void addRetrieverCallback(Consumer<Document> retrieverCallback) {
        retrieverCallbacks.add(retrieverCallback);
    }

    public void removeRetrieverCallback(Consumer<Document> retrieverCallback) {
        retrieverCallbacks.remove(retrieverCallback);
    }

    public Consumer<DocumentRetrievalTrial> getErrorCallback() {
        return errorCallback;
    }

    public void setErrorCallback(Consumer<DocumentRetrievalTrial> errorCallback) {
        this.errorCallback = errorCallback;
    }
}
