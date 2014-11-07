package ws.palladian.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Test for the {@link DatabaseManager} using H2 in-memory database.
 * </p>
 * 
 * @author Philipp Katz
 */
public class DatabaseManagerTest {

    // test prepared statements
    private static final String CREATE_TABLE = "CREATE TABLE test (id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(255), age INTEGER, weight REAL, cool BOOLEAN, PRIMARY KEY (id));";
    private static final String CREATE_TABLE_2 = "CREATE TABLE test2 (name VARCHAR(255), age INTEGER, weight REAL, cool BOOLEAN, PRIMARY KEY (name));";
    private static final String DROP_TABLE = "DROP TABLE test";
    private static final String DROP_TABLE_2 = "DROP TABLE test2";
    private static final String INSERT_TEST = "INSERT INTO test (name, age, weight, cool) VALUES (?, ?, ?, ?)";
    private static final String INSERT_TEST_2 = "INSERT INTO test2 (name, age, weight, cool) VALUES (?, ?, ?, ?)";
    private static final String GET_TEST = "SELECT * FROM test";
    private static final String GET_TEST_BY_NAME = "SELECT * FROM test WHERE name = ?";
    private static final String COUNT_TEST = "SELECT COUNT(*) FROM test";
    private static final String MAX_TEST = "SELECT MAX(id) FROM test";

    // configuration for in-memory database
    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String JDBC_USERNAME = "sa";
    private static final String JDBC_PASSWORD = "";

    // test data
    private final Object[] d1 = {"bob", 30, 70, true};
    private final Object[] d2 = {"mary", 25, 45, true};
    private final Object[] d3 = {"john", 27, 80, false};
    private final Object[] d4 = {"carol", 16, 60, true};
    private final SampleClazz c1 = new SampleClazz("bob", 30, 70, true);
    private final SampleClazz c2 = new SampleClazz("mary", 25, 45, true);
    private final SampleClazz c3 = new SampleClazz("john", 27, 80, false);
    private final SampleClazz c4 = new SampleClazz("carol", 16, 60, true);

    /** The class under test. */
    private DatabaseManager databaseManager;

    @Before
    public void before() {
        databaseManager = DatabaseManagerFactory.create(DatabaseManager.class, JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD);
        databaseManager.runUpdate(CREATE_TABLE);
        databaseManager.runUpdate(CREATE_TABLE_2);
    }

    @After
    public void after() {
        databaseManager.runUpdate(DROP_TABLE);
        databaseManager.runUpdate(DROP_TABLE_2);
    }

    @Test
    public void testRunInsert() {
        assertEquals(1, databaseManager.runInsertReturnId(INSERT_TEST, d1));
        assertEquals(2, databaseManager.runInsertReturnId(INSERT_TEST, d2));
    }

    @Test
    public void testRunQuery() {
        databaseManager.runInsertReturnId(INSERT_TEST, d1);
        databaseManager.runInsertReturnId(INSERT_TEST, d2);
        List<SampleClazz> result = databaseManager.runQuery(new SampleClazzRowConverter(), GET_TEST);
        assertEquals(2, result.size());
        assertEquals("bob", result.get(0).getName());
        assertEquals("mary", result.get(1).getName());
    }

    @Test
    public void testRunSingleQuery() {
        databaseManager.runInsertReturnId(INSERT_TEST, d1);
        databaseManager.runInsertReturnId(INSERT_TEST, d2);
        SampleClazz result = databaseManager.runSingleQuery(new SampleClazzRowConverter(), GET_TEST_BY_NAME, "bob");
        assertEquals("bob", result.getName());
    }

