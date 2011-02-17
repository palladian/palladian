package ws.palladian.web.wiki.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;

import ws.palladian.web.wiki.MediaWikiCrawler;
import ws.palladian.web.wiki.persistence.MWConfigLoader;

/**
 * The configuration of a single {@link MediaWikiCrawler}, used to initialize the Crawler and stored in data base.
 * 
 * @author Sandro Reichert
 */
public class WikiDescriptor {

    /** Unique identifier of a Wiki, created by data base. */
    private int wikiID = -1;

    /** Unique name of the Wiki as written in config file (see {@link MWConfigLoader}. */
    private String wikiName = null;

    /** URL of the Wiki as written in config file (see {@link MWConfigLoader}, like "http://en.wikipedia.org/". */
    private String wikiURL = null;

    /** Path to Wiki API (api.php) if API can not be found at {@link #wikiURL}, like "/w/" for wikipedia. */
    private String relativePathToAPI = null;

    /**
     * Path to wiki pages, relative from {@link #wikiURL}, like /wiki/ as used in wikipedia (resulting path is
     * http://de.wikipedia.org/wiki/)
     */
    private String relativePathToContent = null;

    /** Date the crawler did the last check for new pages. (Search for new pages, not revisions of a page!) */
    private Date timestampLastCheckForModifications = null;

    /** User name the {@link MediaWikiCrawler} uses to log into the Wiki for reading content. */
    private String crawlerUserName = null;

    /** Password the {@link MediaWikiCrawler} uses to log into the Wiki for reading content. */
    private String crawlerPassword = null;

    /**
     * Set of namespace id's to use for crawling. All pages in this namespace are crawled, no page of any other name
     * space.
     */
    private HashSet<Integer> namespacesToCrawl = null;

    /**
     * @return Unique identifier of a Wiki, created by database.
     */
    public final int getWikiID() {
        if (wikiID < 0) {
            throw new IllegalStateException("WikiID has not been set yet, value is " + wikiID);
        }
        return wikiID;
    }

    /**
     * @param wikiID Unique identifier of a Wiki, created by data base.
     */
    public final void setWikiID(int wikiID) {
        if (wikiID < 0) {
            throw new IllegalArgumentException("WikiID has to be > 0, current value is " + wikiID);
        }
        this.wikiID = wikiID;
    }

    /**
     * @return Unique name of the Wiki as written in config file (see {@link MWConfigLoader}.
     */
    public final String getWikiName() {
        if (wikiName == null || wikiName.length() == 0) {
            throw new IllegalStateException("wikiName has not been set yet, value is " + wikiName);
        }
        return wikiName;
    }

    /**
     * @param wikiName Unique name of the Wiki as written in config file (see {@link MWConfigLoader}.
     */
    public final void setWikiName(String wikiName) {
        if (wikiName == null || wikiName.length() == 0) {
            throw new IllegalArgumentException("wikiName has to be set, illeagal value " + wikiName);
        }
        this.wikiName = wikiName;
    }

    /**
     * @return URL of the Wiki as written in config file (see {@link MWConfigLoader}, like "http://en.wikipedia.org/".
     */
    public final String getWikiURL() {
        return wikiURL;
    }

