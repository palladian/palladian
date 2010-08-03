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
    private PreparedStatement psUpdateRolePage;

    /** The ps get role pages. */
    private PreparedStatement psGetRolePages;

    /** The ps insert role page. */
    private PreparedStatement psInsertRolePage;

    /** The ps remove unrelevant role page. */
    private PreparedStatement psRemoveUnrelevantRolePage;

    /** The ps get role page usages. */
    private PreparedStatement psGetRolePageUsages;

    /** The ps insert role page usage. */
    private PreparedStatement psInsertRolePageUsage;

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
        Connection connection = DatabaseManager.getInstance().getConnection();
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
    public ArrayList<RolePage> loadNotUsedRolePagesForEntity(Entity entity) {
        ArrayList<RolePage> rolePages = new ArrayList<RolePage>();

        List<RolePage> allRolePagesForConcept = loadAllRolePagesForConcept(entity.getConcept());
        List<Integer> usedRolePageIDsForEntity = loadUsedRolePageIDsForEntity(entity);

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
    public ArrayList<Integer> loadUsedRolePageIDsForEntity(Entity entity) {

        ArrayList<Integer> rolePageIDs = new ArrayList<Integer>();
        ResultSet resultSet = null;

        try {
            psGetRolePageUsages.setInt(1, entity.getID());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        resultSet = DatabaseManager.getInstance().runQuery(psGetRolePageUsages);
        try {
            while (resultSet.next()) {
                int rolePageID = resultSet.getInt("rolepageID");
                rolePageIDs.add(rolePageID);
            }
        } catch (SQLException e) {

            e.printStackTrace();
        }

        return rolePageIDs;
    }

    /**
     * Load all RolePages which are associated with a specific concept.
     * 
     * @param concept the concept
     * @return the array list
     */
    public ArrayList<RolePage> loadAllRolePagesForConcept(Concept concept) {

        ArrayList<RolePage> dbRolePages = new ArrayList<RolePage>();

        ResultSet resultSet = null;
        try {
            psGetRolePages.setInt(1, concept.getID());
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        resultSet = DatabaseManager.getInstance().runQuery(psGetRolePages);
        try {
            while (resultSet.next()) {
                int rolePageID = resultSet.getInt("id");
                String url = resultSet.getString("url");
                int count = resultSet.getInt("count");
                // int conceptID = resultSet.getInt("conceptID");
                RolePage rolePage = new RolePage(url, count, concept.getID());
                rolePage.setID(rolePageID);

                dbRolePages.add(rolePage);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return dbRolePages;
    }

    /**
     * Adds the rolePage to database.
     * 
     * @param rolePage the role page
     */
    public void insertRolePage(RolePage rolePage) {

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
     * Insert role page usage.
     * 
     * @param rolePage the role page
     * @param entity the entity
     */
    public void insertRolePageUsage(RolePage rolePage, Entity entity) {
        try {
            psInsertRolePageUsage.setInt(1, rolePage.getID());
            psInsertRolePageUsage.setInt(2, entity.getID());

            DatabaseManager.getInstance().runUpdate(psInsertRolePageUsage);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Update a rolePage in database.
     * 
     * @param rolePage the role page
     */
    public void updateRolePage(RolePage rolePage) {

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
     * @param minCount the min count
     */
    public void removeUnrelevantRolePages(int minCount) {
        try {
            psRemoveUnrelevantRolePage.setInt(1, minCount);
            DatabaseManager.getInstance().runUpdate(psRemoveUnrelevantRolePage);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // /**
    // * The main method.
    // *
    // * @param args the arguments
    // */
    // public static void main(String[] args) {
    // RolePageDatabase rpd = new RolePageDatabase();
    //
    // // rpd.removeUnrelevantRolePages(2);
    // //
    // // System.exit(1);
    //
    // Concept concept = new Concept("Mobile Phone");
    // concept.setID(85);
    // Entity entity = new Entity("samsung s8500 wave", concept);
    // entity.setID(99);
    //
    // RolePage rp2 = new RolePage("http://www.phonearena.com", 22, concept.getID());
    // rp2.setID(80);
    // rpd.insertRolePageUsage(rp2, entity);
    //
    //
    //
    // List<RolePage> rolepages = rpd.loadNotUsedRolePagesForEntity(entity);
    //
    // for (RolePage rp : rolepages) {
    // System.out.println(rp.getHostname());
    // // if (rp.getHostname().contains("abc")){
    // // RolePage rp2 = new RolePage("http://www.phonearena.com",22, 0);
    // // rp2.setId(23);
    // // rpd.updateRolePage(rp2);
    // // }
    // }
    // System.out.println(rolepages.size());
    // // RolePage rp2 = new RolePage("www.abc.de",22, 85);
    // // rp2.setId(2);
    // // rpd.updateRolePage(rp2);
    // // rpd.insertRolePage(rp2);
    //
    // }

}
