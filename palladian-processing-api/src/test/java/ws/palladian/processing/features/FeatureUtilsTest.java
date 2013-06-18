/**
 * Created on: 30.12.2012 11:41:57
 */
package ws.palladian.processing.features;

import org.junit.Test;

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
     * {@link ws.palladian.processing.features.FeatureUtils#find(ws.palladian.processing.features.Feature, ws.palladian.processing.features.FeatureVector)}
     * .
     */
    @Test
    public void testFind() {
        FeatureVector fv = new FeatureVector();
        fv.add(new NominalFeature("test", "test"));
        fv.add(new PositionAnnotation("pos", 0, 2, "ab"));
        FeatureUtils.find(new NominalFeature("test", "test"), fv);
        FeatureUtils.find(new PositionAnnotation("pos", 0, 2, "ab"), fv);
    }
}
