package tud.iir.helper;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test cases for the FileHelper class.
 * 
 * @author David Urbansky
 */
public class FileHelperTest extends TestCase {

    public FileHelperTest(String name) {
        super(name);
    }

    @Test
    public void testGetFileName() {
        assertEquals("abc", FileHelper.getFileName("data/temp/abc.jpg"));
        assertEquals("abc", FileHelper.getFileName("abc.jpg"));
        assertEquals("abc", FileHelper.getFileName("abc"));
    }
}
