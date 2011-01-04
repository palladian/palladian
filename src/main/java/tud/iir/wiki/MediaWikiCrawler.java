/**
 * 
 */
package tud.iir.wiki;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.mediawiki.actions.meta.Siteinfo;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.LocalizeHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.wiki.data.Revision;
import tud.iir.wiki.data.WikiDescriptor;
import tud.iir.wiki.data.WikiPage;
import tud.iir.wiki.persistence.MediaWikiDatabase;
import tud.iir.wiki.queries.AllPageTitles;
import tud.iir.wiki.queries.GetRendering;
import tud.iir.wiki.queries.RecentChanges;
import tud.iir.wiki.queries.RevisionsByTitleQuery;

/**
 * Naming convention: all methods starting with "crawl" fetch something from the Wiki API.
 * 
 * @author Sandro Reichert
 * 
 */
/**
 * @author Sandro Reichert
 * 
 */
public class MediaWikiCrawler implements Runnable {

    /** The global logger */
    private static final Logger LOGGER = Logger.getLogger(MediaWikiCrawler.class);

    /**
     * When iterating over a paged result from MediaWiki API that has more than this many entries, write this many
     * entries to database in one step.
     */
    private static final int BULK_WRITE_SIZE = 3000;

    /** The database used to persist results */
    private final MediaWikiDatabase MW_DATABASE;

    /** Basic configuration of the {@link MediaWikiCrawler} */
    private final WikiDescriptor MW_DESCRIPTOR;

    /** The jwbf bot that does all the communication with the MediaWiki API. */
    private final MediaWikiBot BOT;

    /** time period to check the Wiki for new pages */
    private long PAGE_CHECK_INTERVAL = DateHelper.MINUTE_MS;

    /** time period to wake up and check data base for pages that need to be checked for new revisions */
    private final long NEW_REVISIONS_INTERVAL = DateHelper.MINUTE_MS / 3;


    /*
     * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals preventing
     * parallelization.
     */
    // private final Vector<MediaWikiBot> BOT_POOL;

    /*
     * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals preventing
     * parallelization.
     */
    // private final int POOL_SIZE;

    /*
     * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals preventing
     * parallelization.
     */
    // private int nextBot = 0;

    /**
     * Creates the MediaWikiCrawler for the given Wiki. use if want to use threads. problem: not faster with threads
     * since jwbf seems to have some internals preventing parallelization.
     * 
     * @param WIKI_NAME The name of the Wiki this crawler processes.
     * @param POOL_SIZE The number of worker threads to use for this wiki.
     */
    // public MediaWikiCrawler(final String WIKI_NAME, final int POOL_SIZE) {
    // this.MW_DATABASE = MediaWikiDatabase.getInstance();
    // if (!MW_DATABASE.wikiExists(WIKI_NAME)) {
    // throw new IllegalArgumentException("Wiki name \"" + WIKI_NAME
    // + "\" is unknown in data base! Can not create MediaWikiCrawler!");
    // }
    // this.MW_DESCRIPTOR = MW_DATABASE.getWikiDescriptor(WIKI_NAME);
    // this.BOT = new MediaWikiBot(MW_DESCRIPTOR.getWikiApiURL());
    // this.POOL_SIZE = POOL_SIZE;
    // this.BOT_POOL = new Vector<MediaWikiBot>(POOL_SIZE);
    // createBotPool();
    // }

    /**
     * Creates the MediaWikiCrawler for the given Wiki. It fetches its own configuration from the database.
     * 
     * @param wikiName The name of the Wiki this crawler processes.
     */
    public MediaWikiCrawler(final String wikiName) {
        this.MW_DATABASE = MediaWikiDatabase.getInstance();
        if (!MW_DATABASE.wikiExists(wikiName)) {
            throw new IllegalArgumentException("Wiki name \"" + wikiName
                    + "\" is unknown in data base! Can not create MediaWikiCrawler!");
        }
        this.MW_DESCRIPTOR = MW_DATABASE.getWikiDescriptor(wikiName);
        this.BOT = new MediaWikiBot(MW_DESCRIPTOR.getWikiApiURL());
    }

