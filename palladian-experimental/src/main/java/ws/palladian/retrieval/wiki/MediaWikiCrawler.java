/**
 * 
 */
package ws.palladian.retrieval.wiki;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;

import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.mediawiki.actions.meta.Siteinfo;
import net.sourceforge.jwbf.mediawiki.actions.util.RedirectFilter;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.date.LocalizeHelper;
import ws.palladian.retrieval.wiki.data.Revision;
import ws.palladian.retrieval.wiki.data.WikiDescriptor;
import ws.palladian.retrieval.wiki.data.WikiPage;
import ws.palladian.retrieval.wiki.persistence.MWConfigLoader;
import ws.palladian.retrieval.wiki.persistence.MediaWikiDatabase;
import ws.palladian.retrieval.wiki.queries.AllPageTitles;
import ws.palladian.retrieval.wiki.queries.BasicInformationQuery;
import ws.palladian.retrieval.wiki.queries.GetRendering;
import ws.palladian.retrieval.wiki.queries.PageLinksQuery;
import ws.palladian.retrieval.wiki.queries.RecentChanges;
import ws.palladian.retrieval.wiki.queries.RevisionsByTitleQuery;

/**
 * A MediaWiki crawler that connects to a MediaWiki API using the Java Wiki Bot Framework (jwbf).
 * For details see documentation/handout/img/MediaWikiCrawler-UMLStateMachine.png
 * The crawler does a continuous crawling unless it is stopped or login to the Wiki fails for multiple times in a row.
 * 
 * Naming convention: all methods starting with "crawl" fetch something from the Wiki API.
 * 
 * @author Sandro Reichert
 */
public class MediaWikiCrawler implements Runnable {

    /** The global logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaWikiCrawler.class);

    /** do not call LOGGER.isTraceEnabled()() 1000 times */
    private static final boolean TRACE = LOGGER.isTraceEnabled();

    /** do not call LOGGER.isDebugEnabled() 1000 times */
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    /** do not call LOGGER.isInfoEnabled() 1000 times */
    private static final boolean INFO = LOGGER.isInfoEnabled();

    // private static final UncertainRepository repository;

    /** used for logging to periodically write status output */
    private static final int INFO_SIZE = 100;

    /**
     * When iterating over a paged result from MediaWiki API that has more than this many entries, write this many
     * entries to database in one step.
     */
    private static final int BULK_WRITE_SIZE = 3000;

    /** The database used to persist results */
    private final MediaWikiDatabase mwDatabase;

    /** Basic configuration of the {@link MediaWikiCrawler} */
    private final WikiDescriptor mwDescriptor;

    /** The jwbf bot that does all the communication with the MediaWiki API. */
    private final MediaWikiBot bot;

    /** Count the number of consecutive login errors. */
    private int consecutiveLoginErrors = 0;

    /**
     * If more than this many consecutive login errors occur, give up and stop thread. Be careful with this value to
     * avoid stopping the crawler because of network error etc.
     * 
     * @see #LOGIN_RETRY_TIME.
     */
    private int maxConsecutiveLoginErrors = 10;

    /**
     * Time to wait between 2 login errors. Be careful with this value to avoid stopping the crawler because of network
     * error etc.
     * 
     * @see #maxConsecutiveLoginErrors
     */
    private static final long LOGIN_RETRY_TIME = DateHelper.HOUR_MS;

    /** Flag, checked periodically to stop the thread if set to true. */
    private boolean stopThread = false;

    /** time period to check the Wiki for new pages */
    private long pageCheckInterval = DateHelper.MINUTE_MS;

    /** time period to wake up and check database for pages that need to be checked for new revisions */
    private final long newRevisionsInterval = DateHelper.MINUTE_MS;

    /** Synchronized FIFO queue to put new pages. Multiple consumers can process these pages. */
    private final LinkedBlockingQueue<WikiPage> pageQueue;

    /** If pageQueue has less than that many open slots, write out warn messages. */
    private static final int QUEUE_WARN_CAPACITY = 50;

    /**
     * Set to true to speed-up crawling wikis that contain many (red) links to not existing pages (typically in
     * wikipedia). Caution! FASTMODE may skip some links to existing pages, if you need to be sure to get all links
     * to existing pages, set to false.
     */
    private static final boolean FASTMODE = true;

    /** Specifies if continuous crawling mode is entered when initial crawling has been completed. */
    // TODO: would be nice to have this parameter in the yml config file!
    private static final boolean CONTINUOUS_CRAWLING = false;

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

