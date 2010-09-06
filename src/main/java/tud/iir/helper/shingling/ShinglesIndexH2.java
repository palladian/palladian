package tud.iir.helper.shingling;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;

/**
 * TODO this is messy, clean up.
 * 
 * @author Philipp Katz
 *
 */
public class ShinglesIndexH2 extends ShinglesIndexBaseImpl {

    private static final Logger LOGGER = Logger.getLogger(ShinglesIndexH2.class);

    private Connection connection = null;
    private String dbType = "h2";
    private String dbDriver = "org.h2.Driver";
    private String dbHost = "localhost";
    private String dbPort = "3306";
    private String dbName = "shingles";
    private String dbUsername = "root";
    private String dbPassword = "";

    // private PreparedStatement psGetHashById;
    // private PreparedStatement psAddHash;
    // private PreparedStatement psAddDocHash;
    private PreparedStatement psGetDocsByHash;
    private PreparedStatement psGetAllSim;
    private PreparedStatement psGetSim;
    private PreparedStatement psAddDocSim;
    private PreparedStatement psGetNumberOfDocs;
    // private PreparedStatement psLastInsertID;
    private PreparedStatement psGetHashesForDocument;

    private PreparedStatement psGetDocumentsForHashes;

    private PreparedStatement psAddDocument;

    private int getDocumentsForHashesTime = 0;
    private int getHashesForDocumentTime = 0;

    public ShinglesIndexH2() {
        connection = getConnection();
        // TODO Auto-generated constructor stub
    }

