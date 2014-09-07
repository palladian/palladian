package ws.palladian.integrationtests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.ResourceHelper;

/**
 * Helper methods for integration testing.
 * 
 * @author pk
 */
public final class ITHelper {

    /** File path to the test properties (in resources path). */
    private static final String PALLADIAN_TEST_PROPERTIES_PATH = "/palladian-test.properties";

    private ITHelper() {
        // helper
    }

    /**
     * Make sure, all given paths are pointing to directories.
     * 
     * @param paths The paths to check.
     */
    public static void assertDirectory(String... paths) {
        for (String path : paths) {
            assertTrue("Directory" + path + " is not present", new File(path).isDirectory());
        }
    }

    /**
     * Assert, that a value is greater/equal than a specified value.
     * 
     * @param valueName The name of the value (for meaningful failure messages).
     * @param minimumExpected The minimum expected value (i.e. actualValue must be >= minimumExpected).
     * @param actualValue The actual value.
     */
    public static void assertMin(String valueName, double minimumExpected, double actualValue) {
        String msg = valueName + " must be greater/equal " + minimumExpected + ", but was " + actualValue;
        assertTrue(msg, actualValue >= minimumExpected);
    }

    /**
     * Assert, that a value is less/equal than a specified value.
     * 
     * @param valueName The name of the value (for meaningful failure messages).
     * @param minimumExpected The maximum expected value (i.e. actualValue must be >= minimumExpected).
     * @param actualValue The actual value.
     */
    public static void assertMax(String valueName, double maximumExpected, double actualValue) {
        String msg = valueName + " must be less/equal " + maximumExpected + ", but was " + actualValue;
        assertTrue(msg, actualValue <= maximumExpected);
    }

    /**
     * Assert, that we have enough free heap memory.
     * 
     * @param size The size.
     * @param unit The unit.
     */
    public static void assertMemory(long size, SizeUnit unit) {
        long freeMemory = ProcessHelper.getFreeMemory();
        if (freeMemory < unit.toBytes(size)) {
            long freeUnit = unit.convert(freeMemory, SizeUnit.BYTES);
            String msg = String
                    .format("Not enough memory. This test requires at least %d %s of heap memory, but only %d %s are available.",
                            size, unit, freeUnit, unit);
            fail(msg);
        }
    }

    /**
     * Force a garbage collection run.
     */
    public static void forceGc() {
        Object obj = new Object();
        WeakReference<Object> ref = new WeakReference<Object>(obj);
        obj = null;
        while (ref.get() != null) {
            System.gc();
        }
    }

    /**
     * Get the configuration for the integration tests.
     * 
     * @return The configuration.
     * @throws IllegalStateException In any case the configuration could not be loaded (not found, ...).
     */
    public static Configuration getTestConfig() {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration(ResourceHelper.getResourceFile(PALLADIAN_TEST_PROPERTIES_PATH));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(PALLADIAN_TEST_PROPERTIES_PATH + " not found");
        } catch (ConfigurationException e) {
            throw new IllegalStateException("could not load config from " + PALLADIAN_TEST_PROPERTIES_PATH);
        }
        return config;
    }

    /**
     * <p>
     * Verify that the given files with datasets exist. If not, output a warning message and skip the test.
     * </p>
     * 
     * @param datasetName The name of the dataset, used for log output in case the files do not exist.
     * @param filePaths The paths whose existence to verify.
     */
    public static void assumeFile(String datasetName, String... filePaths) {
        boolean runTest = true;
        for (String filePath : filePaths) {
            if (filePath == null || !new File(filePath).isFile()) {
                runTest = false;
                break;
            }
        }
        assumeTrue("Dataset for '" + datasetName
                + "' is missing, test is skipped. Adjust palladian-test.properties to set the correct paths.", runTest);
    }

    /**
     * <p>
     * Verify that the given files with datasets exist. If not, output a warning message and skip the test.
     * </p>
     * 
     * @param datasetName The name of the dataset, used for log output in case the files do not exist.
     * @param directoryPaths The paths whose existence to verify.
     */
    public static void assumeDirectory(String datasetName, String... directoryPaths) {
        boolean runTest = true;
        for (String filePath : directoryPaths) {
            if (filePath == null || !new File(filePath).isDirectory()) {
                runTest = false;
                break;
            }
        }
        assumeTrue("Dataset for '" + datasetName
                + "' is missing, test is skipped. Adjust palladian-test.properties to set the correct paths.", runTest);
    }

}
