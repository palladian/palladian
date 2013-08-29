package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.Instance;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.NumericFeature;

public class MinMaxNormalizationTest {

    @Test
    public void testMinMaxNormalization() {
        List<Instance> instances = CollectionHelper.newArrayList();
        Instance instance1 = new InstanceBuilder().set("v1", 50.).set("v2", 1000.).create("test");
        Instance instance2 = new InstanceBuilder().set("v1", 10.).set("v2", 10000.).create("test");
        Instance instance3 = new InstanceBuilder().set("v1", 5.).set("v2", 10.).create("test");
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);

        MinMaxNormalization minMaxNormalize = new MinMaxNormalization(instances);

        minMaxNormalize.normalize(instances);

        assertEquals(1., instance1.getFeatureVector().get(NumericFeature.class, "v1").getValue(), 0.);
        assertEquals(0.1111, instance2.getFeatureVector().get(NumericFeature.class, "v1").getValue(), 0.001);
        assertEquals(0., instance3.getFeatureVector().get(NumericFeature.class, "v1").getValue(), 0.);

        assertEquals(0.0999, instance1.getFeatureVector().get(NumericFeature.class, "v2").getValue(), 0.001);
        assertEquals(1, instance2.getFeatureVector().get(NumericFeature.class, "v2").getValue(), 0.001);
        assertEquals(0, instance3.getFeatureVector().get(NumericFeature.class, "v2").getValue(), 0.001);

    }

}
