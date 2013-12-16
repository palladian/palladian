package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.NumericFeature;

public class DummyVariableCreatorTest {

    @Test
    public void testDummyVariableCreator() {
        List<Classifiable> dataset = CollectionHelper.newArrayList();
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", true).create());
        dataset.add(new InstanceBuilder().set("f1", "beta").set("f2", false).create());
        dataset.add(new InstanceBuilder().set("f1", "gamma").set("f2", true).create());
        dataset.add(new InstanceBuilder().set("f1", "delta").set("f2", true).create());
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", false).create());
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", true).create());
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset);
        assertEquals(2, dummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, dummyVariableCreator.getCreatedNumericFeatureCount());
        // System.out.println(dummyVariableCreator);

        Classifiable instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        Classifiable converted = dummyVariableCreator.convert(instance);
        assertEquals(5, converted.getFeatureVector().size());
        assertEquals(0., converted.getFeatureVector().get(NumericFeature.class, "f1:alpha").getValue(), 0);
        assertEquals(1., converted.getFeatureVector().get(NumericFeature.class, "f1:beta").getValue(), 0);
        assertEquals(0., converted.getFeatureVector().get(NumericFeature.class, "f1:gamma").getValue(), 0);
        assertEquals(0., converted.getFeatureVector().get(NumericFeature.class, "f1:delta").getValue(), 0);
        assertEquals(0., converted.getFeatureVector().get(NumericFeature.class, "f2").getValue(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(1., converted.getFeatureVector().get(NumericFeature.class, "f2").getValue(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).set("f3", false).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(5, converted.getFeatureVector().size());
    }

}
