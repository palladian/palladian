/**
 * Created on: 20.11.2012 08:33:41
 */
package ws.palladian.classification;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

/**
 * <p>
 * Tests whether the {@link FeatureSelector} works correctly or nor.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class FeatureSelectorTest {

    @Test
    public void test() {
        FeatureVector fv1 = new FeatureVector();
        FeatureVector fv2 = new FeatureVector();
        FeatureVector fv3 = new FeatureVector();

        fv1.add(new NominalFeature("testfeature", "a"));
        fv1.add(new NominalFeature("testfeature", "b"));
        fv1.add(new NominalFeature("testfeature", "c"));
        fv1.add(new NominalFeature("testfeature", "a"));
        fv1.add(new NominalFeature("testfeature", "d"));

        fv2.add(new NominalFeature("testfeature", "a"));
        fv2.add(new NominalFeature("testfeature", "b"));
        fv2.add(new NominalFeature("testfeature", "c"));

        fv3.add(new NominalFeature("testfeature", "d"));
        fv3.add(new NominalFeature("testfeature", "e"));
        fv3.add(new NominalFeature("testfeature", "f"));

        Instance instance1 = new Instance("c1", fv1);
        Instance instance2 = new Instance("c1", fv2);
        Instance instance3 = new Instance("c2", fv3);

        Collection<Instance> instances = new HashSet<Instance>();
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);

        Map<String, Map<String, Double>> chiSquareValues = FeatureSelector.calculateChiSquareValues("testfeature",
                NominalFeature.class, instances);
        System.out.println(chiSquareValues);

        // TODO add assertions here.
    }

}
