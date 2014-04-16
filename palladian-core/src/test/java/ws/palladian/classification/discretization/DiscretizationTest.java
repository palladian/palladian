package ws.palladian.classification.discretization;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.discretization.Binner.Interval;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

public class DiscretizationTest {

    private static final double DELTA = 0.001;

    @Test
    public void testBinner_idealizedData() {
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
        assertFuzzyEquals(asList(4.5, 6.5, 8.5), discretization.getBinner("f").getBoundaries(), DELTA);
        assertEquals(new Interval(NEGATIVE_INFINITY, 4.5), discretization.getBinner("f").getBin(0));
        assertEquals(new Interval(NEGATIVE_INFINITY, 4.5), discretization.getBinner("f").getBin(4.5));
        assertEquals(new Interval(4.5, 6.5), discretization.getBinner("f").getBin(5));
        assertEquals(new Interval(6.5, 8.5), discretization.getBinner("f").getBin(7.5));
        assertEquals(new Interval(8.5, POSITIVE_INFINITY), discretization.getBinner("f").getBin(100));
    }

    @Test
    public void testBinner_wineData() throws FileNotFoundException {
        Iterable<Instance> dataset = new CsvDatasetReader(getResourceFile("/classifier/wineData.csv"), true);
        Discretization discretization = new Discretization(dataset);
        assertFuzzyEquals(asList(12.185, 12.78), discretization.getBinner("alcohol").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(1.42, 2.235), discretization.getBinner("malicAcid").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(2.03), discretization.getBinner("ash").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(17.9), discretization.getBinner("alcalinityOfAsh").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(88.5), discretization.getBinner("magnesium").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(1.84, 2.335), discretization.getBinner("totalPhenols").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(0.975, 1.575, 2.31), discretization.getBinner("flavanoids").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(0.395), discretization.getBinner("nonflavanoidPhenols").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(1.27), discretization.getBinner("proanthocyanins").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(3.46, 7.55), discretization.getBinner("colorIntensity").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(0.785, 0.975, 1.295), discretization.getBinner("hue").getBoundaries(), DELTA);
        assertFuzzyEquals(asList(2.115, 2.475), discretization.getBinner("od280/od315ofDilutedWines").getBoundaries(),
                DELTA);
        assertFuzzyEquals(asList(468., 755., 987.5), discretization.getBinner("proline").getBoundaries(), DELTA);
    }

    private static void assertFuzzyEquals(List<Double> expected, List<Double> actual, double delta) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), delta);
        }
    }

}
