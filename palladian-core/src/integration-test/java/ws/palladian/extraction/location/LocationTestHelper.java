package ws.palladian.extraction.location;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;

public final class LocationTestHelper {
    
    private LocationTestHelper() {
        // helper
    }

    /**
     * Make sure, all given paths are pointing to directories.
     * 
     * @param paths The paths to check.
     */
    public static void assumeDirectory(String... paths) {
        for (String path : paths) {
            assumeTrue(path + " not present", new File(path).isDirectory());
        }
    }

    public static void assertGreater(String valueName, double value, double minimumExpected) {
        String msg = valueName + " must be greater/equal " + minimumExpected + ", but was " + value;
        assertTrue(msg, value >= minimumExpected);
    }

}
