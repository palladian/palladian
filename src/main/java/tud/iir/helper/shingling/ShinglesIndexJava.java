package tud.iir.helper.shingling;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;

/**
 * Shingle index with in-memory Java Object graph. Persistence is achieved via Java serialization.
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexJava extends ShinglesIndexBaseImpl implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SERIALIZED_FILE_PATH = "data/shinglesIndex.ser";

    /** contains shingle hashes and their corresponding documents. */
    private Map<Long, Set<Integer>> hashesDocuments = new HashMap<Long, Set<Integer>>();
    
    /** contains documents and their corresponding sketch. **/
    private Map<Integer, Set<Long>> documentsSketch = new HashMap<Integer, Set<Long>>();

    /** contains a document and its similar/identical documents. */
    private Map<Integer, Set<Integer>> similarDocuments = new TreeMap<Integer, Set<Integer>>();

    // private int numberOfDocuments = 0;

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {
        documentsSketch.put(documentId, sketch);
        for (Long hash : sketch) {
            Set<Integer> documents = hashesDocuments.get(hash);
            if (documents == null) {
                documents = new HashSet<Integer>();
                hashesDocuments.put(hash, documents);
            }
            documents.add(documentId);
        }
        // numberOfDocuments++;
    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {
        Set<Integer> result = hashesDocuments.get(hash);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }
    
    @Override
    public Set<Long> getSketchForDocument(int documentId) {
        return documentsSketch.get(documentId);
    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {
        Set<Integer> similarities = getSimilarDocuments(masterDocumentId);
        if (similarities == null) {
            similarities = new HashSet<Integer>();
            similarDocuments.put(masterDocumentId, similarities);
        }
        similarities.add(similarDocumentId);
    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        return similarDocuments;
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {
        return similarDocuments.get(documentId);
    }

    @Override
    public int getNumberOfDocuments() {
        // return numberOfDocuments;
        return documentsSketch.size();
    }

    public void save() {
        FileHelper.serialize(this, SERIALIZED_FILE_PATH);
    }

    public static ShinglesIndex load() {
        ShinglesIndex index = (ShinglesIndex) FileHelper.deserialize(SERIALIZED_FILE_PATH);
        if (index == null) {
            index = new ShinglesIndexJava();
        }
        return index;
    }
    
    public static void main(String[] args) {
        ShinglesIndexJava index = new ShinglesIndexJava();
        
        
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
