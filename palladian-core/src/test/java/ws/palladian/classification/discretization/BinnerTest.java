package ws.palladian.classification.discretization;

import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

public class BinnerTest {

    private static final double DELTA = 0.001;

    @Test
    public void testBinner_cleanData() {
        Collection<Instance> dataset = CollectionHelper.newArrayList();
        dataset.add(new InstanceBuilder().set("f", 1).create("A"));
        dataset.add(new InstanceBuilder().set("f", 2).create("A"));
        dataset.add(new InstanceBuilder().set("f", 3).create("A"));
        dataset.add(new InstanceBuilder().set("f", 4).create("A"));
        dataset.add(new InstanceBuilder().set("f", 5).create("B"));
        dataset.add(new InstanceBuilder().set("f", 6).create("B"));
        dataset.add(new InstanceBuilder().set("f", 7).create("C"));
        dataset.add(new InstanceBuilder().set("f", 8).create("C"));
        dataset.add(new InstanceBuilder().set("f", 9).create("D"));
        dataset.add(new InstanceBuilder().set("f", 10).create("D"));
        Discretization discretization = new Discretization(dataset);
        assertFuzzyEquals(Arrays.asList(4.5, 6.5, 8.5), discretization.getBinner("f").getBoundaries(), DELTA);
    }

    @Test
    public void testBinner_wineData() throws FileNotFoundException {
        Iterable<Instance> dataset = new CsvDatasetReader(getResourceFile("/classifier/wineData.txt"), false);
        Discretization discretization = new Discretization(dataset);
        assertFuzzyEquals(Arrays.asList(12.185, 12.78), discretization.getBinner("0").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(1.42, 2.235), discretization.getBinner("1").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(2.03), discretization.getBinner("2").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(17.9), discretization.getBinner("3").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(88.5), discretization.getBinner("4").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(1.84, 2.335), discretization.getBinner("5").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(0.975, 1.575, 2.31), discretization.getBinner("6").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(0.395), discretization.getBinner("7").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(1.27), discretization.getBinner("8").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(3.46, 7.55), discretization.getBinner("9").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(0.785, 0.975, 1.295), discretization.getBinner("10").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(2.115, 2.475), discretization.getBinner("11").getBoundaries(), DELTA);
        assertFuzzyEquals(Arrays.asList(468., 755., 987.5), discretization.getBinner("12").getBoundaries(), DELTA);
    }

    private static void assertFuzzyEquals(List<Double> expected, List<Double> actual, double delta) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), delta);
        }
    }

}
