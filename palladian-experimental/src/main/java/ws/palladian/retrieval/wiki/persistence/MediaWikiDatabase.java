package ws.palladian.retrieval.wiki.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.date.LocalizeHelper;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.wiki.MediaWikiCrawler;
import ws.palladian.retrieval.wiki.data.PageTitleCache;
import ws.palladian.retrieval.wiki.data.Revision;
import ws.palladian.retrieval.wiki.data.WikiDescriptor;
import ws.palladian.retrieval.wiki.data.WikiPage;

/**
 * Data base persistence layer that provides adding, removing and reading of data used by {@link MediaWikiCrawler}.
 * 
 * In all removeX()-methods, deletion of foreign keys is done by data base, so make sure foreign keys and "ON DELETE
 * CASCADE" have been set properly!
 * 
 * @author Sandro Reichert
 */
public final class MediaWikiDatabase extends DatabaseManager {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MediaWikiDatabase.class);

    /** do not call LOGGER.isDebugEnabled() 1000 times */
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    public static void main(String[] args) {
        // new MediaWikiDatabase().clearTables();

    }

    private final PageTitleCache cache;

    // ////////////////// prepared statements ////////////////////
    /** see sql */
    private static final String sqlGetNamespaceIDsToCrawl = "SELECT namespaceID FROM namespaces WHERE wikiID = ? AND useForCrawling = 1";

    /** see sql */
    private static final String sqlAllNamespaces = "SELECT namespaceID, useForCrawling FROM namespaces WHERE wikiID = ?";

    /** see sql */
    private static final String sqlAddWiki = "INSERT INTO wikis(wikiName, wikiURL, pathToApi, lastCheckNewPages, crawler_username, crawler_password) VALUES (?,?,?,?,?,?)";

    /** see sql */
    private static final String sqlUpdateWiki = "UPDATE wikis SET wikiURL= ?, pathToApi = ?, pathToContent = ?, lastCheckNewPages = ?, crawler_username = ?, crawler_password = ? WHERE wikiID = ?";

    /** see sql */
    private static final String sqlRemoveWiki = "DELETE FROM wikis WHERE wikiID = ?";

    /** see sql */
    private static final String sqlGetWikiDescriptorByName = "SELECT * FROM wikis WHERE wikiName COLLATE utf8_bin = ?";

    /** see sql */
    private static final String sqlGetWikiDescriptorByID = "SELECT * FROM wikis WHERE wikiID = ?";

    /** see sql */
    private static final String sqlGetAllWikiDescriptors = "SELECT * FROM wikis ORDER BY wikiName";

    /** see sql */
    private static final String sqlAddPage = "INSERT INTO pages(wikiID, pageID, pageTitle, namespaceID, sourceDynamics, pageContent, revisionID, nextCheck) VALUES (?,?,?,?,?,?,?,?)";

    /** see sql */
    private static final String sqlUpdatePage = "UPDATE pages SET revisionID = ?, pageContent = ?, nextCheck = ?, fullURL = ? WHERE wikiID = ? AND pageID = ?";

    /** see sql */
    private static final String sqlUpdatePageNextCheck = "UPDATE pages SET nextCheck = ? WHERE wikiID = ? AND pageID = ?";

    /** see sql */
    private static final String sqlRemoveAllPages = "DELETE FROM pages WHERE wikiID = ?";

    /** see sql */
    private static final String sqlAddNamespace = "INSERT INTO namespaces(wikiID, namespaceID, namespaceName, useForCrawling) VALUES (?,?,?,?)";

    /** see sql */
    private static final String sqlGetNamespace = "SELECT namespaceID FROM namespaces WHERE wikiID = ? AND namespaceID = ?";

    /** see sql */
    private static final String sqlUpdateNamespace = "UPDATE namespaces SET useForCrawling = ? WHERE wikiID = ? AND namespaceID = ?";

    /** see sql */
    private static final String sqlUpdateNamespaceName = "UPDATE namespaces SET namespaceName = ? WHERE wikiID = ? AND namespaceID = ?";

    /** see sql */
    private static final String sqlRemoveNamespace = "DELETE FROM namespaces WHERE wikiID = ? AND namespaceID = ?";

    /** see sql */
    private static final String sqlGetPageByPageID = "SELECT pageTitle, namespaceID, sourceDynamics, pageContent, revisionID, nextCheck, fullURL FROM pages WHERE wikiID = ? AND pageID = ?";

    /** see sql */
    private static final String sqlGetPageIDByPageTitle = "SELECT pageID FROM pages WHERE wikiID = ? AND pageTitle COLLATE utf8_bin = ? ";

    /** see sql */
    private static final String sqlGetPagesToUpdate = "SELECT pageID, pageTitle, namespaceID, sourceDynamics, revisionID, nextCheck FROM pages WHERE wikiID = ? AND (nextCheck IS NULL OR TIMEDIFF(nextCheck, ?) < 0)";

    /** see sql */
    private static final String sqlAddRevision = "INSERT INTO revisions(wikiID, pageID, revisionID, timestamp, author) VALUES (?,?,?,?,?)";

    /** see sql */
    private static final String sqlGetRevisions = "SELECT * FROM revisions WHERE wikiID = ? AND pageID = ?";

    /** see sql */
    private static final String sqlGetAllPageTitlesToCrawl = "SELECT pageTitle FROM pages "
            + "WHERE wikiID = ? AND namespaceID IN "
            + "(SELECT namespaceID FROM namespaces WHERE pages.wikiID = namespaces.wikiID AND useForCrawling = 1)";

    /** see sql */
    private static final String sqlAddHyperlink = "INSERT INTO links(wikiID, pageIDSource, pageIDDest) VALUES (?,?,?)";

    /** see sql */
    private static final String sqlGetOutgoingHyperlinks = "SELECT pageIDDest FROM links WHERE wikiID = ? AND  pageIDSource = ?";

    /** see sql */
    private static final String sqlRemoveHyperlinks = "DELETE FROM links WHERE wikiID = ? AND pageIDSource = ?";

    /**
     * Helper to precompile a PreparedStatement given as SQL-{@link String}.
     * 
     * @param sql The SQL statement to use for the {@link PreparedStatement}
     * @return The precompiled {@link PreparedStatement}.
     * @throws SQLException If a database access error occurs or this method is called on a closed connection.
     */
    // private final PreparedStatement getPreparedStatement(final String sql) throws SQLException {
    //
    // return DatabaseManager.getInstance().getConnection().prepareStatement(sql);
    // }

    /**
     * Converts a date, given in String representation of the SQL data type DATETIME (yyyy-MM-dd hh:mm:ss) to
     * {@link java.util.Date} It is assumed that the dateTime is in UTC {@link TimeZone}.
     * 
     * @param dateTime A Date String representation of the SQL data type DATETIME (yyyy-MM-dd hh:mm:ss)
     * @return The date as a {@link java.util.Date} object.
     * @throws Exception If no valid Date format could be found. See
     *             {@link tud.iir.daterecognition.dates.ExtractedDate#getNormalizedDate()} for details.
     */
    static Date convertSQLDateTimeToDate(final String dateTime) throws Exception {
        LocalizeHelper.setUTCandEnglish();
        Date date = DateParser.findDate(dateTime).getNormalizedDate();
        LocalizeHelper.restoreTimeZoneAndLocale();
        return date;
    }

    /**
     * Constructor, prepares the {@link PageTitleCache}.
     */
    protected MediaWikiDatabase(final DataSource dataSource) {
        super(dataSource);
        cache = new PageTitleCache();
        for (WikiDescriptor wiki : getAllWikiDescriptors()) {
            cache.addWiki(wiki.getWikiID());
        }
    }

    /**
     * Store Wiki-internal hyperlinks from page source to pages destinations, e.g. from
     * http://en.wikipedia.org/wiki/Albert_Einstein to http://en.wikipedia.org/wiki/Nobel_Prize_in_Physics
     * 
     * @param page The page to add all links for.
     * @return The number of links that have been skipped. See error log for details if return value > 0.
     */
    public int addHyperlinks(final WikiPage page) {

        // data for batch update
        List<List<Object>> pageBatchArgs = getHyperlinkArgs(page.getWikiID(), page.getPageID(), page.getHyperLinks());

        int[] result = runBatchUpdate(sqlAddHyperlink, pageBatchArgs);

        int skipCounter = 0;
        for (int r : result) {
            if (r == -1) {
                skipCounter++;
            }
        }
        return skipCounter;
    }

    /**
     * Adds a new Wiki namespace to the database. If namespaceID already exists for this wikiID, nothing is done.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param namespaceID The namespaceID as retrieved from the Wiki.
     * @param namespaceName The name of the namespace (unique per wikiID).
     * @param useForCrawling Set true if (all pages of) this namespace should be included into crawling, false
     *            if not.
     * @return true if namespace was added, false if it was already contained or any problem occurred while adding. See
     *         error log for details.
     */
    public boolean addNamespace(final int wikiID, final int namespaceID, final String namespaceName,
            final boolean useForCrawling) {
        boolean success = false;
        if (DEBUG && namespaceExists(wikiID, namespaceID)) {
            LOGGER.debug("Could not add namespaceID " + namespaceID + " for wikiID " + wikiID
                    + ", it is already contained!");
        } else {
            List<Object> args = new ArrayList<Object>();
            args.add(wikiID);
            args.add(namespaceID);
            args.add(namespaceName);
            args.add(useForCrawling);
            success = runUpdate(sqlAddNamespace, args) >= 0;
        }
        return success;
    }

    /**
     * Adds a set of Wiki pages to the database.
     * 
     * @param wikiPages The pages to add to database.
     * @return The number of pages that have been skipped. See error log for details if return value > 0.
     */
    public int addPages(final Set<WikiPage> wikiPages) {

        // data for batch update
        List<List<Object>> pageBatchArgs = new ArrayList<List<Object>>();
        List<List<Object>> revisionBatchArgs = new ArrayList<List<Object>>();
        List<List<Object>> hyperlinksBatchArgs = new ArrayList<List<Object>>();

        // prepare the data for batch updating
        for (WikiPage page : wikiPages) {
            cache.addPage(page.getWikiID(), page.getTitle(), page.getPageID());
            pageBatchArgs.add(getPageArgs(page));

            if (page.getRevisions() != null) {
                for (Revision revision : page.getRevisions().values()) {
                    List<Object> args = getRevisionArgs(page.getWikiID(), page.getPageID(), revision);
                    revisionBatchArgs.add(args);
                }
            }

            if (page.getHyperLinks() != null) {
                List<List<Object>> currentLinks = getHyperlinkArgs(page.getWikiID(), page.getPageID(), page
                        .getHyperLinks());
                hyperlinksBatchArgs.addAll(currentLinks);
            }
        }
        int[] result = runBatchUpdate(sqlAddPage, pageBatchArgs);
        runBatchUpdate(sqlAddRevision, revisionBatchArgs);
        runBatchUpdate(sqlAddHyperlink, hyperlinksBatchArgs);
        int skipCounter = 0;
        for (int r : result) {
            if (r == -1) {
                skipCounter++;
            }
        }
        return skipCounter;
    }

    /**
     * Adds a revision of a page to the database.
     * 
     * @param wikiID The ID of the Wiki the page is in.
     * @param pageID The pageID as retrieved from the Wiki.
     * @param revision The revision to add to database.
     * @return True if revision has been added to database or false, if it was already in data base or any error
     *         occurred while executing the {@link PreparedStatement}. If false, see error log for details.
     */
    public boolean addRevision(final int wikiID, final int pageID, final Revision revision) {
        List<Object> args = getRevisionArgs(wikiID, pageID, revision);
        return runUpdate(sqlAddRevision, args) >= 0;
    }

    /**
     * Adds all {@link Revision}s to the database. Use this to add multiple revisions for the same page since database
     * optimizations can be used, it is much faster than calling {@link #addRevision(int, int, Revision)} for every
     * single revision.
     * 
     * @param wikiID The ID of the Wiki the page is in.
     * @param pageID The pageID as retrieved from the Wiki.
     * @param revisions The revisions to add to database.
     * @return The number of pages that have been skipped. See error log for details if return value > 0.
     */
    public int addRevisions(final int wikiID, final int pageID, final Collection<Revision> revisions) {
        // return addRevisions(wikiID, pageID, revisions, true);
        List<List<Object>> batchParams = new ArrayList<List<Object>>();
        for (Revision revision : revisions) {
            List<Object> args = getRevisionArgs(wikiID, pageID, revision);
            batchParams.add(args);
        }
        int[] result = runBatchUpdate(sqlAddRevision, batchParams);
        int skipCounter = 0;
        for (int r : result) {
            if (r == -1) {
                skipCounter++;
            }
        }
        return skipCounter;
    }

    // /**
    // * Helper to process the current line of a {@link ResultSet} that contains a {@link WikiDescriptor}. To get the
    // * namespaces to crawl, {@link #getNamespacesToCrawl(int)} is called.
    // *
    // * @param resultSet The {@link ResultSet} to fetch the {@link WikiDescriptor} from.
    // * @return The {@link WikiDescriptor} from the current line, all parameters are set.
    // * @throws SQLException if a parameter was not found in the {@link ResultSet}.
    // */
    // private WikiDescriptor fetchWikiDescriptorFromResultSetRow(final ResultSet resultSet) throws SQLException {
    // WikiDescriptor wd = null;
    // wd = new WikiDescriptor();
    // wd.setWikiID(resultSet.getInt(1));
    // wd.setWikiName(resultSet.getString(2));
    // wd.setWikiURL(resultSet.getString(3));
    // wd.setPathToAPI(resultSet.getString(4));
    // if (resultSet.getString(5) != null && !resultSet.getString(5).equalsIgnoreCase("NULL")) {
    // Date lastCheck = null;
    // try {
    // lastCheck = convertSQLDateTimeToDate(resultSet.getString(5));
    // } catch (Exception e) {
    // LOGGER.error(
    // "Could not process the timestamp the wiki has been checked for new pages the last time. Wiki \""
    // + resultSet.getString(2) + "\", timestamp: " + resultSet.getString(5) + " ", e);
    // }
    // wd.setLastCheckForModifications(lastCheck);
    // }
    // wd.setCrawlerUserName(resultSet.getString(6));
    // wd.setCrawlerPassword(resultSet.getString(7));
    // wd.setNamespacesToCrawl(getNamespacesToCrawl(wd.getWikiID()));
    // return wd;
    // }

    // /**
    // * Adds a new Wiki page and its revisions to the database.
    // *
    // * @param page The {@link WikiPage} to add to the database.
    // * @return True if page has been added to database or false, if it was already in data base or any error occurred
    // * while executing the {@link PreparedStatement}. If false, see error log for details.
    // */
    // private boolean addPage(final WikiPage page) {
    // int errorCount = 0;
    //
    // List<Object> args = getPageArgs(page);
    //
    // errorCount += runUpdate(sqlAddPage, args) >= 0 ? 0 : 1;
    //
    // for (Revision revision : page.getRevisions().values()) {
    // errorCount += addRevision(page.getWikiID(), page.getPageID(), revision) ? 0 : 1;
    // }
    //
    // cache.addPage(page.getWikiID(), page.getTitle(), page.getPageID());
    //
    // return errorCount == 0;
    // }

    /**
     * Adds a Wiki to the database. If a Wiki with the same name already exists, nothing is done. {@see #updateWiki()}
     * for updating an existing Wiki.
     * 
     * @param wd the descriptor of the Wiki to add.
     * @return true if Wiki has been added to data base or false, if it was already in data base or any error occurred
     *         while executing the {@link PreparedStatement}. If false, see error log for details.
     */
    public boolean addWiki(final WikiDescriptor wd) {
        if (wikiExists(wd.getWikiName())) {
            LOGGER.error("Can't add Wiki \"" + wd.getWikiName() + "\", Wiki is already contained in data base!");
            return false;
        }

        List<Object> args = new ArrayList<Object>();
        args.add(wd.getWikiName());
        args.add(wd.getWikiURL().toString());
        args.add(wd.getRelativePathToAPI());
        if (wd.getLastCheckForModifications() != null) {
            args.add(convertDateToSQLDateTime(wd.getLastCheckForModifications()));
        } else {
            args.add(null);
        }
        args.add(wd.getCrawlerUserName());
        args.add(wd.getCrawlerPassword());
        runUpdate(sqlAddWiki, args);

        // set namespaces to crawl
        for (int namespaceID : wd.getNamespacesToCrawl()) {
            addNamespace(getWikiDescriptor(wd.getWikiName()).getWikiID(), namespaceID, null, true);
        }

        cache.addWiki(getWikiDescriptor(wd.getWikiName()).getWikiID());
        return true;
    }

    /**
     * Clear all wiki specific tables.
     */
    public void clearTables() {
        LOGGER.fatal("TRUNCATE all tables!!");
        // NOTE: disable keys before truncating and enable it afterwards! This saves a lot of processing time
        runUpdate("TRUNCATE TABLE revisions");
        runUpdate("TRUNCATE TABLE links");
        runUpdate("TRUNCATE TABLE pages");
        runUpdate("TRUNCATE TABLE namespaces");
        runUpdate("TRUNCATE TABLE wikis");
    }

    /**
     * Converts a given {@link java.util.Date} to the String representation of the SQL data type DATETIME ("yyyy-MM-dd
     * HH:mm:ss") See {@link #convertSQLDateTimeToDate(String)} for vice versa. The returned value is in
     * {@link TimeZone} UTC.
     * 
     * @param date the date to convert
     * @return String representation of the SQL data type DATETIME in {@link TimeZone} UTC.
     */
    private String convertDateToSQLDateTime(final Date date) {
        LocalizeHelper.setUTCandEnglish();
        String dateTime = DateHelper.getDatetime("yyyy-MM-dd HH:mm:ss", date.getTime());
        LocalizeHelper.restoreTimeZoneAndLocale();
        return dateTime;
    }

    /**
     * Returns a Set of all pageIDs the given page has hyperlinks to.
     * 
     * @param wikiID The wikiID to get all namespaces for.
     * @param pageIDSource The pageID to get outgoing hyperlinks for.
     * @return A Set of all pageIDs the given page has hyperlinks to.
     */
    public Set<Integer> getAllHyperlinks(final int wikiID, final int pageIDSource) {

        final HashSet<Integer> hyperlinks = new HashSet<Integer>();
        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int pageIDDest = resultSet.getInt("pageIDDest");
                hyperlinks.add(pageIDDest);
            }
        };

        runQuery(callback, sqlGetOutgoingHyperlinks, wikiID, pageIDSource);
        return hyperlinks;
    }

    // /**
    // * Checks whether the given pageID is already contained for this wikiID.
    // *
    // * Method should be used in debug mode only since it is resource consuming (additional data base request), analyze
    // * SQLexception instead if a page does not exist.
    // *
    // * @param wikiID The ID of the Wiki the namespace is in.
    // * @param pageID The pageID to be found in the Wiki.
    // * @return true if it is contained, false otherwise.
    // */
    // private boolean pageExists(final int wikiID, final int pageID) {
    // boolean pageIDExists = false;
    //
    // try {
    // PreparedStatement psGetPageByPageID = getPreparedStatement(sqlGetPageByPageID);
    // psGetPageByPageID.setInt(1, wikiID);
    // psGetPageByPageID.setInt(2, pageID);
    // ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetPageByPageID);
    // if (resultSet.next()) {
    // pageIDExists = true;
    // }
    // } catch (SQLException e) {
    // LOGGER.error("pageIDExists processing PreparedStatement " + sqlGetPageByPageID, e);
    // }
    // return pageIDExists;
    // }

    // /**
    // * Checks whether the given pageTitle is already contained for Wiki wikiID.
    // *
    // * Method should be used in debug mode only since it is resource consuming (additional data base request), analyze
    // * SQLexception instead if a page does not exist.
    // *
    // * @param wikiID The ID of the Wiki the namespace is in.
    // * @param pageTitle The name of the page (title) to be found in the Wiki.
    // * @return true if it is contained, false otherwise.
    // */
    // private boolean pageExists(final int wikiID, final String pageTitle) {
    // return (getPageID(wikiID, pageTitle) == null) ? false : true;
    // }

    /**
     * Returns all namespaceIDs and the useForCrawling value for the given wikiID.
     * 
     * @param wikiID The wikiID to get all namespaces for.
     * @return All namespaceIDs and the useForCrawling value for the given wikiID.
     */
    public HashMap<Integer, Boolean> getAllNamespaces(final int wikiID) {
        final HashMap<Integer, Boolean> namespaces = new HashMap<Integer, Boolean>();

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int namespaceID = resultSet.getInt("namespaceID");
                boolean useForCrawling = resultSet.getBoolean("useForCrawling");
                namespaces.put(namespaceID, useForCrawling);
            }
        };

        runQuery(callback, sqlAllNamespaces, wikiID);
        return namespaces;
    }

    /**
     * Returns a {@link List} of all page titles that are used for crawling.
     * 
     * @param wikiID The internal Wiki ID to get the pages for.
     * @return A {@link List} of all page titles that are used for crawling.
     */
    public List<String> getAllPageTitlesToCrawl(final int wikiID) {

        RowConverter<String> converter = new RowConverter<String>() {

            @Override
            public String convert(ResultSet resultSet) throws SQLException {
                return resultSet.getString(1);
            }
        };
        return runQuery(converter, sqlGetAllPageTitlesToCrawl, wikiID);
    }

    /**
     * Fetches the general information about all Wikis from the database and returns them.
     * 
     * @return List<WikiDescriptor> with all Wikis and all parameters set.
     */
    public List<WikiDescriptor> getAllWikiDescriptors() {
        List<WikiDescriptor> wds = runQuery(new WikiDescriptorRowConverter(), sqlGetAllWikiDescriptors);
        for (WikiDescriptor wd : wds) {
            wd.setNamespacesToCrawl(getNamespacesToCrawl(wd.getWikiID()));
        }
        return wds;
    }

    /**
     * Prepares a {@link List} to be used as parameters for queries using {@link #sqlAddHyperlink}
     * 
     * @param wikiID The wikiID the hyperlink belongs to.
     * @param pageIDSource The pageID the hyperlinks comes from.
     * @param hyperLinks The pageIDs pageIDSource has hyperlinks to.
     * @return List containing {{wikiID, pageIDSource, pageIDDest_1}, {wikiID, pageIDSource, pageIDDest_2}}
     */
    private List<List<Object>> getHyperlinkArgs(final int wikiID, final int pageIDSource, final Set<Integer> hyperLinks) {
        List<List<Object>> hyperlinksBatchArgs = new ArrayList<List<Object>>();
        for (Integer link : hyperLinks) {
            List<Object> args = new ArrayList<Object>();
            args.add(wikiID);
            args.add(pageIDSource);
            args.add(link);
            hyperlinksBatchArgs.add(args);
        }
        return hyperlinksBatchArgs;
    }

    /**
     * For a given Wiki, all namespaces are returned that should be included into the crawling.
     * 
     * @param wikiID the Wiki to get the namespaces for.
     * @return All namespace IDs that should be included into the crawling.
     */
    public HashSet<Integer> getNamespacesToCrawl(final int wikiID) {

        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        List<Integer> namespacesList = runQuery(converter, sqlGetNamespaceIDsToCrawl, wikiID);
        return new HashSet<Integer>(namespacesList);
    }

    /**
     * Get a complete page from database, i.e page, metadata, revisions and links to other pages.
     * 
     * @param wikiID The ID of the Wiki the page is in.
     * @param pageID The pageID to get.
     * @return A page and all its revisions from database or null if page is not contained in database.
     */
    public WikiPage getPage(final int wikiID, final int pageID) {
        WikiPage page = getPlainPage(wikiID, pageID);
        if (page != null) {
            for (Revision revision : getRevisions(wikiID, pageID)) {
                page.addRevision(revision);
            }
            page.addHyperLinks(getAllHyperlinks(wikiID, pageID));
        }
        return page;
    }

    private List<Object> getPageArgs(final WikiPage page) {
        List<Object> args = new ArrayList<Object>();
        args.add(page.getWikiID());
        args.add(page.getPageID());
        args.add(page.getTitle());
        args.add(page.getNamespaceID());
        if (page.getSourceDynamics() != null) {
            args.add(page.getSourceDynamics());
        } else {
            args.add(null);
        }
        args.add(page.getPageContentHTML() == null ? "" : page.getPageContentHTML());
        if (page.getNewestRevisionID() != null) {
            args.add(page.getNewestRevisionID());
        } else {
            args.add(null);
        }
        if (page.getNextCheck() != null) {
            args.add(convertDateToSQLDateTime(page.getNextCheck()));
        } else {
            args.add(null);
        }
        return args;
    }

    /**
     * Returns the pageID that belongs to the given pageTitle in Wiki wikiID. To speedup the lookup, a
     * {@link PageTitleCache} is used and the database is accessed only in case of a cache miss. Under some
     * circumstances, it is useful to only check the cache without querying the database in case of a cache miss.
     * This is much faster, but may result in a false negative, if the cache size is too small and the page is in the
     * database but not in the cache.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param pageTitle The name of the page (title) to be found in the Wiki.
     * @param cacheOnly Set to <code>true</code> to only check the cache without querying the database in case of a
     *            cache miss. This is much faster, but may result in a false negative, if the cache size is too small
     *            and the page is in the database but not in the cache. Use with caution!
     * @return The pageID that belongs to the given PAGE_TITLE in Wiki WIKI_ID or <code>null</code> if PAGE_TITLE is
     *         unknown in database.
     */
    public Integer getPageID(final int wikiID, final String pageTitle, final boolean cacheOnly) {
        Integer pageID = null;
        if (pageTitle == null) {
            throw new IllegalArgumentException("PAGE_TITLE must not be null");
        }

        pageID = cache.getPageID(wikiID, pageTitle);

        // if cache-miss, load pageID from database.
        if (pageID == null) {
            if (DEBUG) {
                LOGGER.debug("Cache miss on wikiID=" + wikiID + ", pageTitle=" + pageTitle);
            }

            if (cacheOnly == false) {
                RowConverter<Integer> converter = new RowConverter<Integer>() {
                    @Override
                    public Integer convert(ResultSet resultSet) throws SQLException {
                        return resultSet.getInt("pageID");
                    }
                };
                pageID = runSingleQuery(converter, sqlGetPageIDByPageTitle, wikiID, pageTitle);

                if (pageID != null) {
                    cache.addPage(wikiID, pageTitle, pageID);
                }
            }
        }
        return pageID;
    }

    /**
     * Returns a {@link Set} of all pages that should be updated since their predicted date of a new revision is
     * in the past of the given date. The pages' revisions are not returned.
     * 
     * @param wikiID The internal Wiki ID to get the pages for.
     * @param date The date to compare the predicted date with, usually the current date.
     * @return A {@link Set} of all pages that are used for crawling.
     */
    public Set<WikiPage> getPagesToUpdate(final int wikiID, final Date date) {

        RowConverter<WikiPage> converter = new RowConverter<WikiPage>() {

            @Override
            public WikiPage convert(ResultSet resultSet) throws SQLException {
                WikiPage page = new WikiPage();
                page.setWikiID(wikiID);
                page.setPageID(resultSet.getInt(1));
                page.setTitle(resultSet.getString(2));
                page.setNamespaceID(resultSet.getInt(3));
                page.setSourceDynamics(resultSet.getFloat(4));
                page.setNewestRevisionID(resultSet.getLong(5));
                if (resultSet.getString(6) != null && !resultSet.getString(6).equalsIgnoreCase("NULL")) {
                    Date nextCheck = null;
                    try {
                        nextCheck = convertSQLDateTimeToDate(resultSet.getString(6));
                    } catch (Exception e) {
                        LOGGER.error(
                                "Could not process the timestamp the page has been checked for new revisions the last time. Wiki \""
                                        + wikiID + ", page \"" + page.getTitle() + "\", timestamp: "
                                        + resultSet.getString(6) + " ", e);
                    }
                    page.setNextCheck(nextCheck);
                }
                if (DEBUG) {
                    LOGGER.debug("Got page " + page.getTitle() + " to update revisions.");
                }
                return page;
            }
        };

        List<WikiPage> pagesList = runQuery(converter, sqlGetPagesToUpdate, wikiID, convertDateToSQLDateTime(date));
        return new HashSet<WikiPage>(pagesList);
    }

    /**
     * Adds all {@link Revision}s to the database. Use this to add multiple revisions since database optimizations can
     * be used, it is much faster than calling {@link #addRevision(int, int, Revision)} for every single revision.
     * 
     * @param wikiID The ID of the Wiki the page is in.
     * @param pageID The pageID as retrieved from the Wiki.
     * @param revisions The revisions to add to database.
     * @param modifyAutoCommit If true, the database's autocommit is set to false, all revisions are added and executed
     *            all together by setting autocommit back to true. If this method is called from inside a method that
     *            also modifies autocommit, set this parameter to false to prevent committing the results to early.
     * @return The number of pages that have been skipped. See error log for details if return value > 0.
     */
    // private int addRevisions(final int wikiID, final int pageID, final Collection<Revision> revisions,
    // boolean modifyAutoCommit) {
    //
    // if (modifyAutoCommit) {
    // setAutoCommit(false);
    // }
    //
    // int skipCounter = 0;
    // boolean added = false;
    // for (final Revision revision : revisions) {
    // added = addRevision(wikiID, pageID, revision);
    // skipCounter += (!added) ? 1 : 0;
    // }
    //
    // if (modifyAutoCommit) {
    // setAutoCommit(true);
    // }
    // return skipCounter;
    // }

    // /**
    // * Checks whether the given triple (wikiID, pageID, revisionID) is already contained in the database.
    // *
    // * @param wikiID The ID of the Wiki the namespace is in.
    // * @param pageID The Wiki's pageID.
    // * @param revisionID The Wiki's id of the revision to be found.
    // * @return true if it is contained, false otherwise.
    // */
    // private boolean revisionExists(final int wikiID, final int pageID, final double revisionID) {
    // boolean revisionExists = false;
    // try {
    // PreparedStatement psGetRevision = getPreparedStatement(sqlGetRevision);
    // psGetRevision.setInt(1, wikiID);
    // psGetRevision.setInt(2, pageID);
    // psGetRevision.setDouble(3, revisionID);
    // ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetRevision);
    // if (resultSet.next()) {
    // revisionExists = true;
    // }
    // } catch (SQLException e) {
    // LOGGER.error("revisionExists processing PreparedStatement " + sqlGetRevision, e);
    // }
    // return revisionExists;
    // }

    /**
     * Get a page without its revisions. (data from table pages only: faster.)
     * 
     * @param wikiID The ID of the Wiki the searched page is in.
     * @param pageID The pageID of the page to get information about.
     * @return {@link WikiPage} containing data from table pages but not from table revisions or <code>null</code> if
     *         this page is not contained in the given wiki.
     * @see #getPage(int, int)
     */
    public WikiPage getPlainPage(final int wikiID, final int pageID) {

        RowConverter<WikiPage> converter = new RowConverter<WikiPage>() {

            @Override
            public WikiPage convert(ResultSet resultSet) throws SQLException {
                WikiPage page = new WikiPage();
                page.setWikiID(wikiID);
                page.setPageID(pageID);
                page.setTitle(resultSet.getString("pageTitle"));
                page.setNamespaceID(resultSet.getInt("namespaceID"));
                page.setSourceDynamics(resultSet.getFloat("sourceDynamics"));
                page.setPageContentHTML(resultSet.getString("pageContent"));
                page.setNewestRevisionID(resultSet.getLong("revisionID"));

                String nextCheckS = resultSet.getString("nextCheck");
                if (nextCheckS != null && !nextCheckS.equalsIgnoreCase("NULL")) {
                    Date nextCheckD = null;
                    try {
                        nextCheckD = convertSQLDateTimeToDate(nextCheckS);
                    } catch (Exception e) {
                        LOGGER.error(
                                "Could not process the timestamp the page has been checked for new revisions the last time. Wiki "
                                        + page.getWikiID() + ", page title: " + page.getTitle() + " ", e);
                    }
                    page.setNextCheck(nextCheckD);
                }
                if (resultSet.getString("fullURL") != null) {
                    try {
                        page.setPageURL(new URL(resultSet.getString("fullURL")));
                    } catch (MalformedURLException e) {
                        LOGGER.error("DB contains invalid URL of page \"" + page.getTitle() + "\". ", e);
                    }

                }
                return page;
            }
        };

        return runSingleQuery(converter, sqlGetPageByPageID, wikiID, pageID);
    }

    /**
     * Prepares a {@link List} to be used as parameters for queries using {@link #sqlAddRevision}
     * 
     * @param wikiID The wikiID the revision belongs to.
     * @param pageID The pageID the revision belongs to.
     * @param revision The {@link Revision} itself.
     * @return List containing {wikiID, pageID, revisionID, timestamp in SQL format, author}
     */
    private List<Object> getRevisionArgs(final int wikiID, final int pageID, final Revision revision) {
        List<Object> args = new ArrayList<Object>();
        args.add(wikiID);
        args.add(pageID);
        args.add(revision.getRevisionID());
        args.add(convertDateToSQLDateTime(revision.getTimestamp()));
        args.add(revision.getAuthor());
        return args;
    }

    /**
     * Fetches all revisions for a given page and wiki from database.
     * 
     * @param wikiID The ID of the Wiki the page is in.
     * @param pageID The pageID to get all revisions for.
     * @return A collection containing all revisions of the given page and wiki.
     */
    public Collection<Revision> getRevisions(final int wikiID, final int pageID) {
        final Collection<Revision> revisions = new HashSet<Revision>();

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                Long revisionID = resultSet.getLong("revisionID");

                String timestampString = resultSet.getString("timestamp");
                Date timestamp = null;
                if (timestampString != null && !timestampString.equalsIgnoreCase("NULL")) {
                    try {
                        timestamp = convertSQLDateTimeToDate(timestampString);
                    } catch (Exception e) {
                        LOGGER.error("Could not process the timestamp for wikiID \"" + wikiID + "\", pageID \""
                                + pageID + "\", revisionID \"" + revisionID + "\", timestamp \"" + timestampString
                                + "\": ", e);
                    }
                }

                String user = resultSet.getString("author");
                Revision revision = new Revision(revisionID, timestamp, user);
                revisions.add(revision);
            }
        };

        runQuery(callback, sqlGetRevisions, wikiID, pageID);
        return revisions;
    }

    /**
     * Fetches the general information about the given wikiID from the database and returns it as a
     * {@link WikiDescriptor} where all parameters are set.
     * 
     * @param wikiID The internal ID of the wiki to get information for.
     * @return WikiDescriptor with all parameters set.
     */
    public WikiDescriptor getWikiDescriptor(final int wikiID) {
        WikiDescriptor wd = runSingleQuery(new WikiDescriptorRowConverter(), sqlGetWikiDescriptorByID, wikiID);
        if (wd != null) {
            wd.setNamespacesToCrawl(getNamespacesToCrawl(wd.getWikiID()));
        }
        return wd;
    }

    /**
     * Fetches the general information about the given wikiName from the database and returns it as a
     * {@link WikiDescriptor} where all parameters are set.
     * 
     * @param wikiName The name of the wiki to get information for.
     * @return WikiDescriptor with all parameters set.
     */
    public WikiDescriptor getWikiDescriptor(final String wikiName) {
        WikiDescriptor wd = runSingleQuery(new WikiDescriptorRowConverter(), sqlGetWikiDescriptorByName, wikiName);
        if (wd != null) {
            wd.setNamespacesToCrawl(getNamespacesToCrawl(wd.getWikiID()));
        }
        return wd;
    }

    /**
     * Checks whether the given namespaceID is already contained for this wikiID.
     * 
     * Method should be used in debug mode only since it is resource consuming (additional data base request), analyze
     * SQLexception instead if a namespace does not exist.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param namespaceID A namespaceID as retrieved from the Wiki.
     * @return true if it is contained, false otherwise.
     */
    private boolean namespaceExists(final int wikiID, final int namespaceID) {
        return entryExists(sqlGetNamespace, wikiID, namespaceID);
    }

    /**
     * Remove all entries in table "links" for the given wiki and page as source of a link.
     * 
     * @param wikiID The wikiID of the Wiki.
     * @param title The name of the page that is the source of a link.
     * @return true if update was successful, false if any problem occurred while updating. If false, see error log for
     *         details.
     */
    public boolean removeAllHyperlinks(final int wikiID, final String title) {
        final Integer pageID = getPageID(wikiID, title, false);
        if (pageID == null) {
            LOGGER.error("Could not remove links for page \"" + title + "\", page it is unknown to database!");
            return false;
        }

        List<Object> args = new ArrayList<Object>();
        args.add(wikiID);
        args.add(pageID);

        return runUpdate(sqlRemoveHyperlinks, args) >= 0;
    }

    /**
     * Removes all pages for the given Wiki ID. (Use if a wiki has never been completely crawled, but some pages from a
     * previous crawling attempt are still in data base.)
     * 
     * @param wikiID the Wiki ID to remove all pages for.
     * @return true if removal was successful, false if Wiki does not exist in data base or any problem occurred while
     *         updating. If false, see error log for details.
     */
    public boolean removeAllPages(final int wikiID) {
        boolean success = false;
        if (DEBUG && !wikiExists(getWikiDescriptor(wikiID).getWikiName())) {
            LOGGER.debug("Could not remove all pages for Wiki  \"" + getWikiDescriptor(wikiID).getWikiName()
                    + "\" because it is not contained in the data base.");
        } else {
            success = runUpdate(sqlRemoveAllPages, wikiID) >= 0;
            cache.removeAllPages(wikiID);
        }
        return success;
    }

    /**
     * Removes the given namespace for the given Wiki. Additionally, all pages and their revisions are removed that
     * belong to this namespace.
     * 
     * @param wikiID the Wiki ID to remove.
     * @return true if removal was successful, false if Wiki does not exist in data base or any problem occurred while
     *         updating. If false, see error log for details.
     */
    public boolean removeNamespace(final int wikiID, final int namespaceID) {
        boolean success = false;
        if (DEBUG && !namespaceExists(wikiID, namespaceID)) {
            LOGGER.debug("Could not remove namespaceID \"" + namespaceID + "\" for wiki \""
                    + getWikiDescriptor(wikiID).getWikiName() + "\" because it is not contained in the data base.");
        } else {
            success = runUpdate(sqlRemoveNamespace, wikiID, namespaceID) >= 0;
        }
        return success;
    }

    /**
     * Removes all content for the given Wiki ID: general information, all namespaces, all pages and all revisions.
     * 
     * @param wikiID the Wiki ID to remove.
     * @return true if removal was successful, false if Wiki does not exist in data base or any problem occurred while
     *         updating. If false, see error log for details.
     */
    public boolean removeWiki(final int wikiID) {
        boolean success = false;
        if (DEBUG && !wikiExists(getWikiDescriptor(wikiID).getWikiName())) {
            LOGGER.debug("Could not remove Wiki  \"" + getWikiDescriptor(wikiID).getWikiName()
                    + "\" because it is not contained in the data base.");
        } else {
            success = runUpdate(sqlRemoveWiki, wikiID) >= 0;
            cache.removeWiki(wikiID);
        }
        return success;
    }

    /**
     * Updates the parameter useForCrawling in the data base.
     * 
     * @param wikiID The wikiID of the Wiki the namespace is in.
     * @param namespaceID The namespaceID to update.
     * @param useForCrawling set to true if namespace should be included into crawling, false otherwise.
     * @return true if update was successful, false if namespaceID does not exist for this wikiID in data base or any
     *         problem occurred while updating. If false, see error log for details.
     */
    private boolean updateNamespace(final int wikiID, final int namespaceID, final boolean useForCrawling) {
        boolean success = false;
        if (DEBUG && !namespaceExists(wikiID, namespaceID)) {
            LOGGER.debug("Could not update namespace with ID \"" + namespaceID
                    + "\" because it is not contained in the data base.");
        } else {
            List<Object> args = new ArrayList<Object>();
            args.add(useForCrawling);
            args.add(wikiID);
            args.add(namespaceID);
            success = runUpdate(sqlUpdateNamespace, args) >= 0;
        }
        return success;
    }

    /**
     * Updates the parameter namespaceName in the data base.
     * 
     * @param wikiID The wikiID of the Wiki the namespace is in.
     * @param namespaceID The namespaceID to update.
     * @param namespaceName The new name to write to data base.
     * @return true if update was successful, false if namespaceID does not exist for this wikiID in data base or any
     *         problem occurred while updating. If false, see error log for details.
     */
    public boolean updateNamespaceName(final int wikiID, final int namespaceID, final String namespaceName) {
        boolean success = false;
        if (DEBUG && !namespaceExists(wikiID, namespaceID)) {
            LOGGER.debug("Could not update namespace with ID \"" + namespaceID
                    + "\" because it is not contained in the data base.");
        } else {
            List<Object> args = new ArrayList<Object>();
            args.add(namespaceName);
            args.add(wikiID);
            args.add(namespaceID);
            success = runUpdate(sqlUpdateNamespaceName, args) >= 0;
        }
        return success;
    }

    /**
     * Update the time stamp to check this page the next time for new revisions.
     * 
     * @param wikiID The wikiID of the Wiki.
     * @param pageID The pageID of the page to update.
     * @param nextCheck The predicted date to check this page for new revisions.
     * @return true if update was successful, false if any problem occurred while updating. If false, see error log for
     *         details.
     */
    public boolean updatePage(final int wikiID, final int pageID, final Date nextCheck) {

        List<Object> args = new ArrayList<Object>();
        if (nextCheck != null) {
            args.add(convertDateToSQLDateTime(nextCheck));
        } else {
            args.add(null);
        }
        args.add(wikiID);
        args.add(pageID);

        return runUpdate(sqlUpdatePageNextCheck, args) >= 0;
    }

    /**
     * Updates the parameters revisionID and pageContent in table pages, used to set these values to the most recent
     * revision in Wiki.
     * 
     * @param wikiID The wikiID of the Wiki.
     * @param pageID The Wiki's pageID to update.
     * @param revisionID the revisionID of the pageContent.
     * @param pageContent the page content, including HTML mark-up but no wiki mark-up.
     * @param nextCheck The predicted date to check this page for new revisions.
     * @param fullURL The page's URL.
     * @return true if update was successful, false if any problem occurred while updating. If false, see error log for
     *         details.
     */
    public boolean updatePage(final int wikiID, final int pageID, final long revisionID, final String pageContent,
            final Date nextCheck, final String fullURL) {

        List<Object> args = new ArrayList<Object>();
        args.add(revisionID);
        args.add(pageContent);
        if (nextCheck != null) {
            args.add(convertDateToSQLDateTime(nextCheck));
        } else {
            args.add(null);
        }
        args.add(fullURL);
        args.add(wikiID);
        args.add(pageID);

        return runUpdate(sqlUpdatePage, args) >= 0;
    }

    // /**
    // * En/disable the auto commit functionality of the data base. If disabled, execution of {@link PreparedStatement}s
    // * is scheduled till auto commit is enabled or a manual {@link #commit()} is called.
    // *
    // * @param autoCommit auto commit value to set.
    // */
    // private void setAutoCommit(final boolean autoCommit) {
    // try {
    // DatabaseManager.getInstance().getConnection().setAutoCommit(autoCommit);
    // } catch (SQLException e) {
    // LOGGER.error("Could not set Connection.setAutoCommit(" + autoCommit + ") ", e);
    // }
    // }
    //
    // /**
    // * Commit all scheduled {@link PreparedStatement}s.
    // */
    // @SuppressWarnings("unused")
    // private void commit() {
    // try {
    // DatabaseManager.getInstance().getConnection().commit();
    // } catch (SQLException e) {
    // LOGGER.error("Could not commit() Connection ", e);
    // }
    // }

    /**
     * Updates the parameters wikiURL, pathToAPI, crawler_username, crawler_password and the namespaces to be crawled
     * in the data base.
     * 
     * @param wd the Wiki to update.
     * @return true if update was successful, false if Wiki does not exist in data base or any problem occurred while
     *         updating. If false, see error log for details.
     */
    public boolean updateWiki(final WikiDescriptor wd) {
        return updateWiki(wd, true);
    }

    /**
     * Updates the parameters wikiURL, pathToAPI, pathToContent, crawler_username, crawler_password in the data base.
     * The namespaces to be crawled are updated iff updateNamespaces is set to {@code true}.
     * 
     * @param wd the Wiki to update.
     * @param updateNamespaces Set to true to update namespaces or false to skip them.
     * @return true if update was successful, false if Wiki does not exist in data base or any problem occurred while
     *         updating. If false, see error log for details.
     */
    private boolean updateWiki(final WikiDescriptor wd, final boolean updateNamespaces) {

        int errorCount = 0;
        if (DEBUG && !wikiExists(wd.getWikiName())) {
            LOGGER.debug("Could not update Wiki \"" + wd.getWikiName()
                    + "\" because it is not contained in the data base.");
        } else {

            List<Object> args = new ArrayList<Object>();
            args.add(wd.getWikiURL().toString());
            args.add(wd.getRelativePathToAPI());
            args.add(wd.getRelativePathToContent());
            if (wd.getLastCheckForModifications() != null) {
                args.add(convertDateToSQLDateTime(wd.getLastCheckForModifications()));
            } else {
                args.add(null);
            }
            args.add(wd.getCrawlerUserName());
            args.add(wd.getCrawlerPassword());
            args.add(wd.getWikiID());
            errorCount += runUpdate(sqlUpdateWiki, args) >= 0 ? 0 : 1;

            // update namespaces to crawl. If there is any namespace specified in the WikiDescriptor, set
            // crawling=true for all namespaces in the list, for all others in database, set crawling=false
            if (updateNamespaces) {
                int wikiID = wd.getWikiID();
                HashMap<Integer, Boolean> namespacesDB = getAllNamespaces(wikiID);
                boolean resetLastCheck = false;
                if (wd.getNamespacesToCrawl().size() > 0) {
                    @SuppressWarnings("unchecked")
                    HashSet<Integer> nameSpacesWD = (HashSet<Integer>) (wd.getNamespacesToCrawl()).clone();
                    for (int nameSpaceIDInDB : namespacesDB.keySet()) {

                        // namespace was in configuration and db, useForCrawling was set to false in db but is
                        // changed to true, than set set date lastCheckNewPages to null for this wiki since the new
                        // namespace has never been checked.
                        if (nameSpacesWD.contains(nameSpaceIDInDB)) {
                            if (!namespacesDB.get(nameSpaceIDInDB)) {
                                errorCount += updateNamespace(wikiID, nameSpaceIDInDB, true) ? 0 : 1;
                                resetLastCheck = true;
                            }
                        } else {
                            errorCount += updateNamespace(wikiID, nameSpaceIDInDB, false) ? 0 : 1;
                        }
                        nameSpacesWD.remove(nameSpaceIDInDB);
                    }
                    for (int nameSpaceIDInFile : nameSpacesWD) {
                        errorCount += addNamespace(wikiID, nameSpaceIDInFile, null, true) ? 0 : 1;
                    }
                } else {
                    for (int namespaceIDInDB : namespacesDB.keySet()) {
                        errorCount += removeNamespace(wikiID, namespaceIDInDB) ? 0 : 1;
                    }
                }

                if (resetLastCheck) {
                    wd.setLastCheckForModifications(null);
                    updateWiki(wd, false);
                }
            }

        }
        return errorCount == 0;
    }

    /**
     * Checks whether the given wikiName is already contained in database.
     * 
     * @param wikiName The name of the wiki to get information for.
     * @return true if it is contained, false otherwise.
     */
    public boolean wikiExists(final String wikiName) {
        return getWikiDescriptor(wikiName) == null ? false : true;
    }

}