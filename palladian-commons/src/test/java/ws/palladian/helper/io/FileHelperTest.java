package ws.palladian.helper.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

/**
 * Test cases for the FileHelper class.
 * 
 * @author David Urbansky
 */
public class FileHelperTest {

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
    public void testGetFileType() {
        assertEquals("jpg", FileHelper.getFileType("data/temp/abc.jpg"));
        assertEquals("jpg", FileHelper.getFileType("abc.jpg"));
        assertEquals("jpg", FileHelper.getFileType("http://www.test.com/abc.jpg"));
        assertEquals("aspx", FileHelper.getFileType("http://www.test.com/abc.aspx?param1=123"));
        assertEquals("aspx", FileHelper.getFileType("http://www.test.com/abc.aspx?param1=123&Web.Offset=abc"));
    }

    @Test
    public void testAppendToFileName() {
        assertEquals("data/temp/abc_0.jpg", FileHelper.appendToFileName("data/temp/abc.jpg", "_0"));
        assertEquals("abcX123.jpg", FileHelper.appendToFileName("abc.jpg", "X123"));
    }

    @Test
    public void testIsFileName() {
        assertEquals(true, FileHelper.isFileName(" website.html"));
        assertEquals(true, FileHelper.isFileName("test.ai "));
        assertEquals(false, FileHelper.isFileName(".just a sentence. "));
        assertEquals(false, FileHelper.isFileName("everything..."));
    }

    @Test
    public void testRename() throws FileNotFoundException {
        // System.out.println(FileHelper.rename(new
        // File("data/test/sampleTextForTagging.txt"),"sampleTextForTagging_tagged"));
        String renamedFile = FileHelper.getRenamedFilename(new File(ResourceHelper.getResourcePath("/empty.txt")),
                "empty_tagged");
        renamedFile = renamedFile.substring(renamedFile.lastIndexOf(File.separatorChar) + 1);
        assertEquals("empty_tagged.txt", renamedFile);
    }

    @Test
    public void testGetTempFile() throws IOException {
        assertFalse(FileHelper.getTempFile().exists());
        assertTrue(FileHelper.getTempFile().createNewFile());
        assertFalse(FileHelper.getTempFile().equals(FileHelper.getTempFile()));
    }

}
