package ws.palladian.utils;

import org.junit.Test;
import ws.palladian.utils.MultilabelEvaluator.Result;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.collection.CollectionHelper.newHashSet;

public class MultilabelEvaluatorTest {

    private static final double DELTA = 0.001;

    @Test
    public void testMultilabelEvaluator() {
        Set<Integer> actual = newHashSet(1, 2, 3);
        Set<Integer> assigned = newHashSet(1, 2, 4, 5);
        Result result = new MultilabelEvaluator().add(actual, assigned);
        assertEquals(0.5, result.getPrecision(), DELTA);
        assertEquals(0.6666666667, result.getRecall(), DELTA);
        assertEquals(0.5714285714, result.getF1(), DELTA);

        // edge cases

        actual = emptySet();
        assigned = emptySet();
        result = new MultilabelEvaluator().add(actual, assigned);
        assertEquals(1, result.getF1(), DELTA);

        actual = newHashSet(1);
        assigned = emptySet();
        result = new MultilabelEvaluator().add(actual, assigned);
        assertEquals(1, result.getPrecision(), DELTA);
        assertEquals(0, result.getRecall(), DELTA);
        assertEquals(0, result.getF1(), DELTA);

        actual = emptySet();
        assigned = newHashSet(1);
        result = new MultilabelEvaluator().add(actual, assigned);
        assertEquals(0, result.getPrecision(), DELTA);
        assertEquals(1, result.getRecall(), DELTA);
        assertEquals(0, result.getF1(), DELTA);
    }

}
