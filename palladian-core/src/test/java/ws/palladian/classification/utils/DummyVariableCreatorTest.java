package ws.palladian.classification.utils;

import org.junit.Test;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.value.NullValue;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DummyVariableCreatorTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testDummyVariableCreator() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset);
        assertEquals(2, dummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, dummyVariableCreator.getCreatedNumericFeatures().size());
        // System.out.println(dummyVariableCreator);

        FeatureVector instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        FeatureVector converted = dummyVariableCreator.convert(instance);
        assertEquals(5, converted.size());
        // assertNull(converted.get("f1"));
        // assertNull(converted.get("f2"));
        assertEquals(0, converted.getNumeric("f1:alpha").getInt());
        assertEquals(1, converted.getNumeric("f1:beta").getInt());
        assertEquals(0, converted.getNumeric("f1:gamma").getInt());
        assertEquals(0, converted.getNumeric("f1:delta").getInt());
        assertEquals(0, converted.getNumeric("f2:true").getInt());
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(1., converted.getNumeric("f2:true").getInt(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).set("f3", false).create();
        converted = dummyVariableCreator.convert(instance);

        // changed behavior, unknown values should not be dropped
        // assertEquals(5, converted.size());
        assertEquals(6, converted.size());
        assertEquals(false, converted.getBoolean("f3").getBoolean());
    }

    @Test
    public void testDummyVariableCreator_sparse() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset, false, false);
        assertEquals(2, dummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, dummyVariableCreator.getCreatedNumericFeatures().size());
        // System.out.println(dummyVariableCreator);

        FeatureVector instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        FeatureVector converted = dummyVariableCreator.convert(instance);
        assertEquals(1, converted.size());
        // assertNull(converted.get("f1"));
        // assertNull(converted.get("f2"));
        assertTrue(converted.get("f1:alpha").isNull());
        assertEquals(1, converted.getNumeric("f1:beta").getInt());
        assertTrue(converted.get("f1:gamma").isNull());
        assertTrue(converted.get("f1:delta").isNull());
        assertTrue(converted.get("f2:true").isNull());
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(1., converted.getNumeric("f2:true").getInt(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).set("f3", false).create();
        converted = dummyVariableCreator.convert(instance);

        // changed behavior, unknown values should not be dropped
        // assertEquals(5, converted.size());
        assertEquals(3, converted.size());
        assertEquals(false, converted.getBoolean("f3").getBoolean());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDummyVariableCreatorKeepOriginalValues() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset, true);
        FeatureVector instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        FeatureVector converted = dummyVariableCreator.apply(instance);
        assertEquals(7, converted.size());
        assertFalse(converted.get("f1").isNull());
        assertFalse(converted.get("f2").isNull());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testNullValueHandling() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset);

        FeatureVector instance = new InstanceBuilder().set("f1", NullValue.NULL).create();
        FeatureVector converted = dummyVariableCreator.convert(instance);
        assertEquals(0, converted.getNumeric("f1:alpha").getInt());
        assertEquals(0, converted.getNumeric("f1:beta").getInt());
        assertEquals(0, converted.getNumeric("f1:gamma").getInt());
        assertEquals(0, converted.getNumeric("f1:delta").getInt());
    }

    private Dataset makeDataset() {
        List<Instance> dataset = new ArrayList<>();
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", true).create(false));
        dataset.add(new InstanceBuilder().set("f1", "beta").set("f2", false).create(false));
        dataset.add(new InstanceBuilder().set("f1", "gamma").set("f2", true).create(false));
        dataset.add(new InstanceBuilder().set("f1", "delta").set("f2", true).create(false));
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", false).create(false));
        dataset.add(new InstanceBuilder().set("f1", "alpha").set("f2", true).create(false));
        return new DefaultDataset(dataset);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSerialization() throws IOException {
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(makeDataset());
        File tempFile = new File(FileHelper.getTempDir(), "dummyVariableCreator.ser");
        FileHelper.serialize(dummyVariableCreator, tempFile.getPath());

        DummyVariableCreator deserializedDummyVariableCreator = FileHelper.deserialize(tempFile.getPath());
        assertEquals(2, deserializedDummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, deserializedDummyVariableCreator.getCreatedNumericFeatures().size());

        assertEquals(dummyVariableCreator.getCreatedNumericFeatures(), deserializedDummyVariableCreator.getCreatedNumericFeatures());

        tempFile.delete();
    }

    @Test
    public void testSerialization_existingFile() throws IOException {
        DummyVariableCreator deserializedDummyVariableCreator = FileHelper.deserialize(ResourceHelper.getResourcePath("/model/dummyVariableCreator_v1.ser"));
        assertEquals(2, deserializedDummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, deserializedDummyVariableCreator.getCreatedNumericFeatures().size());
    }

}
