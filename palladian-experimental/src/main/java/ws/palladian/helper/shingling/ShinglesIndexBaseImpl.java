package ws.palladian.helper.shingling;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Base ShinglesIndex implementation, with common functionality. {@link #openIndex()} and {@link #saveIndex()} can be
 * overridden by subclasses as necessary.
 * 
 * @author Philipp Katz
 * 
 */
public abstract class ShinglesIndexBaseImpl implements ShinglesIndex {
    
    /** class logger. */
    protected static final Logger LOGGER = Logger.getLogger(ShinglesIndexBaseImpl.class);
    
    /** default directory where to store serialized shingles. */
    public static final String INDEX_FILE_BASE_PATH = "data/temp/shingles/";
    
    /** name of the index. */
    private String indexName = "shingles";
    
    @Override
    public String getIndexName() {
        return indexName;
    }
    
    @Override
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    @Override
    public void openIndex() {
        // do nothing by default, override if necessary.
    }
    
    @Override
    public void saveIndex() {
        // do nothing by default, override if necessary.
    }
    
    @Override
    public void deleteIndex() {
        // do nothing by default, override if necessary.
    }

    @Override
    public Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch) {

        Map<Integer, Set<Long>> result = new HashMap<Integer, Set<Long>>();

        for (Long hash : sketch) {
            Set<Integer> documentsForHash = getDocumentsForHash(hash);
            for (Integer document : documentsForHash) {
                result.put(document, getSketchForDocument(document));
            }
        }

        return result;
    }

}
