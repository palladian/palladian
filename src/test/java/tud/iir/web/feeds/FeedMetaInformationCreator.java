/**
 * Created on: 29.07.2010 16:08:58
 */
package tud.iir.web.feeds;


import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import tud.iir.web.feeds.meta.MetaInformationCreator;

/**
 * <p>
 * 
 * </p>
 *
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 *
 */
public class FeedMetaInformationCreator {
    
    private MetaInformationCreator objectOfClassUnderTest;
    
    @Test
    @Ignore
    public void testCalculateAverageSize() throws Exception {
        objectOfClassUnderTest = new MetaInformationCreator();
        File exampleFile = new File(FeedMetaInformationCreator.class.getResource("/datasets/feedPosts/exampleDataset.csv").toURI());
//        Double average = objectOfClassUnderTest.calculateAverageSize(exampleFile, 2);
//        assertEquals(new Double(9.0),average);
    }

}
