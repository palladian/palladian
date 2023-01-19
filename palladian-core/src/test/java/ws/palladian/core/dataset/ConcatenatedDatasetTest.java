package ws.palladian.core.dataset;

import org.junit.Test;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ConcatenatedDatasetTest {
    @Test
    public void testConcatenatedDataset() {
        Dataset d1 = createTestDataset(10, "dataset_1");
        Dataset d2 = createTestDataset(5, "dataset_2");
        Dataset d3 = createTestDataset(15, "dataset_3");

        ConcatenatedDataset concatenatedDataset = new ConcatenatedDataset(d1, d2, d3);
        assertEquals(30, concatenatedDataset.size());
        assertEquals(30, CollectionHelper.count(concatenatedDataset.iterator()));

        // CollectionHelper.print(concatenatedDataset);

    }

    private static Dataset createTestDataset(int numItems, String prefix) {
        Collection<Instance> instances = new ArrayList<>();
        for (int i = 0; i < numItems; i++) {
            InstanceBuilder builder = new InstanceBuilder();
            builder.set("item", prefix + "_item_" + i);
            instances.add(builder.create(true));
        }
        return new DefaultDataset(instances);
    }
}
