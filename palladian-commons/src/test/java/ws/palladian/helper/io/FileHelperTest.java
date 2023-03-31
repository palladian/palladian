package ws.palladian.helper.io;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test cases for the FileHelper class.
 *
 * @author David Urbansky
 */
public class FileHelperTest {
    @Test
    public void testGetFileName() {
        assertEquals("ConvocationProgram2017_WEB",
                FileHelper.getFileName("http://www.excelsior.edu/documents/1384577/8431827/ConvocationProgram2017_WEB.pdf/dcd4f1dd-cec4-5b11-2210-cd6cd4828c2e"));
        assertEquals("abc", FileHelper.getFileName("data/temp/abc.jpg"));
        assertEquals("abc", FileHelper.getFileName("abc.jpg"));
        assertEquals("abc", FileHelper.getFileName("abc"));
        assertEquals("abc", FileHelper.getFileName("http://images/abc"));
    }

    @Test
    public void testGetFilePath() {
        assertEquals("data/temp/", FileHelper.getFilePath("data/temp/abc.jpg"));
        assertEquals("", FileHelper.getFilePath("abc.jpg"));
    }

    @Test
    public void testGetFileType() {
        assertEquals("xlsx", FileHelper.getFileType("https://debitoor.de/funktionen/musterrechnung/de_debitoor_invoice_template.xlsx&usg=AOvVaw1LlUFdLNIi5JIvwjoMqldf"));
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
        String renamedFile = FileHelper.getRenamedFilename(new File(ResourceHelper.getResourcePath("/empty.txt")), "empty_tagged");
        renamedFile = renamedFile.substring(renamedFile.lastIndexOf(File.separatorChar) + 1);
        assertEquals("empty_tagged.txt", renamedFile);
    }

    @Test
    public void testGetTempFile() throws IOException {
        assertFalse(FileHelper.getTempFile().exists());
        assertTrue(FileHelper.getTempFile().createNewFile());
        assertFalse(FileHelper.getTempFile().equals(FileHelper.getTempFile()));
    }

    @Test
    public void testL4z() {
        String string = "test string 123\nnext line";
        String filePath = "test.lz4";
        FileHelper.writeToFile(filePath, string);
        String actual = FileHelper.tryReadFileToStringNoReplacement(filePath);
        assertEquals(string + "\n", actual);
        FileHelper.delete(filePath);
    }
}
