package ws.palladian.classification.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.value.BooleanValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.helper.io.FileHelper;

public class DummyVariableCreatorTest {

    @Test
    public void testDummyVariableCreator() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset);
        assertEquals(2, dummyVariableCreator.getNominalFeatureCount());
        assertEquals(5, dummyVariableCreator.getCreatedNumericFeatureCount());
        // System.out.println(dummyVariableCreator);

        FeatureVector instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        FeatureVector converted = dummyVariableCreator.convert(instance);
        assertEquals(5, converted.size());
        assertTrue(converted.get("f1").isNull());
        assertTrue(converted.get("f2").isNull());
        assertEquals(0, ((NumericValue)converted.get("f1:alpha")).getInt());
        assertEquals(1, ((NumericValue)converted.get("f1:beta")).getInt());
        assertEquals(0, ((NumericValue)converted.get("f1:gamma")).getInt());
        assertEquals(0, ((NumericValue)converted.get("f1:delta")).getInt());
        assertEquals(0, ((NumericValue)converted.get("f2:true")).getInt());
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).create();
        converted = dummyVariableCreator.convert(instance);
        assertEquals(1., ((NumericValue)converted.get("f2:true")).getInt(), 0);
        instance = new InstanceBuilder().set("f1", "beta").set("f2", true).set("f3", false).create();
        converted = dummyVariableCreator.convert(instance);
        
        // changed behavior, unknown values should not be dropped
        // assertEquals(5, converted.size());
        assertEquals(6, converted.size());
        assertEquals(false, ((BooleanValue)converted.get("f3")).getBoolean());
    }
    
    @Test
    public void testDummyVariableCreatorKeepOriginalValues() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset, true);
        FeatureVector instance = new InstanceBuilder().set("f1", "beta").set("f2", false).create();
        FeatureVector converted = dummyVariableCreator.compute(instance);
        assertEquals(7, converted.size());
        assertFalse(converted.get("f1").isNull());
        assertFalse(converted.get("f2").isNull());
    }
    
    @Test
    public void testNullValueHandling() {
        Dataset dataset = makeDataset();
        DummyVariableCreator dummyVariableCreator = new DummyVariableCreator(dataset);

    	FeatureVector instance = new InstanceBuilder().set("f1", NullValue.NULL).create();
    	FeatureVector converted = dummyVariableCreator.convert(instance);
        assertEquals(0, ((NumericValue)converted.get("f1:alpha")).getInt());
        assertEquals(0, ((NumericValue)converted.get("f1:beta")).getInt());
        assertEquals(0, ((NumericValue)converted.get("f1:gamma")).getInt());
        assertEquals(0, ((NumericValue)converted.get("f1:delta")).getInt());
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
