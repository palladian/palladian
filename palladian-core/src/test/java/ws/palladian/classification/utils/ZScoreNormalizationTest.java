package ws.palladian.classification.utils;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.NumericFeature;

public class ZScoreNormalizationTest {

    @Test
    public void testZScoreNormalization() {
        List<Classifiable> Features = CollectionHelper.newArrayList();
        Features.add(new InstanceBuilder().set("v1", 35.).create());
        Features.add(new InstanceBuilder().set("v1", 36.).create());
        Features.add(new InstanceBuilder().set("v1", 46.).create());
        Features.add(new InstanceBuilder().set("v1", 68.).create());
        Features.add(new InstanceBuilder().set("v1", 70.).create());

        ZScoreNormalization normalization = new ZScoreNormalization(Features);
        // System.out.println(normalization);
        assertEquals(-0.9412, normalization.normalize(new NumericFeature("v1", 35.)).getValue(), 0.001);
        assertEquals(-0.8824, normalization.normalize(new NumericFeature("v1", 36.)).getValue(), 0.001);
        assertEquals(-0.2941, normalization.normalize(new NumericFeature("v1", 46.)).getValue(), 0.001);
        assertEquals(1, normalization.normalize(new NumericFeature("v1", 68.)).getValue(), 0.001);
        assertEquals(1.1176, normalization.normalize(new NumericFeature("v1", 70.)).getValue(), 0.001);
    }

}
