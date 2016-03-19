package ws.palladian.utils;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.Instance;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class CsvDatasetWriter implements Closeable {
	private final File outputCsv;
	private Set<String> expectedFeatures;

	public CsvDatasetWriter(File outputCsv) {
		this.outputCsv = Objects.requireNonNull(outputCsv, "outputCsv must not be null");
		if (outputCsv.exists()) {
			throw new IllegalArgumentException(outputCsv + " already exists");
		}
	}
	
	public void append(Instance instance) {
		if (expectedFeatures == null) {
			expectedFeatures = instance.getVector().keys();
		}
		if (!expectedFeatures.equals(instance.getVector().keys())) {
			throw new IllegalArgumentException("The given vector names are different from the initial vector names.");
		}
		ClassificationUtils.appendCsv(instance, outputCsv);
	}

	@Override
	public void close() throws IOException {
		// nothing here yet
	}

}
