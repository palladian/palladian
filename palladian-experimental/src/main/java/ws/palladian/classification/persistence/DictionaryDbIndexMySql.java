package ws.palladian.classification.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;

public class DictionaryDbIndexMySql extends DictionaryIndex {

    // database paramenters
    private Connection connection = null;
    private String dbType = "mysql";
    private String dbDriver = "com.mysql.jdbc.Driver";
    private String dbHost = "localhost";
    private String dbPort = "3306";
    private String dbName = "";
    private String dbUsername = "root";
    private String dbPassword = "";

    // // prepared statements

    // use three tables, slower but more space efficient
    private PreparedStatement psRead = null;
    private PreparedStatement psLastInsertID = null;
    private PreparedStatement psGetWordID = null;
    private PreparedStatement psGetCategoryID = null;
    private PreparedStatement psInsertWord = null;
    private PreparedStatement psInsertCategory = null;
    private PreparedStatement psInsertTuple = null;
    private PreparedStatement psUpdateTuple = null;
    private PreparedStatement psGetWordCategoryTuple = null;

    private PreparedStatement psTruncateDictionary = null;
    private PreparedStatement psTruncateTuples = null;
    private PreparedStatement psTruncateCategories = null;
    private PreparedStatement psTruncateTuple2 = null;

    // use one table to store information (faster but waste of space)
    private PreparedStatement psReadTuple2 = null;
    private PreparedStatement psInsertTuple2 = null;
    private PreparedStatement psUpdateTuple2 = null;
    private PreparedStatement psGetWordCategoryTuple2 = null;

    /** if fastmode = true, only one table will be used to store all information */
    private boolean fastMode = true;

    public DictionaryDbIndexMySql(String dbName, String dbUsername, String dbPassword, String indexPath) {
        setDbName(dbName);
        setDbUsername(dbUsername);
        setDbPassword(dbPassword);
        setIndexPath(indexPath);
        connection = getConnection();
    }

    public DictionaryDbIndexMySql(String dbName, String dbUsername, String dbPassword) {
        setDbName(dbName);
        setDbUsername(dbUsername);
        setDbPassword(dbPassword);
        connection = getConnection();
    }

    private Connection getConnection() {

        String url = "jdbc:" + getDbType() + ":";

        if (getDbType().equals("mysql")) {
            url += "//" + getDbHost() + ":" + getDbPort() + "/";
        }

        if (!getDbType().equals("mysql")) {
            url += getDbName();
        }

        if (getDbType().equals("mysql")) {
            url += "?create=true;useServerPrepStmts=false&cachePrepStmts=false";
        }

        try {
            Class.forName(getDbDriver());
            connection = DriverManager.getConnection(url, getDbUsername(), getDbPassword());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        try {

            // create database if it does not yet exist
            PreparedStatement psCreateDB;
            PreparedStatement psCreateTable1;
            PreparedStatement psCreateTable2;
            PreparedStatement psCreateTable3;
            PreparedStatement psCreateTable4;
            PreparedStatement psUseDB;

            psCreateDB = connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + getDbName());
            runUpdate(psCreateDB);

            psUseDB = connection.prepareStatement("USE " + getDbName());
            runUpdate(psUseDB);
            // connection = DriverManager.getConnection(url, getDbUsername(), getDbPassword());

            psCreateTable1 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS `dictionary_index` (`word` varchar(25) NOT NULL,`category` varchar(25) NOT NULL,`relevance` double NOT NULL, PRIMARY KEY (`word`,`category`)) ENGINE=MYISAM CHARACTER SET utf8 COLLATE utf8_general_ci;");
            runUpdate(psCreateTable1);

            psCreateTable2 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS `categories` (`id` int(10) unsigned NOT NULL auto_increment,`name` varchar(50) NOT NULL, PRIMARY KEY  (`id`), KEY `name` (`name`)) ENGINE=MyISAM CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT=1;");
            runUpdate(psCreateTable2);

            psCreateTable3 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS `dictionary` (`id` bigint(20) unsigned NOT NULL auto_increment,`word` varchar(50) NOT NULL,PRIMARY KEY(`id`),  KEY `word` (`word`)) ENGINE=MyISAM CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT=1;");
            runUpdate(psCreateTable3);

            psCreateTable4 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS `tuples` (`wordID` bigint(20) unsigned NOT NULL,`categoryID` int(10) unsigned NOT NULL,`relevance` double NOT NULL, PRIMARY KEY (`wordID`,`categoryID`)) ENGINE=MyISAM CHARACTER SET utf8 COLLATE utf8_general_ci;");
            runUpdate(psCreateTable4);

            psRead = connection
                    .prepareStatement("SELECT categories.name,tuples.relevance FROM dictionary,tuples,categories WHERE dictionary.word = ? AND dictionary.id = tuples.wordID AND categories.id = tuples.categoryID");
            psLastInsertID = connection.prepareStatement("SELECT LAST_INSERT_ID()");
            psGetWordID = connection.prepareStatement("SELECT id FROM dictionary WHERE word = ?");
            psGetCategoryID = connection.prepareStatement("SELECT id FROM categories WHERE name = ?");
            psInsertWord = connection.prepareStatement("INSERT INTO dictionary SET word = ?");
            psInsertCategory = connection.prepareStatement("INSERT INTO categories SET name = ?");
            psInsertTuple = connection.prepareStatement("INSERT INTO tuples SET wordID = ?, categoryID = ?, relevance = ?");

            psUpdateTuple = connection.prepareStatement("UPDATE tuples SET relevance = ? WHERE wordID = ? AND categoryID = ?");
            psGetWordCategoryTuple = connection.prepareStatement("SELECT wordID FROM tuples WHERE wordID = ? AND categoryID = ?");

            psReadTuple2 = connection.prepareStatement("SELECT category,relevance FROM dictionary_index WHERE word = ?");
            psInsertTuple2 = connection.prepareStatement("INSERT INTO dictionary_index SET word = ?, category = ?, relevance = ?");
            psUpdateTuple2 = connection.prepareStatement("UPDATE dictionary_index SET relevance = ? WHERE word = ? AND category = ?");
            psGetWordCategoryTuple2 = connection.prepareStatement("SELECT word FROM dictionary_index WHERE word = ? AND category = ?");

            psTruncateDictionary = connection.prepareStatement("TRUNCATE TABLE dictionary");
            psTruncateTuples = connection.prepareStatement("TRUNCATE TABLE tuples");
            psTruncateCategories = connection.prepareStatement("TRUNCATE TABLE categories");
            psTruncateTuple2 = connection.prepareStatement("TRUNCATE TABLE dictionary_index");

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return connection;
    }

    private int runUpdate(PreparedStatement preparedStatement) {
        int result = 0;
        try {
            result = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage() + ", " + preparedStatement.toString());
            e.printStackTrace();
            System.exit(1);
            return -1;
        }
        return result;
    }

