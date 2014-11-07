package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.NumericFeature;

public class ZScoreNormalizerTest {

    @Test
    public void testZScoreNormalization() {
        List<Classifiable> features = CollectionHelper.newArrayList();
        features.add(new InstanceBuilder().set("v1", 35.).create());
        features.add(new InstanceBuilder().set("v1", 36.).create());
        features.add(new InstanceBuilder().set("v1", 46.).create());
        features.add(new InstanceBuilder().set("v1", 68.).create());
        features.add(new InstanceBuilder().set("v1", 70.).create());

        Normalization normalization = new ZScoreNormalizer().calculate(features);
        // System.out.println(normalization);
        assertEquals(-0.9412, normalization.normalize(new NumericFeature("v1", 35.)).getValue(), 0.001);
        assertEquals(-0.8824, normalization.normalize(new NumericFeature("v1", 36.)).getValue(), 0.001);
        assertEquals(-0.2941, normalization.normalize(new NumericFeature("v1", 46.)).getValue(), 0.001);
        assertEquals(1, normalization.normalize(new NumericFeature("v1", 68.)).getValue(), 0.001);
        assertEquals(1.1176, normalization.normalize(new NumericFeature("v1", 70.)).getValue(), 0.001);
    }

    @Test
    public void testNormalizationWithEqualMinMax() {
        Collection<Classifiable> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("test", 0.9).create());
        instances.add(new InstanceBuilder().set("test", 0.9).create());
        Normalization normalization = new ZScoreNormalizer().calculate(instances);
        assertEquals(0, normalization.normalize(new NumericFeature("test", 0.9)).getValue(), 0.001);
        assertEquals(-0.9, normalization.normalize(new NumericFeature("test", 0)).getValue(), 0.001);
        assertEquals(0.9, normalization.normalize(new NumericFeature("test", 1.8)).getValue(), 0.001);
    }

}
