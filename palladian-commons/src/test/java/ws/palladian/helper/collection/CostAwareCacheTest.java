package ws.palladian.helper.collection;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class CostAwareCacheTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testCache() {
        int cacheSize = 3;
        CostAwareCache<String, Integer> costAwareCache = new CostAwareCache<>(cacheSize);
        CostEntry<String, Integer> entry;
        entry = new CostEntry<>("a", 1, 2);
        costAwareCache.tryAdd(entry);
        entry = new CostEntry<>("b", 2, 3);
        costAwareCache.tryAdd(entry);
        entry = new CostEntry<>("c", 3, 1);
        costAwareCache.tryAdd(entry);
        entry = new CostEntry<>("d", 4, 0);
        costAwareCache.tryAdd(entry);
        entry = new CostEntry<>("e", 5, 10);
        costAwareCache.tryAdd(entry);

        collector.checkThat(costAwareCache.size(), Matchers.is(cacheSize));
        collector.checkThat(costAwareCache.getFirst().getKey(), Matchers.is("a"));
        collector.checkThat(costAwareCache.getLast().getKey(), Matchers.is("e"));

        collector.checkThat(costAwareCache.tryGet("a"), Matchers.is(1));
        collector.checkThat(costAwareCache.tryGet("d"), Matchers.nullValue());
    }
}