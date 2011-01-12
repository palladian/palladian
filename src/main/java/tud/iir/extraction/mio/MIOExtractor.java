/**
 * This class is the entry-point to InCoFi.
 * It allows to retrieve multimedia, interactive objects.
 * Concepts and Entities are loaded.
 * InCoFiConfiguration is initialized.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.Extractor;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;

public final class MIOExtractor extends Extractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOExtractor.class);

    /** The maximum number of extraction threads. */
    private static final int MAX_EXTRACTION_THREADS = 1;

    /** The path to the MIO model. */
    static String MIO_MODEL_PATH;

    /**
     * Instantiates a new MIOExtractor.
     */
    private MIOExtractor() {
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        addSuffixesToBlackList(Extractor.URL_TEXTUAL_BLACKLIST);

        // loadInCoFiConfiguration and prepare to use as singleton
        InCoFiConfiguration configuration = loadConfiguration();

        // its a trick for creating a singleton because of yml
        InCoFiConfiguration.instance = configuration;

        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MIO_MODEL_PATH = config.getString("models.palladian.mio");
        } else {
            MIO_MODEL_PATH = "";
        }
    }

    static class SingletonHolder {
        static MIOExtractor instance = new MIOExtractor();
    }

    public static MIOExtractor getInstance() {
        return SingletonHolder.instance;
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
    public void startExtraction(final boolean continueFromLastExtraction) {

        LOGGER.info("start MIO extraction");

        // reset stopped command
        setStopped(false);

        // load concepts and attributes from ontology (and rdb)
        KnowledgeManager kManager = DatabaseManager.getInstance().loadOntology();
        setKnowledgeManager(kManager);

        // loop until exit called
        while (!isStopped()) {

            // concepts
            ArrayList<Concept> concepts = knowledgeManager.getConcepts(true);

            // iterate through all concepts
            for (Concept currentConcept : concepts) {

                // load Entities from DB for current concept
                currentConcept.loadEntities(true);

                if (isStopped()) {
                    LOGGER.info("mio extraction process stopped");
                    // clean the SWF-File-DownloadDirectory
                    // FileHelper.cleanDirectory( InCoFiConfiguration.getInstance().tempDirPath);
                    break;
                }

                // iterate through all entities of current concept
                List<Entity> conceptEntities;
                if (continueFromLastExtraction) {
                    conceptEntities = currentConcept.getEntitiesByDate();
                } else {
                    conceptEntities = currentConcept.getEntities();
                }

                // conceptEntities.add(new Entity("Bill Gates" + Math.random(), new Concept("Person")));

                // wait for a certain time when no entities were found, then restart
                if (conceptEntities.isEmpty()) {
                    LOGGER.info("no entities for mio extraction, continue with next concept");
                    continue;
                }

                extractionThreadGroup = new ThreadGroup("mioExtractionThreadGroup" + DateHelper.getCurrentDatetime());

                for (Entity currentEntity : conceptEntities) {

                    if (isStopped()) {
                        LOGGER.info("mio extraction process stopped");
                        break;
                    }

                    // update live status
                    ExtractionProcessManager.liveStatus.setCurrentAction("Search for MIOs for "
                            + currentEntity.getName() + " (" + currentConcept.getName() + ")");

                    currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                    LOGGER.info("  start mio extraction process for entity \"" + currentEntity.getName() + "\" ("
                            + currentEntity.getConcept().getName() + ")");
                    Thread mioThread = new EntityMIOExtractionThread(extractionThreadGroup,
                            currentEntity.getSafeName() + "MIOExtractionThread", currentEntity, getKnowledgeManager());
                    mioThread.start();

                    LOGGER.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                    // sleep for a short time to let the threads start and increase the thread counter, otherwise too
                    // many threads get started
                    try {
                        Thread.sleep(2 * DateHelper.SECOND_MS);
                    } catch (InterruptedException e) {
                        LOGGER.warn(e.getMessage());
                        setStopped(true);
                    }

                    while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                        if (!waitForFreeThreadSlot(LOGGER, MAX_EXTRACTION_THREADS)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Save the extractionResults to database.
     * 
     * @param saveResults the save results
     */
    @Override
    protected void saveExtractions(final boolean saveResults) {
        if (saveResults) {
            LOGGER.info("saving MIOExtractionResults");
            getKnowledgeManager().saveExtractions();
        }

        // remove temporarily downloaded swf files
        FileHelper.delete(InCoFiConfiguration.getInstance().tempDirPath, true);
    }

    /**
     * load the concept-specific SearchVocabulary from configuration-file.
     * 
     * @return the conceptSearchVocabulary
     */
    private InCoFiConfiguration loadConfiguration() {
        InCoFiConfiguration returnValue = null;
        try {
            final InCoFiConfiguration config = Yaml.loadType(new File("config/inCoFiConfigurations.yml"),
                    InCoFiConfiguration.class);

            returnValue = config;
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
        return returnValue;
    }

    /**
     * Check if URL is allowed.
     * 
     * @param url the URL
     * @return true, if the URL allowed
     */
    @Override
    public boolean isURLallowed(final String url) {
        return super.isURLallowed(url);
    }

    /**
     * The main method.
     * 
     * @param abc the arguments
     */
    public static void main(final String[] abc) {

        final MIOExtractor mioEx = MIOExtractor.getInstance();
        mioEx.startExtraction(false);
        // mioEx.stopExtraction(true);
    }

}
