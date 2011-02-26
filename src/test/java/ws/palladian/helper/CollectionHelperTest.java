package ws.palladian.helper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Philipp Katz
 */
public class CollectionHelperTest {

    @Test
    public void testListToMap() {
        List<Double> list = Arrays.asList(1., 1., 1., 2., 1., 3., 4., 2., 3.);
        Map<Double, Integer> map = CollectionHelper.toMap(list);
        Assert.assertEquals(4, (int) map.get(1.));
        Assert.assertEquals(2, (int) map.get(2.));
        Assert.assertEquals(2, (int) map.get(3.));
        Assert.assertEquals(1, (int) map.get(4.));
        Assert.assertEquals(4, (int) map.size());
    }

}
