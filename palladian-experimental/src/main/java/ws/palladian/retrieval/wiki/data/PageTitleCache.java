/**
 * 
 */
package ws.palladian.retrieval.wiki.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache to store mappings page title -> pageID to get pageIDs from page titles without asking the database (since
 * this would require the row pageTitle to be indexed which is slow.)
 * <p>
 * For every wikiID, an own {@link LRUMap}<{@link PageTitle}, pageID> is created that has a maximum capacity of
 * {@link #MAX_CAPACITY}.
 * </p>
 * 
 * @author Sandro Reichert
 */
public class PageTitleCache {

    /** the logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(PageTitleCache.class);

    /** do not call LOGGER.isDebugEnabled() 1000 times */
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    /** The internal cache */
    private final Map<Integer, LRUMap<PageTitle, Integer>> cache;

    /** Maximum capacity of the {@link LRUMap}, use to prevent map from relocating */
	// TODO: configure me in mwCrawlerConfiguration.yml
    private static final int MAX_CAPACITY = 100000;

    /**
     * Default constructor.
     */
    public PageTitleCache() {
        cache = new HashMap<Integer, LRUMap<PageTitle, Integer>>();
    }

    /**
     * Adds a new {@link LRUMap} to {@link #cache} to store mappings of page titles to pageIDs for the given wiki.
     * 
     * @param wikiName The name of the Wiki to add to the {@link #cache}.
     */
    public void addWiki(final int wikiID) {
        if (cache.containsKey(wikiID)) {
            LOGGER.error("PAGE_TITLE_CACHE already contains wikiID \"" + wikiID + "\"");
        } else {
            cache.put(wikiID, new LRUMap<PageTitle, Integer>(MAX_CAPACITY));
            if (DEBUG) {
                LOGGER.debug("Added a new LRUMap for wikiID=" + wikiID + " to the cache.");
            }
        }
    }

    /**
     * Removes the given Wiki from the {@link #cache}.
     * 
     * @param wikiID The Wiki to remove from the cache.
     */
    public void removeWiki(final int wikiID) {
        if (cache.containsKey(wikiID)) {
            cache.remove(wikiID);
        } else {
            LOGGER.error("PAGE_TITLE_CACHE does not contain wikiID " + wikiID);
        }
    }


    /**
     * Add a mapping for the given {@link WikiPage}'s title -> pageID to to the {@link #cache}.
     * 
     * @param page The page to add to the cache.
     */
    public void addPage(final int wikiID, final String pageTitle, final int pageID) {
        if (!cache.containsKey(wikiID)) {
            addWiki(wikiID);
        }
        final LRUMap<PageTitle, Integer> lruMap = cache.get(wikiID);
        lruMap.put(new PageTitle(pageTitle), pageID);
        if (DEBUG) {
            LOGGER.debug("Added mapping wikiID=" + wikiID + " page=\"" + pageTitle + "\" -> pageID=" + pageID
                    + " to cache.");
        }
    }

    /**
     * Removes all mappings of page titles to pageIDs in {@link #cache} for the given wikiID.
     * 
     * @param wikiID The Wiki to remove all cache entries for.
     */
    public void removeAllPages(final int wikiID) {
        if (cache.containsKey(wikiID)) {
            cache.put(wikiID, new LRUMap<PageTitle, Integer>(MAX_CAPACITY));
            if (DEBUG) {
                LOGGER.debug("Removed all pages for wikiID=" + wikiID + " from cache.");
            }
        } else {
            if (DEBUG) {
                LOGGER.debug("Nothing to remove from cache for wikiID=" + wikiID + ".");
            }
        }
    }

    /**
     * Lookup in {@link #cache}, return the pageID that belongs to the given pageTitle in Wiki wikiID.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param pageTitle The name of the page (title) to be found in the Wiki.
     * @return The pageID or <code>null</code> if there is no mapping for the given parameters.
     */
    public Integer getPageID(final int wikiID, final String pageTitle) {
        Integer pageID = null;
        if (cache.containsKey(wikiID)) {
            pageID = cache.get(wikiID).get(new PageTitle(pageTitle));
        }
        if (DEBUG) {
            if (pageID != null) {
                LOGGER.debug("Cachehit for wikiID=" + wikiID + ", pageTitle=\"" + pageTitle + "\", returned pageID="
                        + pageID);
            } else {
                LOGGER.debug("CacheMISS for wikiID " + wikiID + " page " + pageTitle);
            }
        }
        return pageID;
    }

}
