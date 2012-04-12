package ws.palladian.retrieval.wiki.data;

import java.util.List;

import org.apache.log4j.Logger;

/**
 * Represents the content of the MediaWiki configuration file. See call hierarchy of {@link #getInstance()} for
 * file name.
 * 
 * @author Sandro Reichert
 * @author Philipp Katz
 */
public class MWCrawlerConfiguration {

    /** The global logger */
    private static final Logger LOGGER = Logger.getLogger(MWCrawlerConfiguration.class);

    /** The instance. */
    public static MWCrawlerConfiguration instance = null;

    /** The list of Wiki descriptors found in the config file. */
    public List<WikiDescriptorYAML> wikiConfigurations;

    /**
     * Gets the single instance of MWCrawlerConfiguration.
     * 
     * @return single instance of MWCrawlerConfiguration
     */
    public static MWCrawlerConfiguration getInstance() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MWdiaWiki Configuration loaded from file: " + instance.toString());
        }
        return instance;
    }

    /**
     * Get all Wiki configurations from config file.
     * 
     * @return all Wiki configurations from config file, each Wiki is represented by one {@link WikiDescriptorYAML}.
     */
    public final List<WikiDescriptorYAML> getWikiConfigurations() {
        return wikiConfigurations;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MWCrawlerConfiguration [wikiConfigurations=" + wikiConfigurations + "]";
    }

}
