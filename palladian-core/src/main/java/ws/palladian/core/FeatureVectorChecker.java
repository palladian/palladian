package ws.palladian.core;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

public class FeatureVectorChecker {

	public static List<String> checkForNaNOrInfinite(FeatureVector vector) {
		List<String> output = new ArrayList<>();
		for (VectorEntry<String, Value> entry : vector) {
			if (entry.value() instanceof NumericValue) {
				NumericValue numericValue = (NumericValue) entry.value();
				double doubleValue = numericValue.getDouble();
				if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
					output.add(entry.key() + "=" + entry.value());
				}
			}
		}
		if (output.size() > 0) {
			System.out.println(output);
		}
		return output;
	}
	
	public static void main(String[] args) {
		float nan = Float.NaN;
		int nanInt = (int) nan;
		System.out.println(nanInt);
		
		double nanDouble = nan;
		System.out.println(nanDouble);
	}

}
