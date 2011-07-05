package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

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
        Assert.assertEquals(4, map.size());
    }
    
    @Test
    public void testGetElementsByType() {
        List<Object> list = new ArrayList<Object>();
        list.add("string");
        list.add(Double.valueOf(1));
        list.add(Float.valueOf(1));
        list.add(Boolean.TRUE);
        list.add(Integer.valueOf(1));
        assertEquals(3, CollectionHelper.getElementsByType(Number.class, list).size());
        assertEquals(1, CollectionHelper.getElementsByType(String.class, list).size());
        assertEquals(0, CollectionHelper.getElementsByType(List.class, list).size());
    }
    
    @Test
    public void removeNullElementsTest() {
        ArrayList<String> array = new ArrayList<String>();
        String temp = null;
        array.add(temp);
        temp = "1";
        array.add(temp);
        temp = "2";
        array.add(temp);
        temp = null;
        array.add(temp);
        temp = "3";
        array.add(temp);
        temp = null;
        array.add(temp);
        temp = "4";
        array.add(temp);
        temp = null;
        array.add(temp);
        array = CollectionHelper.removeNullElements(array);
        assertEquals(4, array.size());
        for (int i = 0; i < array.size(); i++) {
            assertEquals(i + 1, Integer.parseInt(array.get(i)));
        }
    }

}
