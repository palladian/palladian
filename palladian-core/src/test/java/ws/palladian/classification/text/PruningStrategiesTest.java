package ws.palladian.classification.text;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel.PruningStrategy;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.helper.collection.MapBuilder;

public class PruningStrategiesTest {
    @Test
    public void testEntropyPruningStrategy() {
        PruningStrategy pruningStrategy = new PruningStrategies.EntropyPruningStrategy(3, .95);
        Map<String, Integer> entriesMap = MapBuilder.createPut("one", 2).put("two", 8).put("three", 5).create();
        TermCategoryEntries entries = new MapTermCategoryEntries("test", entriesMap);
        assertFalse(pruningStrategy.remove(entries));

        entriesMap = MapBuilder.createPut("one", 5).put("two", 5).put("three", 5).create();
        entries = new MapTermCategoryEntries("test", entriesMap);
        assertTrue(pruningStrategy.remove(entries));

        pruningStrategy = new PruningStrategies.EntropyPruningStrategy(3, 1);
        assertTrue(pruningStrategy.remove(entries));

    }
}
