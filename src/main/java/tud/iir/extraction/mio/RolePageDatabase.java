package tud.iir.extraction.mio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

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
        psGetRolePages = connection.prepareStatement("SELECT * FROM rolepages"); // `rolepages`
        psUpdateRolePage = connection.prepareStatement("UPDATE rolepages SET url = ?, count = ? WHERE id = ?");
        psInsertRolePage = connection.prepareStatement("INSERT INTO rolepages SET url = ?,count = ?");
    }

    /**
     * Load concepts.
     * 
     * @return the array list
     */
    public ArrayList<RolePage> loadRolePages() {

        ArrayList<RolePage> dbRolePages = new ArrayList<RolePage>();

        ResultSet resultSet = null;

        resultSet = DatabaseManager.getInstance().runQuery(psGetRolePages);
        try {
            while (resultSet.next()) {
                int rolePageID = resultSet.getInt("id");
                String url = resultSet.getString("url");
                int count = resultSet.getInt("count");
                RolePage rolePage = new RolePage(url, count);
                rolePage.setId(rolePageID);

                dbRolePages.add(rolePage);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return dbRolePages;
    }
    
   

    /**
     * Adds the role page.
     *
     * @param rolePage the role page
     */
    public void addRolePage(RolePage rolePage) {

        try {
            psInsertRolePage.setString(1, rolePage.getHostname());
            psInsertRolePage.setInt(2, rolePage.getCount());
         

            DatabaseManager.getInstance().runUpdate(psInsertRolePage);

        } catch (SQLException e) {
            LOGGER.error("insertRolePage", e);
        }

    }
    
    /**
     * Insert role page.
     *
     * @param rolePage the role page
     */
    public void insertRolePage(RolePage rolePage) {

        try {
            psUpdateRolePage.setString(1, rolePage.getHostname());
            psUpdateRolePage.setInt(2, rolePage.getCount());
            psUpdateRolePage.setInt(3, rolePage.getId());

            DatabaseManager.getInstance().runUpdate(psUpdateRolePage);

        } catch (SQLException e) {
            LOGGER.error("addRolePage", e);
        }

    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
