package ws.palladian.tagging;

import ws.palladian.classification.CategoryEntries;

public interface KnowledgeBaseCommunicatorInterface {
    public abstract CategoryEntries categoryEntriesInKB(String entityName);

    public abstract EntityList getTrainingEntities(double percentage);
}
