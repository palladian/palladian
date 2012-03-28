package ws.palladian.helper.shingling;

import static wb.Db.bt_Get;
import static wb.Db.bt_Next;
import static wb.Db.bt_Put;
import static wb.Db.createDb;
import static wb.Ents.initWb;
import static wb.Segs.makeSeg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wb.Han;
import ws.palladian.helper.io.FileHelper;

/**
 * ShinglesIndex implementation using "WB B-Tree Database". The API is plain shocking and seems to be ported directly
 * from C.
 * 
 * TODO this does not work if we have non contiuous IDs ... like 1, 2, 9, 17, ...
 * 
 * http://people.csail.mit.edu/jaffer/WB
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexWB extends ShinglesIndexBaseImpl {

    private Han hashesDocuments;
    private Han documentsSketch;
    private Han similarDocuments;

    @Override
    public void openIndex() {

        initWb(75, 150, 4096);
        wb.Seg seg = makeSeg(getIndexFileName(), 4096);

        hashesDocuments = createDb(seg, 'T', "hashesDocuments");
        documentsSketch = createDb(seg, 'T', "documentsSketch");
        similarDocuments = createDb(seg, 'T', "similarDocuments");

    }
    
    @Override
    public void deleteIndex() {
        
        if (FileHelper.fileExists(getIndexFileName())) {
            FileHelper.delete(getIndexFileName());
        }
        
    }

    private String getIndexFileName() {
        return INDEX_FILE_BASE_PATH + getIndexName() + "_wb";
    }

    // public ShinglesIndexWB() {
    // initWb(75, 150, 4096);
    // wb.Seg seg = makeSeg("tmp_" + System.currentTimeMillis(), 4096);
    //
    // hashesDocuments = createDb(seg, 'T', "hashesDocuments");
    // documentsSketch = createDb(seg, 'T', "documentsSketch");
    // similarDocuments = createDb(seg, 'T', "similarDocuments");
    //
    // }

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {

        int count = 0;
        for (Long hash : sketch) {

            // insert documentId -> hash
            bt_Put(documentsSketch, documentId + "_" + count, String.valueOf(hash));
            count++;

            // insert hash -> documentId
            // seek for last hash entry and append
            int count2 = 0;
            while (bt_Get(hashesDocuments, hash + "_" + count2) != null) {
                count2++;
            }
            bt_Put(hashesDocuments, hash + "_" + count2, String.valueOf(documentId));

        }

    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {

        int count = 0;
        while (bt_Get(similarDocuments, masterDocumentId + "_" + count) != null) {
            count++;
        }
        bt_Put(similarDocuments, masterDocumentId + "_" + count, String.valueOf(similarDocumentId));

    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {
        Set<Integer> result = new HashSet<Integer>();

        int count = 0;
        String tmp;
        while ((tmp = bt_Get(hashesDocuments, hash + "_" + count)) != null) {
            result.add(Integer.valueOf(tmp));
            count++;
        }

        return result;
    }

    @Override
    public int getNumberOfDocuments() {

        int result = 0;
        String key = "";
        while ((key = bt_Next(documentsSketch, key)) != null) {
            result = Integer.valueOf(key.split("_")[0]);
        }
        return result;

    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {
        Set<Integer> result = new HashSet<Integer>();

        int count = 0;
        String tmp;
        while ((tmp = bt_Get(similarDocuments, documentId + "_" + count)) != null) {
            result.add(Integer.valueOf(tmp));
            count++;
        }

        return result;
    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        Map<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
        Set<Integer> similarDocs = null;
        int currentMaster = -1;

        String key = "";
        while ((key = bt_Next(similarDocuments, key)) != null) {
            String[] split = key.split("_");
            int masterDocId = Integer.valueOf(split[0]);
            int similarDocId = Integer.valueOf(split[1]);
            if (currentMaster != masterDocId) {
                similarDocs = new HashSet<Integer>();
                result.put(masterDocId, similarDocs);
                currentMaster = masterDocId;
            }
            similarDocs.add(similarDocId);
        }

        return result;
    }

    @Override
    public Set<Long> getSketchForDocument(int documentId) {
        Set<Long> result = new HashSet<Long>();

        int count = 0;
        String tmp;
        while ((tmp = bt_Get(documentsSketch, documentId + "_" + count)) != null) {
            result.add(Long.valueOf(tmp));
            count++;
        }

        return result;
    }

}
