package ws.palladian.helper.shingling;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.btree.BTree;
import jdbm.helper.IntegerComparator;
import jdbm.helper.LongComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import ws.palladian.helper.io.FileHelper;

/**
 * Implementation of a ShinglesIndex which uses B+Trees via JDBM.
 * 
 * http://jdbm.sourceforge.net/
 * http://www.antonioshome.net/blog/2006/20060224-1.php
 * http://directory.apache.org/apacheds/1.5/table-and-cursor-implementations.html
 * 
 * @author Philipp Katz
 */
public class ShinglesIndexJDBM extends ShinglesIndexBaseImpl {

    /** class logger. */
    // private static final Logger LOGGER = Logger.getLogger(ShinglesIndexJDBM.class);

    /** manager for the JDBM data structures. */
    private RecordManager recordManager;

    /** contains all hashes and associated documents. */
    private BTree hashesDocuments;

    /** contains all documents and their sketch/hashes. */
    private BTree documentsSketch;

    /** contains a document and its similar/identical documents. */
    private BTree similarDocuments;

    /**
     * For testing purposes, will use a temp. file with random name as index.
     */
    // ShinglesIndexJDBM() {
    // this("tmp_" + System.currentTimeMillis());
    // }

    @Override
    public void openIndex() {

        try {

            // disable transactions, yields in a great speed up and we do not need rollback :)
            Properties properties = new Properties();
            properties.setProperty(RecordManagerOptions.DISABLE_TRANSACTIONS, "true");
            
            recordManager = RecordManagerFactory.createRecordManager(INDEX_FILE_BASE_PATH + getIndexName() + "_jdbm",
                    properties);

            hashesDocuments = loadOrCreateBTree(recordManager, "hashesDocuments", new LongComparator());
            documentsSketch = loadOrCreateBTree(recordManager, "documentsSketch", new IntegerComparator());
            similarDocuments = loadOrCreateBTree(recordManager, "similarDocuments", new IntegerComparator());

        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public void deleteIndex() {

        String indexFiles = INDEX_FILE_BASE_PATH + getIndexName() + "_jdbm";

        if (FileHelper.fileExists(indexFiles + ".db")) {
            FileHelper.delete(indexFiles + ".db");
        }

        if (FileHelper.fileExists(indexFiles + ".lg")) {
            FileHelper.delete(indexFiles + ".lg");
        }

    }

    // public ShinglesIndexJDBM(String indexFile) {
    // try {
    //
    // recordManager = RecordManagerFactory.createRecordManager(indexFile, new Properties());
    //
    // hashesDocuments = loadOrCreateBTree(recordManager, "hashesDocuments", new LongComparator());
    // documentsSketch = loadOrCreateBTree(recordManager, "documentsSketch", new IntegerComparator());
    // similarDocuments = loadOrCreateBTree(recordManager, "similarDocuments", new IntegerComparator());
    //
    // } catch (IOException e) {
    // LOGGER.error(e);
    // }
    // }

    @SuppressWarnings("unchecked")
    @Override
    public void addDocument(int documentId, Set<Long> sketch) {
        try {

            documentsSketch.insert(documentId, sketch, true);

            for (Long hash : sketch) {
                Set<Integer> documents = (Set<Integer>) hashesDocuments.find(hash);
                if (documents == null) {
                    documents = new HashSet<Integer>();
                }
                documents.add(documentId);
                hashesDocuments.insert(hash, documents, true); // true means "overwrite" existing record.
            }

            // write to disk, no autocommit here!
            recordManager.commit();

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {
        try {

            Set<Integer> similarDocs = (Set<Integer>) similarDocuments.find(masterDocumentId);
            if (similarDocs == null) {
                similarDocs = new HashSet<Integer>();
            }
            similarDocs.add(similarDocumentId);
            similarDocuments.insert(masterDocumentId, similarDocs, true);
            recordManager.commit();

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Integer> getDocumentsForHash(long hash) {
        Set<Integer> result = null;

        try {
            result = (Set<Integer>) hashesDocuments.find(hash);
            if (result == null) {
                result = Collections.emptySet();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @Override
    public int getNumberOfDocuments() {
        return documentsSketch.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Long> getSketchForDocument(int documentId) {
        Set<Long> result = null;

        try {
            result = (Set<Long>) documentsSketch.find(documentId);
            if (result == null) {
                result = Collections.emptySet();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {
        Set<Integer> result = null;

        try {
            result = (Set<Integer>) similarDocuments.find(documentId);
            if (result == null) {
                result = Collections.emptySet();
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        Map<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();

        try {
            TupleBrowser browse = similarDocuments.browse();
            Tuple tuple = new Tuple();
            while (browse.getNext(tuple)) {
                Integer masterDocumentId = (Integer) tuple.getKey();
                Set<Integer> similarDocumentIds = (Set<Integer>) tuple.getValue();
                result.put(masterDocumentId, similarDocumentIds);
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;
    }

    /**
     * Obtains a BTree used to index objects, or creates it if it does not exist.
     * From: http://www.antonioshome.net/blog/2006/20060224-1.php
     * 
     * @param aRecordManager the database.
     * @param aName the name of the BTree.
     * @param aComparator the Comparator object used to sort the elements of the BTree.
     * @return the BTree with that name.
     * @throws IOException if an I/O error happens.
     */
    private static BTree loadOrCreateBTree(RecordManager aRecordManager, String aName, Comparator<?> aComparator)
            throws IOException {
        // So you can't remember the recordID of the B-Tree? Well, let's
        // try to remember it from its name...
        long recordID = aRecordManager.getNamedObject(aName);
        BTree tree = null;

        if (recordID == 0) {
            LOGGER.debug("create new BTree " + aName);
            // Well, the B-Tree has not been previously stored,
            // so let's create one
            tree = BTree.createInstance(aRecordManager, aComparator);
            // store it with the given name
            aRecordManager.setNamedObject(aName, tree.getRecid());
            // And commit changes
            aRecordManager.commit();
        } else {
            LOGGER.debug("load existing BTree " + aName);
            // Yes, we already created this B-Tree in a previous run,
            // so let's retrieve it from the record manager
            tree = BTree.load(aRecordManager, recordID);
        }
        return tree;
    }

}
