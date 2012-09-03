package ws.palladian.retrieval.wiki.data;

import java.util.HashSet;

import ws.palladian.retrieval.wiki.MediaWikiCrawler;
import ws.palladian.retrieval.wiki.persistence.MWConfigLoader;

/**
 * Simple Helper to load the config from a YAML file.
 * 
 * @author Sandro Reichert
 * 
 */
public class WikiDescriptorYAML {

    /** Unique name of the Wiki as written in config file (see {@link MWConfigLoader}. */
    public String wikiName = null;

    /** URL of the Wiki as written in config file (see {@link MWConfigLoader}, like "http://en.wikipedia.org/". */
    public String wikiURL = null;

    /** Path to Wiki API (api.php) if API can not be found at {@link #wikiURL}, like "/w/" for wikipedia. */
    public String pathToAPI = null;

    /**
     * Path to wiki pages, relative from {@link #wikiURL}, like /wiki/ as used in wikipedia (resulting path is
     * http://de.wikipedia.org/wiki/)
     */
    public String pathToContent = null;

    /** User name the {@link MediaWikiCrawler} uses to log into the Wiki for reading content. */
    public String crawlerUserName = null;

    /** Password the {@link MediaWikiCrawler} uses to log into the Wiki for reading content. */
    public String crawlerPassword = null;

    /**
     * namespace id's to use for crawling, separated by commas ",". All pages in this namespace are crawled, no page
     * of any other namespace.
     */
    public String namespacesToCrawl = null;

    /**
     * Default empty constructor, all parameters are set separately.
     */
    public WikiDescriptorYAML() {
    }

    /**
     * 
     * Converts the internal String representation of {@link #namespacesToCrawl} to a set and returns it. All pages in
     * this namespace are crawled, no page of any other namespace. If no namespace is specified in the config file,
     * null is returned (which is interpreted as all names paces have to be retrieved from the Wiki, so the complete
     * Wiki is crawled).
     * 
     * @return Set of namespace id's to use for crawling or null, if no namespaces are defined.
     */
    public final HashSet<Integer> getNamespacesToCrawl() {
        HashSet<Integer> output = null;
        if (namespacesToCrawl != null && !namespacesToCrawl.equalsIgnoreCase("")) {
            output = new HashSet<Integer>();
            final String outputArray[] = namespacesToCrawl.split(",");
            for (int i = 0; i < outputArray.length; i++) {
                if (!outputArray[i].trim().equalsIgnoreCase("")) {
                    output.add(Integer.parseInt(outputArray[i].trim()));
                }
            }
        }
        return output;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "WikiDescriptorYAML [wikiName=" + wikiName + ", wikiURL=" + wikiURL + ", pathToAPI=" + pathToAPI
                + ", pathToContent=" + pathToContent + ", crawlerUserName=" + crawlerUserName + ", crawlerPassword="
                + crawlerPassword + ", namespacesToCrawl=" + namespacesToCrawl + "]";
    }

}
