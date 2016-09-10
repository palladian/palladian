package ws.palladian.core.dataset.split;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

public class RandomSplitTest {
	
	@Test
	public void testRandomTestSplit() {
		Dataset data = createRandomData(100);
		RandomSplit split = new RandomSplit(data, 0.75);
		assertEquals(75, split.getTrain().size());
		assertEquals(25, split.getTest().size());
	}
	
	static Dataset createRandomData(int amount) {
		List<Instance> data = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			data.add(new InstanceBuilder().set("index", i).create(i % 2 == 0));
		}
		return new DefaultDataset(data);
	}

}
