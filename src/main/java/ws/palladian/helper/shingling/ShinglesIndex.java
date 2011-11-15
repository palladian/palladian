package ws.palladian.helper.shingling;

import java.util.Map;
import java.util.Set;

/**
 * Defines an Index to store Shingle specific data. The model includes documents represented by a unique ID and their
 * sketches, which are sets of hashed n-grams. The interface allows the lookup of documents based on their sketch or
 * hashes und the lookup of sketches for specific documents. Further we keep a references between similar/identical
 * documents.
 * 
 * @author Philipp Katz
 * 
 */
public interface ShinglesIndex {

    /**
     * Set the name of this index. For instance, we might have different document collections which use their own
     * indices.
     * 
     * @param name
     */
    void setIndexName(String name);

    /**
     * Get the name of this index.
     * 
     * @return
     */
    String getIndexName();

    /**
     * Open the index for usage. This must be the first call to the index instance.
     */
    void openIndex();

    /**
     * Save the index, if necessary.
     */
    void saveIndex();

    /**
     * Delete the index, e.g. its corresponding files. This is intended for clean up after unit testing.
     */
    void deleteIndex();

    /**
     * Add a document which is represented by an ID and its sketch (aka. set of hashes) to the index.
     * 
     * @param documentId
     * @param sketch
     */
    void addDocument(int documentId, Set<Long> sketch);

    /**
     * Get all document IDs for the specified hash.
     * 
     * @param hash
     * @return
     */
    Set<Integer> getDocumentsForHash(long hash);

    /**
     * Get all documents for the specified sketch. This will return all documents which contain at least one hash from
     * the sketch.
     * 
     * @param sketch
     * @return
     * @depr_ecated this is generally slow.
     */
    //@Deprecated
    Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch);

    /**
     * Get the sketch for a stored document.
     * 
     * @param documentId
     * @return
     */
    Set<Long> getSketchForDocument(int documentId);

    /**
     * Get count of stored documents.
     * 
     * @return
     */
    int getNumberOfDocuments();

    /**
     * Get similar documents for a specific document.
     * 
     * @param documentId
     * @return
     */
    Set<Integer> getSimilarDocuments(int documentId);

    /**
     * Add a similarity relation between two documents.
     * 
     * @param masterDocumentId
     * @param similarDocumentId
     */
    void addDocumentSimilarity(int masterDocumentId, int similarDocumentId);

    /**
     * Get a map of all similar documents.
     * 
     * @return
     */
    Map<Integer, Set<Integer>> getSimilarDocuments();

}