package tud.iir.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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

    /**
     * Get a {@link Connection} from the {@link ConnectionManager}.
     * 
     * @return
     * @throws SQLException
     */
    protected final Connection getConnection() throws SQLException {
        return ConnectionManager.getInstance().getConnection();
    }

    /**
     * Run a query operation on the database.
     * 
     * @param <T>
     * @param callback
     * @param converter
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final <T> int runQuery(ResultCallback<T> callback, RowConverter<T> converter, String sql, Object... args) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int counter = 0;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            rs = ps.executeQuery();

            while (rs.next() && callback.isLooping()) {
                T item = converter.convert(rs);
                callback.processResult(item, ++counter);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(connection, ps, rs);
        }

        return counter;

    }

    /**
     * 
     * @param callback
     * @param sql
     * @param args
     * @return
     */
    public final int runQuery(ResultCallback<Map<String, Object>> callback, String sql, Object... args) {
        return runQuery(callback, new SimpleRowConverter(), sql, args);
    }

    /**
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, String sql, Object... args) {

        final List<T> result = new ArrayList<T>();

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result.add(object);
            }

        };

        runQuery(callback, converter, sql, args);

        return result;
    }

    /**
     * Allows to iterate over available items without buffering the content of the whole result in memory first, using a
     * standard Iterator interface. The underlying Iterator implementation does not allow modifications, so a call to
     * {@link Iterator#remove()} will cause an {@link UnsupportedOperationException}. The ResultSet which is used by the
     * implementation is closed, after the last element has been retrieved, if you break the iteration loop, you must
     * manually call {@link ResultIterator#close()}}.
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, Object... args) {

        ResultIterator<T> result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);

            // do not buffer the whole ResultSet in memory, but use streaming to save memory
            // http://webmoli.com/2009/02/01/jdbc-performance-tuning-with-optimal-fetch-size/
            // TODO make this a global option?
            ps.setFetchSize(Integer.MIN_VALUE);

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            resultSet = ps.executeQuery();
            result = new ResultIterator<T>(connection, ps, resultSet, converter);

        } catch (SQLException e) {
            LOGGER.error(e);
            close(connection, ps, resultSet);
        }

        return result;

    }

    /**
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, List<Object> args) {
        return runQueryWithIterator(converter, sql, args.toArray());
    }

    /**
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    @SuppressWarnings("unchecked")
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, Object... args) {

        final Object[] result = new Object[1];

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result[0] = object;
                breakLoop();
            }
        };

        runQuery(callback, converter, sql, args);

        return (T) result[0];

    }

    /**
     * 
     * @param sql
     * @param args
     * @return
     */
    public final Map<String, Object> runSingleQuery(String sql, Object... args) {
        return runSingleQuery(new SimpleRowConverter(), sql, args);
    }

    /**
     * Run an update operation and return the number of affected rows.
     * 
     * @param updateStatement Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(String updateStatement, Object... args) {

        int affectedRows;
        Connection connection = null;
        PreparedStatement ps = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(updateStatement);

            // do we need a special treatment for NULL values here?
            // if you should stumble across this comment while debugging,
            // the answer is likely: yes, we do!
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            affectedRows = ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            affectedRows = -1;
        } finally {
            close(connection, ps);
        }

        return affectedRows;

    }
    
    /**
     * 
     * @param updateStatement
     * @param args
     * @return
     */
    public final int runUpdate(String updateStatement, List<Object> args) {
        return runUpdate(updateStatement, args.toArray());
    }

    /**
     * Run an update operation and return the insert ID.
     * 
     * @param updateStatement Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The generated ID, or 0 if no row was inserted, or -1 if an error occurred.
     */
    public final int runUpdateReturnId(String updateStatement, Object... args) {

        int generatedId;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(updateStatement, Statement.RETURN_GENERATED_KEYS);

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                generatedId = 0;
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            generatedId = -1;
        } finally {
            close(connection, ps, rs);
        }

        return generatedId;

    }

    /**
     * 
     * @param updateStatement
     * @param args
     * @return
     */
    public final int runUpdateReturnId(String updateStatement, List<Object> args) {
        return runUpdateReturnId(updateStatement, args.toArray());
    }

    /**
     * 
     * @param connection
     */
    public static final void close(Connection connection) {
        close(connection, null, null);
    }

    /**
     * Convenient helper method to close database resources.
     * 
     * @param connection
     * @param statement
     */
    public static final void close(Connection connection, Statement statement) {
        close(connection, statement, null);
    }

    public static final void close(Connection connection, ResultSet resultSet) {
        close(connection, null, resultSet);
    }

    /**
     * Convenient helper method to close database resources.
     * 
     * @param connection
     * @param statement
     * @param resultSet
     */
    public static final void close(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error("error closing ResultSet : " + e.getMessage());
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.error("error closing Statement : " + e.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("error closing Connection : " + e.getMessage());
            }
        }
    }

    /**
     * *************************************************************************************************
     * interaction
     * *************************************************************************************************.
     */

    /**
     * Test procedure.
     */
    //    public void testProcedure() {
    //        Connection connection = null;
    //        Statement stmt = null;
    //        ResultSet rs = null;
    //        try {
    //            connection = getConnection();
    //            stmt = connection.createStatement();
    //            rs = stmt.executeQuery("SELECT source_voting_function()");
    //            if (rs.next()) {
    //                LOGGER.info(rs.getInt(1));
    //            }
    //        } catch (SQLException e) {
    //            LOGGER.error(e);
    //        } finally {
    //            close(connection, stmt, rs);
    //        }
    //        LOGGER.info("finished");
    //    }

    /**
     * Sql script to grab the worst performing indexes in the whole server. Source:
     * http://forge.mysql.com/tools/tool.php?id=85
     * 
     */
    //    public void getWorstIndices() {
    //        runQuery("SELECT t.TABLE_SCHEMA AS `db`" + ", t.TABLE_NAME AS `table`" + ", s.INDEX_NAME AS `index name`"
    //                + ", s.COLUMN_NAME AS `field name`" + ", s.SEQ_IN_INDEX `seq in index`"
    //                + ", s2.max_columns AS `# cols`" + ", s.CARDINALITY AS `card`" + ", t.TABLE_ROWS AS `est rows`"
    //                + ", ROUND(((s.CARDINALITY / IFNULL(t.TABLE_ROWS, 0.01)) * 100), 2) AS `sel %`"
    //                + "FROM INFORMATION_SCHEMA.STATISTICS s" + "INNER JOIN INFORMATION_SCHEMA.TABLES t"
    //                + "ON s.TABLE_SCHEMA = t.TABLE_SCHEMA" + "AND s.TABLE_NAME = t.TABLE_NAME" + "INNER JOIN (" + "SELECT"
    //                + "TABLE_SCHEMA" + ", TABLE_NAME" + ", INDEX_NAME" + ", MAX(SEQ_IN_INDEX) AS max_columns"
    //                + "FROM INFORMATION_SCHEMA.STATISTICS" + "WHERE TABLE_SCHEMA != 'mysql'"
    //                + "GROUP BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME" + ") AS s2" + "ON s.TABLE_SCHEMA = s2.TABLE_SCHEMA"
    //                + "AND s.TABLE_NAME = s2.TABLE_NAME" + "AND s.INDEX_NAME = s2.INDEX_NAME"
    //                + "WHERE t.TABLE_SCHEMA != 'mysql'" + // filter out the mysql system
    //                // DB
    //                "AND t.TABLE_ROWS > 10" + // only tables with some rows
    //                "AND s.CARDINALITY IS NOT NULL" + // need at least one non-NULL value in the field
    //                "AND (s.CARDINALITY / IFNULL(t.TABLE_ROWS, 0.01)) < 1.00" + // selectivity < 1.0 b/c unique indexes are
    //                // perfect anyway
    //                "ORDER BY `sel %`, s.TABLE_SCHEMA, s.TABLE_NAME" + // switch to `sel %` DESC for best non-unique indexes
    //                "LIMIT 20;");
    //    }

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        //        DatabaseManager dm = new DatabaseManager();
        //
        //        ResultCallback<Map<String, Object>> callback = new ResultCallback<Map<String, Object>>() {
        //
        //            @Override
        //            public void processResult(Map<String, Object> object, int number) {
        //                // System.out.println(object);
        //            }
        //        };

        // int result = dm.runQuery(callback, new SimpleRowConverter(), "SELECT * FROM feeds");
        // System.out.println("matches: " + result);


        // Map<String, Object> result2 = dm.runSingleQuery("SELECT * FROM feeds WHERE feedUrl = 'http://www.lexusreports.com/rss/master'");
        // System.out.println(result2);

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