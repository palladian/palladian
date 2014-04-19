package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class SetSimilaritiesTest {

    private static final double DELTA = 0.001;
    private static final Set<Integer> set1 = CollectionHelper.newHashSet(1, 2, 3, 4);
    private static final Set<Integer> set2 = CollectionHelper.newHashSet(1, 2, 3, 6);
    private static final Set<Integer> set3 = CollectionHelper.newHashSet(1, 2, 3, 4);
    private static final Set<Integer> set4 = CollectionHelper.newHashSet(5, 6, 7, 8);
    private static final Set<Integer> set5 = CollectionHelper.newHashSet(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final Set<Integer> emptySet = Collections.emptySet();

    @Test
    public void testDice() {
        assertEquals(0.75, SetSimilarities.DICE.calculate(set1, set2), DELTA);
        assertEquals(1.0, SetSimilarities.DICE.calculate(set1, set3), DELTA);
        assertEquals(0.0, SetSimilarities.DICE.calculate(set1, set4), DELTA);
        assertEquals(0.5714, SetSimilarities.DICE.calculate(set1, set5), DELTA);
        assertEquals(0.0, SetSimilarities.DICE.calculate(emptySet, emptySet), DELTA);
        assertEquals(0.0, SetSimilarities.DICE.calculate(emptySet, set1), DELTA);
        assertEquals(0.0, SetSimilarities.DICE.calculate(set1, emptySet), DELTA);
    }

    @Test
    public void testJaccard() {
        assertEquals(0.6, SetSimilarities.JACCARD.calculate(set1, set2), DELTA);
        assertEquals(1.0, SetSimilarities.JACCARD.calculate(set1, set3), DELTA);
        assertEquals(0.0, SetSimilarities.JACCARD.calculate(set1, set4), DELTA);
        assertEquals(0.4, SetSimilarities.JACCARD.calculate(set1, set5), DELTA);
        assertEquals(0.0, SetSimilarities.JACCARD.calculate(emptySet, emptySet), DELTA);
        assertEquals(0.0, SetSimilarities.JACCARD.calculate(emptySet, set1), DELTA);
        assertEquals(0.0, SetSimilarities.JACCARD.calculate(set1, emptySet), DELTA);
    }

    @Test
    public void testOverlap() {
        assertEquals(0.75, SetSimilarities.OVERLAP.calculate(set1, set2), DELTA);
        assertEquals(1.0, SetSimilarities.OVERLAP.calculate(set1, set3), DELTA);
        assertEquals(0.0, SetSimilarities.OVERLAP.calculate(set1, set4), DELTA);
        assertEquals(1.0, SetSimilarities.OVERLAP.calculate(set1, set5), DELTA);
        assertEquals(0.0, SetSimilarities.OVERLAP.calculate(emptySet, emptySet), DELTA);
        assertEquals(0.0, SetSimilarities.OVERLAP.calculate(emptySet, set1), DELTA);
        assertEquals(0.0, SetSimilarities.OVERLAP.calculate(set1, emptySet), DELTA);
    }
}
