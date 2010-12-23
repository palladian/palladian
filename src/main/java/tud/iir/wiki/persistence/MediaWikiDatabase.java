package tud.iir.wiki.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.helper.DateHelper;
import tud.iir.helper.LocalizeHelper;
import tud.iir.persistence.DatabaseManager;
import tud.iir.wiki.MediaWikiCrawler;
import tud.iir.wiki.data.Revision;
import tud.iir.wiki.data.WikiDescriptor;
import tud.iir.wiki.data.WikiPage;

    /**
 * Data base persistence layer that provides adding, removing and reading of data used by {@link MediaWikiCrawler}.
 * 
 * In all removeX()-methods, deletion of foreign keys is done by data base, so make sure foreign keys and "ON DELETE
 * CASCADE" have been set properly!
 * 
 * @author Sandro Reichert
 */
    public class MediaWikiDatabase {

    /** the instance of this class */
    private final static MediaWikiDatabase INSTANCE = new MediaWikiDatabase();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MediaWikiDatabase.class);

    /** do not call LOGGER.isDebugEnabled() 1000 times */
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    // ////////////////// prepared statements ////////////////////
    private PreparedStatement psGetNamespaceIDsToCrawl;
    private PreparedStatement psAllNamespaces;
    private PreparedStatement psAddWiki;
    private PreparedStatement psUpdateWiki;
    private PreparedStatement psRemoveWiki;
    private PreparedStatement psGetWikiDescriptorByName;
    private PreparedStatement psGetWikiDescriptorByID;
    private PreparedStatement psGetAllWikiDescriptors;
    private PreparedStatement psAddPage;
    private PreparedStatement psUpdatePage;
    private PreparedStatement psUpdatePageNextCheck;
    private PreparedStatement psRemoveAllPages;
    private PreparedStatement psAddNamespace;
    private PreparedStatement psGetNamespace;
    private PreparedStatement psUpdateNamespace;
    private PreparedStatement psUpdateNamespaceName;
    private PreparedStatement psRemoveNamespace;
    private PreparedStatement psGetPageByPageID;
    private PreparedStatement psGetPageByTitle;
    private PreparedStatement psGetPageIDByPageTitle;
    private PreparedStatement psGetPagesToUpdate;
    private PreparedStatement psAddRevision;
    private PreparedStatement psGetRevision;
    private PreparedStatement psGetAllPageTitlesToCrawl;


    /**
     * Constructor that prepares all {@link PreparedStatement}s.
     */
    private MediaWikiDatabase() {
        try {
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
            }
    }

    /**
     * Get the single instance of this class.
     * 
     * @return The single instance of this class.
     */
    public static MediaWikiDatabase getInstance() {
        return INSTANCE;
    }

    /**
     * Precompile all {@link PreparedStatement}s.
     * 
     * @throws SQLException If a database access error occurs or this method is called on a closed connection.
     */
    private void prepareStatements() throws SQLException {
        Connection connection = DatabaseManager.getInstance().getConnection();

        psGetNamespaceIDsToCrawl = connection
                .prepareStatement("SELECT namespaceID FROM namespaces WHERE wikiID = ? AND useForCrawling = 1");

        psAllNamespaces = connection
                .prepareStatement("SELECT namespaceID, useForCrawling FROM namespaces WHERE wikiID = ?");

        psAddWiki = connection
                .prepareStatement("INSERT INTO wikis(wikiName, wikiURL, pathToApi, lastCheckNewPages, crawler_username, crawler_password) VALUES (?,?,?,?,?,?)");

        psUpdateWiki = connection
                .prepareStatement("UPDATE wikis SET wikiURL= ?, pathToApi = ?, lastCheckNewPages = ?, crawler_username = ?, crawler_password = ? WHERE wikiID = ?");

        psUpdateNamespace = connection
                .prepareStatement("UPDATE namespaces SET useForCrawling = ? WHERE wikiID = ? AND namespaceID = ?");

        psUpdateNamespaceName = connection
                .prepareStatement("UPDATE namespaces SET namespaceName = ? WHERE wikiID = ? AND namespaceID = ?");

        psRemoveNamespace = connection.prepareStatement("DELETE FROM namespaces WHERE wikiID = ? AND namespaceID = ?");

        psRemoveWiki = connection.prepareStatement("DELETE FROM wikis WHERE wikiID = ?");

        psGetWikiDescriptorByName = connection
                .prepareStatement("SELECT * FROM wikis WHERE wikiName COLLATE utf8_bin = ?");

        psGetWikiDescriptorByID = connection.prepareStatement("SELECT * FROM wikis WHERE wikiID = ?");

        psGetAllWikiDescriptors = connection.prepareStatement("SELECT * FROM wikis ORDER BY wikiName");

        psAddPage = connection
                .prepareStatement("INSERT INTO pages(wikiID, pageID, pageTitle, namespaceID, sourceDynamics, pageContent, revisionID, nextCheck) VALUES (?,?,?,?,?,?,?,?)");

        psUpdatePage = connection
                .prepareStatement("UPDATE pages SET revisionID = ?, pageContent = ?, nextCheck = ? WHERE wikiID = ? AND pageID = ?");

        psUpdatePageNextCheck = connection
                .prepareStatement("UPDATE pages SET nextCheck = ? WHERE wikiID = ? AND pageTitle COLLATE utf8_bin = ?");

        psRemoveAllPages = connection.prepareStatement("DELETE FROM pages WHERE wikiID = ?");

        psAddNamespace = connection
                .prepareStatement("INSERT INTO namespaces(wikiID, namespaceID, namespaceName, useForCrawling) VALUES (?,?,?,?)");

        psGetNamespace = connection
                .prepareStatement("SELECT namespaceID FROM namespaces WHERE wikiID = ? AND namespaceID = ?");

        psGetPageByPageID = connection.prepareStatement("SELECT pageID FROM pages WHERE wikiID = ? AND pageID = ?");

        psGetPageByTitle = connection
                .prepareStatement("SELECT pageID, namespaceID, sourceDynamics, pageContent, revisionID, nextCheck FROM pages WHERE wikiID = ? AND pageTitle COLLATE utf8_bin = ?");

        psGetPageIDByPageTitle = connection
                .prepareStatement("SELECT pageID FROM pages WHERE wikiID = ? AND pageTitle COLLATE utf8_bin = ? ");

        psGetPagesToUpdate = connection
                .prepareStatement("SELECT pageID, pageTitle, namespaceID, sourceDynamics, revisionID, nextCheck FROM pages WHERE wikiID = ? AND (nextCheck IS NULL OR  TIMEDIFF(nextCheck, ?) < 0)");

        psAddRevision = connection
                .prepareStatement("INSERT INTO revisions(wikiID, pageID, revisionID, timestamp, author) VALUES (?,?,?,?,?)");

        psGetRevision = connection
                .prepareStatement("SELECT * FROM revisions WHERE wikiID = ? AND pageID = ? AND revisionID = ?");

        psGetAllPageTitlesToCrawl = connection
                .prepareStatement("SELECT pageTitle FROM pages WHERE wikiID = ? AND namespaceID IN (SELECT namespaceID FROM namespaces WHERE pages.wikiID = namespaces.wikiID AND useForCrawling = 1)");

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
     * Converts a date, given in String representation of the SQL data type DATETIME (yyyy-MM-dd hh:mm:ss) to
     * {@link java.util.Date} It is assumed that the dateTime is in UTC {@link TimeZone}.
     * 
     * @param dateTime A Date String representation of the SQL data type DATETIME (yyyy-MM-dd hh:mm:ss)
     * @return The date as a {@link java.util.Date} object.
     * @throws Exception If no valid Date format could be found. See
     *             {@link tud.iir.daterecognition.dates.ExtractedDate#getNormalizedDate()} for details.
     */
    private Date convertSQLDateTimeToDate(final String dateTime) throws Exception {
        LocalizeHelper.setUTCandEnglish();
        Date date = DateGetterHelper.findDate(dateTime).getNormalizedDate();
        LocalizeHelper.restoreTimeZoneAndLocale();
        return date;
        }

    /**
     * For a given Wiki, all namespaces are returned that should be included into the crawling.
     * 
     * @param wikiID the Wiki to get the namespaces for.
     * @return All namespace IDs that should be included into the crawling.
     */
    public HashSet<Integer> getNamespacesToCrawl(final int wikiID) {
        HashSet<Integer> namespaces = new HashSet<Integer>();
        try {
            psGetNamespaceIDsToCrawl.setInt(1, wikiID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetNamespaceIDsToCrawl);
            while (resultSet.next()) {
                namespaces.add(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error("getNamespacesToCrawl processing PreparedStatement " + psGetNamespaceIDsToCrawl.toString(), e);
            }
        return namespaces;
    }

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
        try {
            psAddWiki.setString(1, wd.getWikiName());
            psAddWiki.setString(2, wd.getWikiURL());
            psAddWiki.setString(3, wd.getPathToAPI());
            if (wd.getLastCheckForModifications() != null) {
                psAddWiki.setString(4, convertDateToSQLDateTime(wd.getLastCheckForModifications()));
            } else {
                psAddWiki.setNull(4, java.sql.Types.DATE);
            }
            psAddWiki.setString(5, wd.getCrawlerUserName());
            psAddWiki.setString(6, wd.getCrawlerPassword());
            DatabaseManager.getInstance().runUpdate(psAddWiki);

            // set namespaces to crawl
            for (int namespaceID : wd.getNamespacesToCrawl()) {
                addNamespace(getWikiDescriptor(wd.getWikiName()).getWikiID(), namespaceID, null, true);
            }
            return true;
        } catch (SQLException e) {
            LOGGER.error("addNewWiki processing PreparedStatement " + psAddWiki.toString(), e);
            return false;
            }
    }

    /**
     * Fetches the general information about the given wikiName from the database and returns it as a
     * {@link WikiDescriptor} where all parameters are set.
     * 
     * @param wikiName The name of the wiki to get information for.
     * @return WikiDescriptor with all parameters set.
     */
    public WikiDescriptor getWikiDescriptor(final String wikiName) {
        WikiDescriptor wd = null;
        try {
            psGetWikiDescriptorByName.setString(1, wikiName);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetWikiDescriptorByName);
            if (resultSet.next()) {
                wd = fetchWikiDescriptorFromResultSetRow(resultSet);
            }
        } catch (SQLException e) {
            LOGGER.error("getWikiDescriptor processing PreparedStatement " + psGetWikiDescriptorByName.toString(), e);
            }
        return wd;
    }

    /**
     * Fetches the general information about the given wikiID from the database and returns it as a
     * {@link WikiDescriptor} where all parameters are set.
     * 
     * @param wikiID The internal ID of the wiki to get information for.
     * @return WikiDescriptor with all parameters set.
     */
    public WikiDescriptor getWikiDescriptor(final int wikiID) {
        WikiDescriptor wd = null;
        try {
            psGetWikiDescriptorByID.setInt(1, wikiID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetWikiDescriptorByID);
            if (resultSet.next()) {
                wd = fetchWikiDescriptorFromResultSetRow(resultSet);
            }
        } catch (SQLException e) {
            LOGGER.error("getWikiDescriptor processing PreparedStatement " + psGetWikiDescriptorByID.toString(), e);
            }
        return wd;
    }

    /**
     * Fetches the general information about all Wikis from the database and returns them.
     * 
     * @return List<WikiDescriptor> with all Wikis and all parameters set.
     */
    public List<WikiDescriptor> getAllWikiDescriptors() {
        List<WikiDescriptor> wd = new LinkedList<WikiDescriptor>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAllWikiDescriptors);
            while (resultSet.next()) {
                wd.add(fetchWikiDescriptorFromResultSetRow(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.error("getWikiDescriptor processing PreparedStatement " + psGetWikiDescriptorByName.toString(), e);
            }
        return wd;
    }

    /**
     * Helper to process the current line of a {@link ResultSet} that contains a {@link WikiDescriptor}. To get the
     * namespaces to crawl, {@link #getNamespacesToCrawl(int)} is called.
     * 
     * @param resultSet The {@link ResultSet} to fetch the {@link WikiDescriptor} from.
     * @return The {@link WikiDescriptor} from the current line, all parameters are set.
     * @throws SQLException if a parameter was not found in the {@link ResultSet}.
     */
    private WikiDescriptor fetchWikiDescriptorFromResultSetRow(final ResultSet resultSet) throws SQLException {
        WikiDescriptor wd = null;
        wd = new WikiDescriptor();
        wd.setWikiID(resultSet.getInt(1));
        wd.setWikiName(resultSet.getString(2));
        wd.setWikiURL(resultSet.getString(3));
        wd.setPathToAPI(resultSet.getString(4));
        if (resultSet.getString(5) != null && !resultSet.getString(5).equalsIgnoreCase("NULL")) {
            Date lastCheck = null;
            try {
                lastCheck = convertSQLDateTimeToDate(resultSet.getString(5));
            } catch (Exception e) {
                LOGGER.error(
                        "Could not process the timestamp the wiki has been checked for new pages the last time. Wiki \""
                                + resultSet.getString(2) + "\", timestamp: " + resultSet.getString(5) + " ", e);
            }
            wd.setLastCheckForModifications(lastCheck);
            }
        wd.setCrawlerUserName(resultSet.getString(6));
        wd.setCrawlerPassword(resultSet.getString(7));
        wd.setNamespacesToCrawl(getNamespacesToCrawl(wd.getWikiID()));
        return wd;
    }

    /**
     * Adds a new Wiki page and its revisions to the database.
     * 
     * @param page The {@link WikiPage} to add to the database.
     * @return True if page has been added to database or false, if it was already in data base or any error occurred
     *         while executing the {@link PreparedStatement}. If false, see error log for details.
     */
    private boolean addPage(final WikiPage page) {

        int errorCount = 0;

            try {
            psAddPage.setInt(1, page.getWikiID());
            psAddPage.setInt(2, page.getPageID());
            psAddPage.setString(3, page.getTitle());
            psAddPage.setInt(4, page.getNamespaceID());
            if (page.getSourceDynamics() != null) {
                psAddPage.setFloat(5, page.getSourceDynamics());
            } else {
                psAddPage.setNull(5, java.sql.Types.FLOAT);
            }
            psAddPage.setString(6, (page.getPageContent() == null) ? "" : page.getPageContent());
            if (page.getNewestRevisionID() != null) {
                psAddPage.setLong(7, page.getNewestRevisionID());
            } else {
                psAddPage.setNull(7, java.sql.Types.BIGINT);
            }
            if (page.getNextCheck() != null) {
                psAddPage.setString(8, convertDateToSQLDateTime(page.getNextCheck()));
            } else {
                psAddPage.setNull(8, java.sql.Types.DATE);
            }
            errorCount += ((DatabaseManager.getInstance().runUpdate(psAddPage)) >= 0) ? 0 : 1;

            for (Revision revision : page.getRevisions().values()) {
                errorCount += (addRevision(page.getWikiID(), page.getPageID(), revision)) ? 0 : 1;
            }
        } catch (SQLException e) {
            LOGGER.error("Cant add page to database, pageID " + page.getPageID() + " \"" + page.getTitle()
                    + "\", namespace " + page.getNamespaceID() + " for wikiID " + page.getWikiID() + ": "
                    + e.getMessage());
            errorCount++;
            }
        return (errorCount == 0);
    }

    /**
     * Adds a set of Wiki pages to the database. Use this to add multiple pages since database optimizations can be
     * used, it is much faster than calling {@link #addPage(int, int, String, int, Float, String, Long)} for every
     * single page.
     * 
     * @param wikiPages The pages to add to database.
     * @return The number of pages that have been skipped. See error log for details if return value > 0.
     */
    public int addPages(final Set<WikiPage> wikiPages) {

        setAutoCommit(false);
        int skipCounter = 0;
        boolean added = false;
        for (final WikiPage page : wikiPages) {
            added = addPage(page);
            skipCounter += (!added) ? 1 : 0;
            }
        setAutoCommit(true);
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
            try {
                psAddNamespace.setInt(1, wikiID);
                psAddNamespace.setInt(2, namespaceID);
                psAddNamespace.setString(3, namespaceName);
                psAddNamespace.setBoolean(4, useForCrawling);
                success = ((DatabaseManager.getInstance().runUpdate(psAddNamespace)) >= 0); // ? true : false;
            } catch (SQLException e) {
                LOGGER.error("psAddNamespace processing PreparedStatement " + psAddNamespace.toString(), e);
            }
            }
        return success;
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
        boolean namespaceExists = false;

        try {
            psGetNamespace.setInt(1, wikiID);
            psGetNamespace.setInt(2, namespaceID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetNamespace);
            if (resultSet.next()) {
                namespaceExists = true;
            }
        } catch (SQLException e) {
            LOGGER.error("namespaceExists processing PreparedStatement " + psGetNamespace.toString(), e);
            }
        return namespaceExists;
    }

    /**
     * Checks whether the given pageID is already contained for this wikiID.
     * 
     * Method should be used in debug mode only since it is resource consuming (additional data base request), analyze
     * SQLexception instead if a page does not exist.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param pageID The pageID to be found in the Wiki.
     * @return true if it is contained, false otherwise.
     */
    private boolean pageExists(final int wikiID, final int pageID) {
        boolean pageIDExists = false;

        try {
            psGetPageByPageID.setInt(1, wikiID);
            psGetPageByPageID.setInt(2, pageID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetPageByPageID);
            if (resultSet.next()) {
                pageIDExists = true;
            }
        } catch (SQLException e) {
            LOGGER.error("pageIDExists processing PreparedStatement " + psGetPageByPageID.toString(), e);
        }
        return pageIDExists;
        }

    /**
     * Checks whether the given pageTitle is already contained for Wiki wikiID.
     * 
     * Method should be used in debug mode only since it is resource consuming (additional data base request), analyze
     * SQLexception instead if a page does not exist.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param pageTitle The name of the page (title) to be found in the Wiki.
     * @return true if it is contained, false otherwise.
     */
    private boolean pageExists(final int wikiID, final String pageTitle) {
        return (getPageID(wikiID, pageTitle) == null) ? false : true;
    }

    /**
     * Returns the pageID that belongs to the given pageTitle in Wiki wikiID
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param pageTitle The name of the page (title) to be found in the Wiki.
     * @return The pageID that belongs to the given PAGE_TITLE in Wiki WIKI_ID or null if PAGE_TITLE is unknown in
     *         database.
     */
    public Integer getPageID(final int wikiID, final String pageTitle) {
        Integer pageID = null;
        if (pageTitle == null) {
            throw new IllegalArgumentException("PAGE_TITLE must not be null");
            }
            try {
            psGetPageIDByPageTitle.setInt(1, wikiID);
            psGetPageIDByPageTitle.setString(2, pageTitle);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetPageIDByPageTitle);
            if (resultSet.next()) {
                pageID = resultSet.getInt(1);
            }
            } catch (SQLException e) {
            LOGGER.error("getPageID processing PreparedStatement " + psGetPageIDByPageTitle.toString(), e);
            }
        return pageID;
    }

    /**
     * Get a page without its revisions. (data from table pages only)
     * 
     * @param wikiID The ID of the Wiki the searched page is in.
     * @param pageTitle The name of the page (title) to get information about.
     * @return {@link WikiPage} containing data from table pages but not from table revisions.
     */
    public WikiPage getPage(final int wikiID, final String pageTitle) {
        WikiPage page = null;
        if (pageTitle == null) {
            throw new IllegalArgumentException("PAGE_TITLE must not be null");
            }
        try {
            psGetPageByTitle.setInt(1, wikiID);
            psGetPageByTitle.setString(2, pageTitle);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetPageByTitle);
            if (resultSet.next()) {
                page = new WikiPage();
                page.setWikiID(wikiID);
                page.setTitle(pageTitle);
                page.setPageID(resultSet.getInt(1));
                page.setNamespaceID(resultSet.getInt(2));
                page.setSourceDynamics(resultSet.getFloat(3));
                page.setPageContent(resultSet.getString(4));
                page.setNewestRevisionID(resultSet.getLong(5));

                if (resultSet.getString(6) != null && !resultSet.getString(6).equalsIgnoreCase("NULL")) {
                    Date lastCheck = null;
                    try {
                        lastCheck = convertSQLDateTimeToDate(resultSet.getString(6));
                    } catch (Exception e) {
                        LOGGER.error(
                                "Could not process the timestamp the page has been checked for new revisions the last time. Wiki "
                                        + page.getWikiID() + ", page title: " + page.getTitle() + " ", e);
                    }
                    page.setNextCheck(lastCheck);
                    }
                }
        } catch (SQLException e) {
            LOGGER.error("getPage processing PreparedStatement " + psGetPageByTitle.toString(), e);
            }
        return page;
    }

    /**
     * Checks whether the given wikiName is already contained in database.
     * 
     * @param wikiName The name of the wiki to get information for.
     * @return true if it is contained, false otherwise.
     */
    public boolean wikiExists(final String wikiName) {
        return (getWikiDescriptor(wikiName) == null) ? false : true;
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
        boolean success = false;
        if (DEBUG && revisionExists(wikiID, pageID, revision.getRevisionID())) {
            LOGGER.debug("Could not add revisionID=" + revision.getRevisionID() + "to database. WikiID=" + wikiID
                    + ", pageID = " + pageID + ", timestamp=" + revision.getTimestamp()
                    + ". , it is already contained in data base!");
        } else {
            try {
                psAddRevision.setInt(1, wikiID);
                psAddRevision.setInt(2, pageID);
                psAddRevision.setLong(3, revision.getRevisionID());
                psAddRevision.setString(4, convertDateToSQLDateTime(revision.getTimestamp()));
                psAddRevision.setString(5, revision.getAuthor());
                success = ((DatabaseManager.getInstance().runUpdate(psAddRevision)) >= 0); // ? true : false;
            } catch (SQLException e) {
                LOGGER.error("Could not add revisionID=" + revision.getRevisionID() + "to database. WikiID=" + wikiID
                        + ", pageID = " + pageID + ", timestamp=" + revision.getTimestamp() + ". " + e);
            }
            }
        return success;
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
        return addRevisions(wikiID, pageID, revisions, true);
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
    private int addRevisions(final int wikiID, final int pageID, final Collection<Revision> revisions,
            boolean modifyAutoCommit) {

        if (modifyAutoCommit) {
            setAutoCommit(false);
            }

        int skipCounter = 0;
        boolean added = false;
        for (final Revision revision : revisions) {
            added = addRevision(wikiID, pageID, revision);
            skipCounter += (!added) ? 1 : 0;
            }

        if (modifyAutoCommit) {
            setAutoCommit(true);
            }
        return skipCounter;
    }

    /**
     * Checks whether the given triple (wikiID, pageID, revisionID) is already contained in the database.
     * 
     * @param wikiID The ID of the Wiki the namespace is in.
     * @param pageID The Wiki's pageID.
     * @param revisionID The Wiki's id of the revision to be found.
     * @return true if it is contained, false otherwise.
     */
    private boolean revisionExists(final int wikiID, final int pageID, final double revisionID) {
        boolean revisionExists = false;
        try {
            psGetRevision.setInt(1, wikiID);
            psGetRevision.setInt(2, pageID);
            psGetRevision.setDouble(3, revisionID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetRevision);
            if (resultSet.next()) {
                revisionExists = true;
            }
            } catch (SQLException e) {
            LOGGER.error("revisionExists processing PreparedStatement " + psGetRevision.toString(), e);
        }
        return revisionExists;
        }

    /**
     * Returns a {@link List} of all page titles that are used for crawling.
     * 
     * @param wikiID The internal Wiki ID to get the pages for.
     * @return A {@link List} of all page titles that are used for crawling.
     */
    public List<String> getAllPageTitlesToCrawl(final int wikiID) {
        List<String> titles = new LinkedList<String>();

        try {
            psGetAllPageTitlesToCrawl.setInt(1, wikiID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAllPageTitlesToCrawl);
            while (resultSet.next()) {
                titles.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "getAllPageTitlesToCrawl processing PreparedStatement " + psGetAllPageTitlesToCrawl.toString(), e);
        }
        return titles;
    }

    /**
     * Returns a {@link List} of all page titles that should be updated since their predicted date of a new revision is
     * in the past of the given date.
     * 
     * @param wikiID The internal Wiki ID to get the pages for.
     * @param date The date to compare the predicted date with, usually the current date.
     * @return A {@link List} of all page titles that are used for crawling.
     */
    public HashSet<WikiPage> getPagesToUpdate(final int wikiID, final Date date) {
        HashSet<WikiPage> pagesToUpdate = new HashSet<WikiPage>();

        try {
            psGetPagesToUpdate.setInt(1, wikiID);
            psGetPagesToUpdate.setString(2, convertDateToSQLDateTime(date));
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetPagesToUpdate);

            while (resultSet.next()) {
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
                pagesToUpdate.add(page);
                }
        } catch (SQLException e) {
            LOGGER.error("getGetPagesToUpdate processing PreparedStatement " + psGetPagesToUpdate.toString(), e);
            }
        return pagesToUpdate;
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
            try {
                psRemoveWiki.setInt(1, wikiID);
                success = ((DatabaseManager.getInstance().runUpdate(psRemoveWiki)) >= 0); // ? true : false;
            } catch (SQLException e) {
                LOGGER.error("removeWiki processing PreparedStatement " + psRemoveWiki.toString(), e);
                }
                    }
        return success;
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
            try {
                psRemoveAllPages.setInt(1, wikiID);
                success = ((DatabaseManager.getInstance().runUpdate(psRemoveAllPages)) >= 0); // ? true : false;
                if (DEBUG) {
                    LOGGER.debug("Removed all pages for Wiki \"" + getWikiDescriptor(wikiID).getWikiName() + "\"");
                }
            } catch (SQLException e) {
                LOGGER.error("removeAllPages processing PreparedStatement " + psRemoveAllPages.toString(), e);
                }
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
            try {
                psRemoveNamespace.setInt(1, wikiID);
                psRemoveNamespace.setInt(2, namespaceID);
                success = ((DatabaseManager.getInstance().runUpdate(psRemoveNamespace)) >= 0); // ? true : false;
            } catch (SQLException e) {
                LOGGER.error("removeNamespace processing PreparedStatement " + psRemoveNamespace.toString(), e);
                }
        }
        return success;
    }

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
     * Updates the parameters wikiURL, pathToAPI, crawler_username, crawler_password in the data base. The namespaces to
     * be crawled are updated iff updateNamespaces is set to {@code true}.
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
            try {
                psUpdateWiki.setString(1, wd.getWikiURL());
                psUpdateWiki.setString(2, wd.getPathToAPI());
                if (wd.getLastCheckForModifications() != null) {
                    psUpdateWiki.setString(3, convertDateToSQLDateTime(wd.getLastCheckForModifications()));
                } else {
                    psUpdateWiki.setNull(3, java.sql.Types.DATE);
                }
                psUpdateWiki.setString(4, wd.getCrawlerUserName());
                psUpdateWiki.setString(5, wd.getCrawlerPassword());
                psUpdateWiki.setInt(6, wd.getWikiID());
                errorCount += ((DatabaseManager.getInstance().runUpdate(psUpdateWiki)) >= 0) ? 0 : 1;

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
                                    errorCount += (updateNamespace(wikiID, nameSpaceIDInDB, true)) ? 0 : 1;
                                    resetLastCheck = true;
                                }
                            } else {
                                errorCount += (updateNamespace(wikiID, nameSpaceIDInDB, false)) ? 0 : 1;
                                }
                            nameSpacesWD.remove(nameSpaceIDInDB);
                        }
                        for (int nameSpaceIDInFile : nameSpacesWD) {
                            errorCount += (addNamespace(wikiID, nameSpaceIDInFile, null, true)) ? 0 : 1;
                        }
                    } else {
                        for (int namespaceIDInDB : namespacesDB.keySet()) {
                            errorCount += (removeNamespace(wikiID, namespaceIDInDB)) ? 0 : 1;
                            }
                        }

                    if (resetLastCheck) {
                        wd.setLastCheckForModifications(null);
                        updateWiki(wd, false);
                                }
                            }

            } catch (SQLException e) {
                LOGGER.error("updateWiki processing PreparedStatement " + psUpdateWiki.toString(), e);
                errorCount++;
            }
            }
        return (errorCount == 0);
        }

    /**
     * Returns all namespaceIDs and the useForCrawling value for the given wikiID.
     * 
     * @param wikiID The wikiID to get all namespaces for.
     * @return All namespaceIDs and the useForCrawling value for the given wikiID.
     */
    public HashMap<Integer, Boolean> getAllNamespaces(final int wikiID) {
        HashMap<Integer, Boolean> namespaces = new HashMap<Integer, Boolean>();
            try {
            psAllNamespaces.setInt(1, wikiID);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psAllNamespaces);
            while (resultSet.next()) {
                namespaces.put(resultSet.getInt(1), resultSet.getBoolean(2));
            }
            } catch (SQLException e) {
            LOGGER.error("getAllNamespaces processing PreparedStatement " + psAllNamespaces.toString(), e);
        }
        return namespaces;
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
            try {
                psUpdateNamespace.setBoolean(1, useForCrawling);
                psUpdateNamespace.setInt(2, wikiID);
                psUpdateNamespace.setInt(3, namespaceID);
                success = ((DatabaseManager.getInstance().runUpdate(psUpdateNamespace)) >= 0) ? true : false;
            } catch (SQLException e) {
                LOGGER.error("updateNamespace processing PreparedStatement " + psUpdateNamespace.toString(), e);
            }
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
            try {
                psUpdateNamespaceName.setString(1, namespaceName);
                psUpdateNamespaceName.setInt(2, wikiID);
                psUpdateNamespaceName.setInt(3, namespaceID);
                success = ((DatabaseManager.getInstance().runUpdate(psUpdateNamespaceName)) >= 0) ? true : false;
            } catch (SQLException e) {
                LOGGER.error("updateNamespaceName processing PreparedStatement " + psUpdateNamespaceName.toString(), e);
                }
            }
        return success;
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
     * @return true if update was successful, false if any problem occurred while updating. If false, see error log for
     *         details.
     */
    public boolean updatePage(final int wikiID, final int pageID, final long revisionID, final String pageContent,
            final Date nextCheck) {
        boolean success = false;
        if (DEBUG && !pageExists(wikiID, pageID)) {
            LOGGER.debug("Could not update page with ID \"" + pageID
                    + "\" because it is not contained in the data base.");
        } else {
            try {
                psUpdatePage.setLong(1, revisionID);
                psUpdatePage.setString(2, pageContent);
                if (nextCheck != null) {
                    psUpdatePage.setString(3, convertDateToSQLDateTime(nextCheck));
                } else {
                    psUpdatePage.setNull(3, java.sql.Types.DATE);
                }
                psUpdatePage.setInt(4, wikiID);
                psUpdatePage.setInt(5, pageID);
                success = ((DatabaseManager.getInstance().runUpdate(psUpdatePage)) >= 0) ? true : false;
            } catch (SQLException e) {
                LOGGER.error("updatepage processing PreparedStatement " + psUpdatePage.toString(), e);
            }
        }
        return success;
    }

    /**
     * En/disable the auto commit functionality of the data base. If disabled, execution of {@link PreparedStatement}s
     * is scheduled till auto commit is enabled or a manual {@link #commit()} is called.
     * 
     * @param autoCommit auto commit value to set.
     */
    private void setAutoCommit(final boolean autoCommit) {
            try {
            DatabaseManager.getInstance().getConnection().setAutoCommit(autoCommit);
            } catch (SQLException e) {
            LOGGER.error("Could not set Connection.setAutoCommit(" + autoCommit + ") ", e);
            }
    }

    /**
     * Commit all scheduled {@link PreparedStatement}s.
     */
    @SuppressWarnings("unused")
    private void commit() {
            try {
            DatabaseManager.getInstance().getConnection().commit();
            } catch (SQLException e) {
            LOGGER.error("Could not commit() Connection ", e);
        }
    }

    /**
     * Update the time stamp to check this page the next time for new revisions.
     * 
     * @param wikiID The wikiID of the Wiki.
     * @param pageTitle The title of the page to update.
     * @param nextCheck The predicted date to check this page for new revisions.
     * @return true if update was successful, false if any problem occurred while updating. If false, see error log for
     *         details.
     */
    public boolean updatePage(final int wikiID, final String pageTitle, final Date nextCheck) {
        boolean success = false;
        if (DEBUG && !pageExists(wikiID, pageTitle)) {
            LOGGER.debug("Could not update page with ID \"" + pageTitle
                    + "\" because it is not contained in the data base.");
        } else {
            try {
                psUpdatePageNextCheck.setInt(2, wikiID);
                psUpdatePageNextCheck.setString(3, pageTitle);
                if (nextCheck != null) {
                    psUpdatePageNextCheck.setString(1, convertDateToSQLDateTime(nextCheck));
                } else {
                    psUpdatePageNextCheck.setNull(1, java.sql.Types.DATE);
                }
                success = ((DatabaseManager.getInstance().runUpdate(psUpdatePageNextCheck)) >= 0) ? true : false;
            } catch (SQLException e) {
                LOGGER.error("updatePageNextCheck processing PreparedStatement " + psUpdatePageNextCheck.toString(), e);
                }
        }
        return success;
    }

    }