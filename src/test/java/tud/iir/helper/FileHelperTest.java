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

    @Test
    public void testGetFilePath() {
        assertEquals("data/temp/", FileHelper.getFilePath("data/temp/abc.jpg"));
        assertEquals("", FileHelper.getFilePath("abc.jpg"));
    }

    @Test
    public void testAppendToFileName() {
        assertEquals("data/temp/abc_0.jpg", FileHelper.appendToFileName("data/temp/abc.jpg", "_0"));
        assertEquals("abcX123.jpg", FileHelper.appendToFileName("abc.jpg", "X123"));
    }
}