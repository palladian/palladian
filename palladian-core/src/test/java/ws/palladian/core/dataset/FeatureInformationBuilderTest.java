package ws.palladian.core.dataset;

import org.junit.Test;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.ImmutableStringValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.collection.CollectionHelper.newHashSet;

public class FeatureInformationBuilderTest {

    @Test
    public void testBuildFromDataset() {
        List<Instance> instances = new ArrayList<>();
        instances.add(new InstanceBuilder().set("a", "a").set("b", 1).set("c", 1.).set("d", true).create(true));
        instances.add(new InstanceBuilder().set("a", "b").set("b", 5).set("c", 2.).set("e", false).create(true));
        FeatureInformation featureInformation = FeatureInformationBuilder.fromInstances(instances).create();
        assertEquals(5, featureInformation.count());
        assertEquals(newHashSet("a"), featureInformation.getFeatureNamesOfType(ImmutableStringValue.class));
        assertEquals(newHashSet("b"), featureInformation.getFeatureNamesOfType(ImmutableIntegerValue.class));
        assertEquals(newHashSet("c"), featureInformation.getFeatureNamesOfType(ImmutableDoubleValue.class));
        assertEquals(newHashSet("d", "e"), featureInformation.getFeatureNamesOfType(ImmutableBooleanValue.class));
        // System.out.println(featureInformation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildFromDataset_incompatibleTypes() {
        List<Instance> instances = new ArrayList<>();
        instances.add(new InstanceBuilder().set("a", "a").create(true));
        instances.add(new InstanceBuilder().set("a", 1).create(true));
        FeatureInformationBuilder.fromInstances(instances).create();
        // System.out.println(featureInformation);
    }

    @Test
    public void testBuildFromDataset_NullValues() {
        List<Instance> instances = new ArrayList<>();
        instances.add(new InstanceBuilder().setNull("a").set("b", 1).create(true));
        instances.add(new InstanceBuilder().set("a", 1).setNull("b").create(true));
        FeatureInformation featureInformation = FeatureInformationBuilder.fromInstances(instances).create();
        // System.out.println(featureInformation);
        assertEquals(2, featureInformation.count());
        assertEquals(newHashSet("a", "b"), featureInformation.getFeatureNamesOfType(ImmutableIntegerValue.class));
    }

}