    private Connection getConnection() {

        String url;

        // XXX url = "jdbc:" + dbType + ":mem:data/models/" + dbName + ";DB_CLOSE_DELAY=-1";
        // url = "jdbc:" + dbType + ":data/models/" + dbName + ";DB_CLOSE_DELAY=-1";
        url = "jdbc:" + dbType + ":" + INDEX_FILE_BASE_PATH + getIndexName() + ";DB_CLOSE_DELAY=-1";

        try {

            Class.forName(dbDriver);
            connection = DriverManager.getConnection(url, dbUsername, dbPassword);

            // XXX try nonunique index?
            PreparedStatement psCreateTableShingles = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS documentsHashes (documentId INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, hash BIGINT, PRIMARY KEY(documentId, hash))");/*
                                                                                                                                                                                       * CREATE
                                                                                                                                                                                       * INDEX
                                                                                                                                                                                       * IF
                                                                                                                                                                                       * NOT
                                                                                                                                                                                       * EXISTS
                                                                                                                                                                                       * hashIndex
                                                                                                                                                                                       * ON
                                                                                                                                                                                       * documentsHashes
                                                                                                                                                                                       * (
                                                                                                                                                                                       * hash
                                                                                                                                                                                       * )
                                                                                                                                                                                       * ;
                                                                                                                                                                                       * ");
                                                                                                                                                                                       */
            // PreparedStatement psCreateTableShingleDocument = connection
            // .prepareStatement("CREATE TABLE IF NOT EXISTS documentsHashes (id INTEGER UNSIGNED NOT NULL, hashId INTEGER UNSIGNED NOT NULL, PRIMARY KEY (id, hashId));");
            PreparedStatement psCreateTableDocumentSimilarities = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS documentSimilarities (masterId INTEGER UNSIGNED NOT NULL, simId INTEGER UNSIGNED NOT NULL, PRIMARY KEY (masterId, simId));");

            psCreateTableShingles.executeUpdate();
            // psCreateTableShingleDocument.executeUpdate();
            psCreateTableDocumentSimilarities.executeUpdate();

            // psGetHashById = connection.prepareStatement("SELECT id FROM hashes WHERE hash = ?");

            psAddDocument = connection.prepareStatement("INSERT INTO documentsHashes VALUES(?, ?)");

            // psAddHash = connection.prepareStatement("INSERT INTO hashes VALUES(null, ?)");
            // psAddDocHash = connection.prepareStatement("INSERT INTO documentsHashes VALUES(?, ?)");
            psGetDocsByHash = connection.prepareStatement("SELECT documentId FROM documentsHashes WHERE hash = ?");
            psGetAllSim = connection.prepareStatement("SELECT masterId FROM documentSimilarities");
            psGetSim = connection.prepareStatement("SELECT simId FROM documentSimilarities WHERE masterId = ?");
            psAddDocSim = connection.prepareStatement("INSERT INTO documentSimilarities VALUES(?, ?)");
            psGetNumberOfDocs = connection.prepareStatement("SELECT COUNT(DISTINCT documentId) FROM documentsHashes");
            // psLastInsertID = connection.prepareStatement("SELECT LAST_INSERT_ID()");
            psGetHashesForDocument = connection
                    .prepareStatement("SELECT hash FROM documentsHashes WHERE documentId = ?");

            psGetDocumentsForHashes = connection
                    .prepareStatement("SELECT documentId FROM documentsHashes WHERE hash IN (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            // DatabaseMetaData metaData = connection.getMetaData();
            // System.out.println("batch updates supported: " + metaData.supportsBatchUpdates());

        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return connection;
    }

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {

        try {

            for (Long hash : sketch) {

                // // check if we already have the hash
                // psGetHashById.setLong(1, hash);
                // int hashId = runQueryID(psGetHashById);
                //
                // if (hashId == -1) {
                // psAddHash.setLong(1, hash);
                // runUpdate(psAddHash);
                // hashId = runQueryID(psLastInsertID);
                // }
                //
                // // add relation to documentId
                // psAddDocHash.setInt(1, documentId);
                // psAddDocHash.setInt(2, hashId);
                // runUpdate(psAddDocHash);

                psAddDocument.setLong(1, documentId);
                psAddDocument.setLong(2, hash);
                runUpdate(psAddDocument);
                // System.out.println(result);

            }

        } catch (SQLException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {

        Set<Integer> result = new HashSet<Integer>();

        try {

            psGetDocsByHash.setLong(1, hash);
            ResultSet resultSet = runQuery(psGetDocsByHash);
            while (resultSet.next()) {
                result.add(resultSet.getInt(1));
            }
            resultSet.close();

        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return result;
    }
    
    @Override
    public Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch) {
        
        Map<Integer, Set<Long>> result = new HashMap<Integer, Set<Long>>();

        
        Set<Integer> idsToCheck = getDocumentIdsForSketch(sketch);
        for (Integer integer : idsToCheck) {
            Set<Long> documentSketch = getSketchForDocument(integer);
            result.put(integer, documentSketch);
        }
        
        // TODO Auto-generated method stub
        return result;
    }

    private Set<Integer> getDocumentIdsForSketch(Set<Long> sketch) {
        LOGGER.trace(">getDocumentsForSketch " + sketch);
        StopWatch sw = new StopWatch();

        Set<Integer> result = new HashSet<Integer>();

        try {
            /*
             * // dynamically create the statement ...
             * String statement = "SELECT d.id FROM documentsHashes AS d, hashes AS h WHERE hashId = h.id AND hash IN ("
             * + StringUtils.join(hashes, ",") + ")";
             * // System.out.println(statement);
             * Statement sqlStatement = connection.createStatement();
             * ResultSet resultSet = sqlStatement.executeQuery(statement);
             */

            int count = 1;

            for (Long long1 : sketch) {
                psGetDocumentsForHashes.setLong(count, long1);
                count++;
            }

            if (count < 200) {
                for (int i = count; i <= 200; i++) {
                    psGetDocumentsForHashes.setNull(i, Types.BIGINT);
                }
            }

            ResultSet resultSet = psGetDocumentsForHashes.executeQuery();

            while (resultSet.next()) {
                result.add(resultSet.getInt(1));
            }
            resultSet.close();

        } catch (SQLException e) {
            LOGGER.error(e);
        }

        getDocumentsForHashesTime += sw.getElapsedTime();
        LOGGER.trace("<getDocumentsForSketch " + result.size() + " " + result);
        return result;
    }

    public Set<Long> getSketchForDocument(int documentId) {
        LOGGER.trace(">getSketchForDocument " + documentId);
        StopWatch sw = new StopWatch();

        Set<Long> result = new HashSet<Long>();

        try {

            // String statement =
            // "SELECT hash FROM hashes, documentsHashes WHERE hashes.id = hashId AND documentsHashes.Id = " +
            // documentId;
            // System.out.println(statement);

            // Statement sqlStatement = connection.createStatement();
            // ResultSet resultSet = sqlStatement.executeQuery(statement);

            psGetHashesForDocument.setInt(1, documentId);
            ResultSet resultSet = runQuery(psGetHashesForDocument);
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }

        } catch (SQLException e) {
            LOGGER.error(e);
        }

        getHashesForDocumentTime += sw.getElapsedTime();
        LOGGER.trace("<getSketchForDocument " + result.size() + " " + result);
        return result;
    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {

        try {

            psAddDocSim.setInt(1, masterDocumentId);
            psAddDocSim.setInt(2, similarDocumentId);
            runUpdate(psAddDocSim);

        } catch (SQLException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {

        Map<Integer, Set<Integer>> result = new TreeMap<Integer, Set<Integer>>();

        try {

            ResultSet resultSet = runQuery(psGetAllSim);
            while (resultSet.next()) {
                int masterDoc = resultSet.getInt(1);
                Set<Integer> docSims = getSimilarDocuments(masterDoc);
                result.put(masterDoc, docSims);
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {

        Set<Integer> result = new HashSet<Integer>();

        try {
            psGetSim.setInt(1, documentId);
            ResultSet resultSet = runQuery(psGetSim);
            while (resultSet.next()) {
                result.add(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @Override
    public int getNumberOfDocuments() {

        int result = runQueryID(psGetNumberOfDocs);

        return result;
    }

    private int runUpdate(PreparedStatement preparedStatement) {
        int result = 0;
        try {
            result = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage() + ", " + preparedStatement.toString());
            result = -1;
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
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return rs;
    }
    
    /**
     * Clear all data in tables.
     */
    public void clearTables() {
        
        try {
            connection.prepareStatement("TRUNCATE TABLE documentsHashes").executeUpdate();
            connection.prepareStatement("TRUNCATE TABLE documentSimilarities").executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        
        
    }
    
    @Override
    public void deleteIndex() {
        
        String file = INDEX_FILE_BASE_PATH + getIndexName() + ".h2.db";
        
        if (FileHelper.fileExists(file)) {
            FileHelper.delete(file);
        }
        
    }


    public static void main(String[] args) {

        ShinglesIndexH2 index = new ShinglesIndexH2();
        ShinglesIndexTracer tracer = new ShinglesIndexTracer(index);

        StopWatch sw = new StopWatch();
        Shingles shingles = new Shingles(tracer);
        shingles.addDocumentsFromFile("data/tag_training_small_50.txt");
        // shingles.addDocumentsFromFile("data/tag_training_NEW_1000.txt");
        System.out.println("elapsed time : " + sw.getElapsedTimeString());

        // index.printPerformance();

        System.out.println("------");
        System.out.println(shingles.getSimilarityReport());

        System.out.println(tracer.getTraceResult());
        /**
         * document add : 816 -----> 918
         * document lookup : 52433 ----> 3199 :)
         */


    }



}
