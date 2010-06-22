package tud.iir.extraction.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tud.iir.knowledge.Entity;

/**
 * The EntityExtraction class stores entity extractions.
 * 
 * @author David Urbansky
 */
class EntityExtraction {
    private Entity entity;
    private HashMap<String, Integer> retrievalExtractionAndQueryTypes;

    public EntityExtraction() {
        retrievalExtractionAndQueryTypes = new HashMap<String, Integer>();
    }

    public void addExtraction(int retrievalExtractionType, int queryType) {
        String key = retrievalExtractionType + "_" + queryType;
        if (retrievalExtractionAndQueryTypes.containsKey(key)) {
            int count = retrievalExtractionAndQueryTypes.get(key);
            retrievalExtractionAndQueryTypes.put(key, count + 1);
        } else {
            retrievalExtractionAndQueryTypes.put(key, 1);
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public String printRetrievalExtractionAndQueryTypes() {
        StringBuilder sb = new StringBuilder();

        Iterator<Map.Entry<String, Integer>> i = retrievalExtractionAndQueryTypes.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Integer> entry = i.next();
            sb.append(entry.getKey() + ":" + entry.getValue() + ",");
        }

        return sb.toString().substring(0, sb.length() - 1);
    }
}