package ws.palladian.core.featurevector;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.Value;
import ws.palladian.helper.functional.Factory;

public class FlyweightVectorBuilder implements Factory<FeatureVector> {
	
	private final FlyweightVectorSchema schema;
	private Value[] values;

	FlyweightVectorBuilder(FlyweightVectorSchema schema) {
		this.schema = schema;
		this.values = new Value[schema.size()];
	}
	
	public FlyweightVectorBuilder set(String name, Value value) {
		schema.set(name, value, values);
		return this;
	}

	@Override
	public FeatureVector create() {
		return new FlyweightFeatureVector(schema, values);
	}

}
