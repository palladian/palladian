/**
 * This class retrieves MIOs for one given entity. Therefore, retrieving MIOs can be parallelized on the entity level.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.classification.mio.MIOClassifier;
import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.MIO;
import tud.iir.knowledge.Source;

/**
 * The EntityMIOExtractionThread extracts MIOs for one given entity. Therefore, extracting MIOs can be parallelized on
 * the entity level.
 * 
 * @author Martin Werner
 */
public class EntityMIOExtractionThread extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EntityMIOExtractionThread.class);

    /** The central entity. */
    private final transient Entity entity;

    /** The knowledgeManager. */
    private final transient KnowledgeManager knowledgeManager;

    /**
     * Instantiates a new entity MIOExtractionThread.
     * 
     * @param threadGroup the thread group
     * @param entityName the entityName
     * @param entity the entity
     * @param knowledgeManager the knowledgeManager
     */
    public EntityMIOExtractionThread(ThreadGroup threadGroup, String entityName, Entity entity,
            KnowledgeManager knowledgeManager) {
        super(threadGroup, entityName);
        this.entity = entity;
        this.knowledgeManager = knowledgeManager;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        MIOExtractor.getInstance().increaseThreadCount();
        long timeStamp1 = System.currentTimeMillis();

        // get MIO containing pages
        List<MIO> mios = analyzeEntity(false);

        // if no MIO was found, redo with focus on weak-interactive MIOs (it must be allowed in configFile)
        if (mios.isEmpty() && InCoFiConfiguration.getInstance().redoWeak) {
            LOGGER.info("REDO with focus on weakInteraction for " + entity.getName());
            analyzeEntity(true);
        }

        // load the trainedClassifier
        MIOClassifier mioClass = new MIOClassifier();
        mioClass.loadTrainedClassifier(MIOExtractor.MIO_MODEL_PATH);

        Set<MIO> mioSet = new HashSet<MIO>();
        for (MIO mio : mios) {

            // calculate trust
            mioClass.classify(mio);
            mioSet.add(mio);
        }

        // remove MIOs that do not fulfill the trust-limit
        mioSet = removeLowTrustedMIOs(mioSet);

        // detect and save rolePages
        detectRolePages(mioSet, entity);

        // check that directURL- and findPageURL-length is not longer than 767
        for (MIO mio : mioSet) {
            if (mio.getDirectURL().length() >= 767) {
                String directURL = mio.getDirectURL().substring(0, 766);
                mio.setDirectURL(directURL);
            }
            if (mio.getFindPageURL().length() >= 767) {
                String findPageURL = mio.getFindPageURL().substring(0, 766);
                mio.setFindPageURL(findPageURL);
            }

            // prepare to save the retrieved MIOs with existing database-scheme
            prepareSavingResults(mio);
        }

        LOGGER.info("Thread finished in " + DateHelper.getRuntime(timeStamp1) + "  " + mios.size() + " MIOs for \""
                + entity.getName() + "\" were found.");

        // save extraction-results to database
        knowledgeManager.saveExtractions();

        MIOExtractor.getInstance().decreaseThreadCount();
    }

    /**
     * Start analyzing an entity in detail.
     * 
     * @param weakFlag the flag for retrieving weak-interactive MIOs (redo-function)
     * @return the list of retrieved MIOs
     */
    private List<MIO> analyzeEntity(boolean weakFlag) {
        MIOPageRetriever pageRetr = new MIOPageRetriever();
        List<MIOPage> mioPages = pageRetr.retrieveMIOPages(entity, weakFlag);

        // MIOAnalysis (content & context)
        UniversalMIOExtractor mioExtr = new UniversalMIOExtractor(entity);
        List<MIO> mios = mioExtr.analyzeMIOPages(mioPages);

        return mios;
    }

    /**
     * Detect rolePages.
     * 
     * @param mioSet the set of retrieved MIOs
     * @param entity the entity
     */
    private void detectRolePages(Set<MIO> mioSet, Entity entity) {
        RolePageDetector rpDetector = new RolePageDetector(entity);
        rpDetector.detectRolePages(mioSet);
    }

    /**
     * Prepare the saving of extractionResults.
     * Associate Attribute, FactValue, Trust and Fact with Entity.
     * 
     * @param mio the MIO
     */
    private void prepareSavingResults(final MIO mio) {
        final Attribute attribute = new Attribute(mio.getInteractivityGrade() + " mio", Attribute.VALUE_STRING,
                entity.getConcept());
        attribute.setExtractedAt(new Date(System.currentTimeMillis()));

        final FactValue factValue = new FactValue(mio.getDirectURL(), new Source(mio.getFindPageURL()), 0);
        factValue.setTrust(mio.getTrust());
        factValue.setExtractedAt(new Date(System.currentTimeMillis()));

        entity.addFactAndValue(new Fact(attribute), factValue);
    }

    /**
     * Removes the low-trusted MIOs.
     * 
     * @param mioSet the set of MIOs
     * @return the set of MIOs which fulfill the trustLimit
     */
    private Set<MIO> removeLowTrustedMIOs(final Set<MIO> mioSet) {
        // get the excludingTrustLimit from config
        final double trustLimit = InCoFiConfiguration.getInstance().excludingTrustLimit;

        final Set<MIO> resultSet = new HashSet<MIO>();
        for (MIO mio : mioSet) {
            if (mio.getTrust() >= trustLimit) {
                resultSet.add(mio);
            }
        }
        return resultSet;
    }
}
