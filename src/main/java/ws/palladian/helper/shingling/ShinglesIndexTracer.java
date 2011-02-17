package ws.palladian.helper.shingling;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;

/**
 * Decorator to allow performance testing.
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexTracer implements ShinglesIndex {

    private static final Logger LOGGER = Logger.getLogger(ShinglesIndexTracer.class);

    private ShinglesIndex profiled;

    private long addDocument = 0;
    private long addDocumentSimilarity = 0;
    private long getDocumentsForHash = 0;
    private long getDocumentsForSketch = 0;
    private long getNumberOfDocuments = 0;
    private long getSimilarDocuments = 0;
    private long getSimilarDocuments2 = 0;
    private long getSketchForDocument = 0;

    public ShinglesIndexTracer(ShinglesIndex profiled) {
        this.profiled = profiled;
    }

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {
        LOGGER.trace(">addDocument " + documentId + " " + sketch);
        StopWatch sw = new StopWatch();
        profiled.addDocument(documentId, sketch);
        addDocument += sw.getElapsedTime();
        LOGGER.trace("<addDocument [" + sw.getElapsedTime() + "]");
    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {
        LOGGER.trace(">addDocumentSimilarity " + masterDocumentId + " " + similarDocumentId);
        StopWatch sw = new StopWatch();
        profiled.addDocumentSimilarity(masterDocumentId, similarDocumentId);
        addDocumentSimilarity += sw.getElapsedTime();
        LOGGER.trace("<addDocumentSimilarity [" + sw.getElapsedTime() + "]");
    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {
        LOGGER.trace(">getDocumentsForHash " + hash);
        StopWatch sw = new StopWatch();
        Set<Integer> result = profiled.getDocumentsForHash(hash);
        getDocumentsForHash += sw.getElapsedTime();
        LOGGER.trace("<getDocumentsForHash [" + sw.getElapsedTime() + "] " + result.size() + " " + result);
        return result;
    }

    @Override
    public Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch) {
        LOGGER.trace(">getDocumentsForSketch " + sketch);
        StopWatch sw = new StopWatch();
        Map<Integer, Set<Long>> result = profiled.getDocumentsForSketch(sketch);
        getDocumentsForSketch += sw.getElapsedTime();
        LOGGER.trace("<getDocumentsForSketch [" + sw.getElapsedTime() + "] " + result.size() + " " + result);
        return result;
    }

    @Override
    public int getNumberOfDocuments() {
        LOGGER.trace(">getNumberOfDocuments");
        StopWatch sw = new StopWatch();
        int result = profiled.getNumberOfDocuments();
        getNumberOfDocuments += sw.getElapsedTime();
        LOGGER.trace("<getNumberOfDocuments [" + sw.getElapsedTime() + "] " + result);
        return result;
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {
        LOGGER.trace(">getSimilarDocuments");
        StopWatch sw = new StopWatch();
        Set<Integer> result = profiled.getSimilarDocuments(documentId);
        getSimilarDocuments += sw.getElapsedTime();
        LOGGER.trace("<getSimilarDocuments [" + sw.getElapsedTime() + "] " + result.size());
        return result;
    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        LOGGER.trace(">getSimilarDocuments2");
        StopWatch sw = new StopWatch();
        Map<Integer, Set<Integer>> result = profiled.getSimilarDocuments();
        getSimilarDocuments2 += sw.getElapsedTime();
        LOGGER.trace("<getSimilarDocuments2 [" + sw.getElapsedTime() + "] " + result.size());
        return result;
    }

    @Override
    public Set<Long> getSketchForDocument(int documentId) {
        LOGGER.trace(">getSketchForDocument");
        StopWatch sw = new StopWatch();
        Set<Long> result = profiled.getSketchForDocument(documentId);
        getSketchForDocument += sw.getElapsedTime();
        LOGGER.trace("<getSketchForDocument [" + sw.getElapsedTime() + "] " + result.size());
        return result;
    }

    public String getTraceResult() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- result for: ").append(profiled).append(" ---\n");
        sb.append("addDocument: ").append(addDocument).append(" ms\n");
        sb.append("addDocumentSimilarity: ").append(addDocumentSimilarity).append(" ms\n");
        sb.append("getDocumentsForSketch: ").append(getDocumentsForSketch).append(" ms\n");
        sb.append("getDocumentsForHash: ").append(getDocumentsForHash).append(" ms\n");
        sb.append("getNumberOfDocuments: ").append(getNumberOfDocuments).append(" ms\n");
        sb.append("getSimilarDocuments: ").append(getSimilarDocuments).append(" ms\n");
        sb.append("getSimilarDocuments2: ").append(getSimilarDocuments2).append(" ms\n");
        sb.append("getSketchForDocument: ").append(getSketchForDocument).append(" ms\n");
        sb.append("-----------------------------------\n");
        sb.append("total time for index: ").append(
                addDocument + addDocumentSimilarity + getDocumentsForSketch + getDocumentsForHash
                        + getNumberOfDocuments + getSimilarDocuments + getSimilarDocuments2 + getSketchForDocument)
                .append(" ms");
        return sb.toString();
    }

    @Override
    public void deleteIndex() {
        profiled.deleteIndex();
    }

    @Override
    public String getIndexName() {
        return profiled.getIndexName();
    }

    @Override
    public void openIndex() {
        profiled.openIndex();
    }

    @Override
    public void saveIndex() {
        profiled.saveIndex();
    }

    @Override
    public void setIndexName(String name) {
        profiled.setIndexName(name);
    }

}