    // /**
    // * Creates the MediaWikiCrawler for the given Wiki. use if want to use threads. problem: not faster with threads
    // * since jwbf seems to have some internals preventing parallelization.
    // *
    // * @param WIKI_NAME The name of the Wiki this crawler processes.
    // * @param POOL_SIZE The number of worker threads to use for this wiki.
    // */
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
     * @param pageQueue Synchronized FIFO queue to put new pages. Multiple consumers can process these pages.
     */
    public MediaWikiCrawler(final String wikiName, LinkedBlockingQueue<WikiPage> pageQueue, MediaWikiDatabase mwDatabase) {
        this.mwDatabase = mwDatabase;
        if (!mwDatabase.wikiExists(wikiName)) {
            throw new IllegalArgumentException("Wiki name \"" + wikiName
                    + "\" is unknown in data base! Can not create MediaWikiCrawler!");
        }
        this.mwDescriptor = mwDatabase.getWikiDescriptor(wikiName);
        this.bot = new MediaWikiBot(mwDescriptor.getWikiApiURL());
        this.pageQueue = pageQueue;
    }

    /**
     * Login MediaWiki crawler to API if user name is set in {@link #mwDescriptor}. If not, nothing is done.
     * If already logged in, nothing is done.
     * 
     * @return true if login was successful (or login is not required for reading) and false if any error
     *         occurred.
     */
    private boolean login() {
        if (bot.isLoggedIn() || mwDescriptor.getCrawlerUserName().length() == 0) {
            return true;
        }

        boolean loginSuccessful = false;

        try {
            bot.login(mwDescriptor.getCrawlerUserName(), mwDescriptor.getCrawlerPassword());
            loginSuccessful = true;
            consecutiveLoginErrors = 0;
            if (DEBUG) {
                LOGGER.debug("Login successful for user \"" + mwDescriptor.getCrawlerUserName()
                        + "\" and  MediaWiki API at " + mwDescriptor.getWikiApiURL().toString());
            }
        } catch (ActionException e1) {
            consecutiveLoginErrors++;
            LOGGER.error("Login to MediaWiki \"" + mwDescriptor.getWikiApiURL().toString() + "\" failed + "
                    + consecutiveLoginErrors + " time(s) in a row!", e1);

            if (consecutiveLoginErrors < maxConsecutiveLoginErrors) {

                // wait and try to login again
                try {
                    Thread.sleep(LOGIN_RETRY_TIME);
                } catch (InterruptedException e) {
                    if (DEBUG) {
                        LOGGER.debug("", e);
                    }
                }
                login();
            } else {
                LOGGER.error("Could not log in MediaWiki \"" + mwDescriptor.getWikiApiURL().toString() + " for "
                        + maxConsecutiveLoginErrors + " times in a row - I give up!");
                stopCrawler();
            }
        }
        return loginSuccessful;
    }

    /**
     * Retrieves for the given pageTitle its content as HTML and the revisionID of the most recent page and stores them
     * in db.
     * 
     * @param pageTitle The title of the page to crawl.
     * @return <code>true</code> if page has been processed successful, false otherwise.
     */
    private boolean crawlAndStorePage(final String pageTitle) {
        BasicInformationQuery basicInfo = null;
        StopWatch stopWatch = null;
        if (TRACE) {
            stopWatch = new StopWatch();
        }
        try {
            basicInfo = new BasicInformationQuery(bot, pageTitle);
        } catch (VersionException e) {
            LOGGER.error("Retrieving basic Information from Wiki is not supported by this Wiki version", e);
            return false;
        }

        // process the single page that was requested.
        if (!basicInfo.hasNext()) {
            LOGGER.warn("Can't get basic information for page \"" + pageTitle + "\", page skipped.");
            return false;
        }

        WikiPage page = basicInfo.next();
        // Check whether there is a result. If not, the page has been deleted.
        if (page != null) {

            if (TRACE) {
                LOGGER.trace("[API] Processing basic information of page \"" + pageTitle + "\" took "
                        + stopWatch.getElapsedTimeString());
            }

            final String htmlContent = crawlPageContent(pageTitle);

            if (TRACE) {
                stopWatch.start();
            }

            String pageURL = "";
            // Is URL value set, i.e. fetched from API?
            if (page.getPageURL() == null) {
                // try reconstruct it from title
                page.setTitle(pageTitle);
                reconstructPageURLfromTitle(page);
            }
            // URL now set?
            if (page.getPageURL() != null) {
                pageURL = page.getPageURL().toString();
            }

            boolean pageUpdated = mwDatabase.updatePage(mwDescriptor.getWikiID(), page.getPageID(), page
                    .getNewestRevisionID(), htmlContent, predictNextCheck(page.getPageID()), pageURL);

            if (!pageUpdated || DEBUG) {
                final String msg = "Page \"" + pageTitle + "\" has HTML content: " + htmlContent;
                if (pageUpdated) {
                    LOGGER.debug(msg);
                } else {
                    LOGGER.error(msg + "\n   HTML content has NOT  been written to database.");
                }
            }
            if (TRACE) {
                LOGGER.trace("[DB ] Updating database for page \"" + pageTitle + "\" took "
                        + stopWatch.getElapsedTimeString());
            }

            return pageUpdated;

        } else { // if there is no page, the page has been deleted from the wiki
            // TODO: add deleted-Flag to database and set page status to deleted
            if (DEBUG) {
                LOGGER.debug("Could not process page \"" + pageTitle + "\", page seems to be deleted.");
            }
            return false;
        }
    }

