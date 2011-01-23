package tud.iir.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.helper.ConfigHolder;

/**
 * The DatabaseManager writes and reads data to the database.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DatabaseManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    /** The configuration file can be found under config/palladian.properties. */
    private static PropertiesConfiguration config;

    private Connection connection;


    // TODO in entities domainID instead of conceptID (or no cascade or best new table that connects an entity with all
    // synonyms) because deleting a
    // concept (one synonym) might lead to deletion of all entities for that concept
    /**
     * Instantiates a new database manager.
     */
    protected DatabaseManager() {

        config = ConfigHolder.getInstance().getConfig();
        config.setThrowExceptionOnMissing(true);

    }

    static class SingletonHolder {
        static DatabaseManager instance = new DatabaseManager();
    }

    /**
     * Gets the single instance of DatabaseManager.
     * 
     * @return single instance of DatabaseManager
     */
    public static DatabaseManager getInstance() {
        try {
            if (SingletonHolder.instance.connection == null || SingletonHolder.instance.connection != null
                    && SingletonHolder.instance.connection.isClosed()) {
                SingletonHolder.instance.establishConnection();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        }

        return SingletonHolder.instance;
    }

    public void establishConnection(String driver, String type, String host, String port, String name,
            String username, String password) throws SQLException, ClassNotFoundException {

        Class.forName(driver);
        String url = "jdbc:" + type + "://" + host + ":" + port + "/" + name;
        url += "?useServerPrepStmts=false&cachePrepStmts=false";
        connection = DriverManager.getConnection(url, username, password);
    }

    /**
     * Load DB driver, establish DB connection, prepare statements.
     * 
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void establishConnection() throws SQLException, ClassNotFoundException {

        Class.forName(config.getString("db.driver"));
        String url = "jdbc:" + config.getString("db.type") + "://" + config.getString("db.host") + ":"
        + config.getString("db.port") + "/" + config.getString("db.name");
        url += "?useServerPrepStmts=false&cachePrepStmts=false";
        connection = DriverManager.getConnection(url, config.getString("db.username"), config.getString("db.password"));

    }

    /**
     * Return the connection.
     * 
     * @return
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Gets the last insert id.
     * 
     * @return the last insert id
     */
    public int getLastInsertID() {

        int lastInsertID = -1;

        try {
            PreparedStatement psLastInsertID = connection.prepareStatement("SELECT LAST_INSERT_ID()");
            ResultSet rs = runQuery(psLastInsertID);
            rs.next();
            lastInsertID = Integer.valueOf(rs.getString(1));
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return lastInsertID;
    }

    /**
     * Run query.
     * 
     * @param query the query
     * @return the result set
     */
    public ResultSet runQuery(String query) {
        return runQuery(query, "");
    }

    /**
     * Run query.
     * 
     * @param statement the statement
     * @return the result set
     */
    public ResultSet runQuery(PreparedStatement statement) {
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

    /**
     * Run query.
     * 
     * @param query the query
     * @param text the text
     * @return the result set
     */
    public ResultSet runQuery(String query, String text) {
        ResultSet rs = null;
        PreparedStatement ps = null;
        Statement stmt = null;
        try {
            if (text.length() > 0) {
                // ps = connection.prepareStatement(query,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
                // ps.setFetchSize(Integer.MIN_VALUE);
                ps = connection.prepareStatement(query);
                ps.setString(1, text);
                rs = ps.executeQuery();
            } else {
                // stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
                // stmt.setFetchSize(Integer.MIN_VALUE);
                stmt = connection.createStatement();
                rs = stmt.executeQuery(query);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }

    /**
     * Run query.
     * 
     * @param query the query
     * @param texts the texts
     * @return the result set
     */
    public ResultSet runQuery(String query, String[] texts) {
        ResultSet rs = null;

        try {
            if (texts.length > 0) {
                java.sql.PreparedStatement ps = connection.prepareStatement(query);
                for (int i = 1; i <= texts.length; i++) {
                    ps.setString(i, texts[i - 1]);
                }
                rs = ps.executeQuery();
            } else {
                Statement stmt = connection.createStatement();
                rs = stmt.executeQuery(query);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }

    /**
     * Execute a prepared statement.
     * 
     * @param preparedStatement The prepared statement.
     * @return (1) the row count for SQL Data Manipulation Language (DML) statements or (2) 0 for SQL statements that
     *         return nothing or (3) -1 if an {@link SQLException} has been caught and written to error log.
     */
    public int runUpdate(PreparedStatement preparedStatement) {
        int result = 0;
        try {
            result = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            return -1;
        }
        return result;
    }

    /**
     * Run update.
     * 
     * @param update the update
     * @return the int
     */
    public int runUpdate(String update) {
        return runUpdate(update, "");
    }

    /**
     * Run update.
     * 
     * @param update the update
     * @param text the text
     * @return the int
     */
    public int runUpdate(String update, String text) {
        int result = 0;
        try {
            // Statement stmt = connection.createStatement();
            if (text.length() > 0) {
                java.sql.PreparedStatement ps = connection.prepareStatement(update);
                ps.setString(1, text);
                result = ps.executeUpdate();
                ps.close();
            } else {
                Statement stmt = connection.createStatement();
                result = stmt.executeUpdate(update);
                stmt.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    /**
     * Run update.
     * 
     * @param update the update
     * @param texts the texts
     * @return the int
     */
    public int runUpdate(String update, String[] texts) {
        int result = 0;
        try {
            if (texts.length > 0) {
                java.sql.PreparedStatement ps = connection.prepareStatement(update);
                for (int i = 1; i <= texts.length; i++) {
                    ps.setString(i, texts[i - 1]);
                }
                result = ps.executeUpdate();
                ps.close();
            } else {
                Statement stmt = connection.createStatement();
                result = stmt.executeUpdate(update);
                stmt.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }


    /**
     * *************************************************************************************************
     * interaction
     * *************************************************************************************************.
     */



    /**
     * Test procedure.
     */
    public void testProcedure() {
        try {
            Statement stmt = connection.createStatement();
            // ResultSet rs = stmt.executeQuery("CALL source_voting_procedure");
            ResultSet rs = stmt.executeQuery("SELECT source_voting_function()");
            if (rs.next()) {
                System.out.println(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("finished");
    }

    /**
     * Sql script to grab the worst performing indexes in the whole server. Source:
     * http://forge.mysql.com/tools/tool.php?id=85
     * 
     */
    public void getWorstIndices() {
        runQuery("SELECT t.TABLE_SCHEMA AS `db`" + ", t.TABLE_NAME AS `table`" + ", s.INDEX_NAME AS `index name`"
                + ", s.COLUMN_NAME AS `field name`" + ", s.SEQ_IN_INDEX `seq in index`"
                + ", s2.max_columns AS `# cols`" + ", s.CARDINALITY AS `card`" + ", t.TABLE_ROWS AS `est rows`"
                + ", ROUND(((s.CARDINALITY / IFNULL(t.TABLE_ROWS, 0.01)) * 100), 2) AS `sel %`"
                + "FROM INFORMATION_SCHEMA.STATISTICS s" + "INNER JOIN INFORMATION_SCHEMA.TABLES t"
                + "ON s.TABLE_SCHEMA = t.TABLE_SCHEMA" + "AND s.TABLE_NAME = t.TABLE_NAME" + "INNER JOIN (" + "SELECT"
                + "TABLE_SCHEMA" + ", TABLE_NAME" + ", INDEX_NAME" + ", MAX(SEQ_IN_INDEX) AS max_columns"
                + "FROM INFORMATION_SCHEMA.STATISTICS" + "WHERE TABLE_SCHEMA != 'mysql'"
                + "GROUP BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME" + ") AS s2" + "ON s.TABLE_SCHEMA = s2.TABLE_SCHEMA"
                + "AND s.TABLE_NAME = s2.TABLE_NAME" + "AND s.INDEX_NAME = s2.INDEX_NAME"
                + "WHERE t.TABLE_SCHEMA != 'mysql'" + // filter out the mysql system
                // DB
                "AND t.TABLE_ROWS > 10" + // only tables with some rows
                "AND s.CARDINALITY IS NOT NULL" + // need at least one non-NULL value in the field
                "AND (s.CARDINALITY / IFNULL(t.TABLE_ROWS, 0.01)) < 1.00" + // selectivity < 1.0 b/c unique indexes are
                // perfect anyway
                "ORDER BY `sel %`, s.TABLE_SCHEMA, s.TABLE_NAME" + // switch to `sel %` DESC for best non-unique indexes
        "LIMIT 20;");
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        // DatabaseManager.getInstance().clearCompleteDatabase();
        //
        // KnowledgeManager km = new KnowledgeManager();
        // Concept c = new Concept("smartphone");
        // c.addSynonym("cell phone");
        // c.addSynonym("cellular phone");
        // c.addSynonym("mobile phone");
        // c.addSynonym("handphone");
        // c.addEntity(new Entity("Apple iPhone"));
        // km.addConcept(c);
        //
        // c = new Concept("car");
        // km.addConcept(c);
        //
        // km.saveExtractions();
        // DatabaseManager.getInstance().updateOntology(km);

        // ArrayList<Concept> concepts = DatabaseManager.getInstance().loadConcepts();
        // for (Concept concept : concepts) {
        // System.out.println(concept);
        // }

        // snippet testing //

        /*
         * Entity entity = new Entity("iPhone 3GS", new Concept("Mobile Phone")); entity.setID(1); WebResult webresult =
         * new
         * WebResult(SourceRetrieverManager.YAHOO, 45, "http://www.slashgear.com/iphone-3gs-reviews-2648062/"); Snippet
         * snippet = new Snippet( entity,
         * webresult,
         * "The iPhone 3GS striking physical similarity to the last-gen 3G came as a mild disappointment back at the WWDC, but we've come to appreciate the stability.  It underscores the evolutionary, rather than revolutionary, nature of this update and, perhaps more importantly, it means accessories acquired for the iPhone 3G will still have a place with the new 3GS."
         * ); SnippetFeatureExtractor.setFeatures(snippet); int snippetID =
         * DatabaseManager.getInstance().addSnippet(snippet); if (snippetID > 0) { try {
         * DatabaseManager.getInstance().psUpdateSnippet.setDouble(1, 3.0);
         * DatabaseManager.getInstance().psUpdateSnippet.setInt(2, snippetID);
         * DatabaseManager.getInstance().runUpdate(DatabaseManager.getInstance().psUpdateSnippet); } catch (SQLException
         * e) { e.getStackTrace(); } }
         */

        // Connection con = DatabaseManager.getInstance().getConnection();
        // System.out.println(DatabaseManager.getInstance().getTotalConceptsNumber());

        //
        // //System.out.println("URL: " + con.getCatalog());
        // System.out.println("Connection: " + con);
        //
        // Statement stmt = con.createStatement();
        //
        // //stmt.executeUpdate("CREATE TABLE t1 (id int,name varchar(255))");
        // //stmt.executeUpdate("INSERT INTO t1 SET name='abc'");
        // stmt.executeUpdate("INSERT INTO `values` SET value='abc'");
        // System.out.println(DatabaseManager.getInstance().getLastInsertID());
        // con.close();
        //
        // DatabaseManager.getInstance().clearCompleteDatabase();
        // domainManager.createBenchmarkConcepts();
        // KnowledgeManager domainManager = new KnowledgeManager();
        // domainManager.createBenchmarkConcepts();
        // DatabaseManager.getInstance().updateOntology();
        // DatabaseManager.getInstance().saveExtractions(domainManager);

        // DatabaseManager.getInstance().loadOntology();

        // try {
        // ArrayList<Entity> al = DatabaseManager.getInstance().loadEntities(new Concept("Country"),2, 2, false);
        // for (int i = 0; i < al.size(); i++) {
        // System.out.println(al.get(i).getName() + al.get(i).getLastSearched());
        // }
        // } catch (Exception e) {
        //
        // e.printStackTrace();
        // }

        // String s = "'a'`'sdfa ";
        // DatabaseManager.getInstance().runUpdate("INSERT INTO t1 set name = ?",s);

        // DatabaseManager.getInstance().runUpdate("INSERT INTO feeds SET url = ?","sdfcxv");

        // DatabaseManager.getInstance().clearCompleteDatabase();
        // DatabaseManager.getInstance().updateOntology();
        // KnowledgeManager dm = DatabaseManager.getInstance().loadOntology();
        // System.out.println("-----------------------------------");
        // ArrayList<Concept> concepts = dm.getConcepts();
        // for (int i = 0; i < concepts.size(); i++) {
        // System.out.println(concepts.get(i).getName()+" syns: "+concepts.get(i).getSynonymsToString());
        // HashSet<Attribute> attributes = concepts.get(i).getAttributes();
        // Iterator<Attribute> ai = attributes.iterator();
        // while (ai.hasNext()) {
        // Attribute a = ai.next();
        // System.out.println(" "+a.getName()+" "+a.getConcept());
        // }
        // }

        // Date lastSearched = new java.sql.Date(new java.util.Date(System.currentTimeMillis()).getTime());
        // System.out.println(lastSearched);
        // java.util.Date d = new java.util.Date(System.currentTimeMillis());
        // Timestamp ds = new java.sql.Timestamp(d.getTime());
        // System.out.println(d+"_"+ds);
        // DatabaseManager.getInstance().runUpdate("INSERT INTO `entities` SET name = 'te',trust = 0.1,conceptID = 4,lastSearched = '"
        // + ds + "'");
        // //
        //
        // Concept concept = new Concept("Actor");
        // String dateString = DatabaseManager.getInstance().getField("lastSearched","concepts","name = '" +
        // concept.getName() + "'");
        //
        // java.util.Date lastSearched = null;
        // if (dateString != null) lastSearched = new java.util.Date(Timestamp.valueOf(dateString).getTime());
        // concept.setLastSearched(lastSearched);
        // System.out.println(lastSearched+" "+concept.getLastSearched());

        // DatabaseManager.getInstance().cleanUnusedOntologyElements();

        // DatabaseManager.getInstance().clearCompleteDatabase();

        // System.out.println(DatabaseManager.getInstance().entryExists("Country", "concepts", "name"));
        // DatabaseManager.getInstance().addConcept("Country");

        // create sample of entities
        // DatabaseManager.getInstance().createEntityFile2();

        /*
         * long t1 = System.currentTimeMillis(); DatabaseManager.getInstance().booleanEntityTrustVoting(); long t2 =
         * System.currentTimeMillis();
         * System.out.println("stopped, runtime: "+((t2-t1)/1000)+" seconds");
         */

        /*
         * long t1 = System.currentTimeMillis(); DatabaseManager.getInstance().gradualEntityTrustVoting(); long t2 =
         * System.currentTimeMillis(); double hours =
         * (t2-t1)/(1000*60*60); System.out.println("stopped, runtime: "+((t2-t1)/1000)+" seconds ("+hours+"h)");
         */

        // DatabaseManager.getInstance().createEntityTrustChart();

        // DatabaseManager.getInstance().findEntityConnection(12,1,0,new HashSet<Integer>(),new ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(6,13,0,new HashSet<Integer>(),new ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(14,2,0,new HashSet<Integer>(),new ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(12,11,0,new HashSet<Integer>(),new ArrayList<String>());

        // DatabaseManager.getInstance().findEntityConnection(154180,151864,0,new HashSet<Integer>(),new
        // ArrayList<String>());
        // DatabaseManager.getInstance().findEntityConnection(153977,151912,0,new HashSet<Integer>(),new
        // ArrayList<String>());
        // DatabaseManager.getInstance().gradualEntityTrustVoting();

        // int r =
        // DatabaseManager.getInstance().runUpdate("UPDATE sources SET entityTrust = 0.5 WHERE id = 1 AND entityTrust != 0.5");
        // System.out.println(r);

        // long t1 = System.currentTimeMillis();
        //
        // // insert performance test
        // DatabaseManager dbm = DatabaseManager.getInstance();
        // dbm.connection.setAutoCommit(false);
        //
        // java.sql.PreparedStatement ps = null;
        // java.sql.PreparedStatement ps2 = null;
        // java.sql.PreparedStatement ps3 = null;
        // try {
        // ps = dbm.connection.prepareStatement("SELECT id FROM `t1` WHERE name = ? AND foreignKeyField = ?");
        // ps2 = dbm.connection.prepareStatement("INSERT INTO t1 SET name = ?, foreignKeyField = ?");
        // ps3 = dbm.connection.prepareStatement("UPDATE t1 SET name = ?, foreignKeyField = ?");
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        //
        // for (int i = 0; i < 3000; i++) {
        //
        // int randomNumber = (int) Math.floor((Math.random()*10000));
        //
        // //java.sql.PreparedStatement ps = null;
        // int entryID = -1;
        // try {
        // // ps = dbm.connection.prepareStatement("SELECT id FROM `t1` WHERE name = ? AND foreignKeyField = " +
        // randomNumber);
        // ps.setString(1, "vbydferwer"+randomNumber);
        // ps.setInt(2, randomNumber);
        //
        // // entities can belong to different concepts, therefore check name and concept
        // //entryID = dbm.entryExists(ps);
        //
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        //
        // // insert or update
        // if (entryID == -1) {
        // ps2.setString(1, "asdfvcxyv"+randomNumber);
        // ps2.setInt(2, randomNumber);
        // dbm.runUpdate(ps2);
        // //System.out.println(dbm.getLastInsertID());
        // //ps2.addBatch();
        // // dbm.runUpdate("INSERT INTO t1 SET name = '"+"asdfxc"+randomNumber+"', foreignKeyField = " + randomNumber);
        // } else {
        // ps3.setString(1, "hcvhxa"+randomNumber);
        // ps3.setInt(2, randomNumber);
        // dbm.runUpdate(ps3);
        // //ps3.addBatch();
        // // dbm.runUpdate("UPDATE t1 SET name = '"+"asdfxc"+randomNumber+"', foreignKeyField = " + randomNumber);
        // }
        // }
        //
        // //ps2.executeBatch();
        // //ps3.executeBatch();
        // dbm.connection.commit();
        // long t2 = System.currentTimeMillis();
        //
        // System.out.println(t2-t1);
        //
        // // 44 839 with autocommit=yes
        // // 1 877 with autocommit=no
        //
        // // 3000: 3601,2948 with batch
        // // 3000: 6304,5414 without batch
        // // 3000: 125125 without autocommit
        //
        // long t3 = System.currentTimeMillis();
        // DatabaseManager dbm2 = DatabaseManager.getInstance();
        //
        // PreparedStatement ps5 =
        // dbm.connection.prepareStatement("SELECT id FROM `t1` WHERE name = ? AND foreignKeyField > ?");
        //
        // for (int i = 0; i < 100; i++) {
        // int randomNumber = (int) Math.floor((Math.random()*10000));
        // ps5.setString(1, "reztstrz"+randomNumber);
        // ps5.setInt(2, (int)(randomNumber / 10));
        // dbm2.runQuery(ps5);
        // }
        //
        // long t4 = System.currentTimeMillis();
        //
        // System.out.println(t4-t3);
    }
}