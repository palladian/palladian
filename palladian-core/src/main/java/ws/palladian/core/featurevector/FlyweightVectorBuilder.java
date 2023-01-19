package ws.palladian.core.featurevector;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Factory;

import java.util.Objects;

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

    public FlyweightVectorBuilder set(FeatureVector featureVector) {
        Objects.requireNonNull(featureVector, "featureVector was null");
        for (VectorEntry<String, Value> vectorEntry : featureVector) {
            set(vectorEntry.key(), vectorEntry.value());
        }
        return this;
    }

    @Override
    public FeatureVector create() {
        return new FlyweightFeatureVector(schema, values);
    }

}
