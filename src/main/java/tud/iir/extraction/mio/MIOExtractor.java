/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;

import tud.iir.extraction.Extractor;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.persistence.DatabaseManager;

public final class MIOExtractor extends Extractor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(MIOExtractor.class);

    /** The instance. */
    private static MIOExtractor instance = null;

    /** The Constant MAX_EXTRACTION_THREADS. */
    protected static final int MAX_EXTRACTION_THREADS = 3;

    /**
     * Instantiates a new mIO extractor.
     */
    private MIOExtractor() {
        super();
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        addSuffixesToBlackList(Extractor.URL_TEXTUAL_BLACKLIST);
    }

    /**
     * Gets the single instance of MIOExtractor.
     * 
     * @return single instance of MIOExtractor
     */
    public static MIOExtractor getInstance() {
        if (instance == null) {
            instance = new MIOExtractor();
        }
        return instance;
    }

    /**
     * Start extraction of MIOs for entities that are fetched from the knowledge base. Continue from last extraction.
     */
    public void startExtraction() {
        startExtraction(true);
    }

    /**
     * Start extraction.
     * 
     * @param continueFromLastExtraction the continue from last extraction
     */
    public void startExtraction(boolean continueFromLastExtraction) {

        LOGGER.info("start MIO extraction");

        // reset stopped command
        setStopped(false);

        // // load concepts and attributes from ontology (and rdb) and to know
        // what
        // // to extract
        // if (!isBenchmark()) {
        // System.out.println("entires: " + DatabaseManager.getInstance().getTotalConceptsNumber());
        // System.out.println(con.toString());
        // OntologyManager.getInstance().clearCompleteKnowledgeBase();

        // setKnowledgeManager(km);
        // } else {

        // KnowledgeManager km = new KnowledgeManager();

        // setKnowledgeManager(km);
        // }
        //
        // // loop until exit called
        // while (!isStopped()) {

        // concepts
        // ArrayList<Concept> concepts1 = km.getConcepts(true);
        final ArrayList<Concept> concepts = DatabaseManager.getInstance().loadConcepts();

        // create a new concept without databaseusage
        // ArrayList<Concept> concepts = new ArrayList<Concept>();
        // Concept exampleConcept = new Concept("mobilePhone");
        // Entity exampleEntity_1 = new Entity("Samsung S8500 Wave", exampleConcept);
        // Entity exampleEntity_2 = new Entity("HTC Desire", exampleConcept);
        // exampleConcept.addEntity(exampleEntity_1);
        // exampleConcept.addEntity(exampleEntity_2);
        // concepts.add(exampleConcept);
        //
        // Concept headphoneConcept = new Concept("headphone");
        // Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        // Entity headphone2 = new Entity("Sennheiser HD800", headphoneConcept);
        // headphoneConcept.addEntity(headphone1);
        // headphoneConcept.addEntity(headphone2);
        // concepts.add(headphoneConcept);

        // loadSearchVocabulary
        final ConceptSearchVocabulary searchVoc = loadSearchVocabulary();
        // iterate through all concepts
        for (Concept currentConcept : concepts) {

            // System.out.println("Concept: " + currentConcept.getName());

            if (currentConcept.getName().equals("mobilePhone")) {
                // load Entities from DB for current concept
                currentConcept.loadEntities(false);
            }

            if (isStopped()) {
                LOGGER.info("mio extraction process stopped");
                break;
            }

            // iterate through all entities for current concept
            // if (!isBenchmark()) {
            // currentConcept.loadEntities(continueFromLastExtraction);
            // }
            ArrayList<Entity> conceptEntities;
            if (continueFromLastExtraction) {
                conceptEntities = currentConcept.getEntitiesByDate();
            } else {
                conceptEntities = currentConcept.getEntities();
            }

            // wait for a certain time when no entities were found, then
            // restart
            if (conceptEntities.isEmpty()) {
                LOGGER.info("no entities for mio extraction, continue with next concept");
                continue;
            }

            final ThreadGroup extractionThreadGroup = new ThreadGroup("mioExtractionThreadGroup");

            for (Entity currentEntity : conceptEntities) {

                if (isStopped()) {
                    LOGGER.info("mio extraction process stopped");
                    break;
                }

                // System.out.println("Concept: " + currentConcept.getName() + "Entity: " +
                // currentEntity.getName());

                currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                LOGGER.info("  start mio extraction process for entity \"" + currentEntity.getName() + "\" ("
                        + currentEntity.getConcept().getName() + ")");
                final Thread mioThread = new EntityMIOExtractionThread(extractionThreadGroup,
                        currentEntity.getSafeName() + "MIOExtractionThread", currentEntity, searchVoc);
                mioThread.start();

                LOGGER.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());
                // System.out.println("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                int count = 0;
                while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                    LOGGER.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ") "
                            + extractionThreadGroup.activeCount() + "," + extractionThreadGroup.activeGroupCount());
                    if (extractionThreadGroup.activeCount() + extractionThreadGroup.activeGroupCount() == 0) {
                        LOGGER.warn("apparently " + getThreadCount()
                                + " threads have not finished correctly but thread group is empty, continuing...");
                        resetThreadCount();
                        break;
                    }
                    // System.out.println("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ")");
                    ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                    if (isStopped()) {
                        count++;
                    }
                    if (count > 1) {
                        LOGGER.info("waited 25 iterations after stop has been called, breaking now");
                        break;
                    }
                }

            }
        }
        // }

        // // save extraction results after each full loop
        // if (!isBenchmark()) {
        // getKnowledgeManager().saveExtractions();
        // } else {
        // getKnowledgeManager().evaluateBenchmarkExtractions();
        // logger.info("finished benchmark");
        // // break;
        // }
        // }

    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.Extractor#saveExtractions(boolean)
     */
    @Override
    protected void saveExtractions(boolean saveResults) {
        if (saveResults && !isBenchmark()) {
            // System.out.println("save extractions now");
            getKnowledgeManager().saveExtractions();
        }
    }

    /**
     * load the concept-specific SearchVocabulary from .yml-file
     * 
     * @return the concept search vocabulary
     */
    private ConceptSearchVocabulary loadSearchVocabulary() {
        try {
            final ConceptSearchVocabulary cSearchVoc = Yaml.loadType(new File(
                    "data/knowledgeBase/conceptSearchVocabulary.yml"), ConceptSearchVocabulary.class);

            return cSearchVoc;
        } catch (FileNotFoundException e) {

            LOGGER.error(e.getMessage());
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.Extractor#isURLallowed(java.lang.String)
     */
    public boolean isURLallowed(final String url) {
        super.addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        super.addSuffixesToBlackList(Extractor.URL_TEXTUAL_BLACKLIST);
        return super.isURLallowed(url);
    }

    /**
     * The main method.
     * 
     * @param abc the arguments
     */
    public static void main(final String[] abc) {
        // Controller.getInstance();

        // long t1 = System.currentTimeMillis();
        final MIOExtractor mioEx = MIOExtractor.getInstance();

        // mioEx.setKnowledgeManager(DatabaseManager.getInstance().loadOntology());
        // se.setBenchmark(true);

        mioEx.startExtraction(false);
        // se.stopExtraction(true);

        // DateHelper.getRuntime(t1, true);
    }

}