    private int runQueryID(PreparedStatement statement) {
        ResultSet rs = runQuery(statement);

        try {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return -1;
    }

    private ResultSet runQuery(PreparedStatement statement) {
        ResultSet rs = null;

        try {
            rs = statement.executeQuery();
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }

    @Override
    public void empty() {
        runUpdate(psTruncateDictionary);
        runUpdate(psTruncateTuples);
        runUpdate(psTruncateCategories);
        runUpdate(psTruncateTuple2);
        LOGGER.info("deleted the complete index (truncated all tables)");
    }

    @Override
    /**
     * Read from the database.
     * @param word The word in the dictionary.
     */
    public CategoryEntries read(String word) {
        if (fastMode) {
            return read1(word);
        }
        return read3(word);
    }

    /**
     * Read the word from the unnormalized table with all information (faster).
     * 
     * @param word The word to look up.
     * @return The category entries for the word.
     */
    public CategoryEntries read1(String word) {

        CategoryEntries categoryEntries = new CategoryEntries();

        try {
            psReadTuple2.setString(1, word);
            ResultSet rs = runQuery(psReadTuple2);

            while (rs.next()) {

                String categoryName = rs.getString("category");
                Category category = dictionary.getCategories().getCategoryByName(categoryName);

                if (category == null) {
                    category = new Category(categoryName);
                    // category.setIndexedPrior(rs.getDouble("categoryPrior"));
                    dictionary.getCategories().add(category);
                }

                if (categoryEntries.getCategoryEntry(categoryName) == null) {
                    CategoryEntry ce = new CategoryEntry(categoryEntries, category, rs.getDouble("relevance"));

                    LOGGER.debug("add " + ce);

                    categoryEntries.add(ce);
                }

            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return categoryEntries;
    }

    /**
     * Read the word from the 3 normalized tables (more space efficient).
     * 
     * @param word The word to look up.
     * @return The category entries for the word.
     */
    public CategoryEntries read3(String word) {

        CategoryEntries categoryEntries = new CategoryEntries();

        try {
            psRead.setString(1, word);
            ResultSet rs = runQuery(psRead);

            while (rs.next()) {

                String categoryName = rs.getString("categories.name");
                Category category = dictionary.getCategories().getCategoryByName(categoryName);

                if (category == null) {
                    category = new Category(categoryName);
                    // category.setIndexedPrior(rs.getDouble("categories.prior"));
                    dictionary.getCategories().add(category);
                }

                if (categoryEntries.getCategoryEntry(categoryName) == null) {
                    CategoryEntry ce = new CategoryEntry(categoryEntries, category, rs.getDouble("tuples.relevance"));

                    LOGGER.debug("add " + ce);

                    categoryEntries.add(ce);
                }

            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return categoryEntries;
    }

    @Override
    public void update(String word, CategoryEntries categoryEntries) {
        if (fastMode) {
            update1(word, categoryEntries);
        } else {
            update3(word, categoryEntries);
        }
    }

    @Override
    public void update(String word, CategoryEntry categoryEntry) {
        if (fastMode) {
            update1(word, categoryEntry);
        } else {
            update3(word, categoryEntry);
        }
    }

    /**
     * Update category information for a given word in 1 table with all information (faster).
     * 
     * @param word The word to update.
     * @param categoryEntries The category entries for the word.
     */
    private void update1(String word, CategoryEntries categoryEntries) {

        word = word.toLowerCase();

        for (CategoryEntry categoryEntry : categoryEntries) {

            update1(word, categoryEntry);

        }

    }

    private void update1(String word, CategoryEntry categoryEntry) {
        try {
            String categoryName = categoryEntry.getCategory().getName();

            // check whether there is an entry for the word and the category already
            psGetWordCategoryTuple2.setString(1, word);
            psGetWordCategoryTuple2.setString(2, categoryName);
            ResultSet entries = runQuery(psGetWordCategoryTuple2);
            if (!entries.next()) {
                write1(word, categoryEntry);
            } else {

                psUpdateTuple2.setDouble(1, categoryEntry.getAbsoluteRelevance());
                psUpdateTuple2.setString(2, word);
                psUpdateTuple2.setString(3, categoryName);
                runUpdate(psUpdateTuple2);

            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Update category information for a given word in 3 tables (more space efficient).
     * 
     * @param word The word to update.
     * @param categoryEntries The category entries for the word.
     */
    private void update3(String word, CategoryEntries categoryEntries) {

        word = word.toLowerCase();

        for (CategoryEntry categoryEntry : categoryEntries) {
            update3(word, categoryEntry);
        }

    }

    private void update3(String word, CategoryEntry categoryEntry) {

        word = word.toLowerCase();

        try {

            psGetWordID.setString(1, word);
            int wordID = runQueryID(psGetWordID);
            psGetCategoryID.setString(1, categoryEntry.getCategory().getName());
            int categoryID = runQueryID(psGetCategoryID);

            if (wordID == -1 || categoryID == -1) {

                write3(word, categoryEntry);

            } else {

                psGetWordCategoryTuple.setInt(1, wordID);
                psGetWordCategoryTuple.setInt(2, categoryID);
                ResultSet entry = runQuery(psGetWordCategoryTuple);
                if (!entry.next()) {

                    write3(word, categoryEntry);

                } else {

                    psUpdateTuple.setDouble(1, categoryEntry.getAbsoluteRelevance());
                    psUpdateTuple.setInt(2, wordID);
                    psUpdateTuple.setInt(3, categoryID);
                    runUpdate(psUpdateTuple);

                }
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void write(String word, CategoryEntries categoryEntries) {
        if (fastMode) {
            write1(word, categoryEntries);
        } else {
            write3(word, categoryEntries);
        }
    }

    @Override
    public void write(String word, CategoryEntry categoryEntry) {
        if (fastMode) {
            write1(word, categoryEntry);
        } else {
            write3(word, categoryEntry);
        }
    }

    /**
     * Write a word with its category entries into the dictionary (1 table, faster).
     * 
     * @param word The word to write.
     * @param categoryEntries The category entries for the word.
     */
    private void write1(String word, CategoryEntries categoryEntries) {

        word = word.toLowerCase();

        for (CategoryEntry categoryEntry : categoryEntries) {
            write1(word, categoryEntry);
        }

    }

    private void write1(String word, CategoryEntry categoryEntry) {
        try {

            // add tuple
            psInsertTuple2.setString(1, word);
            psInsertTuple2.setString(2, categoryEntry.getCategory().getName());
            // psInsertTuple2.setDouble(3, categoryEntry.getCategory().getPrior());
            psInsertTuple2.setDouble(3, categoryEntry.getAbsoluteRelevance());
            runUpdate(psInsertTuple2);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Write a word with its category entries into the dictionary (3 tables, more space efficient).
     * 
     * @param word The word to write.
     * @param categoryEntries The category entries for the word.
     */
    public void write3(String word, CategoryEntries categoryEntries) {

        word = word.toLowerCase();

        for (CategoryEntry categoryEntry : categoryEntries) {
            write3(word, categoryEntry);
        }

    }

    private void write3(String word, CategoryEntry categoryEntry) {

        try {
            psGetWordID.setString(1, word);
            int wordID = runQueryID(psGetWordID);

            // if word does not exist, add it
            if (wordID == -1) {
                psInsertWord.setString(1, word);
                runUpdate(psInsertWord);
                wordID = runQueryID(psLastInsertID);
            }

            psGetCategoryID.setString(1, categoryEntry.getCategory().getName());
            int categoryID = runQueryID(psGetCategoryID);

            // if category does not exist, add it
            if (categoryID == -1) {
                psInsertCategory.setString(1, categoryEntry.getCategory().getName());
                // psInsertCategory.setDouble(2, categoryEntry.getCategory().getPrior());
                runUpdate(psInsertCategory);
                categoryID = runQueryID(psLastInsertID);
            }

            // add tuple
            psInsertTuple.setInt(1, wordID);
            psInsertTuple.setInt(2, categoryID);
            psInsertTuple.setDouble(3, categoryEntry.getAbsoluteRelevance());
            runUpdate(psInsertTuple);

        } catch (SQLException e) {
            LOGGER.error("word: " + word + ", " + e.getMessage());
        }

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

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
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

    public boolean isFastMode() {
        return fastMode;
    }

    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean openReader() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void openWriter() {
        // TODO Auto-generated method stub
    }

}