    /**
     * @param wikiUrl URL of the Wiki as written in config file (see {@link MWConfigLoader}, like
     *            "http://en.wikipedia.org/".
     */
    public final void setWikiURL(String wikiUrl) {
        try {
            new URL(wikiUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Incorrect Wiki URL " + wikiUrl, e);
        }
        this.wikiURL = wikiUrl;
    }

    /**
     * @return Path to Wiki API (api.php) if API can not be found at {@link #wikiURL}, like "/w/" for wikipedia
     */
    public final String getRelativePathToAPI() {
        return relativePathToAPI;
    }

    /**
     * @param relativePathToAPI Path to Wiki API (api.php) if API can not be found at {@link #wikiURL}, like "/w/" for wikipedia
     */
    public final void setRelativePathToAPI(String relativePathToAPI) {
        this.relativePathToAPI = (relativePathToAPI == null) ? "" : relativePathToAPI.trim();
    }

    /**
     * @return Path to wiki pages, relative from {@link #wikiURL}, like /wiki/ as used in wikipedia (resulting path is
     *         http://de.wikipedia.org/wiki/)
     */
    public final String getRelativePathToContent() {
        return relativePathToContent;
    }

    /**
     * @return Absolute path to wiki pages, like http://de.wikipedia.org/wiki/
     */
    public final String getAbsoltuePathToContent() {
        String absolutePath = "";
        absolutePath = wikiURL;
        if (wikiURL.endsWith("/")) { // e.g. "http://en.wikipedia.org/"
            if (relativePathToContent.startsWith("/")) { // e.g. "/wiki/"
                absolutePath += relativePathToContent.substring(1, relativePathToContent.length()); // "http://en.wikipedia.org/wiki/"
            } else { // e.g. "wiki/"
                absolutePath += relativePathToContent; // = "http://en.wikipedia.org/wiki/"
            }
        } else { // e.g. "http://en.wikipedia.org"
            if (relativePathToContent.startsWith("/")) { // e.g. "/wiki/"
                absolutePath += relativePathToContent; // = "http://en.wikipedia.org/wiki/"
            } else { // e.g. "wiki/"
                absolutePath += "/" + relativePathToContent; // = "http://en.wikipedia.org/wiki/"
            }
        }
        return absolutePath;
    }

    /**
     * @param pathToContent Path to wiki pages, relative from {@link #wikiURL}, like /wiki/ as used in wikipedia
     *            (resulting path is http://de.wikipedia.org/wiki/)
     */
    public final void setRelativePathToContent(String pathToContent) {
        this.relativePathToContent = (pathToContent == null) ? "" : pathToContent.trim();
    }

    /**
     * @return the Date the crawler checked for new pages or revisions the last time. Null if crawler never checked the
     *         Wiki yet.
     */
    public final Date getLastCheckForModifications() {
        return timestampLastCheckForModifications;
    }

    /**
     * @param timestampLastCheckForModifications Date the crawler did the last check for new pages and new revisions.
     */
    public final void setLastCheckForModifications(Date timestampLastCheckForModifications) {
        this.timestampLastCheckForModifications = timestampLastCheckForModifications;
    }

    /**
     * @return User name the {@link MediaWikiCrawler} uses to log into the Wiki for reading content.
     */
    public final String getCrawlerUserName() {
        if (crawlerUserName == null) {
            throw new IllegalStateException("crawlerUserName has not been set yet, value is " + crawlerUserName);
        }
        return crawlerUserName;
    }

    /**
     * @param crawlerUserName User name the {@link MediaWikiCrawler} uses to log into the Wiki for reading content. If
     *            set to null or empty String, the API is accessed without logging in.
     */
    public final void setCrawlerUserName(String crawlerUserName) {
        this.crawlerUserName = (crawlerUserName == null) ? "" : crawlerUserName;
    }

    /**
     * @return Password the {@link MediaWikiCrawler} uses to log into the Wiki for reading content.
     */
    public final String getCrawlerPassword() {
        if (crawlerPassword == null) {
            throw new IllegalStateException("crawlerPassword has not been set yet, value is " + crawlerPassword);
        }
        return crawlerPassword;
    }

    /**
     * @param crawlerPassword Password the {@link MediaWikiCrawler} uses to log into the Wiki for reading content.
     */
    public final void setCrawlerPassword(String crawlerPassword) {
        this.crawlerPassword = (crawlerPassword == null) ? "" : crawlerPassword;
    }

    /**
     * @return Set of namespace id's to use for crawling. All pages in this namespace are crawled, no page of any
     *         other namespace.
     */
    public final HashSet<Integer> getNamespacesToCrawl() {
        return namespacesToCrawl;
    }

    /**
     * Returns the namespaces that are used for crawling as array.
     * 
     * @return The namespaces that are used for crawling.
     */
    public final int[] getNamespacesToCrawlAsArray() {
        int[] namespaces = new int[namespacesToCrawl.size()];
        int i = 0;
        for (int nameSpaceID : namespacesToCrawl) {
            namespaces[i] = nameSpaceID;
            i++;
        }
        return namespaces;
    }

    /**
     * @param namespacesToCrawl Set of namespace id's to use for crawling. All pages in this namespace are crawled, no
     *            page of any other namespace.
     */
    public final void setNamespacesToCrawl(HashSet<Integer> namespacesToCrawl) {
        this.namespacesToCrawl = namespacesToCrawl;
    }

    /**
     * Returns the URL of the API's folder. The URL does not contain the page name of the API itself (api.php) since it
     * is added by the jwbf framework.
     * 
     * @return URL of the API's folder, like "http://en.wikipedia.org/w/"
     */
    public final URL getWikiApiURL() {
        String wikiPath = wikiURL.toString();
        String wikiAPI = (wikiPath.endsWith("/")) ? wikiPath : wikiPath + "/";
        wikiAPI += (relativePathToAPI.startsWith("/")) ? relativePathToAPI.substring(1, relativePathToAPI.length()) : relativePathToAPI;
        wikiAPI += (relativePathToAPI.equalsIgnoreCase("") || relativePathToAPI.endsWith("/")) ? "" : "/";

        URL wikiApiURL = null;
        try {
            wikiApiURL = new URL(wikiAPI);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Syntax error in relative path to API: " + relativePathToAPI + " ", e);
        }
        return wikiApiURL;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "WikiDescriptor [wikiID=" + wikiID + ", wikiName=" + wikiName + ", wikiURL=" + wikiURL + ", pathToAPI="
                + relativePathToAPI + ", pathToContent=" + relativePathToContent + ", timestampLastCheckForModifications="
                + timestampLastCheckForModifications + ", crawlerUserName=" + crawlerUserName + ", crawlerPassword="
                + crawlerPassword + ", namespacesToCrawl=" + namespacesToCrawl + "]";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((crawlerPassword == null) ? 0 : crawlerPassword.hashCode());
        result = prime * result + ((crawlerUserName == null) ? 0 : crawlerUserName.hashCode());
        result = prime * result + ((namespacesToCrawl == null) ? 0 : namespacesToCrawl.hashCode());
        result = prime * result + ((relativePathToAPI == null) ? 0 : relativePathToAPI.hashCode());
        result = prime * result + ((relativePathToContent == null) ? 0 : relativePathToContent.hashCode());
        result = prime * result
                + ((timestampLastCheckForModifications == null) ? 0 : timestampLastCheckForModifications.hashCode());
        result = prime * result + wikiID;
        result = prime * result + ((wikiName == null) ? 0 : wikiName.hashCode());
        result = prime * result + ((wikiURL == null) ? 0 : wikiURL.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WikiDescriptor other = (WikiDescriptor) obj;
        if (crawlerPassword == null) {
            if (other.crawlerPassword != null)
                return false;
        } else if (!crawlerPassword.equals(other.crawlerPassword))
            return false;
        if (crawlerUserName == null) {
            if (other.crawlerUserName != null)
                return false;
        } else if (!crawlerUserName.equals(other.crawlerUserName))
            return false;
        if (namespacesToCrawl == null) {
            if (other.namespacesToCrawl != null)
                return false;
        } else if (!namespacesToCrawl.equals(other.namespacesToCrawl))
            return false;
        if (relativePathToAPI == null) {
            if (other.relativePathToAPI != null)
                return false;
        } else if (!relativePathToAPI.equals(other.relativePathToAPI))
            return false;
        if (relativePathToContent == null) {
            if (other.relativePathToContent != null)
                return false;
        } else if (!relativePathToContent.equals(other.relativePathToContent))
            return false;
        if (timestampLastCheckForModifications == null) {
            if (other.timestampLastCheckForModifications != null)
                return false;
        } else if (!timestampLastCheckForModifications.equals(other.timestampLastCheckForModifications))
            return false;
        if (wikiID != other.wikiID)
            return false;
        if (wikiName == null) {
            if (other.wikiName != null)
                return false;
        } else if (!wikiName.equals(other.wikiName))
            return false;
        if (wikiURL == null) {
            if (other.wikiURL != null)
                return false;
        } else if (!wikiURL.equals(other.wikiURL))
            return false;
        return true;
    }
}
