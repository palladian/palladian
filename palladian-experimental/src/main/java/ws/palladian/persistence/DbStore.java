package ws.palladian.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows one to save data into a database instead of keeping it in memory.
 * 
 * TODO inherit from {@link DatabaseManager}?
 * 
 * @author David Urbansky
 */
public class DbStore {

    protected static final Logger logger = LoggerFactory.getLogger(DbStore.class);

    // database parameters
    private Connection connection = null;
    private String dbType = "h2";// "mysql";
    private String dbDriver = "org.h2.Driver";// "com.mysql.jdbc.Driver";
    private String dbHost = "localhost";
    private String dbPort = "3306";
    private final String dbName = "dbstore";
    private String tableName = "";
    private String dbUsername = "root";
    private String dbPassword = "";

    // // prepared statements
    // private PreparedStatement psLastInsertID = null;

    // use one table to store information (faster but waste of space)
    private PreparedStatement psGetByKey = null;
    // private PreparedStatement psGetByValue = null;
    private PreparedStatement psKeyExists = null;
    private PreparedStatement psInsertTuple = null;
    private PreparedStatement psUpdateTuple = null;
    private PreparedStatement psRemoveTuple = null;
    private PreparedStatement psTruncate = null;

    // number of objects in the store
    private int size = 0;

    public DbStore(String tableName) {
        setTableName(tableName);
        connection = getConnection();
    }

    public DbStore(String tableName, String dbUsername, String dbPassword) {
        setTableName(tableName);
        setDbUsername(dbUsername);
        setDbPassword(dbPassword);
        connection = getConnection();
    }

    private Connection getConnection() {

        // String url = "jdbc:" + getDbType() + "://" + getDbHost() + ":" + getDbPort();
        // url += "?useServerPrepStmts=false&cachePrepStmts=false";

        String url = "jdbc:" + getDbType() + ":~/" + dbName;

        try {
            Class.forName(getDbDriver());
            connection = DriverManager.getConnection(url, getDbUsername(), getDbPassword());
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage());
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        try {

            // create database if it does not yet exist
            // PreparedStatement psCreateDB;
            PreparedStatement psCreateTable;
            // PreparedStatement psUseDB;

            // psCreateDB = connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + dbName);
            // runUpdate(psCreateDB);
            //
            // psUseDB = connection.prepareStatement("USE " + dbName);
            // runUpdate(psUseDB);

            // psCreateTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + getTableName() +
            // "` (`key` varchar(255) NOT NULL,`value` varchar(255) NOT NULL, PRIMARY KEY (`key`)) ENGINE=MyISAM DEFAULT CHARSET=latin1;");
            psCreateTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + getTableName()
                    + " (key varchar(255) NOT NULL PRIMARY KEY,value varchar(255) NOT NULL)");
            runUpdate(psCreateTable);

            // psLastInsertID = connection.prepareStatement("SELECT LAST_INSERT_ID()");
            psGetByKey = connection.prepareStatement("SELECT value FROM " + getTableName() + " WHERE key = ?");
            // psGetByValue = connection.prepareStatement("SELECT key FROM " + getTableName() + " WHERE value = ?");
            psKeyExists = connection.prepareStatement("SELECT key FROM " + getTableName() + " WHERE key = ?");
            psInsertTuple = connection.prepareStatement("INSERT INTO " + getTableName() + " VALUES(?,?)");
            psUpdateTuple = connection.prepareStatement("UPDATE " + getTableName() + " SET value = ? WHERE key = ?");
            psRemoveTuple = connection.prepareStatement("DELETE FROM " + getTableName() + " WHERE key = ?");
            psTruncate = connection.prepareStatement("TRUNCATE TABLE " + getTableName());

        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return connection;
    }

    private int runUpdate(PreparedStatement preparedStatement) {
        int result = 0;
        try {
            result = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return -1;
        }
        return result;
    }

    /*
     * private int runQueryID(PreparedStatement statement) { ResultSet rs = runQuery(statement); try { if (rs.next()) { return rs.getInt(1); } } catch
     * (SQLException e) { logger.error(e.getMessage()); } return -1; }
     */

    private ResultSet runQuery(PreparedStatement statement) {
        ResultSet rs = null;

        try {
            rs = statement.executeQuery();
        } catch (NumberFormatException e) {
            logger.error(e.getMessage() + ", " + statement.toString());
        } catch (SQLException e) {
            logger.error(e.getMessage() + ", " + statement.toString());
        } catch (NullPointerException e) {
            logger.error(e.getMessage() + ", " + statement.toString());
        }

        return rs;
    }

    /**
     * Empty the dbstore.
     */
    public void clear() {
        runUpdate(psTruncate);
        size = 0;
    }

    /**
     * Read the word from the unnormalized table with all information (faster).
     * 
     * @param word The word to look up.
     * @return The category entries for the word.
     */
    public Object get(String key) {
        return getByKey(key);
    }

    public Object getByKey(String key) {

        Object value = null;

        try {
            psGetByKey.setString(1, key);
            ResultSet rs = runQuery(psGetByKey);

            if (rs.next()) {

                return rs.getObject(1);

            }

        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return value;
    }

    public void put(String key, int value) {
        put(key, String.valueOf(value));
    }

    public void put(String key, double value) {
        put(key, String.valueOf(value));
    }

    public void put(String key, String value) {
        try {

            // check whether key exists already
            if (keyExists(key)) {
                // update
                psUpdateTuple.setString(1, value);
                psUpdateTuple.setString(2, key);
                runUpdate(psUpdateTuple);
            } else {
                // add
                psInsertTuple.setString(1, key);
                psInsertTuple.setString(2, value);
                runUpdate(psInsertTuple);
                size++;
            }

        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public void remove(String key) {
        try {

            // check whether key exists already
            if (keyExists(key)) {
                // remove
                psRemoveTuple.setString(1, key);
                runUpdate(psRemoveTuple);
                size--;
            }

        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    private boolean keyExists(String key) {
        try {
            psKeyExists.setString(1, key);
            ResultSet rs = runQuery(psKeyExists);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public int size() {
        return size;
    }

}
