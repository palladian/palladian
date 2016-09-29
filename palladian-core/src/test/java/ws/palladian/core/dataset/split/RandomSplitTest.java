package ws.palladian.core.dataset.split;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.RandomDataset;

public class RandomSplitTest {
	
	@Test
	public void testRandomTestSplit() {
		Dataset data = new RandomDataset(100);
		RandomSplit split = new RandomSplit(data, 0.75);
		assertEquals(75, split.getTrain().size());
		assertEquals(25, split.getTest().size());
	}

}
