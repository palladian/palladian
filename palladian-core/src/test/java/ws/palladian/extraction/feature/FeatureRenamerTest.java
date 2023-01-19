package ws.palladian.extraction.feature;

import org.junit.Test;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class FeatureRenamerTest {
    @Test
    public void testFeatureRenamer() {
        Instance instance = new InstanceBuilder().set("a", 1).set("b", 2).create(true);

        Dataset dataset = new DefaultDataset(Arrays.asList(instance));
        dataset = dataset.transform(new FeatureRenamer("^.*$", "renamed_$0"));

        Instance transformedInstance = dataset.iterator().next();

        Iterator<VectorEntry<String, Value>> iterator = transformedInstance.getVector().iterator();
        assertEquals(1, ((NumericValue) iterator.next().value()).getInt());
        assertEquals(2, ((NumericValue) iterator.next().value()).getInt());

        assertEquals(1, transformedInstance.getVector().getNumeric("renamed_a").getInt());
        assertEquals(2, transformedInstance.getVector().getNumeric("renamed_b").getInt());
    }

}
