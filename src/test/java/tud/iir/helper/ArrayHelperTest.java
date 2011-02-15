package tud.iir.helper;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

public class ArrayHelperTest {
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
        array = ArrayHelper.removeNullElements(array);
        assertEquals(4, array.size());
        for (int i = 0; i < array.size(); i++) {
            assertEquals(i + 1, Integer.parseInt(array.get(i)));
        }
    }
}
