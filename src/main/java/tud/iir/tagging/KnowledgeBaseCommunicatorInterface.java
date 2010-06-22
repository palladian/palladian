package tud.iir.tagging;

import tud.iir.classification.CategoryEntries;

public interface KnowledgeBaseCommunicatorInterface {
    public abstract CategoryEntries categoryEntriesInKB(String entityName);

    public abstract EntityList getTrainingEntities(double percentage);
}
