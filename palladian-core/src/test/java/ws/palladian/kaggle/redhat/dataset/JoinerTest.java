package ws.palladian.kaggle.redhat.dataset;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.helper.collection.CollectionHelper;

public class JoinerTest {
	@Test
	public void testJoiner() {
		List<Instance> instances1 = new ArrayList<>();
		instances1.add(new InstanceBuilder().set("join", "a").set("value1", "1").create(true));
		instances1.add(new InstanceBuilder().set("join", "b").set("value1", "2").create(true));
		instances1.add(new InstanceBuilder().set("join", "b").set("value1", "3").create(true));
		instances1.add(new InstanceBuilder().set("join", "c").set("value1", "4").create(true));

		List<Instance> instances2 = new ArrayList<>();
		instances2.add(new InstanceBuilder().set("join", "a").set("value2", "q").create(true));
		instances2.add(new InstanceBuilder().set("join", "a").set("value2", "r").create(true));
		instances2.add(new InstanceBuilder().set("join", "b").set("value2", "s").create(true));
		instances2.add(new InstanceBuilder().set("join", "b").set("value2", "t").create(true));

		DefaultDataset dataset1 = new DefaultDataset(instances1);
		DefaultDataset dataset2 = new DefaultDataset(instances2);
		
		// should give 6 instances, with 3 features
		Dataset joined = Joiner.join(dataset1, dataset2, "join");
		int numResults = CollectionHelper.count(joined.iterator());
		assertEquals(6, numResults);
		assertEquals(3, joined.getFeatureInformation().count());
	}

}