    @Test
    public void testRunQueryWithIterator() {
        databaseManager.runInsertReturnId(INSERT_TEST, d1);
        databaseManager.runInsertReturnId(INSERT_TEST, d2);
        ResultIterator<SampleClazz> iterator = databaseManager.runQueryWithIterator(new SampleClazzRowConverter(),
                GET_TEST);
        assertTrue(iterator.hasNext());
        assertEquals("bob", iterator.next().getName());
        assertTrue(iterator.hasNext());
        assertEquals("mary", iterator.next().getName());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testRunBatchInsert() {
        final List<SampleClazz> test = Arrays.asList(c1, c2, c3, c4);
        final int[] expectedIds = new int[] {1, 2, 3, 4};
        int insertedRows = databaseManager.runBatchInsert(INSERT_TEST, new BatchDataProvider() {

            @Override
            public List<Object> getData(int number) {
                List<Object> data = new ArrayList<Object>();
                SampleClazz testClazz = test.get(number);
                data.add(testClazz.getName());
                data.add(testClazz.getAge());
                data.add(testClazz.getWeight());
                data.add(testClazz.isCool());
                return data;
            }

            @Override
            public int getCount() {
                return test.size();
            }

            @Override
            public void insertedItem(int number, int generatedId) {
                assertEquals(expectedIds[number], generatedId);
            }
        });
        assertEquals(4, insertedRows);
    }

    @Test
    public void testRunBatchInsertWithoutIDs() {
        final List<SampleClazz> test = Arrays.asList(c1, c2, c3, c4);
        int insertedRows = databaseManager.runBatchInsert(INSERT_TEST_2, new CollectionBatchDataProvider<SampleClazz>(
                test) {
            @Override
            public List<? extends Object> getData(SampleClazz nextItem) {
                List<Object> data = new ArrayList<Object>();
                data.add(nextItem.getName());
                data.add(nextItem.getAge());
                data.add(nextItem.getWeight());
                data.add(nextItem.isCool());
                return data;
            }

            @Override
            public void insertedItem(int number, int generatedId) {
                assertEquals(-1, generatedId);
            }
        });
        assertEquals(4, insertedRows);
    }

    @Test
    public void testRollback() {
        final List<SampleClazz> test = Arrays.asList(c1, c2, c3, c1);
        int insertedRows = databaseManager.runBatchInsert(INSERT_TEST_2, new CollectionBatchDataProvider<SampleClazz>(
                test) {
            @Override
            public List<? extends Object> getData(SampleClazz nextItem) {
                List<Object> data = new ArrayList<Object>();
                data.add(nextItem.getName());
                data.add(nextItem.getAge());
                data.add(nextItem.getWeight());
                data.add(nextItem.isCool());
                return data;
            }
        });
        assertEquals(0, insertedRows);
        assertEquals(0,
                (int)databaseManager.runSingleQuery(OneColumnRowConverter.INTEGER, "SELECT COUNT(*) FROM test2;"));
    }

    @Test
    public void testRunBatchUpdateReturnIds() {
        List<List<Object>> params = new ArrayList<List<Object>>();
        params.add(Arrays.asList(d1));
        params.add(Arrays.asList(d2));
        params.add(Arrays.asList(d3));
        params.add(Arrays.asList(d4));
        int[] generatedIds = databaseManager.runBatchInsertReturnIds(INSERT_TEST, params);
        assertEquals(4, generatedIds.length);
        assertEquals(1, generatedIds[0]);
        assertEquals(2, generatedIds[1]);
        assertEquals(3, generatedIds[2]);
        assertEquals(4, generatedIds[3]);
    }

    @Test
    public void runAggregateQuery() {
        databaseManager.runInsertReturnId(INSERT_TEST, d1);
        databaseManager.runInsertReturnId(INSERT_TEST, d2);
        databaseManager.runInsertReturnId(INSERT_TEST, d3);
        int aggregateResult = databaseManager.runAggregateQuery(COUNT_TEST);
        assertEquals(3, aggregateResult);
        aggregateResult = databaseManager.runAggregateQuery(MAX_TEST);
        assertEquals(3, aggregateResult);
    }

    @Test
    public void testReflectionRowConverter() {
        databaseManager.runInsertReturnId(INSERT_TEST, d1);
        databaseManager.runInsertReturnId(INSERT_TEST, d2);
        // test with null value
        databaseManager.runInsertReturnId(INSERT_TEST, "mary", null, 45, true);

        RowConverter<SampleClazz> rowConverter = ReflectionRowConverter.create(SampleClazz.class);
        List<SampleClazz> result = databaseManager.runQuery(rowConverter, GET_TEST);
        assertEquals(3, result.size());
        assertEquals("mary", result.get(2).getName());
        assertEquals(0, result.get(2).getAge());
        assertEquals(45., result.get(2).getWeight(), 0);
        assertEquals(true, result.get(2).isCool());
    }

}
