package ws.palladian.classification.encode;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.classification.encode.FrequencyEncoder.NullValueStrategy;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;

public class FrequencyEncoderTest {

	private static final double DELTA = 0.001;
	private Dataset dataset;

	@Before
	public void setup() {
		dataset = createTestDataset();
	}

	@After
	public void cleanup() {
		dataset = null;
	}

	@Test
	public void testWithRegularValues() {
		FrequencyEncoder frequencyEncoder = new FrequencyEncoder(dataset);
		Instance testInstance = new InstanceBuilder().set("f1", "a").set("f2", "a").create(true);
		FeatureVector transformedVector = frequencyEncoder.apply(testInstance).getVector();
		assertEquals(3. / 8, transformedVector.getNumeric("f1_frequency").getDouble(), DELTA);
		assertEquals(5. / 8, transformedVector.getNumeric("f2_frequency").getDouble(), DELTA);
	}

	@Test
	public void testWithNullValues_frequency() {
		FrequencyEncoder frequencyEncoder = new FrequencyEncoder(dataset, NullValueStrategy.ASSIGN_FREQUENCY);
		Instance testInstance = new InstanceBuilder().setNull("f1").setNull("f2").create(true);
		FeatureVector transformedVector = frequencyEncoder.apply(testInstance).getVector();
		assertEquals(0, transformedVector.getNumeric("f1_frequency").getDouble(), DELTA);
		assertEquals(3. / 8, transformedVector.getNumeric("f2_frequency").getDouble(), DELTA);
	}

	@Test
	public void testWithNullValues_null() {
		FrequencyEncoder frequencyEncoder = new FrequencyEncoder(dataset, NullValueStrategy.KEEP_NULL);
		Instance testInstance = new InstanceBuilder().setNull("f1").setNull("f2").create(true);
		FeatureVector transformedVector = frequencyEncoder.apply(testInstance).getVector();
		assertTrue(transformedVector.get("f1_frequency").isNull());
		assertTrue(transformedVector.get("f2_frequency").isNull());
	}

	private static final Dataset createTestDataset() {
		List<Instance> instances = new ArrayList<>();
		instances.add(new InstanceBuilder().set("f1", "a").set("f2", "a").create(true));
		instances.add(new InstanceBuilder().set("f1", "a").set("f2", "a").create(true));
		instances.add(new InstanceBuilder().set("f1", "a").set("f2", "a").create(true));
		instances.add(new InstanceBuilder().set("f1", "b").set("f2", "a").create(true));
		instances.add(new InstanceBuilder().set("f1", "b").set("f2", "a").create(true));
		instances.add(new InstanceBuilder().set("f1", "c").setNull("f2").create(true));
		instances.add(new InstanceBuilder().set("f1", "d").setNull("f2").create(true));
		instances.add(new InstanceBuilder().set("f1", "e").setNull("f2").create(true));
		return new DefaultDataset(instances);

	}
}
