/**
 * Created on: 30.12.2012 11:41:57
 */
package ws.palladian.processing.features;

import org.junit.Test;

import ws.palladian.processing.features.utils.FeatureUtils;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public class FeatureUtilsTest {

    /**
     * Test method for
     * {@link ws.palladian.processing.features.utils.FeatureUtils#find(ws.palladian.processing.features.Feature, ws.palladian.processing.features.BasicFeatureVectorImpl)}
     * .
     */
    @Test
    public void testFind() {
        FeatureVector fv = new BasicFeatureVectorImpl();
        fv.add(new NominalFeature("test", "test"));
        fv.add(new PositionAnnotation("ab", 0));
        FeatureUtils.find(new NominalFeature("test", "test"), fv);
        FeatureUtils.find(new PositionAnnotation("ab", 0), fv);
    }
}
