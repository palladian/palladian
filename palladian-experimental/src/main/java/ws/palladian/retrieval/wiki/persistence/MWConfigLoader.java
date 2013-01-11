package ws.palladian.retrieval.wiki.persistence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.wiki.MediaWikiCrawler;
import ws.palladian.retrieval.wiki.data.MWCrawlerConfiguration;
import ws.palladian.retrieval.wiki.data.WikiDescriptor;
import ws.palladian.retrieval.wiki.data.WikiDescriptorYAML;
import ws.palladian.retrieval.wiki.data.WikiPage;

/**
 * @author Sandro Reichert
 * @author Philipp Katz
 *
 */
public final class MWConfigLoader {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MWConfigLoader.class);

    /** Relative path to MediaWiki crawler configuration file in YAML */
    protected static final String CONFIG_FILE_PATH = "config/mwCrawlerConfiguration.yml";

    /** The instance of this class. */
    protected static MWConfigLoader instance = null;

    /** The {@link MediaWikiDatabase} which acts as persistence layer. */
    protected final MediaWikiDatabase mwDatabase = DatabaseManagerFactory.create(MediaWikiDatabase.class, ConfigHolder.getInstance().getConfig());

    /**
     * Instantiates a new MWConfigLoader.
     * 
     * @param pageQueue The queue that is used by {@link MediaWikiCrawler}s to put processed pages in and consumers that
     *            process these pages.
     */
    private MWConfigLoader(LinkedBlockingQueue<WikiPage> pageQueue) {
        // load MWCrawlerConfiguration from file and prepare to use as singleton
        MWCrawlerConfiguration configuration = loadConfigurationFromConfigFile();

        // its a trick for creating a singleton because of yml
        MWCrawlerConfiguration.instance = configuration;

        processConfiguration();
        createCrawlers(pageQueue);
    }

    /**
     * Does the complete initialization of the MediaWiki crawlers when called the first time. The configuration is
     * loaded from local configuration file {@link #CONFIG_FILE_PATH}, written to database and crawlers are created.
     * Additional calls of have no effect.
     * 
     * @param pageQueue The queue that is used by {@link MediaWikiCrawler}s to put processed pages in and consumers that
     *            process these pages.
     */
    public static void initialize(LinkedBlockingQueue<WikiPage> pageQueue) {
        if (instance == null) {
            instance = new MWConfigLoader(pageQueue);
        } else {
            LOGGER.warn("MediaWiki crawlers have already been initialized! Doing nothing.");
        }
    }

    /**
     * Load the concept-specific MWCrawlerConfiguration from configuration-file {@link #CONFIG_FILE_PATH}.
     * 
     * @return the MWCrawlerConfiguration, loaded from config file.
     */
    private MWCrawlerConfiguration loadConfigurationFromConfigFile() {
        MWCrawlerConfiguration returnValue = null;
        try {
            
            // set up the YAML parser and define the class mapping
            Constructor constructor = new Constructor(MWCrawlerConfiguration.class);
            TypeDescription wikiConfigDescription = new TypeDescription(MWCrawlerConfiguration.class);
            wikiConfigDescription.putListPropertyType("wikiConfigurations", WikiDescriptorYAML.class);
            constructor.addTypeDescription(wikiConfigDescription);
            Yaml yaml = new Yaml(constructor);
            
            returnValue = (MWCrawlerConfiguration) yaml.load(new FileInputStream(CONFIG_FILE_PATH));
            
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
        return returnValue;
    }

    /**
     * <p>
     * Loads the crawler configuration from file (see {@link #loadConfigurationFromConfigFile()}) and--if already
     * existent--from database, and writes (updates) config to database.
     * </p>
     * <p>
     * Details:<br />
     * <ul>
     * <li>load configDB from db</li>
     * <li>load configFile from file</li>
     * <li>validate configFile</li>
     * <li>replace missing values in configFile by defaults</li>
     * <li>loop through WikiDescriptors from file, update entries already existing in configDB and add new Wikis to db</li>
     * <li>remove orphan Wikis and their content from db that are not in configFile</li>
     * </ul>
     * </p>
     */
    private void processConfiguration() {
        // load known Wikis from db
        TreeMap<String, WikiDescriptor> wikisInDB = new TreeMap<String, WikiDescriptor>();
        for (WikiDescriptor wd : mwDatabase.getAllWikiDescriptors()) {
            wikisInDB.put(wd.getWikiName(), wd);
        }

        // load Wikis from config file, replace null/missing values by defaults (in WikiDescriptor)
        Set<WikiDescriptor> wikisInConfigFile = new HashSet<WikiDescriptor>();
        for (WikiDescriptorYAML wdYAML : MWCrawlerConfiguration.getInstance().getWikiConfigurations()) {
            WikiDescriptor wd = new WikiDescriptor();
            try {
                wd.setWikiName(wdYAML.wikiName);
                wd.setWikiURL(wdYAML.wikiURL);
                wd.setRelativePathToAPI(wdYAML.pathToAPI);
                wd.setRelativePathToContent(wdYAML.pathToContent);
                wd.setCrawlerUserName(wdYAML.crawlerUserName);
                wd.setCrawlerPassword(wdYAML.crawlerPassword);
                wd.setNamespacesToCrawl((wdYAML.getNamespacesToCrawl() == null) ? new HashSet<Integer>() : wdYAML
                        .getNamespacesToCrawl());
                wikisInConfigFile.add(wd);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Could not read Wiki description " + wdYAML.toString() + " : ", e);
            }
        }

        // loop through descriptors from file, update entries already existing in db and add new Wikis to db
        String wikiName = "";
        for (WikiDescriptor wd : wikisInConfigFile) {
            wikiName = wd.getWikiName();
            if (wikisInDB.containsKey(wikiName)) {
                // copy internal stuff from db that can not be contained in config file
                wd.setWikiID(wikisInDB.get(wikiName).getWikiID());
                Date lastCheck = wikisInDB.get(wikiName).getLastCheckForModifications();
                if (lastCheck != null) {
                    wd.setLastCheckForModifications(lastCheck);
                }
                mwDatabase.updateWiki(wd);
                wikisInDB.remove(wd.getWikiName());
            } else {
                mwDatabase.addWiki(wd);
            }
        }

        // remove all Wikis in db that are in db but not in config file
        for (WikiDescriptor wikiToDelete : wikisInDB.values()) {
            mwDatabase.removeWiki(wikiToDelete.getWikiID());
        }
    }

    /**
     * Creates an own {@link MediaWikiCrawler} for every Wiki in the database, running as own thread.
     */
    private void createCrawlers(LinkedBlockingQueue<WikiPage> pageQueue) {
        for (WikiDescriptor wikis : mwDatabase.getAllWikiDescriptors()) {
            Thread mwCrawler = new Thread(new MediaWikiCrawler(wikis.getWikiName(), pageQueue, mwDatabase), "WikID-"
                    + wikis.getWikiID());
            mwCrawler.start();
        }
    }

    /**
     * Debug helper to reset the database. All Wikis and their complete content is removed from the database.
     * Use with caution...
     */
    @SuppressWarnings("unused")
    private void resetDatabase() {
        LOGGER.warn("Reseting database! ");
        for (WikiDescriptor wiki : mwDatabase.getAllWikiDescriptors()) {
            mwDatabase.removeWiki(wiki.getWikiID());
            LOGGER.warn("Removed all data for Wiki \"" + wiki.getWikiName() + "\".");
        }
    }

}