    /**
     * Calls the MediaWiki API and retrieves for the given pageTitle its content rendered as HTML.
     * 
     * @param pageTitle The title of the page to crawl.
     * @return The page's HTML content.
     */
    private String crawlPageContent(final String pageTitle) {
        String htmlContent = null;

        StopWatch stopWatch = null;
        if (TRACE) {
            stopWatch = new StopWatch();
        }

        try {
            GetRendering renderedText = new GetRendering(bot, pageTitle);
            htmlContent = renderedText.getHtml();
        } catch (VersionException e) {
            LOGGER.error("Could not retrieve page content for page \"" + pageTitle + "\" ", e);
        }

        if (TRACE) {
            LOGGER.trace("[API] Crawling page content of page \"" + pageTitle + "\" took "
                    + stopWatch.getElapsedTimeString());
        }

        return htmlContent;
    }

    /**
     * Retrieves all page titles, their pageID and their namespace from the wiki, if namespace included in
     * {@link #mwDescriptor} and stores them in database.
     */
    private void crawlAndStoreAllPageTitles() {
        AllPageTitles apt = null;

        for (int namespaceID : mwDescriptor.getNamespacesToCrawl()) {
            StopWatch stopWatch = new StopWatch();
            if (INFO) {
                LOGGER.info("Start crawling of all page titles for namespaceID=" + namespaceID + ".");
            }

            try {
                // apt = new AllPageTitles(bot, namespaceID);
                apt = new AllPageTitles(bot, null, "Dresden", RedirectFilter.all, namespaceID); // test for Philipp
            } catch (VersionException e) {
                LOGGER.error("Retrieving all page titles from Wiki is not supported by this version", e);
                return;
            }

            int pagesSkipped = 0;
            HashSet<WikiPage> pagesToAdd = new HashSet<WikiPage>();
            int counter = 0;
            for (WikiPage page : apt) {
                counter++;
                page.setWikiID(mwDescriptor.getWikiID());
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

            if (INFO) {
                LOGGER
                        .info("Finished crawling of all page titles for namespaceID=" + namespaceID + ".   " + counter
                                + " page titles have been added to database, crawling took "
                                + stopWatch.getElapsedTimeString());
            }
        }
    }

    /**
     * Helper, adds the given set of {@link WikiPage}s to the database and counts the number of skipped pages.
     * 
     * @param pagesToAdd The pages to be added to the data base. Caution: the set is cleared after adding to data base,
     *            call method with a copy if you want to keep it.
     * @param previousSkips The number of skipped pages in previous call(s).
     * @param addedPages The number of pages that have been added in previous call(s) plus the number of pages in the
     *            given set. Required for logging purpose only.
     * @return The accumulated number of skipped pages, that is skipCounter plus the number of pages that have been
     *         skipped in the current call.
     */
    private int addPagesToDB(final HashSet<WikiPage> pagesToAdd, final int previousSkips, int addedPages) {
        if (pagesToAdd.size() == 0) {
            return previousSkips;
        }

        final int skipped = mwDatabase.addPages(pagesToAdd);
        final int skipCounter = previousSkips + skipped;
        final int addCounter = addedPages - skipped;

        if (INFO) {
            LOGGER.info("Processed " + (addCounter + skipCounter) + " page titles for namespace "
                    + pagesToAdd.iterator().next().getNamespaceID() + " : added " + addCounter + " , skipped "
                    + skipCounter + " in total.");
        }

        return skipCounter;
    }

    /**
     * Fetches all revisions for the given page from the API and stores them in the database. A revision is identified
     * by its revisionID, duplicate revisionIDs (revisionIDs already contained in database) are ignored.
     * 
     * @param pageTitle The name of the page to get revisions for.
     */
    private void crawlAndStoreAllRevisionsByTitle(final String pageTitle) {

        /*
         * use if want to use threads. problem: not faster with threads since jwbf seems to have some internals
         * preventing parallelization.
         */
        // Thread revisionCrawler = new Thread(new RevisionThread(getNextBot(), MW_DESCRIPTOR, MW_DATABASE.getPage(
        // MW_DESCRIPTOR.getWikiID(), PAGE_NAME)));
        // revisionCrawler.start();

        crawlRevisionsByTitle(pageTitle, null, false);

    }

    /**
     * Fetches all revisions newer than revisionIDStart for the given page from the API and stores them in the
     * database. A revision is identified by its revisionID, duplicate revisionIDs (revisionIDs already contained in
     * database) are ignored.
     * 
     * @param pageTitle The name of the page to get revisions for.
     * @param revisionIDStart The revisionID to start from, only newer revisions are retrieved.
     * @param skipFirst If true, skip the first returned revision. Use to get revisions that are newer than
     *            revisionIDStart.
     * @return The number of revisions that have been added. Negative value in case of an error (see error log).
     */
    private int crawlRevisionsByTitle(final String pageTitle, final Long revisionIDStart, final boolean skipFirst) {
        StopWatch stopWatch1 = null;
        StopWatch stopWatch2 = null;

        if (TRACE) {
            stopWatch1 = new StopWatch();
            stopWatch2 = new StopWatch();
        }
        final Integer pageID = mwDatabase.getPageID(mwDescriptor.getWikiID(), pageTitle, false);
        if (TRACE) {
            LOGGER.trace("[DB ] crawlRevisionsByTitle, get pageID from Database for page \"" + pageTitle + "\" took "
                    + stopWatch1.getElapsedTimeString());
            stopWatch1.start();
        }

        if (pageID == null) {
            LOGGER.error("Page name \"" + pageTitle + "\" does not exist for Wiki \"" + mwDescriptor.getWikiName()
                    + "\" in database.");
            return -1;
        }

        // prepare query for revisions
        RevisionsByTitleQuery rbtq = null;
        try {
            rbtq = new RevisionsByTitleQuery(bot, pageTitle, revisionIDStart);
        } catch (VersionException e) {
            LOGGER.error("Fetching the revisions is not supported by this Wiki version. " + e);
        }

        long dbTime = 0L;

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
                if (TRACE) {
                    stopWatch2.start();
                }
                revisionsSkipped += mwDatabase.addRevisions(mwDescriptor.getWikiID(), pageID, revisions);
                revisions.clear();
                if (TRACE) {
                    dbTime += stopWatch2.getElapsedTime();
                }
            }
        }

