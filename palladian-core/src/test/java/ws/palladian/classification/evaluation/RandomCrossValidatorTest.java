package ws.palladian.classification.evaluation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.dataset.split.TrainTestSplit;
import ws.palladian.helper.collection.CollectionHelper;

public class RandomCrossValidatorTest {

	@Test
	public void testCrossValidator() {
		Dataset data = createRandomData(100);
		RandomCrossValidator crossValidator = new RandomCrossValidator(data, 10);
		for (TrainTestSplit fold : crossValidator) {
			int trainSize = CollectionHelper.count(fold.getTrain().iterator());
			int testSize = CollectionHelper.count(fold.getTest().iterator());
			assertEquals(90, trainSize);
			assertEquals(10, testSize);
			
			// test that train and test are disjunct
			Set<Instance> trainSet = CollectionHelper.newHashSet(fold.getTrain());
			Set<Instance> testSet = CollectionHelper.newHashSet(fold.getTest());
			trainSet.removeAll(testSet);
			testSet.removeAll(trainSet);
			assertEquals(90, trainSet.size());
			assertEquals(10, testSet.size());
		}
	}

	static Dataset createRandomData(int amount) {
		List<Instance> data = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			data.add(new InstanceBuilder().set("index", i).create(i % 2 == 0));
		}
		return new DefaultDataset(data);
	}

}
