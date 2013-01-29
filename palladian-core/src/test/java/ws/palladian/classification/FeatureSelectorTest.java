/**
 * Created on: 20.11.2012 08:33:41
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

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
    private List<Instance> fixture;

    @Before
    public void setUp() {
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

        fixture = new ArrayList<Instance>(3);
        fixture.add(instance1);
        fixture.add(instance2);
        fixture.add(instance3);
    }

    @After
    public void tearDown() {
        fixture = null;
    }

    @Test
    public void testChiSquareFeatureSelection() {
        Map<String, Map<String, Double>> chiSquareValues = FeatureSelector.calculateChiSquareValues("testfeature",
                NominalFeature.class, fixture);
        // System.out.println(chiSquareValues);

        Assert.assertThat(chiSquareValues.get("a").get("c1"), Matchers.is(Matchers.closeTo(3.0, 0.0001)));
        Assert.assertThat(chiSquareValues.get("d").get("c2"), Matchers.is(Matchers.closeTo(0.75, 0.0001)));
    }

    @Test
    public void testInformationGainFeatureExtraction() throws Exception {
        Map<NominalFeature, Double> result = FeatureSelector.calculateInformationGain("testfeature",
                NominalFeature.class, fixture);

        Assert.assertThat(result.get(new NominalFeature("testfeature", "d")),
                Matchers.closeTo(0.6759197036979384, 0.001));
        Assert.assertThat(result.get(new NominalFeature("testfeature", "b")),
                Matchers.closeTo(0.9638892693751062, 0.001));
        Assert.assertThat(result.get(new NominalFeature("testfeature", "c")),
                Matchers.closeTo(0.9638892693751062, 0.001));
        Assert.assertThat(result.get(new NominalFeature("testfeature", "a")),
                Matchers.closeTo(0.9638892693751062, 0.001));
        Assert.assertThat(result.get(new NominalFeature("testfeature", "f")),
                Matchers.closeTo(0.9638892693751062, 0.001));
        Assert.assertThat(result.get(new NominalFeature("testfeature", "e")),
                Matchers.closeTo(0.9638892693751062, 0.001));
    }

    @Test
    public void testNumericFeature() throws Exception {
        List<Instance> dataset = new ArrayList<Instance>();
        FeatureVector fV1 = new FeatureVector();
        fV1.add(new NumericFeature("numeric", 1.0d));
        Instance instance1 = new Instance("a", fV1);
        dataset.add(instance1);
        FeatureVector fV2 = new FeatureVector();
        fV2.add(new NumericFeature("numeric", 2.0d));
        Instance instance2 = new Instance("b", fV2);
        dataset.add(instance2);
        FeatureVector fV3 = new FeatureVector();
        fV3.add(new NumericFeature("numeric", 3.0d));
        Instance instance3 = new Instance("a", fV3);
        dataset.add(instance3);

        Map<NumericFeature, Double> result = FeatureSelector.calculateInformationGain("numeric", NumericFeature.class,
                dataset);
        System.out.println(result);
    }
}