        // update database
        if (TRACE) {
            stopWatch2.start();
        }

        revisionsSkipped += mwDatabase.addRevisions(mwDescriptor.getWikiID(), pageID, revisions);

        if (TRACE) {
            dbTime += stopWatch2.getElapsedTime();
            LOGGER.trace("[API] crawlRevisionsByTitle, get Revisions from API for page \"" + pageTitle + "\" took "
                    + DateHelper.getRuntime(0L, (stopWatch1.getElapsedTime() - dbTime)));
            LOGGER
                    .trace("[DB ] crawlRevisionsByTitle, add " + (revisionCounter - revisionsSkipped)
                            + " Revisions to database for page \"" + pageTitle + "\" took "
                            + DateHelper.getRuntime(0L, dbTime));
            stopWatch1.start();
        }

        mwDatabase.updatePage(mwDescriptor.getWikiID(), pageID, predictNextCheck(pageID));

        if (DEBUG) {
            LOGGER.debug("Processed " + revisionCounter + " revision(s) for page \"" + pageTitle + "\" : added "
                    + (revisionCounter - revisionsSkipped) + " , skipped " + revisionsSkipped);
        }
        if (TRACE) {
            LOGGER.trace("[DB ] crawlRevisionsByTitle, update page \"" + pageTitle + "\" in database took "
                    + stopWatch1.getElapsedTimeString());
        }

