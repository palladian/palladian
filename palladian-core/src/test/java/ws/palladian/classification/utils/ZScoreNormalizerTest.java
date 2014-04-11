package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

public class ZScoreNormalizerTest {

    @Test
    public void testZScoreNormalization() {
        List<FeatureVector> features = CollectionHelper.newArrayList();
        features.add(new InstanceBuilder().set("v1", 35.).create());
        features.add(new InstanceBuilder().set("v1", 36.).create());
        features.add(new InstanceBuilder().set("v1", 46.).create());
        features.add(new InstanceBuilder().set("v1", 68.).create());
        features.add(new InstanceBuilder().set("v1", 70.).create());

        Normalization normalization = new ZScoreNormalizer().calculate(features);
        // System.out.println(normalization);
        assertEquals(-0.9412, normalization.normalize("v1", 35.), 0.001);
        assertEquals(-0.8824, normalization.normalize("v1", 36.), 0.001);
        assertEquals(-0.2941, normalization.normalize("v1", 46.), 0.001);
        assertEquals(1, normalization.normalize("v1", 68.), 0.001);
        assertEquals(1.1176, normalization.normalize("v1", 70.), 0.001);
    }

    @Test
    public void testNormalizationWithEqualMinMax() {
        Collection<FeatureVector> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("test", 0.9).create());
        instances.add(new InstanceBuilder().set("test", 0.9).create());
        Normalization normalization = new ZScoreNormalizer().calculate(instances);
        assertEquals(0, normalization.normalize("test", 0.9), 0.001);
        assertEquals(-0.9, normalization.normalize("test", 0), 0.001);
        assertEquals(0.9, normalization.normalize("test", 1.8), 0.001);
    }

}
