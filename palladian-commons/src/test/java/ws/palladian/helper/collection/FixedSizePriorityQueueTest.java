package ws.palladian.helper.collection;

import java.util.Comparator;

import org.junit.Assert;
import org.junit.Test;

public class FixedSizePriorityQueueTest {

	@Test
	public void testFixedSizePriorityQueue() {
		FixedSizePriorityQueue<Integer> fixedSizePriorityQueue = new FixedSizePriorityQueue<>(3,
				new Comparator<Integer>() {
					@Override
					public int compare(Integer o1, Integer o2) {
						return o1.compareTo(o2);
					}
				});
		fixedSizePriorityQueue.add(1);
		fixedSizePriorityQueue.add(7);
		fixedSizePriorityQueue.add(2);
		fixedSizePriorityQueue.add(8);
		fixedSizePriorityQueue.add(0);
		fixedSizePriorityQueue.add(9);
		fixedSizePriorityQueue.add(4);
		fixedSizePriorityQueue.add(5);
		fixedSizePriorityQueue.add(3);
		fixedSizePriorityQueue.add(6);
		Assert.assertArrayEquals(new Integer[] { 7, 8, 9 }, fixedSizePriorityQueue.asList().toArray(new Integer[0]));
	}

}