        return revisionCounter - revisionsSkipped;
    }

    /**
     * Use if you want to crawl all revisions for all pages in the database whose namespace is marked for crawling. To
     * get the revisions for a single page, use {@link #crawlAndStoreAllRevisionsByTitle(String)}. All revisions are
     * stored in database. For the most recent revision, the page content is also stored.
     */
    private void crawlAllPages() {
        StopWatch stopWatch = new StopWatch();
        if (INFO) {
            LOGGER.info("Start crawling content and revisions of all pages.");
        }

        int pageCounter = 0;
        StopWatch watch2 = null;
        for (String pageTitle : mwDatabase.getAllPageTitlesToCrawl(mwDescriptor.getWikiID())) {
            if (TRACE) {
                watch2 = new StopWatch();
            }

            boolean success = crawlAndStorePage(pageTitle);
            if (success) {
                crawlAndStoreAllRevisionsByTitle(pageTitle);
                crawlAndStoreAllPageLinks(pageTitle);
                processNewPage(pageTitle);
            }

            if (INFO && ++pageCounter % INFO_SIZE == 0) {
                LOGGER.info("Crawled " + pageCounter + " pages so far.");
            }

            if (TRACE) {
                LOGGER.trace("----- Processing page \"" + pageTitle + "\" took " + watch2.getElapsedTimeString()
                        + " -----");
            }
        }

        if (INFO) {
            LOGGER.info("finished crawling content, links and revisions of all pages. Crawling of  " + pageCounter
                    + " pages took " + stopWatch.getElapsedTimeString());
        }
    }

    /**
     * Retrieves all namespaces from Wiki API and store them to database. If no namespaceID already exists (first start
     * of crawler, empty parameter in config file), all namespaces are written to database and useForCrawling is set to
     * true. If at least one namespace already existed in db, all existing namespaces are updated (their names), all new
     * namespaces are added to db but useForCrawling is set to false.
     */
    private void crawlAndStoreNamespaces() {
        StopWatch stopWatch = new StopWatch();
        if (INFO) {
            LOGGER.info("Start crawling all namespaces.");
        }

        Siteinfo si = new Siteinfo();
        int namespaceCount = 0;
        try {
            bot.performAction(si);

            Map<Integer, String> namespacesAPI = si.getNamespaces();
            Set<Integer> namespacesDB = mwDatabase.getAllNamespaces(mwDescriptor.getWikiID()).keySet();

            if (namespacesDB.size() == 0) {
                for (int namespaceID : namespacesAPI.keySet()) {
                    mwDatabase
                            .addNamespace(mwDescriptor.getWikiID(), namespaceID, namespacesAPI.get(namespaceID), true);
                }
            } else {
                for (int namespaceID : namespacesAPI.keySet()) {
                    if (namespacesDB.contains(namespaceID)) {
                        mwDatabase.updateNamespaceName(mwDescriptor.getWikiID(), namespaceID, namespacesAPI
                                .get(namespaceID));
                    } else {
                        mwDatabase.addNamespace(mwDescriptor.getWikiID(), namespaceID, namespacesAPI.get(namespaceID),
                                false);
                    }
                }
            }
            // update own namespacesToCrawl
            mwDescriptor.setNamespacesToCrawl(mwDatabase.getNamespacesToCrawl(mwDescriptor.getWikiID()));
            namespaceCount = namespacesAPI.size();
        } catch (Exception e) {
            LOGGER.error("Could not crawl namespaces.", e);
        }

        if (INFO) {
            LOGGER.info("Finished crawling all namespaces. Found " + namespaceCount + " namespaces, crawling took "
                    + stopWatch.getElapsedTimeString());
        }
    }

    /**
     * Use to crawl a wiki for the first time. Removes all pages and their revisions for the own Wiki and calls methods
     * to crawl all namespaces, all page titles, all pages and their revisions.
     */
    private void crawlCompleteWiki() {
        StopWatch stopWatch = new StopWatch();
        if (INFO) {
            LOGGER.info("Start complete crawling of Wiki \"" + mwDescriptor.getWikiName() + "\".");
        }

        crawlAndStoreNamespaces();
        mwDatabase.removeAllPages(mwDescriptor.getWikiID());
        crawlAndStoreAllPageTitles();
        crawlAllPages();

        if (INFO) {
            LOGGER.info("Initial crawling of Wiki \"" + mwDescriptor.getWikiName()
                    + "\" has been completed. Crawling took " + stopWatch.getElapsedTimeString());
        }
    }

    /**
     * Retrieves for the given page title all links to other wiki pages and stores them in the database.
     * 
     * @param pageTitleSource The page to get all outbound wiki links for.
     */
    private void crawlAndStoreAllPageLinks(final String pageTitleSource) {
        if (DEBUG) {
            LOGGER.debug("Try getting all links from page " + pageTitleSource);
        }
        PageLinksQuery links = null;
        try {
            links = new PageLinksQuery(bot, pageTitleSource, mwDescriptor.getNamespacesToCrawl());
        } catch (VersionException e) {
            LOGGER.error("Fetching the links is not supported by this Wiki version. " + e);
        }

        // remove all Links for the current page
        mwDatabase.removeAllHyperlinks(mwDescriptor.getWikiID(), pageTitleSource);

        final WikiPage page = new WikiPage();
        page.setWikiID(mwDescriptor.getWikiID());
        page.setPageID(mwDatabase.getPageID(mwDescriptor.getWikiID(), pageTitleSource, FASTMODE));

        // run query and process results
        for (WikiPage pageDest : links) {

            // get pageIDs from db since we got only titles from API.
            final Integer pageIDDest = mwDatabase.getPageID(mwDescriptor.getWikiID(), pageDest.getTitle(), true);
            // check whether page exists
            if (pageIDDest == null) { // identified a link to not existing page (link has red font in Wiki)
                if (DEBUG) {
                    LOGGER.debug("Page \"" + pageTitleSource + "\" links to page \"" + pageDest.getTitle()
                            + "\", but this destination page does not yet exist in the Wiki, "
                            + "so we do not store the hyperlink to it.");
                }
            } else {
                page.addHyperLink(pageIDDest);
            }
        }

        // add links to database
        mwDatabase.addHyperlinks(page);
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
     * the database. Only the newest revision is fetched and stored.
     */
    private void crawlAndStoreNewPages() {
        // prepare query
        RecentChanges newPages = null;
        try {
            newPages = new RecentChanges(bot, convertDateToWikiFormat(mwDescriptor.getLastCheckForModifications()),
                    mwDescriptor.getNamespacesToCrawlAsArray(), "new");
        } catch (VersionException e) {
            LOGGER.error("Retrieving recent changes from Wiki is not supported by this version. "
                    + "Caution! The crawler will not get any new page or revision!", e);
            return;
        }

        // run query and process results
        int pagesSkipped = 0;
        HashSet<WikiPage> pagesToAdd = new HashSet<WikiPage>();
        int counter = 0;
        for (WikiPage page : newPages) {
            counter++;
            if (DEBUG) {
                LOGGER.debug("Found new page \"" + page.getTitle() + "\", pageid: " + page.getPageID());
            }
            page.setWikiID(mwDescriptor.getWikiID());
            page.setPageContentHTML(crawlPageContent(page.getTitle()));
            pagesToAdd.add(page);

            if (counter % BULK_WRITE_SIZE == 0) {
                pagesSkipped = addPagesToDB(pagesToAdd, pagesSkipped, (counter - pagesSkipped));
                pagesToAdd.clear();
                // break; // debugging code, used to limit number of pages to crawl wikipedia
            }
        }
        addPagesToDB(pagesToAdd, pagesSkipped, (counter - pagesSkipped));

        for (WikiPage page : pagesToAdd) {
            processNewPage(page.getTitle());
        }
    }

    /**
     * Calls the MediaWiki API, retrieves all revisions created since the last check for new pages/revisions and stores
     * them in the database. For the newest revision, the HTML content is fetched and stored.
     */
    private void crawlAndStoreNewRevisions() {
        // prepare query
        RecentChanges newPages = null;
        try {
            newPages = new RecentChanges(bot, convertDateToWikiFormat(mwDescriptor.getLastCheckForModifications()),
                    mwDescriptor.getNamespacesToCrawlAsArray(), "edit");
        } catch (VersionException e) {
            LOGGER.error("Retrieving recent changes from Wiki is not supported by this version. "
                    + "Caution! The crawler will not get any new page or revision!", e);
            return;
        }

        // run query and process results
        for (WikiPage page : newPages) {
            if (DEBUG) {
                LOGGER.debug("Found " + page.getRevisions().size() + " new revision(s) for page \"" + page.getTitle()
                        + "\", pageid: " + page.getPageID());
            }
            /**
             * Workaround: this may happen if one limits crawlAndStoreAllPageTitles to a prefix like page titles
             * starting with "Dresden". When entering continuous crawling, we get new revisions for all pages ->
             * which includes pages that haven't been crawled before.
             */
            if (mwDatabase.getPageID(mwDescriptor.getWikiID(), page.getTitle(), false) == null) {
                LOGGER.error("Found new revision for page \"" + page.getTitle()
                        + "\" but page does not exist in database! Discarding revision.");
                continue;
            }
            mwDatabase.addRevisions(mwDescriptor.getWikiID(), page.getPageID(), page.getRevisions().values());
            boolean success = crawlAndStorePage(page.getTitle());
            // if (counter == BULK_WRITE_SIZE) break; //debug code, use to limit number of pages to crawl wikipedia

            if (success) {
                processNewPage(page.getTitle());
            }
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
     * Reconstructs a {@link URL} from page title and {@link WikiDescriptor#getAbsoltuePathToContent()} and sets it to
     * the given {@link WikiPage} in case no error occurred while reconstructing the url.
     * Caution. Not completely tested!
     * 
     * @param page The page to construct the URL for.
     * @return <code>true</code> if the page's {@link URL} could be reconstructed, <code>false</code> if any error
     *         occurred.
     */
    private boolean reconstructPageURLfromTitle(final WikiPage page) {
        boolean success = false;
        try {
            String baseURL = mwDescriptor.getAbsoltuePathToContent().toString();
            String encodedTitle = URLEncoder.encode(page.getTitle(), "UTF-8");

            // "http://en.wikipedia.org/wiki/Golden+Age" -> "http://en.wikipedia.org/wiki/Golden_Age"
            String convertedTitle = encodedTitle.replaceAll("[+]", "_");

            URL pageURL = new URL(baseURL + convertedTitle);
            page.setPageURL(pageURL);
            success = true;
            if (DEBUG) {
                LOGGER.debug("Constructed URL = " + pageURL.toString());
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Could not encode page title \"" + page.getTitle() + "\". ", e);
        } catch (MalformedURLException e) {
            LOGGER.error("Could not reconstruct URL of page \"" + page.getTitle() + "\". ", e);
        } catch (NullPointerException e) {
            LOGGER.error("Could not reconstruct URL of page \"" + page.getTitle()
                    + "\", absolute path to content is unknown. ", e);
        }
        return success;
    }

    /**
     * Puts a new or updated page and all revisions to the pageQueue to notify consumers on this new or updated page.
     * 
     * @param pageTitle The title of the new or updated page
     */
    private void processNewPage(final String pageTitle) {
        final Integer pageID = mwDatabase.getPageID(mwDescriptor.getWikiID(), pageTitle, false);
        if (pageID != null) {
            WikiPage page = mwDatabase.getPage(mwDescriptor.getWikiID(), pageID);

            // String baseURL = mwDescriptor.getAbsoltuePathToContent();
            // URL pageURL = null;
            // try {
            // String encodedTitle = URLEncoder.encode(page.getTitle(), "UTF-8");
            // pageURL = new URL(baseURL + encodedTitle);
            // } catch (UnsupportedEncodingException e) {
            // LOGGER.error("Could not encode page title \"" + page.getTitle() + "\" ", e);
            // } catch (MalformedURLException e) {
            // LOGGER.error("Could not create URL of page \"" + page.getTitle() + "\" ", e);
            // }
            // page.setPageURL(pageURL);

            try {
                if (DEBUG) {
                    LOGGER.debug("queue size: " + pageQueue.size());
                }
                if (pageQueue.remainingCapacity() < QUEUE_WARN_CAPACITY) {
                    LOGGER
                            .warn("Queue to PageConsumers almost full, increase number of consumers! Remaining capacity: "
                                    + pageQueue.remainingCapacity());
                }

                pageQueue.put(page);
            } catch (InterruptedException e) {
                LOGGER.warn("", e);
            } catch (NullPointerException e) {
                LOGGER.error("Could not put page \"" + pageTitle + "\" to pageQuele: page does not exist.");
            }
        }
    }

    /**
     * Helper to stop this {@link MediaWikiCrawler} {@link Thread}. Sets the internal flag to stop the current
     * {@link Thread}. The stop-flag is checked periodically.
     */
    public synchronized void stopCrawler() {
        stopThread = true;
    }

    /**
     * Helper to stop this {@link MediaWikiCrawler} {@link Thread}. If {@code true} is returned, the current thread
     * should stop.
     * 
     * @return {@code true} if the current thread should stop.
     */
    private synchronized boolean threadShouldStop() {
        return stopThread;
    }

    /**
     * Central Method that controls the {@link MediaWikiCrawler}. If the Wiki is processed the first time, all data is
     * fetched from the API and processed. Afterwards, continuous crawling is entered: the Wiki is periodically checked
     * for new pages or revisions.
     */
    @Override
    public void run() {
        if (INFO) {
            LOGGER.info("Start crawling Wiki \"" + mwDescriptor.getWikiName() + "\".");
        }
        if (FASTMODE) {
            LOGGER
                    .warn("Crawler uses the fastmode, detection of links to other wiki pages may be erroneous (links are dropped even if page is existing).");
        }

        login();

        if (!threadShouldStop()) {
            // check for first start of this wiki
            if (mwDescriptor.getLastCheckForModifications() == null) {
                if (INFO) {
                    LOGGER.info("Wiki \"" + mwDescriptor.getWikiName() + "\" hasn't been crawled completely before.");
                }

                Date currentDate = new Date();
                crawlCompleteWiki();
                mwDescriptor.setLastCheckForModifications(currentDate);
                mwDatabase.updateWiki(mwDescriptor);
            }

            // enter continuous crawling
            if (INFO) {
                LOGGER.info("Entering continuous crawling mode for Wiki \"" + mwDescriptor.getWikiName() + "\".");
            }

            if (CONTINUOUS_CRAWLING) {
                while (true) {
                    long wokeUp = System.currentTimeMillis();

                    // always check whether we are logged in.
                    login();
                    if (threadShouldStop()) {
                        break;
                    }

                    // check for new pages that have been created since last check (or since complete crawl)
                    if ((System.currentTimeMillis() - mwDescriptor.getLastCheckForModifications().getTime()) > pageCheckInterval) {
                        Date currentDate = new Date();

                        crawlAndStoreNamespaces();
                        crawlAndStoreNewPages();
                        crawlAndStoreNewRevisions();

                        mwDescriptor.setLastCheckForModifications(currentDate);
                        mwDatabase.updateWiki(mwDescriptor);
                    }

                    // following lines might be used to evaluate the prediction of new revisions
                    // int added = 0;
                    // for (WikiPage pageToUpdate : MW_DATABASE.getPagesToUpdate(MW_DESCRIPTOR.getWikiID(), new Date()))
                    // {
                    // if (DEBUG) {
                    // LOGGER.debug("Checking page \"" + pageToUpdate.getTitle() + "\" for new revisions.");
                    // }
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
                    if (timeElapsed < newRevisionsInterval) {
                        if (DEBUG) {
                            LOGGER.debug("Nothing to do for Wiki \"" + mwDescriptor.getWikiName()
                                    + "\", going to sleep for " + (newRevisionsInterval - timeElapsed)
 / 1000
                                    + " seconds.");
                        }

                        try {
                            Thread.sleep(newRevisionsInterval - timeElapsed);
                        } catch (InterruptedException e) {
                            LOGGER.warn(e.getMessage());
                        }

                    } else {
                        LOGGER.error("Could not process all tasks for Wiki \"" + mwDescriptor.getWikiName()
                                + "\" in time, processing took "
 + ((timeElapsed - newRevisionsInterval) / 1000)
                                + " seconds, but should have been done within "
 + (newRevisionsInterval / 1000)
                                + " seconds. Please provide more resources!");
                    }
                }
            }
        }
        if (INFO) {
            LOGGER.info("Crawler has been stopped. Goodbye!");
        }
    }

    /**
     * Main method to initialize the {@link MediaWikiCrawler}s.
     * 
     * @param args the command line arguments.
     */
    public static void main(String[] args) throws Exception {
        final int queueCapacity = 1000;
        final int pageConsumers = 5;

        LinkedBlockingQueue<WikiPage> pageQueue = new LinkedBlockingQueue<WikiPage>(queueCapacity);
        MWConfigLoader.initialize(pageQueue);

        for (int i = 1; i <= pageConsumers; i++) {
            Thread consumer = new Thread(new PageConsumer(pageQueue), "Consum-" + i);
            consumer.start();
        }
    }

}
