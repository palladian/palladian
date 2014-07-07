package ws.palladian.classification.text;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.text.DictionaryModel.PruningStrategy;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;

public class PruningStrategiesTest {
    @Test
    public void testEntropyPruningStrategy() {
        PruningStrategy pruningStrategy = new PruningStrategies.EntropyPruningStrategy(3, .95);
        TermCategoryEntries entries = new ImmutableTermCategoryEntries("test", new CategoryEntriesBuilder()
                .set("one", 2).set("two", 8).set("three", 5).create());
        assertFalse(pruningStrategy.remove(entries));

        entries = new ImmutableTermCategoryEntries("test", new CategoryEntriesBuilder().set("one", 5).set("two", 5)
                .set("three", 5).create());
        assertTrue(pruningStrategy.remove(entries));

        pruningStrategy = new PruningStrategies.EntropyPruningStrategy(3, 1);
        assertTrue(pruningStrategy.remove(entries));
    }
}
