package ws.palladian.helper.shingling;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

/**
 * Shingle index with in-memory Java Object graph. Persistence is achieved via Java serialization.
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexJava extends ShinglesIndexBaseImpl {

    /** the actual class which contains the index. */
    private ShinglesIndexJavaData data = new ShinglesIndexJavaData();

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {
        data.getDocumentsSketch().put(documentId, sketch);
        for (Long hash : sketch) {
            Set<Integer> documents = data.getHashesDocuments().get(hash);
            if (documents == null) {
                documents = new HashSet<Integer>();
                data.getHashesDocuments().put(hash, documents);
            }
            documents.add(documentId);
        }
        // numberOfDocuments++;
    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {
        Set<Integer> result = data.getHashesDocuments().get(hash);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public Set<Long> getSketchForDocument(int documentId) {
        return data.getDocumentsSketch().get(documentId);
    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {
        Set<Integer> similarities = getSimilarDocuments(masterDocumentId);
        if (similarities == null) {
            similarities = new HashSet<Integer>();
            data.getSimilarDocuments().put(masterDocumentId, similarities);
        }
        similarities.add(similarDocumentId);
    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        return data.getSimilarDocuments();
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {
        return data.getSimilarDocuments().get(documentId);
    }

    @Override
    public int getNumberOfDocuments() {
        // return numberOfDocuments;
        return data.getDocumentsSketch().size();
    }

    @Override
    public void openIndex() {

        if (FileHelper.fileExists(getIndexFileName())) {
            try {
                data = FileHelper.deserialize(getIndexFileName());
            } catch (IOException e) {
                data = new ShinglesIndexJavaData();
            }
        } else {
            data = new ShinglesIndexJavaData();
        }

    }

    @Override
    public void saveIndex() {
        try {
            FileHelper.serialize(data, getIndexFileName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void deleteIndex() {

        if (FileHelper.fileExists(getIndexFileName())) {
            FileHelper.delete(getIndexFileName());
        }

    }

    /**
     * Get the file name of this index.
     * 
     * @return
     */
    private String getIndexFileName() {
        return INDEX_FILE_BASE_PATH + getIndexName() + ".ser";
    }

    public static void main(String[] args) {
        ShinglesIndexBaseImpl index = new ShinglesIndexJava();

        StopWatch sw = new StopWatch();
        System.out.println("started");
        Shingles shingles = new Shingles(index);
        // shingles.addDocumentsFromFile("data/tag_training_small_50.txt");
        // shingles.addDocumentsFromFile("data/tag_training_NEW_1000.txt");
        shingles.addDocumentsFromFile("data/tag_training_NEW_10000.txt");
        System.out.println("elapsed time : " + sw.getElapsedTimeString());

        // index.printPerformance();

        System.out.println("------");
        System.out.println(shingles.getSimilarityReport());

    }

}

/**
 * The actual class for serialization.
 * 
 * @author Philipp Katz
 * 
 */
class ShinglesIndexJavaData implements Serializable {

    private static final long serialVersionUID = 8981118690413896686L;

    /** contains shingle hashes and their corresponding documents. */
    private Map<Long, Set<Integer>> hashesDocuments = new HashMap<Long, Set<Integer>>();

    /** contains documents and their corresponding sketch. **/
    private Map<Integer, Set<Long>> documentsSketch = new HashMap<Integer, Set<Long>>();

    /** contains a document and its similar/identical documents. */
    private Map<Integer, Set<Integer>> similarDocuments = new TreeMap<Integer, Set<Integer>>();

    /**
     * @return the hashesDocuments
     */
    public Map<Long, Set<Integer>> getHashesDocuments() {
        return hashesDocuments;
    }

    /**
     * @param hashesDocuments the hashesDocuments to set
     */
    public void setHashesDocuments(Map<Long, Set<Integer>> hashesDocuments) {
        this.hashesDocuments = hashesDocuments;
    }

    /**
     * @return the documentsSketch
     */
    public Map<Integer, Set<Long>> getDocumentsSketch() {
        return documentsSketch;
    }

    /**
     * @param documentsSketch the documentsSketch to set
     */
    public void setDocumentsSketch(Map<Integer, Set<Long>> documentsSketch) {
        this.documentsSketch = documentsSketch;
    }

    /**
     * @return the similarDocuments
     */
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        return similarDocuments;
    }

    /**
     * @param similarDocuments the similarDocuments to set
     */
    public void setSimilarDocuments(Map<Integer, Set<Integer>> similarDocuments) {
        this.similarDocuments = similarDocuments;
    }

}
