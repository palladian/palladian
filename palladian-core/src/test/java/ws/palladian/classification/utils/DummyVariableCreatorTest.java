package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.NumericValue;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

public class DummyVariableCreatorTest {

    @Test
    public void testDummyVariableCreator() {
        List<FeatureVector> dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset);
        assertEquals(2, dummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, dummyVariableCreator.getCreatedNumericFeatureCount());
        // System.out.println(dummyVariableCreator);

        FeatureVector instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        FeatureVector converted = dummyVariableCreator.convert(instance);
        assertEquals(5, converted.size());
        assertEquals(0., ((NumericValue)converted.get("f1:alpha")).getDouble(), 0);
        assertEquals(1., ((NumericValue)converted.get("f1:beta")).getDouble(), 0);
        assertEquals(0., ((NumericValue)converted.get("f1:gamma")).getDouble(), 0);
        assertEquals(0., ((NumericValue)converted.get("f1:delta")).getDouble(), 0);
        assertEquals(0., ((NumericValue)converted.get("f2")).getDouble(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(1., ((NumericValue)converted.get("f2")).getDouble(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).set("f3", false).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(5, converted.size());
    }

    private List<FeatureVector> makeDataset() {
        List<FeatureVector> dataset = CollectionHelper.newArrayList();
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", true).create());
        dataset.add(new InstanceBuilder().set("f1", "beta").set("f2", false).create());
        dataset.add(new InstanceBuilder().set("f1", "gamma").set("f2", true).create());
        dataset.add(new InstanceBuilder().set("f1", "delta").set("f2", true).create());
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", false).create());
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", true).create());
        return dataset;
    }

    @Test
    public void testSerialization() throws IOException {
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(makeDataset());
        File tempFile = new File(FileHelper.getTempDir(), "dummyVariableCreator.ser");
        FileHelper.serialize(dummyVariableCreator, tempFile.getPath());

        dummyVariableCreator = FileHelper.deserialize(tempFile.getPath());
        assertEquals(2, dummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, dummyVariableCreator.getCreatedNumericFeatureCount());
        tempFile.delete();
    }

}
