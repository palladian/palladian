package ws.palladian.classification.utils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.NumericValue;
import ws.palladian.helper.collection.CollectionHelper;

public class MinMaxNormalizerTest {

    @Test
    public void testMinMaxNormalization() {
        List<FeatureVector> vectors = CollectionHelper.newArrayList();
        FeatureVector fv1 = new InstanceBuilder().set("v1", 50.).set("v2", 1000.).create();
        FeatureVector fv2 = new InstanceBuilder().set("v1", 10.).set("v2", 10000.).create();
        FeatureVector fv3 = new InstanceBuilder().set("v1", 5.).set("v2", 10.).create();
        vectors.add(fv1);
        vectors.add(fv2);
        vectors.add(fv3);

        Normalization normalization = new MinMaxNormalizer().calculate(vectors);
        fv1 = normalization.normalize(fv1);
        fv2 = normalization.normalize(fv2);
        fv3 = normalization.normalize(fv3);

        assertEquals(1., ((NumericValue)fv1.get("v1")).getDouble(), 0.);
        assertEquals(0.1111, ((NumericValue)fv2.get("v1")).getDouble(), 0.001);
        assertEquals(0., ((NumericValue)fv3.get("v1")).getDouble(), 0.);

        assertEquals(0.0999, ((NumericValue)fv1.get("v2")).getDouble(), 0.001);
        assertEquals(1, ((NumericValue)fv2.get("v2")).getDouble(), 0.001);
        assertEquals(0, ((NumericValue)fv3.get("v2")).getDouble(), 0.001);

    }

    @Test
    public void testNormalizationWithEqualMinMax() throws Exception {
        Collection<FeatureVector> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("test", 0.9d).create());
        instances.add(new InstanceBuilder().set("test", 0.9d).create());
        Normalization normalization = new MinMaxNormalizer().calculate(instances);

        double result = normalization.normalize("test", 5.0d);
        assertThat(result, is(4.1));
    }

}
