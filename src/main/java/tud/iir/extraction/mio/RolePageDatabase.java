/**
 * This class realizes the loading and saving of RolePages.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.persistence.DatabaseManager;

public class RolePageDatabase {

    /** the logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(RolePageDatabase.class);

    /** The ps update role page. */
    private transient PreparedStatement psUpdateRolePage;

    /** The ps get role pages. */
    private transient PreparedStatement psGetRolePages;

    /** The ps insert role page. */
    private transient PreparedStatement psInsertRolePage;

    /** The ps remove unrelevant role page. */
    private transient PreparedStatement psRemoveUnrelevantRolePage;

    /** The ps get role page usages. */
    private transient PreparedStatement psGetRolePageUsages;

    /** The ps insert role page usage. */
    private transient PreparedStatement psInsertRolePageUsage;

    /**
     * Instantiates a new RolePageDatabase.
     */
    RolePageDatabase() {
        try {
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
        }
    }

    /**
     * Prepare statements.
     * 
     * @throws SQLException the sQL exception
     */
    private void prepareStatements() throws SQLException {
        // // prepared statements for feeds
        final Connection connection = DatabaseManager.getInstance().getConnection();
        psGetRolePages = connection.prepareStatement("SELECT * FROM rolepages WHERE conceptID = ?"); // `rolepages`
        psUpdateRolePage = connection.prepareStatement("UPDATE rolepages SET url = ?, count = ? WHERE id = ?");
        psInsertRolePage = connection.prepareStatement("INSERT INTO rolepages SET url = ?,count = ?, conceptID = ?");
        psRemoveUnrelevantRolePage = connection.prepareStatement("DELETE FROM rolepages WHERE count < ?");
        psGetRolePageUsages = connection.prepareStatement("SELECT * FROM rolepage_usages WHERE entityID=?");
        psInsertRolePageUsage = connection
                .prepareStatement("INSERT INTO rolepage_usages SET rolepageID = ?, entityID = ?");
    }

    /**
     * Load all rolePages, that were not already used for the specific entity.
     * 
     * @param entity the entity
     * @return the array list
     */
    public List<RolePage> loadNotUsedRolePagesForEntity(final Entity entity) {
        final ArrayList<RolePage> rolePages = new ArrayList<RolePage>();

        final List<RolePage> allRolePagesForConcept = loadAllRolePagesForConcept(entity.getConcept());
        final List<Integer> usedRolePageIDsForEntity = loadUsedRolePageIDsForEntity(entity);

        for (RolePage rolePage : allRolePagesForConcept) {

            if (!usedRolePageIDsForEntity.contains(rolePage.getID())) {
                rolePages.add(rolePage);
            }
        }

        return rolePages;
    }

    /**
     * Load all IDs of rolePages that where already used for a specific entity.
     * 
     * @param entity the entity
     * @return the array list
     */
    public List<Integer> loadUsedRolePageIDsForEntity(final Entity entity) {

        final ArrayList<Integer> rolePageIDs = new ArrayList<Integer>();
        ResultSet resultSet = null;

        try {
            psGetRolePageUsages.setInt(1, entity.getID());
            resultSet = DatabaseManager.getInstance().runQuery(psGetRolePageUsages);
            while (resultSet.next()) {
                final int rolePageID = resultSet.getInt("rolepageID");
                rolePageIDs.add(rolePageID);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return rolePageIDs;
    }

    /**
     * Load all RolePages which are associated with a specific concept.
     * 
     * @param concept the concept
     * @return the array list
     */
    public List<RolePage> loadAllRolePagesForConcept(final Concept concept) {

        final ArrayList<RolePage> dbRolePages = new ArrayList<RolePage>();

        ResultSet resultSet = null;
        try {
            psGetRolePages.setInt(1, concept.getID());
            resultSet = DatabaseManager.getInstance().runQuery(psGetRolePages);
            while (resultSet.next()) {
                final int rolePageID = resultSet.getInt("id");
                final String url = resultSet.getString("url");
                final int count = resultSet.getInt("count");
                // int conceptID = resultSet.getInt("conceptID");
                final RolePage rolePage = new RolePage(url, count, concept.getID());
                rolePage.setID(rolePageID);

                dbRolePages.add(rolePage);
            }

        } catch (SQLException e1) {
            LOGGER.error(e1.getMessage());
        }

        return dbRolePages;
    }

    /**
     * Adds the rolePage to database.
     * 
     * @param rolePage the rolePage
     */
    public void insertRolePage(final RolePage rolePage) {

        try {
            psInsertRolePage.setString(1, rolePage.getHostname());
            psInsertRolePage.setInt(2, rolePage.getCount());
            psInsertRolePage.setInt(3, rolePage.getConceptID());

            DatabaseManager.getInstance().runUpdate(psInsertRolePage);

        } catch (SQLException e) {
            LOGGER.error("insertRolePage", e);
        }

    }

    /**
     * Insert rolePage usage.
     * 
     * @param rolePage the rolePage
     * @param entity the entity
     */
    public void insertRolePageUsage(final RolePage rolePage, final Entity entity) {
        try {
            psInsertRolePageUsage.setInt(1, rolePage.getID());
            psInsertRolePageUsage.setInt(2, entity.getID());

            DatabaseManager.getInstance().runUpdate(psInsertRolePageUsage);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * Update a rolePage in database.
     * 
     * @param rolePage the rolePage
     */
    public void updateRolePage(final RolePage rolePage) {

        try {
            psUpdateRolePage.setString(1, rolePage.getHostname());
            psUpdateRolePage.setInt(2, rolePage.getCount());
            psUpdateRolePage.setInt(3, rolePage.getID());
            DatabaseManager.getInstance().runUpdate(psUpdateRolePage);
        } catch (SQLException e) {
            LOGGER.error("addRolePage", e);
        }

    }

    /**
     * Remove all rolePages from database that don't fit a concrete minimalCount.
     * 
     * @param minCount the minimalCount
     */
    public void removeUnrelevantRolePages(final int minCount) {
        try {
            psRemoveUnrelevantRolePage.setInt(1, minCount);
            DatabaseManager.getInstance().runUpdate(psRemoveUnrelevantRolePage);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

    }
}
