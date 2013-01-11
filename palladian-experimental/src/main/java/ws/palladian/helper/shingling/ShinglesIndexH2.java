package ws.palladian.helper.shingling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

/**
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexH2 extends ShinglesIndexBaseImpl {

    // private static final Logger LOGGER = Logger.getLogger(ShinglesIndexH2.class);

    private String dbType = "h2";
    private String dbDriver = "org.h2.Driver";
    private String dbUsername = "root";
    private String dbPassword = "";

    private PreparedStatement psGetDocsByHash;
    private PreparedStatement psGetAllSim;
    private PreparedStatement psGetSim;
    private PreparedStatement psAddDocSim;
    private PreparedStatement psGetNumberOfDocs;
    private PreparedStatement psGetHashesForDocument;
    private PreparedStatement psGetDocumentsForHashes;
    private PreparedStatement psAddDocument;

    private Connection connection;

    // public ShinglesIndexH2() {
    //
    // moved this to openIndex()
    //
    // }

    @Override
    public void openIndex() {

        
        try {

            Class.forName(dbDriver);
            String url = "jdbc:" + dbType + ":" + INDEX_FILE_BASE_PATH + getIndexName() + ";DB_CLOSE_DELAY=-1";
            connection = DriverManager.getConnection(url, dbUsername, dbPassword);

            PreparedStatement psCreateTableShingles = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS documentsHashes (documentId INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, hash BIGINT, PRIMARY KEY(documentId, hash)); "
                            + "CREATE INDEX IF NOT EXISTS hashIndex ON documentsHashes(documentId); "
                            + "CREATE INDEX IF NOT EXISTS documentIdIndex ON documentsHashes(hash);");

            PreparedStatement psCreateTableDocumentSimilarities = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS documentSimilarities (masterId INTEGER UNSIGNED NOT NULL, simId INTEGER UNSIGNED NOT NULL, PRIMARY KEY (masterId, simId)); "
                            + "CREATE INDEX IF NOT EXISTS masterIdIndex ON documentSimilarities(masterId);");

            psCreateTableShingles.executeUpdate();
            psCreateTableDocumentSimilarities.executeUpdate();

            psAddDocument = connection.prepareStatement("INSERT INTO documentsHashes VALUES(?, ?)");

            psGetDocsByHash = connection.prepareStatement("SELECT documentId FROM documentsHashes WHERE hash = ?");
            psGetAllSim = connection.prepareStatement("SELECT masterId FROM documentSimilarities");
            psGetSim = connection.prepareStatement("SELECT simId FROM documentSimilarities WHERE masterId = ?");
            psAddDocSim = connection.prepareStatement("INSERT INTO documentSimilarities VALUES(?, ?)");
            psGetNumberOfDocs = connection.prepareStatement("SELECT COUNT(DISTINCT documentId) FROM documentsHashes");
            psGetHashesForDocument = connection
                    .prepareStatement("SELECT hash FROM documentsHashes WHERE documentId = ?");

            // TODO hardcoded 200 hashes for now
            psGetDocumentsForHashes = connection
                    .prepareStatement("SELECT documentId FROM documentsHashes WHERE hash IN (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

        } catch (ClassNotFoundException e) {
            LOGGER.error("ClassNotFoundException while loading {}", dbDriver, e);
        } catch (SQLException e) {
            LOGGER.error("SQLException while setting up the database", e);
        }

    }

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {

        try {

            for (Long hash : sketch) {

                psAddDocument.setLong(1, documentId);
                psAddDocument.setLong(2, hash);
                runUpdate(psAddDocument);

            }

        } catch (SQLException e) {
            LOGGER.error("SQLException while adding document", e);
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
            LOGGER.error("SQLException while getting hash for document", e);
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

        return result;
    }

    private Set<Integer> getDocumentIdsForSketch(Set<Long> sketch) {
        LOGGER.trace(">getDocumentsForSketch " + sketch);

        Set<Integer> result = new HashSet<Integer>();

        try {

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
            LOGGER.error("SQLException while getting document IDs for sketch", e);
        }

        LOGGER.trace("<getDocumentsForSketch " + result.size() + " " + result);
        return result;
    }

    public Set<Long> getSketchForDocument(int documentId) {
        LOGGER.trace(">getSketchForDocument " + documentId);

        Set<Long> result = new HashSet<Long>();

        try {

            psGetHashesForDocument.setInt(1, documentId);
            ResultSet resultSet = runQuery(psGetHashesForDocument);
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }

        } catch (SQLException e) {
            LOGGER.error("SQLException while getting sketch for document", e);
        }

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
            LOGGER.error("SQLException while adding document similarity", e);
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
            LOGGER.error("SQLException while getting similar documents", e);
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
            LOGGER.error("SQLException while getting similar documents", e);
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
