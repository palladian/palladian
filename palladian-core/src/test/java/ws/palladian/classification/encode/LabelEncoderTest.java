package ws.palladian.classification.encode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

public class LabelEncoderTest {
	@Test
	public void testLabelEncoder() {
		List<Instance> instances = new ArrayList<>();
		instances.add(new InstanceBuilder().set("value", "a").create("dummy"));
		instances.add(new InstanceBuilder().set("value", "z").create("dummy"));
		instances.add(new InstanceBuilder().set("value", "b").create("dummy"));
		Dataset dataset = new DefaultDataset(instances);
		LabelEncoder labelEncoder = new LabelEncoder(dataset);

		Instance transformedInstance = labelEncoder.apply(new InstanceBuilder().set("value", "a").create("dummy"));
		assertEquals(0, transformedInstance.getVector().getNumeric("value_labelEncoded").getInt());

		Instance transformedInstance2 = labelEncoder.apply(new InstanceBuilder().set("value", "b").create("dummy"));
		assertEquals(1, transformedInstance2.getVector().getNumeric("value_labelEncoded").getInt());

		Instance transformedInstance3 = labelEncoder.apply(new InstanceBuilder().set("value", "z").create("dummy"));
		assertEquals(2, transformedInstance3.getVector().getNumeric("value_labelEncoded").getInt());

		Instance transformedInstance4 = labelEncoder.apply(new InstanceBuilder().set("value", "x").create("dummy"));
		assertTrue(transformedInstance4.getVector().get("value_labelEncoded").isNull());
	}
}
