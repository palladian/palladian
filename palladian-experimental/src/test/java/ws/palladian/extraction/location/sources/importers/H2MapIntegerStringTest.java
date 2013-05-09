package ws.palladian.extraction.location.sources.importers;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;

public class H2MapIntegerStringTest {

    @Test
    public void testH2Map() {
        File tempDir = FileHelper.getTempDir();
        File tempFile = new File(tempDir, "h2Test");
        H2MapIntegerString map = H2MapIntegerString.open(tempFile);

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals("three", map.put(3, "four"));

        assertEquals(3, map.size());
        assertEquals(3, map.entrySet().size());
        assertEquals(3, map.keySet().size());
        assertEquals(3, map.values().size());
    }

}
