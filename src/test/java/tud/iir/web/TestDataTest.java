package tud.iir.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.syndication.io.XmlReader;

public class TestDataTest {
    
    @Test
    public void checkTestDataEncoding() {

        String filePath1 = "/home/pk/workspace/Palladian/src/test/resources/feeds/feed102.xml";
        String filePath2 = TestDataTest.class.getResource("/feeds/feed102.xml").getFile();
        
        Assert.assertEquals("UTF-16LE", getXmlEncoding(filePath1));
        Assert.assertEquals("UTF-16LE", getXmlEncoding(filePath2));

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
