package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class NumericVectorTest {

    @Test
    public void testCosineSimMetric() {

        Map<String, Double> temp = CollectionHelper.newHashMap();
        temp.put("cow", 3.);
        temp.put("pig", 2.);
        temp.put("dog", 0.);
        temp.put("cat", 5.);
        temp.put("log", 0.);
        temp.put("bug", 0.);
        temp.put("fox", 0.);
        temp.put("ape", 2.);
        temp.put("man", 0.);
        temp.put("car", 0.);
        NumericVector<String> vector1 = new ImmutableNumericVector<String>(temp);

        temp = CollectionHelper.newHashMap();
        temp.put("cow", 1.);
        temp.put("pig", 2.);
        temp.put("dog", 0.);
        temp.put("cat", 0.);
        // temp.put("log", 0.);
        // temp.put("bug", 0.);
        temp.put("fox", 0.);
        temp.put("ape", 1.);
        temp.put("man", 0.);
        temp.put("car", 2.);
        NumericVector<String> vector2 = new ImmutableNumericVector<String>(temp);

        temp = CollectionHelper.newHashMap();
        NumericVector<String> vector3 = new ImmutableNumericVector<String>(temp);

        assertEquals(12, vector1.sum(), 0.01);
        assertEquals(6, vector2.sum(), 0.01);
        assertEquals(6.48, vector1.norm(), 0.01);
        assertEquals(3.16, vector2.norm(), 0.01);
        assertEquals(9, vector1.dot(vector2), 0.01);
        assertEquals(0.44, vector1.cosine(vector2), 0.01);
        assertEquals(0, vector1.cosine(vector3), 0.01);
        assertEquals(5.83, vector1.euclidean(vector2), 0.01);
        
        NumericVector<String> addedVector = vector1.add(vector2);
        assertEquals(4, addedVector.get("cow"), 0.01);
        assertEquals(4, addedVector.get("pig"), 0.01);
        assertEquals(0, addedVector.get("dog"), 0.01);
    }

}
