package ws.palladian.classification.nominal;

import org.junit.Test;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NominalClassifierTest {

    @Test
    public void testNominalClassifier() {
        NominalClassifier nominalClassifier = new NominalClassifier();

        // create an instance to classify
        List<Instance> trainInstances = new ArrayList<>();
        trainInstances.add(new InstanceBuilder().set("f", "f1").create("A"));
        trainInstances.add(new InstanceBuilder().set("f", "f1").create("B"));
        trainInstances.add(new InstanceBuilder().set("f", "f1").create("B"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("A"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("A"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("A"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("B"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("B"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("B"));
        trainInstances.add(new InstanceBuilder().set("f", "f2").create("B"));

        NominalClassifierModel model = nominalClassifier.train(trainInstances);
        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("A"));
        assertTrue(model.getCategories().contains("B"));

        CategoryEntries result = nominalClassifier.classify(new InstanceBuilder().set("f", "f2").create(), model);

        assertEquals(0.4286, result.getProbability("A"), 0.0001);
        assertEquals(0.5714, result.getProbability("B"), 0.0001);
    }

}
