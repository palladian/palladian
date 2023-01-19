package ws.palladian.classification.evaluation;

import org.junit.Test;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.RandomDataset;
import ws.palladian.core.dataset.split.TrainTestSplit;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class IdBasedCrossValidatorTest {
    @Test
    public void testCrossValidator() {
        Dataset data = new RandomDataset(100);
        IdBasedCrossValidator crossValidator = new IdBasedCrossValidator(data, 10, "index");
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
}
