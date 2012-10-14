package ws.palladian.classification.nominal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

public class NominalClassifierTest {
    
    @Test
    public void testNominalClassifier() {
        NominalClassifier nominalClassifier = new NominalClassifier();

        // create an instance to classify
        List<Instance> trainInstances = CollectionHelper.newArrayList();
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
        
        CategoryEntries categoryEntries = nominalClassifier.classify(new InstanceBuilder().set("f", "f2").create(), model);
        
        CategoryEntry categoryA = categoryEntries.getCategoryEntry("A");
        CategoryEntry categoryB = categoryEntries.getCategoryEntry("B");
        
        assertNotNull(categoryA);
        assertNotNull(categoryB);
        assertEquals(0.4286, categoryA.getProbability(), 0.0001);
        assertEquals(0.5714, categoryB.getProbability(), 0.0001);
    }

}
