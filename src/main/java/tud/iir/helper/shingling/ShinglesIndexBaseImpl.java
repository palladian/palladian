package tud.iir.helper.shingling;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ShinglesIndexBaseImpl implements ShinglesIndex {

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
