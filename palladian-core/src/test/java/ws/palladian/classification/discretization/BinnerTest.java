package ws.palladian.classification.discretization;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;

public class BinnerTest {

    @Test
    public void testBinner_cleanData() {
        Collection<Instance> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("f", 1).create("A"));
        instances.add(new InstanceBuilder().set("f", 2).create("A"));
        instances.add(new InstanceBuilder().set("f", 3).create("A"));
        instances.add(new InstanceBuilder().set("f", 4).create("A"));
        instances.add(new InstanceBuilder().set("f", 5).create("B"));
        instances.add(new InstanceBuilder().set("f", 6).create("B"));
        instances.add(new InstanceBuilder().set("f", 7).create("C"));
        instances.add(new InstanceBuilder().set("f", 8).create("C"));
        instances.add(new InstanceBuilder().set("f", 9).create("D"));
        instances.add(new InstanceBuilder().set("f", 10).create("D"));
        Binner binner = new Binner(instances, "f");
        assertEquals(3, binner.getNumBoundaryPoints());
        assertEquals(0, binner.bin(0));
        assertEquals(0, binner.bin(3));
        assertEquals(1, binner.bin(5.5));
        assertEquals(1, binner.bin(5.5));
        assertEquals(2, binner.bin(7.5));
        assertEquals(3, binner.bin(9.5));
        assertEquals(3, binner.bin(15));
    }

    @Test
    public void testBinner_wineDataSubset() {
        Collection<Instance> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("f", 12.88).create("3"));
        instances.add(new InstanceBuilder().set("f", 12.85).create("3"));
        instances.add(new InstanceBuilder().set("f", 12.87).create("3"));
        instances.add(new InstanceBuilder().set("f", 12.86).create("3"));
        instances.add(new InstanceBuilder().set("f", 13.05).create("1"));
        instances.add(new InstanceBuilder().set("f", 13.05).create("1"));
        instances.add(new InstanceBuilder().set("f", 12.96).create("3"));
        instances.add(new InstanceBuilder().set("f", 13.05).create("2"));
        instances.add(new InstanceBuilder().set("f", 13.05).create("1"));
        instances.add(new InstanceBuilder().set("f", 12.93).create("3"));
        instances.add(new InstanceBuilder().set("f", 13.05).create("1"));
        instances.add(new InstanceBuilder().set("f", 12.93).create("1"));
        instances.add(new InstanceBuilder().set("f", 13.03).create("2"));
        instances.add(new InstanceBuilder().set("f", 13.05).create("2"));
        instances.add(new InstanceBuilder().set("f", 12.99).create("2"));
        Binner binner = new Binner(instances, "f");
        assertEquals(2, binner.getNumBoundaryPoints());
        assertEquals(0, binner.bin(12.5));
        assertEquals(0, binner.bin(12.85));
        assertEquals(1, binner.bin(12.93));
        assertEquals(2, binner.bin(12.99));
        assertEquals(2, binner.bin(13));
    }

    @Test
    public void testBinner_empty() {
        Collection<Instance> instances = Collections.emptyList();
        Binner binner = new Binner(instances, "f");
        assertEquals(0, binner.getNumBoundaryPoints());
    }

}
