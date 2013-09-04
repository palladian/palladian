/**
 * Created on: 20.08.2013 22:29:10
 */
package ws.palladian.classification.featureselection;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.Instance;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.NominalFeature;

/**
 * <p>
 * Tests whether information gain is calculated correctly.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.1
 */
public class InformationGainFormulaTest {

    /**
     * Test method for
     * {@link ws.palladian.classification.featureselection.InformationGainFormula#calculateGain(java.util.List, java.lang.String)}
     * .
     */
    @Test
    public void testCalculateGain() {
        List<Trainable> instances = new ArrayList<Trainable>();
        for (int i = 0; i < 9; i++) {
            instances.add(new Instance("true"));
        }

        for (int i = 0; i < 5; i++) {
            instances.add(new Instance("false"));
        }

        instances.get(0).getFeatureVector().add(new NominalFeature("test", "a"));
        instances.get(1).getFeatureVector().add(new NominalFeature("test", "a"));
        instances.get(2).getFeatureVector().add(new NominalFeature("test", "a"));

        instances.get(3).getFeatureVector().add(new NominalFeature("test", "b"));
        instances.get(4).getFeatureVector().add(new NominalFeature("test", "b"));
        instances.get(5).getFeatureVector().add(new NominalFeature("test", "b"));
        instances.get(6).getFeatureVector().add(new NominalFeature("test", "b"));
        instances.get(7).getFeatureVector().add(new NominalFeature("test", "b"));
        instances.get(8).getFeatureVector().add(new NominalFeature("test", "b"));

        instances.get(9).getFeatureVector().add(new NominalFeature("test", "a"));
        instances.get(10).getFeatureVector().add(new NominalFeature("test", "a"));
        instances.get(11).getFeatureVector().add(new NominalFeature("test", "a"));
        instances.get(12).getFeatureVector().add(new NominalFeature("test", "a"));

        instances.get(13).getFeatureVector().add(new NominalFeature("test", "b"));

        double gain = (new InformationGainFormula(instances)).calculateGain("test");

        assertThat(gain, is(closeTo(0.15184, 0.00001)));
    }

}
