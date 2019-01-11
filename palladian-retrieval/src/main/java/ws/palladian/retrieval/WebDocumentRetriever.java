package ws.palladian.retrieval;

import org.w3c.dom.Document;
import ws.palladian.helper.functional.Predicates;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by David Urbansky on 07.10.2017.
 */
public abstract class WebDocumentRetriever {
    /** The filter for the retriever. */
    private Predicate<? super String> downloadFilter = Predicates.ALL;

    /**
     * The callbacks that are called after each parsed page.
     */
    private final List<Consumer<Document>> retrieverCallbacks = new ArrayList<>();

    public abstract Document getWebDocument(String url);

    public void setDownloadFilter(Predicate<String> downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public Predicate<? super String> getDownloadFilter() {
        return downloadFilter;
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
}
