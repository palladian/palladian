package tud.iir.extraction.snippet;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import tud.iir.extraction.Extractor;
import tud.iir.helper.DateHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;

/**
 * The SnippetExtractor class extends the Extractor singleton class, retrieves all entities from the knowledge base and schedules k thread runs in parallel,
 * where k is the number of entities.
 * 
 * For each entity a separate thread is started. Each thread is a subclass of EntitySnippetExtractionThread. To avoid overloading the system, a threading queue
 * allows to only run i threads in parallel.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class SnippetExtractor extends Extractor {

    private static final Logger logger = Logger.getLogger(SnippetExtractor.class);

    private static SnippetExtractor instance = null;

    protected static final int MAX_EXTRACTION_THREADS = 3;

    private SnippetExtractor() {
    }

    public static SnippetExtractor getInstance() {
        if (instance == null) {
            instance = new SnippetExtractor();
        }
        return instance;
    }

    /**
     * Start extraction of snippets for entities that are fetched from the knowledge base. Continue from last extraction.
     */
    public void startExtraction() {
        startExtraction(true);
    }

    public void startExtraction(boolean continueFromLastExtraction) {

        // reset stopped command
        setStopped(false);

        // load concepts and attributes from ontology (and rdb) and to know what
        // to extract
        if (!isBenchmark()) {
            KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
            setKnowledgeManager(km);
        } else {
            KnowledgeManager km = new KnowledgeManager();
            km.createSnippetBenchmarks();
            setKnowledgeManager(km);
        }

        // loop until exit called
        // while (!isStopped()) {

        // concepts
        ArrayList<Concept> concepts = knowledgeManager.getConcepts(true); // TODO?

        // iterate through all concepts
        for (Concept currentConcept : concepts) {

            System.out.println("Concept: " + currentConcept.getName());

            if (isStopped()) {
                logger.info("snippet extraction process stopped");
                break;
            }

            // iterate through all entities for current concept
            if (!isBenchmark()) {
                currentConcept.loadEntities(continueFromLastExtraction);
            }
            ArrayList<Entity> conceptEntities;
            if (continueFromLastExtraction) {
                conceptEntities = currentConcept.getEntitiesByDate();
            } else {
                conceptEntities = currentConcept.getEntities();
            }

            // wait for a certain time when no entities were found, then
            // restart
            if (conceptEntities.size() == 0) {
                logger.info("no entities for snippet extraction, continue with next concept");
                continue;
            }

            ThreadGroup extractionThreadGroup = new ThreadGroup("snippetExtractionThreadGroup");

            for (Entity currentEntity : conceptEntities) {

                if (isStopped()) {
                    logger.info("snippet extraction process stopped");
                    break;
                }

                currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                logger.info("  start snippet extraction process for entity \"" + currentEntity.getName() + "\" (" + currentEntity.getConcept().getName() + ")");
                Thread snippetThread = new EntitySnippetExtractionThread(extractionThreadGroup, currentEntity.getSafeName() + "SnippetExtractionThread",
                        currentEntity);
                snippetThread.start();

                logger.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());
                System.out.println("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                    logger.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ")");
                    System.out.println("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ")");
                    ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                }

            }
        }

        // save extraction results after each full loop
        if (!isBenchmark()) {
            getKnowledgeManager().saveExtractions();
        } else {
            getKnowledgeManager().evaluateBenchmarkExtractions();
            logger.info("finished benchmark");
            // break;
        }
        // }
    }

    @Override
    protected void saveExtractions(boolean saveResults) {
        if (saveResults && !isBenchmark()) {
            System.out.println("save extractions now");
            getKnowledgeManager().saveExtractions();
        }
    }

    public static void main(String[] abc) {
        // Controller.getInstance();

        long t1 = System.currentTimeMillis();
        SnippetExtractor se = SnippetExtractor.getInstance();
        se.setKnowledgeManager(DatabaseManager.getInstance().loadOntology());
        // se.setBenchmark(true);
        se.startExtraction(false);
        // se.stopExtraction(true);
        DateHelper.getRuntime(t1, System.currentTimeMillis(), true);
    }
}