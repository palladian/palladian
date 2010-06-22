package tud.iir.extraction.entity;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Concept;

public class EntityExtractionThread extends Thread {

    private EntityExtractionTechnique entityExtractionTechnique = null;
    private EntityQuery entityQuery = null;
    private String url = "";
    private Concept concept = null;

    public EntityExtractionThread(ThreadGroup threadGroup, String name, EntityExtractionTechnique entityExtractionTechnique, EntityQuery entityQuery,
            Concept concept, String url) {
        super(threadGroup, name);
        this.entityExtractionTechnique = entityExtractionTechnique;
        this.entityQuery = entityQuery;
        this.concept = concept;
        this.url = url;
    }

    @Override
    public void run() {
        EntityExtractor.getInstance().increaseThreadCount();
        long t1 = System.currentTimeMillis();

        try {
            entityExtractionTechnique.extract(url, entityQuery, concept);
        } catch (Exception e) {
            Logger.getLogger(EntityExtractor.class).error("Exception in at entityExtractionTechnique.extract", e);
        }

        Logger.getLogger(EntityExtractor.class).info("Thread finished in " + DateHelper.getRuntime(t1) + ", tried to extract entities from " + url);
        EntityExtractor.getInstance().decreaseThreadCount();
    }
}
