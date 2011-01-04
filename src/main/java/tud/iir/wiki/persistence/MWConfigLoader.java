package tud.iir.wiki.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;

import tud.iir.wiki.MediaWikiCrawler;
import tud.iir.wiki.data.MWCrawlerConfiguration;
import tud.iir.wiki.data.WikiDescriptor;
import tud.iir.wiki.data.WikiDescriptorYAML;

public class MWConfigLoader {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MWConfigLoader.class);

    /** Relative path to MediaWiki crawler configuration file in YAML */
    private static final String CONFIG_FILE_PATH = "config/mwCrawlerConfiguration.yml";

    /** The instance of this class. */
    private static MWConfigLoader instance = null;

    /** The instance of the {@link MediaWikiDatabase} which acts as persistence layer. */
    private final MediaWikiDatabase MW_DATABASE = MediaWikiDatabase.getInstance();

    /**
     * Instantiates a new MWConfigLoader.
     */
    private MWConfigLoader() {
        // loadInCoFiConfiguration and prepare to use as singleton
        MWCrawlerConfiguration configuration = loadConfigurationFromConfigFile();

        // its a trick for creating a singleton because of yml
        MWCrawlerConfiguration.instance = configuration;
    }

    /**
     * Gets the single instance of MWConfigLoader.
     * 
     * @return single instance of MWConfigLoader
     */
    public static MWConfigLoader getInstance() {
        if (instance == null) {
            instance = new MWConfigLoader();
        }
        return instance;
    }

    /**
     * Load the concept-specific MWCrawlerConfiguration from configuration-file {@link #CONFIG_FILE_PATH}.
     * 
     * TODO: change to load from central Aletheia config
     * 
     * @return the MWCrawlerConfiguration, loaded from config file.
     */
    private MWCrawlerConfiguration loadConfigurationFromConfigFile() {
        MWCrawlerConfiguration returnValue = null;
        try {
            final MWCrawlerConfiguration config = Yaml
                    .loadType(new File(CONFIG_FILE_PATH),
                    MWCrawlerConfiguration.class);

            returnValue = config;
        } catch (FileNotFoundException e) {

            LOGGER.error(e.getMessage());
        }
        return returnValue;
    }

    /**
     * <p>
     * Load the crawler configuration from file (see {@link #loadConfigurationFromConfigFile()}) and--if already
     * existent--from database, and write (updated) config to database.
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
    public void initializeCrawlers() {
        // load known Wikis from db
        TreeMap<String, WikiDescriptor> wikisInDB = new TreeMap<String, WikiDescriptor>();
        for (WikiDescriptor wd : MW_DATABASE.getAllWikiDescriptors()) {
            wikisInDB.put(wd.getWikiName(), wd);
        }

        // load Wikis from config file, replace null/missing values by defaults (in WikiDescriptor)
        Set<WikiDescriptor> wikisInConfigFile = new HashSet<WikiDescriptor>();
        for (WikiDescriptorYAML wdYAML : MWCrawlerConfiguration.getInstance().getWikiConfigurations()) {
            WikiDescriptor wd = new WikiDescriptor();
            try {
                wd.setWikiName(wdYAML.wikiName);
                wd.setWikiURL(wdYAML.wikiURL);
                wd.setPathToAPI(wdYAML.pathToAPI);
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
                wd.setLastCheckForModifications(wikisInDB.get(wikiName).getLastCheckForModifications());
                MW_DATABASE.updateWiki(wd);
                wikisInDB.remove(wd.getWikiName());
            } else {
                MW_DATABASE.addWiki(wd);
            }
        }

        // remove all Wikis in db that are in db but not in config file
        for (WikiDescriptor wikiToDelete : wikisInDB.values()) {
            MW_DATABASE.removeWiki(wikiToDelete.getWikiID());
        }

        createCrawlers();
    }

    /**
     * Creates for every Wiki in the database an own {@link MediaWikiCrawler}.
     */
    private void createCrawlers() {
        for (WikiDescriptor wikis : MW_DATABASE.getAllWikiDescriptors()) {
            // MediaWikiCrawler mwCrawler = new MediaWikiCrawler(wikis.getWikiName());
            // add multi thread support here to run every crawler with own thread or better, multiple threads per Wiki

            Thread mediaWikiCrawler = new Thread(new MediaWikiCrawler(wikis.getWikiName()), "WikID-"
                    + wikis.getWikiID());
            mediaWikiCrawler.start();

            // mwCrawler.run();
        }
    }


    /**
     * Helper for debugging to reset the database.
     */
    @SuppressWarnings("unused")
    private static void resetDatabase() {
        // FIXME: change logging if function is used for normal program execution!
        LOGGER.fatal("Reseting database! ");
        for (WikiDescriptor wiki : MediaWikiDatabase.getInstance().getAllWikiDescriptors()) {
            MediaWikiDatabase.getInstance().removeWiki(wiki.getWikiID());
            LOGGER.fatal("Removed all data for Wiki \"" + wiki.getWikiName() + "\".");
        }
    }

    public static void main(String[] args) throws Exception {
        //
        // Locale.setDefault(Locale.ENGLISH);
        // TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
        // resetDatabase(); // debug code

        MWConfigLoader.getInstance().initializeCrawlers();
    }


}
