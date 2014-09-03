package ws.palladian.integrationtests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.lang.ref.WeakReference;

import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.constants.SizeUnit;

public final class ITHelper {

    private ITHelper() {
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

    /**
     * Assert, that a value is greater/equal a specified value.
     * @param valueName The name of the value (for meaningful failure messages).
     * @param minimumExpected The minimum, expected value (i.e. actualValue must be >= minimumExpected).
     * @param actualValue The actual value.
     */
    public static void assertGreater(String valueName, double minimumExpected, double actualValue) {
        String msg = valueName + " must be greater/equal " + minimumExpected + ", but was " + actualValue;
        assertTrue(msg, actualValue >= minimumExpected);
    }

    /**
     * Assert, that we have enough free heap memory.
     * 
     * @param size The size.
     * @param unit The unit.
     */
    public static void assertMemory(long size, SizeUnit unit) {
        if (ProcessHelper.getFreeMemory() < unit.toBytes(size)) {
            String msg = String.format("Not enough memory. This test requires at least %d %s heap memory.", size, unit);
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

}