    /**
     * Login MediaWiki crawler to API if user name is set in {@link #MW_DESCRIPTOR}. If not, nothing is done.
     * If already logged in, nothing is done.
     * 
     * @return true if login was successful (or login is not required for reading) and false if any error
     *         occurred.
     */
    private boolean login() {
        if (BOT.isLoggedIn() || MW_DESCRIPTOR.getCrawlerUserName().length() == 0) {
            return true;
        }

        boolean loginSuccessful = false;

        try {
            BOT.login(MW_DESCRIPTOR.getCrawlerUserName(), MW_DESCRIPTOR.getCrawlerPassword());
            loginSuccessful = true;
            LOGGER.debug("Login successful for user \"" + MW_DESCRIPTOR.getCrawlerUserName()
                    + "\" and  MediaWiki API at " + MW_DESCRIPTOR.getWikiApiURL().toString());
        } catch (ActionException e1) {
            LOGGER.fatal("Login to MediaWiki " + MW_DESCRIPTOR.getWikiApiURL().toString() + " failed! ", e1);
        }
        return loginSuccessful;
    }

    /**
     * Retrieves for the given pageName its content as HTML and the revisionID of the most recent page and stores them
     * in db.
     * 
     * @param pageName The title of the page to crawl.
     */
    private void crawlAndStorePageContent(final String pageName) {
        SimpleArticle sa;
        try {
            // TODO: room for optimization: whole page is transferred to get most recent revisionID
            sa = new SimpleArticle(BOT.readContent(pageName));

            final String htmlContent = crawlPageContent(pageName);
            final int pageID = MW_DATABASE.getPageID(MW_DESCRIPTOR.getWikiID(), pageName);

            // TODO room for optimization: do we really need the pageID here? use PAGE_NAME as identifier to save 1
            // db-query (-> modify prepared statement to update page)
            boolean pageUpdated = MW_DATABASE.updatePage(MW_DESCRIPTOR.getWikiID(), pageID,
                    Long.parseLong(sa.getRevisionId()), htmlContent, predictNextCheck(pageID));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\n   Page \"" + pageName + "\" has HTML content:\n" + htmlContent
                        + "\n   HTML content has " + (pageUpdated ? "" : "NOT ") + "been written to database.");
            }
        } catch (Exception e) {
            LOGGER.error("Could not retrieve page content for page \"" + pageName + "\" ", e);
        }
    }

    /**
     * Calls the MediaWiki API and retrieves for the given pageName its content rendered as HTML.
     * 
     * @param pageName The title of the page to crawl.
     * @return The page's HTML content.
     */
    private String crawlPageContent(final String pageName) {
        String htmlContent = null;
        try {
            GetRendering renderedText = new GetRendering(BOT, pageName);
            htmlContent = renderedText.getHtml();
        } catch (VersionException e) {
            LOGGER.error("Could not retrieve page content for page \"" + pageName + "\" ", e);
        }
        return htmlContent;
    }

    /**
     * Retrieves all page titles (pageName), their pageID and their namespace from the wiki, if namespace included in
     * {@link #MW_DESCRIPTOR} and stores them in database.
     */
    private void crawlAndStoreAllPageTitles() {
        AllPageTitles apt = null;

        for (int namespaceID : MW_DESCRIPTOR.getNamespacesToCrawl()) {
            try {
                apt = new AllPageTitles(BOT, namespaceID);
            } catch (VersionException e) {
                LOGGER.fatal("Retrieving all page titles from Wiki is not supported by this version", e);
                return;
            }

            int pagesSkipped = 0;
            HashSet<WikiPage> pagesToAdd = new HashSet<WikiPage>();
            int counter = 0;
            for (WikiPage page : apt) {
                counter++;
                page.setWikiID(MW_DESCRIPTOR.getWikiID());
                pagesToAdd.add(page);
                if (counter % BULK_WRITE_SIZE == 0) {
                    pagesSkipped = addPagesToDB(pagesToAdd, pagesSkipped, (counter - pagesSkipped));
                    pagesToAdd.clear();
                    // break; // debugging code, used to limit number of pages to crawl wikipedia
                }
            }
            if (pagesToAdd.size() > 0) {
                addPagesToDB(pagesToAdd, pagesSkipped, (counter - pagesSkipped));
            }
        }
    }

    /**
     * Helper, adds the given set of {@link WikiPage}s to the database and counts the number of skipped pages.
     * 
     * @param pagesToAdd The pages to be added to the data base. Caution: the set is cleared after adding to data base,
     *            call method with a copy if you want to keep it.
     * @param skipCounter The number of skipped pages in previous call(s).
     * @param pageCounter The number of pages that have been added in previous call(s) plus the number of pages in the
     *            given set. Required for logging purpose only.
     * @return The accumulated number of skipped pages, that is skipCounter plus the number of pages that have been
     *         skipped in the current call.
     */
    private int addPagesToDB(HashSet<WikiPage> pagesToAdd, int skipCounter, int pageCounter) {
        if (pagesToAdd.size() == 0) {
            return skipCounter;
        }

        int skipped = MW_DATABASE.addPages(pagesToAdd);
        skipCounter += skipped;
        pageCounter -= skipped;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processed " + (pageCounter + skipCounter) + " pages for namespace "
                    + pagesToAdd.iterator().next().getNamespaceID()
                    + " : added " + pageCounter + " , skipped " + skipCounter + " in total.");
        }
        return skipCounter;
    }

    /**
     * Fetches all revisions for the given page from the API and stores them in the database. A revision is identified
     * by its revisionID, duplicate revisionIDs (revisionIDs already contained in database) are ignored.
     * 
     * @param pageName The name of the page to get revisions for.
     */
    private void crawlAndStoreAllRevisionsByTitle(final String pageName) {

        /*
         * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals
         * preventing parallelization.
         */
        // Thread revisionCrawler = new Thread(new RevisionThread(getNextBot(), MW_DESCRIPTOR, MW_DATABASE.getPage(
        // MW_DESCRIPTOR.getWikiID(), PAGE_NAME)));
        // revisionCrawler.start();

        crawlRevisionsByTitle(pageName, null, false);

    }

    /**
     * Fetches all revisions newer than revisionIDStart for the given page from the API and stores them in the
     * database. A revision is identified by its revisionID, duplicate revisionIDs (revisionIDs already contained in
     * database) are ignored.
     * 
     * @param pageName The name of the page to get revisions for.
     * @param revisionIDStart The revisionID to start from, only newer revisions are retrieved.
     * @param skipFirst If true, skip the first returned revision. Use to get revisions that are newer than
     *            revisionIDStart.
     * @return The number of revisions that have been added. Negative value in case of an error (see error log).
     */
    private int crawlRevisionsByTitle(final String pageName, final Long revisionIDStart, final boolean skipFirst) {
       
        final Integer pageID = MW_DATABASE.getPageID(MW_DESCRIPTOR.getWikiID(), pageName);

        if (pageID == null) {
            LOGGER.error("Page name \"" + pageName + "\" does not exist for Wiki \"" + MW_DESCRIPTOR.getWikiName()
                    + "\" in database.");
            return -1;
        }

        // prepare query for revisions
        RevisionsByTitleQuery rbtq = null;
        try {
            rbtq = new RevisionsByTitleQuery(BOT, pageName, revisionIDStart);
        } catch (VersionException e) {
            LOGGER.error("Fetching the revisions is not supported by this Wiki version. " + e);
        }

        // run query and process results
        int revisionCounter = 0;
        int revisionsSkipped = 0;
        Collection<Revision> revisions = new HashSet<Revision>();
        for (Revision revision : rbtq) {
            Long revisionID = revision.getRevisionID();
            // skip revision with revisionIDStart since we want get newer revisions only. Do not increase
            // revisionsSkipped since it is used to count revisions that should have been added but failed
            if (skipFirst && (revisionID.equals(revisionIDStart))) {
                continue;
            }

            revisions.add(revision);
            revisionCounter++;

            if (revisionCounter % BULK_WRITE_SIZE == 0) {
                revisionsSkipped += MW_DATABASE.addRevisions(MW_DESCRIPTOR.getWikiID(), pageID, revisions);
                revisions.clear();
            }
        }

        // update database
        revisionsSkipped += MW_DATABASE.addRevisions(MW_DESCRIPTOR.getWikiID(), pageID, revisions);

        MW_DATABASE.updatePage(MW_DESCRIPTOR.getWikiID(), pageName, predictNextCheck(pageID));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processed " + revisionCounter + " revision(s) for page \"" + pageName + "\" : added "
                    + (revisionCounter - revisionsSkipped) + " , skipped " + revisionsSkipped);
        }
        return revisionCounter - revisionsSkipped;
    }

    /**
     * Use if you want to crawl all revisions for all pages in the database whose namespace is marked for crawling. To
     * get the revisions for a single page, use {@link #crawlAndStoreAllRevisionsByTitle(String)}. All revisions are
     * stored in database. For the most recent revision, the page content is also stored.
     */
    private void crawlAllPages() {
        for (String pageTitle : MW_DATABASE.getAllPageTitlesToCrawl(MW_DESCRIPTOR.getWikiID())) {
            crawlAndStorePageContent(pageTitle);
            crawlAndStoreAllRevisionsByTitle(pageTitle);
        }
    }

    /**
     * Retrieves all namespaces from Wiki API and store them to database. If no namespaceID already exists (first start
     * of crawler, empty parameter in config file), all namespaces are written to database and useForCrawling is set to
     * true. If at least one namespace already existed in db, all existing namespaces are updated (their names), all new
     * namespaces are added to db but useForCrawling is set to false.
     */
    private void crawlAndStoreNamespaces() {
        Siteinfo si = new Siteinfo();
        try {
            BOT.performAction(si);

            Map<Integer, String> namespacesAPI = si.getNamespaces();
            Set<Integer> namespacesDB = MW_DATABASE.getAllNamespaces(MW_DESCRIPTOR.getWikiID()).keySet();

            if (namespacesDB.size() == 0) {
                for (int namespaceID : namespacesAPI.keySet()) {
                    MW_DATABASE.addNamespace(MW_DESCRIPTOR.getWikiID(), namespaceID, namespacesAPI.get(namespaceID),
                            true);
                }
            } else {
                for (int namespaceID : namespacesAPI.keySet()) {
                    if (namespacesDB.contains(namespaceID)) {
                        MW_DATABASE.updateNamespaceName(MW_DESCRIPTOR.getWikiID(), namespaceID,
                                namespacesAPI.get(namespaceID));
                    } else {
                        MW_DATABASE.addNamespace(MW_DESCRIPTOR.getWikiID(), namespaceID,
                                namespacesAPI.get(namespaceID), false);
                    }
                }
            }
            // update own namespacesToCrawl
            MW_DESCRIPTOR.setNamespacesToCrawl(MW_DATABASE.getNamespacesToCrawl(MW_DESCRIPTOR.getWikiID()));
        } catch (Exception e) {
            LOGGER.error("Could not crawl namespaces.", e);
        }

    }

    /**
     * Use to crawl a wiki for the first time. Removes all pages and their revisions for the own Wiki and calls methods
     * to crawl all namespaces, all page titles, all pages and their revisions.
     */
    private void crawlCompleteWiki() {
        crawlAndStoreNamespaces();
        MW_DATABASE.removeAllPages(MW_DESCRIPTOR.getWikiID());
        crawlAndStoreAllPageTitles();
        crawlAllPages();
    }

    /**
     * Predicts the date this page has to be checked for new revisions the next time.
     * 
     * @param pageID to make prediction for.
     * @return The date this page has to be checked for new revisions the next time.
     * @deprecated Perdiction not required if {@link #crawlAndStoreRecentChanges()} is used for continuous crawling.
     */
    @Deprecated
    private Date predictNextCheck(final int pageID) {

        // FIXME: do a real prediction here!
        return new Date(System.currentTimeMillis() + DateHelper.MINUTE_MS);
    }

    /*
     * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals preventing
     * parallelization.
     */
    // private void createBotPool() {
    // for (int i = 0; i < POOL_SIZE; i++) {
    // MediaWikiBot bot = new MediaWikiBot(MW_DESCRIPTOR.getWikiApiURL());
    // try {
    // bot.login(MW_DESCRIPTOR.getCrawlerUserName(), MW_DESCRIPTOR.getCrawlerPassword());
    // BOT_POOL.add(bot);
    // LOGGER.debug("Login successful for user \"" + MW_DESCRIPTOR.getCrawlerUserName()
    // + "\" and  MediaWiki API at " + MW_DESCRIPTOR.getWikiApiURL().toString());
    // } catch (ActionException e1) {
    // LOGGER.fatal("Login to MediaWiki " + MW_DESCRIPTOR.getWikiApiURL().toString() + " failed! ", e1);
    // }
    // }
    // }

    /*
     * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals preventing
     * parallelization.
     */
    // private MediaWikiBot getNextBot() {
    // MediaWikiBot bot = BOT_POOL.get(nextBot);
    // nextBot = (nextBot == POOL_SIZE - 1) ? 0 : nextBot++;
    // return bot;
    // }



    /**
     * Calls the MediaWiki API, retrieves all new pages created since the last check for new pages and stores them in
     * the database. Only the newest revision is is fetched and stored.
     */
    private void crawlAndStoreNewPages() {
        // prepare query
        RecentChanges newPages = null;
        try {
            newPages = new RecentChanges(BOT, convertDateToWikiFormat(MW_DESCRIPTOR.getLastCheckForModifications()),
                    MW_DESCRIPTOR.getNamespacesToCrawlAsArray(), "new");
        } catch (VersionException e) {
            LOGGER.fatal("Retrieving recent changes from Wiki is not supported by this version. "
                    + "Caution! The crawler will not get any new page or revision!", e);
            return;
        }

        // run query and process results
        int pagesSkipped = 0;
        HashSet<WikiPage> pagesToAdd = new HashSet<WikiPage>();
        int counter = 0;
        for (WikiPage page : newPages) {
            counter++;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found new page \"" + page.getTitle() + "\", pageid: " + page.getPageID());
            }
            page.setWikiID(MW_DESCRIPTOR.getWikiID());
            page.setPageContent(crawlPageContent(page.getTitle()));
            pagesToAdd.add(page);

            if (counter % BULK_WRITE_SIZE == 0) {
                pagesSkipped = addPagesToDB(pagesToAdd, pagesSkipped, (counter - pagesSkipped));
                pagesToAdd.clear();
                // break; // debugging code, used to limit number of pages to crawl wikipedia
            }
        }
        addPagesToDB(pagesToAdd, pagesSkipped, (counter - pagesSkipped));
    }

    /**
     * Calls the MediaWiki API, retrieves all revisions created since the last check for new pages/revisions and stores
     * them in the database. For the newest revision, the HTML content is fetched and stored.
     */
    private void crawlAndStoreNewRevisions() {
        // prepare query
        RecentChanges newPages = null;
        try {
            newPages = new RecentChanges(BOT, convertDateToWikiFormat(MW_DESCRIPTOR.getLastCheckForModifications()),
                    MW_DESCRIPTOR.getNamespacesToCrawlAsArray(), "edit");
        } catch (VersionException e) {
            LOGGER.fatal("Retrieving recent changes from Wiki is not supported by this version. "
                    + "Caution! The crawler will not get any new page or revision!", e);
            return;
        }

        // run query and process results
        for (WikiPage page : newPages) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found " + page.getRevisions().size() + " new revision(s) for page \"" + page.getTitle()
                        + "\", pageid: " + page.getPageID());
            }
            MW_DATABASE.addRevisions(MW_DESCRIPTOR.getWikiID(), page.getPageID(), page.getRevisions().values());
            crawlAndStorePageContent(page.getTitle());
            // if (counter == BULK_WRITE_SIZE) break; //debug code, use to limit number of pages to crawl wikipedia
        }
    }

    /**
     * Converts a {@link Date} to the String representation in MediaWiki format "yyyy-MM-dd'T'HH:mm:ss'Z'" in
     * {@link TimeZone} UTC.
     * 
     * @param date The date to convert to.
     * @return The date's String representation in MediaWiki format "yyyy-MM-dd'T'HH:mm:ss'Z'" in {@link TimeZone} UTC.
     */
    public String convertDateToWikiFormat(final Date date) {
        LocalizeHelper.setTimeZoneUTC();
        String wikiTime = DateHelper.getDatetime("yyyy-MM-dd'T'HH:mm:ss'Z'", date.getTime());
        LocalizeHelper.restoreTimeZone();
        return wikiTime;
    }



    /**
     * Central Method that controls the {@link MediaWikiCrawler}. If the Wiki is processed the first time, all data is
     * fetched from the API and processed. Afterwards, continuous crawling is entered: the Wiki is periodically checked
     * for new pages or revisions.
     */
    public void run() {
        login();

        // check for first start of this wiki
        if (MW_DESCRIPTOR.getLastCheckForModifications() == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Crawling Wiki \"" + MW_DESCRIPTOR.getWikiName()
                        + "\". This Wiki hasn't been crawled completely before.");
            }

            Date currentDate = new Date();
            crawlCompleteWiki();
            MW_DESCRIPTOR.setLastCheckForModifications(currentDate);
            MW_DATABASE.updateWiki(MW_DESCRIPTOR);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Initial crawling of Wiki \"" + MW_DESCRIPTOR.getWikiName() + "\" has been completed.");
            }
        }

        // enter continuous crawling
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Entering continuous crawling mode for wiki Wiki \"" + MW_DESCRIPTOR.getWikiName() + "\".");
        }
        while (true) {
            long wokeUp = System.currentTimeMillis();

            // always check whether we are still logged in.
            login();

            // check for new pages that have been created since last check (or complete crawl)
            if ((System.currentTimeMillis() - MW_DESCRIPTOR.getLastCheckForModifications().getTime()) > PAGE_CHECK_INTERVAL) {
                Date currentDate = new Date();

                crawlAndStoreNamespaces();
                crawlAndStoreNewPages();
                crawlAndStoreNewRevisions();

                MW_DESCRIPTOR.setLastCheckForModifications(currentDate);
                MW_DATABASE.updateWiki(MW_DESCRIPTOR);
            }

            // following lines might be used to evaluate the prediction of new revisions
            // int added = 0;
            // for (WikiPage pageToUpdate : MW_DATABASE.getPagesToUpdate(MW_DESCRIPTOR.getWikiID(), new Date())) {
            // if (LOGGER.isDebugEnabled()) {
            // LOGGER.debug("Checking page \"" + pageToUpdate.getTitle() + "\" for new revisions.");
            // }
            // // LOGGER.info("Checking page \"" + pageToUpdate.getTitle() + "\" for new revisions.");
            //
            // added = crawlRevisionsByTitle(pageToUpdate.getTitle(), pageToUpdate.getNewestRevisionID(), true);
            //
            // // found new revisions ?
            // if (added > 0) {
            // crawlAndStorePageContent(pageToUpdate.getTitle());
            // } else {
            // MW_DATABASE.updatePage(MW_DESCRIPTOR.getWikiID(), pageToUpdate.getTitle(),
            // predictNextCheck(pageToUpdate.getPageID()));
            // }
            // }

            long timeElapsed = System.currentTimeMillis() - wokeUp;
            if (timeElapsed < NEW_REVISIONS_INTERVAL) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Nothing to do for Wiki \"" + MW_DESCRIPTOR.getWikiName() + "\", going to sleep for "
                            + (NEW_REVISIONS_INTERVAL - timeElapsed) / DateHelper.SECOND_MS + " seconds.");
                }
                ThreadHelper.sleep(NEW_REVISIONS_INTERVAL - timeElapsed);
            } else {
                LOGGER.error("Could not process all tasks for Wiki \"" + MW_DESCRIPTOR.getWikiName()
                        + "\" in time, processing took "
                        + ((timeElapsed - NEW_REVISIONS_INTERVAL) / DateHelper.SECOND_MS)
                        + " seconds, but should have been done within "
                        + (NEW_REVISIONS_INTERVAL / DateHelper.SECOND_MS) + " seconds. Please provide more resources!");
            }
        }
    }

}
