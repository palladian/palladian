package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

import com.sun.syndication.io.XmlReader;

public class TestDataTest {

    @Test
    public void checkTestDataEncoding() throws FileNotFoundException {

        String filePath1 = "src/test/resources/feeds/feed102.xml";
        String filePath2 = ResourceHelper.getResourcePath("/feeds/feed102.xml");

        assertEquals("UTF-16LE", getXmlEncoding(filePath1));
        assertEquals("UTF-16LE", getXmlEncoding(filePath2));

    }

    private static String getXmlEncoding(String filePath) {

        try {

            File file = new File(filePath);
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            XmlReader xmlReader = new XmlReader(inputStream);
            // System.out.println("Encoding " + xmlReader.getEncoding());
            return xmlReader.getEncoding();

        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            return "<undetermined>";
        }

    }
}
