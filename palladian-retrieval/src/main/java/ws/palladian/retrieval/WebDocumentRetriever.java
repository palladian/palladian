package ws.palladian.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Predicates;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.helper.NoThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.search.DocumentRetrievalTrial;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by David Urbansky on 07.10.2017.
 */
public abstract class WebDocumentRetriever {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDocumentRetriever.class);

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

    public static final String ORIGINAL_REQUEST_URL = "requestUrl";

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
     * Some APIs require sending headers such as the accept header, so we can specify that globally for all calls with
     * this retriever.
     */
    Map<String, String> globalHeaders = new HashMap<>();
    ;

    public abstract Document getWebDocument(String url);

    public Document getWebDocument(String url, Thread thread) {
        return getWebDocument(url);
    }

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
     * @param urls     the URLs to download.
     * @param callback the callback to be called for each finished download.
     */
    public void getWebDocuments(Collection<String> urls, final Consumer<Document> callback) {
        getWebDocuments(urls, callback, new ProgressMonitor(urls.size(), 1., "DocumentRetriever"));
    }

    public void getWebDocuments(Collection<String> urls, final Consumer<Document> callback, final ProgressMonitor progressMonitor) {
        List<String> urlsList = new ArrayList<>(urls);
        List<String> sublist;
        int num = 10000;
        for (int i = 0; i < urls.size(); i += num) {
            sublist = CollectionHelper.getSublist(urlsList, i, num);

            final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>(sublist);

            ExecutorService executor = Executors.newFixedThreadPool(getNumThreads());

            while (!urlQueue.isEmpty()) {
                final String url = urlQueue.poll();

                Thread ct = new Thread("Retrieving: " + url) {
                    @Override
                    public void run() {
                        Thread.currentThread().setName("Retrieving: " + url);

                        getRequestThrottle().hold();

                        // react file fileTypeConsumer?
                        boolean consumerFound = reactToFileTypeConsumer(url, getFileTypeConsumers());

                        if (!consumerFound) {
                            Document document = getWebDocument(url, Thread.currentThread());
                            if (document != null) {
                                document.setUserData(ORIGINAL_REQUEST_URL, url, null);
                                callback.accept(document);
                            }
                        }

                        if (progressMonitor != null) {
                            progressMonitor.incrementAndPrintProgress();
                        }
                    }
                };

                if (!executor.isShutdown()) {
                    executor.submit(ct);
                }
            }

            // wait for the threads to finish
            executor.shutdown();

            // wait until all threads are finish
            LOGGER.debug("waiting for all " + num + " threads to finish...");
            StopWatch sw = new StopWatch();
            try {
                while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.debug("wait crawling");
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            LOGGER.debug("...all threads finished in " + sw.getTotalElapsedTimeString());
        }
    }

    /**
     * <p>
     * Get multiple URLs in parallel. The number of simultaneous threads for downloading and parsing can be defined
     * using {@link #setNumThreads(int)}.
     * </p>
     *
     * @param urls the URLs to download.
     * @return Set with the downloaded documents, documents which could not be downloaded or parsed successfully, are
     * not included.
     */
    public Set<Document> getWebDocuments(Collection<String> urls) {
        Set<Document> documents = new HashSet<>();

        for (String url : urls) {
            getRequestThrottle().hold();
            Document document = getWebDocument(url);
            documents.add(document);
        }

        return documents;
    }

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
