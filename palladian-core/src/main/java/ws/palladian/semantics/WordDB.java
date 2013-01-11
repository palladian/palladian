package ws.palladian.semantics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.nlp.LoremIpsumGenerator;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>This is a class for accessing an embedded H2 database holding {@link Word}s.</p>
 * 
 * @author David Urbansky
 * 
 */
public class WordDB {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WordDB.class);

    // //////////////////database paramenters ////////////////////
    private Connection connection = null;
    private final String dbType = "h2";
    private final String dbDriver = "org.h2.Driver";
    private final String dbUsername = "root";
    private final String dbPassword = "";
    private String databasePath = "";

    private boolean inMemoryMode = false;

    public static final int MAX_WORD_LENGTH = 30;

    private PreparedStatement psGetWord = null;
    private PreparedStatement psGetWordById = null;
    private PreparedStatement psAddWord = null;
    private PreparedStatement psUpdateWord = null;
    private PreparedStatement psAddSynonym = null;
    private PreparedStatement psDeleteSynonyms = null;
    private PreparedStatement psAddHypernym = null;
    private PreparedStatement psDeleteHypernyms = null;
    private PreparedStatement psGetSynonyms1 = null;
    private PreparedStatement psGetSynonyms2 = null;
    private PreparedStatement psGetHypernyms = null;
    private PreparedStatement psGetHyponyms = null;
    private PreparedStatement psDeleteHyponyms = null;

    public WordDB(String databasePath) {
        this.databasePath = databasePath;
        connection = getConnection();
        setup();
    }

    public void setup() {
        createTables();
    }

    private Connection getConnection() {

        String url;
        if (isInMemoryMode()) {
            url = "jdbc:" + dbType + ":mem:" + databasePath + ";DB_CLOSE_DELAY=-1";
        } else {
            // if (!new File(databasePath + ".h2").exists()) {
            // throw new IllegalArgumentException("Path to word db does not exist: " + databasePath);
            // }
            url = "jdbc:" + dbType + ":" + databasePath;
        }

        try {
            Class.forName(dbDriver);
            connection = DriverManager.getConnection(url, dbUsername, dbPassword);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return connection;
    }

    private void createTables() {
        try {
            // create database if it does not yet exist
            PreparedStatement psCreateTable1;
            PreparedStatement psCreateTable2;
            PreparedStatement psCreateTable3;

            psCreateTable1 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS words (id int(10) unsigned NOT NULL auto_increment PRIMARY KEY,`word` varchar("
                            + MAX_WORD_LENGTH
                            + ") NOT NULL, `plural` varchar("
                            + MAX_WORD_LENGTH
                            + ") NOT NULL,`type` varchar(25) NOT NULL,`language` varchar(20) NOT NULL);CREATE INDEX IF NOT EXISTS iw ON words(word);CREATE INDEX IF NOT EXISTS ip ON words(plural);");
            runUpdate(psCreateTable1);

            psCreateTable2 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS hypernyms (wordId1 int(10) unsigned NOT NULL, wordId2 int(10) NOT NULL, relevance double NOT NULL, PRIMARY KEY (wordId1, wordID2));");
            runUpdate(psCreateTable2);

            psCreateTable3 = connection
                    .prepareStatement("CREATE TABLE IF NOT EXISTS synonyms (wordId1 int(10) unsigned NOT NULL, wordId2 int(10) NOT NULL, relevance double NOT NULL, PRIMARY KEY (wordId1, wordID2));");
            runUpdate(psCreateTable3);

            prepareStatements();

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void prepareStatements() {
        try {

            /**
             * For H2 check:
             * SELECT * from words where word = 'Bier';
             * SELECT wordId1,words.word FROM synonyms,words WHERE wordId2 = 5037 AND words.id = wordId1;
             * SELECT wordId2,words.word FROM synonyms,words WHERE wordId1 = 5037 AND words.id = wordId2;
             */

            psGetWord = connection
                    .prepareStatement("SELECT id, `word`, `plural`, `type`, `language` FROM words WHERE `word` = ? OR `plural` = ?");

            psGetWordById = connection
                    .prepareStatement("SELECT id, `word`, `plural`, `type`, `language` FROM words WHERE id = ?");

            psAddWord = connection.prepareStatement("INSERT INTO words VALUES(DEFAULT,?,?,?,?)");

            psUpdateWord = connection
                    .prepareStatement("UPDATE words SET `plural` = ?, `type` = ?, `language`= ? WHERE id = ?");

            // hypernyms
            psAddHypernym = connection.prepareStatement("MERGE INTO hypernyms KEY(wordId1,wordId2) VALUES(?,?,?)");
            psDeleteHypernyms = connection.prepareStatement("DELETE hypernyms WHERE `wordId1` = ?");
            psGetHypernyms = connection.prepareStatement("SELECT wordId2 FROM hypernyms WHERE wordId1 = ?");

            // synonyms
            psAddSynonym = connection.prepareStatement("MERGE INTO synonyms KEY(wordId1,wordId2) VALUES(?,?,?)");
            psDeleteSynonyms = connection.prepareStatement("DELETE FROM synonyms WHERE `wordId1` = ? OR `wordId2` = ?");
            psGetSynonyms1 = connection.prepareStatement("SELECT wordId1 FROM synonyms WHERE wordId2 = ?");
            psGetSynonyms2 = connection.prepareStatement("SELECT wordId2 FROM synonyms WHERE wordId1 = ?");

            // hyponyms
            psGetHyponyms = connection.prepareStatement("SELECT wordId1 FROM hypernyms WHERE wordId2 = ?");
            psDeleteHyponyms = connection.prepareStatement("DELETE hypernyms WHERE `wordId2` = ?");

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * <p>
     * Load the database into RAM. This allows faster reads.
     * </p>
     * <p>
     * <b>NOTE: Inserts and Updates are only in memory and will be lost after closing the session.</b>
     * </p>
     * 
     * @return <tt>True</tt>, if the database was successfully loaded into the memory, <tt>false</tt> otherwise.
     */
    public boolean loadDbToMemory() {
        return swapDatabaseStorage(true);
    }

    public boolean writeToDisk() {
        return swapDatabaseStorage(false);
    }

    private boolean swapDatabaseStorage(boolean hddToRam) {
        StopWatch sw = new StopWatch();

        boolean success = true;

        Statement stat = null;
        try {
            String tempScriptFile = "script.sql";

            // save db to sql file
            LOGGER.info("dumping databse to " + tempScriptFile);
            stat = connection.createStatement();
            stat.execute("SCRIPT TO '" + tempScriptFile + "'");
            connection.close();

            LOGGER.info("closed database and re-open database");
            setInMemoryMode(hddToRam);
            if (!hddToRam) {
                runUpdate("DROP TABLE words");
                runUpdate("DROP TABLE synonyms");
                runUpdate("DROP TABLE hypernyms");
            }

            // load file into current connection
            LOGGER.info("import " + tempScriptFile + " to in memory database");
            InputStream in = new FileInputStream(tempScriptFile);
            RunScript.execute(connection, new InputStreamReader(in));

            // delete temporary file
            File f = new File(tempScriptFile);
            f.deleteOnExit();

            prepareStatements();

            LOGGER.info("loaded database to memory in " + sw.getElapsedTimeString());

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            success = false;
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            success = false;
        } finally {
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        return success;
    }

    public boolean fillDatabaseFromScript(String scriptPath) {
        boolean success = true;

        runUpdate("DROP TABLE words");
        runUpdate("DROP TABLE synonyms");
        runUpdate("DROP TABLE hypernyms");

        // load file into current connection
        LOGGER.info("import " + scriptPath + " to in database");

        InputStream in = null;
        try {
            in = new FileInputStream(scriptPath);
            RunScript.execute(connection, new InputStreamReader(in));
        } catch (FileNotFoundException e) {
            success = false;
            LOGGER.error(e.getMessage());
        } catch (SQLException e) {
            success = false;
            LOGGER.error(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        return success;
    }

    private boolean runUpdate(String sql) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return runUpdate(stmt);
    }

    private boolean runUpdate(PreparedStatement preparedStatement) {
        boolean success = false;

        try {
            preparedStatement.executeUpdate();
            success = true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage() + ", " + preparedStatement.toString());
        } catch (Exception e) {
            LOGGER.error(e.getMessage() + ", " + preparedStatement.toString());
        }

        return success;
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

    /**
     * <p>
     * Get a word object from the db. The word can be in singular or plural, i.e. "bed" and "beds" will return the same
     * word object.
     * </p>
     * 
     * @param word The string of the word we are searching for.
     * @param caseInsensitive If true, we search for upper-, and lower-case versions, i.e. bed,Bed,beds,Beds.
     * @return The word object.
     */
    public Word getWord(String word, boolean caseInsensitive) {

        Word wordObject = null;

        try {
            psGetWord.setString(1, word);
            psGetWord.setString(2, word);
        } catch (SQLException e1) {
            e1.printStackTrace();
            return wordObject;
        }

        ResultSet rs = runQuery(psGetWord);

        try {
            if (rs.next()) {
                wordObject = new Word(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
            }

            if (wordObject == null && caseInsensitive) {
                word = StringHelper.upperCaseFirstLetter(word);
                return getWord(word, false);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }


        return wordObject;
    }

    public Word getWord(String word) {
        return getWord(word, false);
    }

    private Word getWordById(int wordId) throws SQLException {

        Word wordObject = null;

        psGetWordById.setInt(1, wordId);

        ResultSet rs = runQuery(psGetWordById);

        try {
            if (rs.next()) {
                wordObject = new Word(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return wordObject;
    }

    public boolean addWord(Word word) throws SQLException {
        if (!isAllowedWord(word.getWord())) {
            return false;
        }

        psAddWord.setString(1, word.getWord());
        psAddWord.setString(2, word.getPlural());
        psAddWord.setString(3, word.getType());
        psAddWord.setString(4, word.getLanguage());

        return runUpdate(psAddWord);
    }

    public boolean updateWord(Word word) throws SQLException {
        if (!isAllowedWord(word.getWord())) {
            return false;
        }

        psUpdateWord.setString(1, word.getPlural());
        psUpdateWord.setString(2, word.getType());
        psUpdateWord.setString(3, word.getLanguage());
        psUpdateWord.setInt(4, word.getId());

        return runUpdate(psUpdateWord);
    }

    /**
     * <p>
     * Add a list of synonyms for a word.
     * </p>
     * 
     * @param word The word to add synonyms for.
     * @param synonyms The synonyms for the given word.
     * @throws SQLException
     */
    public void addSynonyms(Word word, List<String> synonyms) throws SQLException {

        synonyms.add(word.getWord());

        for (int i = 0; i < synonyms.size(); i++) {
            String synonym = synonyms.get(i);

            Word synonymWord = getWord(synonym);
            if (synonymWord == null) {
                addWord(new Word(-1, synonym, "", word.getType(), ""));
                synonymWord = getWord(synonym);
            }

            if (synonymWord == null) {
                continue;
            }

            for (int j = i + 1; j < synonyms.size(); j++) {
                String synonym2 = synonyms.get(j);

                Word synonymWord2 = getWord(synonym2);
                if (synonymWord2 == null) {
                    addWord(new Word(-1, synonym2, "", word.getType(), ""));
                    synonymWord2 = getWord(synonym);
                }

                if (synonymWord2 == null) {
                    continue;
                }

                if (synonymWord.getId() == synonymWord2.getId()) {
                    continue;
                }

                psAddSynonym.setInt(1, synonymWord.getId());
                psAddSynonym.setInt(2, synonymWord2.getId());
                psAddSynonym.setDouble(3, 0.5);

                runUpdate(psAddSynonym);
            }
        }
    }

    /**
     * <p>
     * Set a list of synonyms for a word.
     * </p>
     * <p>
     * <b>Note: All existing synonyms will be deleted! If you just want to add synonyms, use addSynonyms() instead</b>
     * </p>
     * 
     * @param word The word to add synonyms for.
     * @param synonyms The synonyms for the given word.
     * @throws SQLException
     */
    public void setSynonyms(Word word, List<String> synonyms) throws SQLException {

        // delete all synonyms for the given word
        deleteAllSynonyms(word);

        // add all synonyms given
        addSynonyms(word, synonyms);
    }

    private void deleteAllSynonyms(Word word) throws SQLException {
        psDeleteSynonyms.setInt(1, word.getId());
        psDeleteSynonyms.setInt(2, word.getId());

        runUpdate(psDeleteSynonyms);
    }

    public void addHypernyms(Word word, List<String> hypernyms) throws SQLException {

        // get all synonyms for the given word
        aggregateInformation(word);
        Set<Word> synonyms = word.getSynonyms();

        for (int i = 0; i < hypernyms.size(); i++) {
            String hypernym = hypernyms.get(i);

            Word hypernymWord = getWord(hypernym);
            if (hypernymWord == null) {
                addWord(new Word(-1, hypernym, "", word.getType(), ""));
                hypernymWord = getWord(hypernym);
            }

            if (hypernymWord == null) {
                continue;
            }

            // add the hypernym for the given word
            psAddHypernym.setInt(1, word.getId());
            psAddHypernym.setInt(2, hypernymWord.getId());
            psAddHypernym.setDouble(3, 0.5);

            runUpdate(psAddHypernym);

            // add the hypernym for every synonym of the word
            for (Word synoynm : synonyms) {

                psAddHypernym.setInt(1, synoynm.getId());
                psAddHypernym.setInt(2, hypernymWord.getId());
                psAddHypernym.setDouble(3, 0.5);

                runUpdate(psAddHypernym);

            }
        }

    }

    /**
     * <p>
     * Set a list of hypernyms for a word.
     * </p>
     * <p>
     * <b>Note: All existing hypernyms will be deleted! If you just want to add hypernyms, use addHypernyms()
     * instead</b>
     * </p>
     * 
     * @param word The word to add synonyms for.
     * @param hypernyms The hypernyms for the given word.
     * @throws SQLException
     */
    public void setHypernyms(Word word, List<String> hypernyms) throws SQLException {

        // delete all synonyms for the given word
        deleteAllHypernyms(word);

        // add all synonyms given
        addHypernyms(word, hypernyms);
    }

    private void deleteAllHypernyms(Word word) throws SQLException {
        psDeleteHypernyms.setInt(1, word.getId());

        runUpdate(psDeleteHypernyms);
    }

    public void addHyponyms(Word word, List<String> hyponyms) throws SQLException {

        // get all synonyms for the given word
        aggregateInformation(word);
        Set<Word> synonyms = word.getSynonyms();

        for (int i = 0; i < hyponyms.size(); i++) {
            String hyponym = hyponyms.get(i);

            Word hyponymWord = getWord(hyponym);
            if (hyponymWord == null) {
                addWord(new Word(-1, hyponym, "", word.getType(), ""));
                hyponymWord = getWord(hyponym);
            }

            if (hyponymWord == null) {
                continue;
            }

            // add the hypernym for the given word
            psAddHypernym.setInt(1, hyponymWord.getId());
            psAddHypernym.setInt(2, word.getId());
            psAddHypernym.setDouble(3, 0.5);

            runUpdate(psAddHypernym);

            // add the hypernym for every synonym of the word
            for (Word synoynm : synonyms) {

                psAddHypernym.setInt(1, hyponymWord.getId());
                psAddHypernym.setInt(2, synoynm.getId());
                psAddHypernym.setDouble(3, 0.5);

                runUpdate(psAddHypernym);

            }
        }

    }

    /**
     * <p>
     * Set a list of hyponyms for a word.
     * </p>
     * <p>
     * <b>Note: All existing hyponyms will be deleted! If you just want to add hyponyms, use addHyponyms() instead</b>
     * </p>
     * 
     * @param word The word to add synonyms for.
     * @param hyponyms The hyponyms for the given word.
     * @throws SQLException
     */
    public void setHyponyms(Word word, List<String> hyponyms) throws SQLException {

        // delete all synonyms for the given word
        deleteAllHyponyms(word);

        // add all synonyms given
        addHyponyms(word, hyponyms);
    }

    private void deleteAllHyponyms(Word word) throws SQLException {
        psDeleteHyponyms.setInt(1, word.getId());

        runUpdate(psDeleteHyponyms);
    }

    public Set<Word> getSynonyms(Word word) {

        Set<Word> synonyms = new LinkedHashSet<Word>();

        if (word == null) {
            return synonyms;
        }

        // only words of the same type can be considered synonyms
        int type = word.getType().hashCode();

        try {
            psGetSynonyms1.setInt(1, word.getId());
            ResultSet rs = runQuery(psGetSynonyms1);
            while (rs.next()) {
                Word synonym = getWordById(rs.getInt(1));
                if (synonym != null && type == synonym.getType().hashCode()) {
                    synonyms.add(synonym);
                }
            }

            psGetSynonyms2.setInt(1, word.getId());
            rs = runQuery(psGetSynonyms2);
            while (rs.next()) {
                Word synonym = getWordById(rs.getInt(1));
                if (synonym != null && type == synonym.getType().hashCode()) {
                    synonyms.add(synonym);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return synonyms;
    }

    public Set<Word> getHypernyms(Word word) {

        Set<Word> hypernyms = new LinkedHashSet<Word>();

        if (word == null) {
            return hypernyms;
        }

        try {
            psGetHypernyms.setInt(1, word.getId());
            ResultSet rs = runQuery(psGetHypernyms);
            while (rs.next()) {
                Word hypernym = getWordById(rs.getInt(1));
                if (hypernym != null) {
                    hypernyms.add(hypernym);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hypernyms;
    }

    public Set<Word> getHyponyms(Word word) {

        Set<Word> hyponyms = new LinkedHashSet<Word>();

        if (word == null) {
            return hyponyms;
        }

        try {
            psGetHyponyms.setInt(1, word.getId());
            ResultSet rs = runQuery(psGetHyponyms);
            while (rs.next()) {
                Word hyponym = getWordById(rs.getInt(1));
                if (hyponym != null) {
                    hyponyms.add(hyponym);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hyponyms;
    }

    public Word aggregateInformation(String wordString) {
        return aggregateInformation(getWord(wordString));
    }

    public Word aggregateInformation(Word word) {

        if (word == null) {
            return word;
        }
        Set<Word> synonyms = getSynonyms(word);
        Set<Word> hypernyms = getHypernyms(word);
        Set<Word> hyponyms = getHyponyms(word);

        word.setSynonyms(synonyms);
        word.setHypernyms(hypernyms);
        word.setHyponyms(hyponyms);

        return word;
    }

    public boolean isInMemoryMode() {
        return inMemoryMode;
    }

    public void setInMemoryMode(boolean inMemoryMode) {
        this.inMemoryMode = inMemoryMode;
        connection = getConnection();
    }

    public void performanceCheck(int numIterations) throws SQLException, FileNotFoundException {
        StopWatch sw = new StopWatch();

        // read from file db
        for (int i = 0; i < numIterations; i++) {
            Word word = getWord(LoremIpsumGenerator.getRandomText(8));
            aggregateInformation(word);
        }
        System.out.println("read word " + numIterations + " times from hdd database in " + sw.getElapsedTimeString());

        // insert into file db
        // for (int i = 0; i < numIterations; i++) {
        // addWord(new Word(-1, LoremIpsumGenerator.getRandomText(5), "", ""));
        // }
        // System.out.println("write word " + numIterations + " times to hdd database in " + sw.getElapsedTimeString());

        loadDbToMemory();

        // read from memory db
        sw.start();
        for (int i = 0; i < numIterations; i++) {
            Word word = getWord(LoremIpsumGenerator.getRandomText(8));
            aggregateInformation(word);
        }
        System.out.println("read word " + numIterations + " times from in-memory database in "
                + sw.getElapsedTimeString());

        // insert into memory db
        // for (int i = 0; i < numIterations; i++) {
        // addWord(new Word(-1, LoremIpsumGenerator.getRandomText(5), "", ""));
        // }
        // System.out.println("write word " + numIterations + " times to in-memory database in "
        // + sw.getElapsedTimeString());

    }

    /**
     * Check whether a given word string is allowed to be entered into the database.
     * 
     * @return <tt>True</tt>, if the word is allowed, <tt>false</tt> otherwise.
     */
    private boolean isAllowedWord(String word) {
        boolean allowed = true;

        if (word.indexOf(" ") > -1) {
            allowed = false;
        } else if (word.indexOf(":") > -1) {
            allowed = false;
        } else if (word.indexOf("/") > -1) {
            allowed = false;
        } else if (word.length() > MAX_WORD_LENGTH) {
            allowed = false;
        }

        return allowed;
    }

    /**
     * <p>
     * Check whether word is of a certain type.
     * </p>
     * 
     * @param word The word to check.
     * @param type Check whether the word is of this type.
     * @return True, if the word is of the given type and false otherwise.
     */
    public boolean isType(String word, String type) {
        boolean isType = false;
        try {
            isType = getWord(word).getType().equalsIgnoreCase(type);
        } catch (Exception e) {
            LOGGER.warn("word " + word + " was not found and the type could not be determined");
        }

        return isType;
    }

    /**
     * Example usage.
     * 
     * @param args
     * @throws SQLException
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws SQLException, FileNotFoundException {

        StopWatch sw = new StopWatch();

        // load a word DB
        // WordDB wordDB = new WordDB("data/temp/wordDatabaseEnglish/");
        WordDB wordDB = new WordDB("data/temp/englishWordDb");
        //WordDB wordDB = new WordDB("data/models/palladian/language/wiktionary_de/");
        // WordDB wordDB = new WordDB("data/temp/wordDatabaseGerman_latest/");

        // you can load the database into the memory for faster read access (requires lots of RAM)
        // wordDB.loadDbToMemory();

        // search a word in the database
        sw.start();
        Word word = wordDB.getWord("freedom");
        wordDB.aggregateInformation(word);
        // Word word = wordDB.getWord("Freiheit");
        LOGGER.info("{}", word);

        word = wordDB.getWord("beer");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        word = wordDB.getWord("health");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        word = wordDB.getWord("Strand");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        word = wordDB.getWord("Fliege");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        word = wordDB.getWord("Kleider");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        word = wordDB.getWord("Bier");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        word = wordDB.getWord("Notebook");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);
        
        word = wordDB.getWord("Walkman");
        wordDB.aggregateInformation(word);
        LOGGER.info("{}", word);

        LOGGER.info(sw.getElapsedTimeString());
        LOGGER.info(sw.getTotalElapsedTimeString());

        // WordDB wordDB = new WordDB("data/models/wiktionary2/");
        // wordDB.performanceCheck(100000);
        // WordDB wordDB = new WordDB("data/models/wiktionary5/");
        // wordDB.fillDatabaseFromScript("script.sql");

        // sw.start();
        // Map<Integer, String> map = new HashMap<Integer, String>();
        // for (int i = 0; i < 100000; i++) {
        // map.put(i, LoremIpsumGenerator.getRandomText(8));
        // }
        // LOGGER.info(sw.getElapsedTimeString());
        //
        // for (int i = 0; i < 100000; i++) {
        // map.get(i);
        // }
        //
        // LOGGER.info(sw.getElapsedTimeString());

    }
}
