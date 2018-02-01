package ws.palladian.classification.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ValueDefinitions;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public class LibSvmDatasetReader extends AbstractDataset {
	private static final class DatasetEntry {
		final double target;
		final int[] featureIndices;
		final double[] featureValues;

		DatasetEntry(double target, int[] featureIndices, double[] featureValues) {
			this.target = target;
			this.featureIndices = featureIndices;
			this.featureValues = featureValues;
		}
	}

	private final List<DatasetEntry> entries;
	private final FeatureInformation featureInformation;

	public LibSvmDatasetReader(File file) {
		final List<DatasetEntry> temp = new ArrayList<>();
		// read the data
		FileHelper.performActionOnEveryLine(file, new LineAction() {
			@Override
			public void performAction(String line, int lineNumber) {
				String[] split = line.split(" ");
				double target = 0;
				int[] featureIndices = new int[split.length - 1];
				double[] featureValues = new double[split.length - 1];
				for (int i = 0; i < split.length; i++) {
					String item = split[i];
					if (i == 0) {
						target = Integer.parseInt(item);
					} else {
						String[] itemSplit = item.split(":");
						featureIndices[i - 1] = Integer.parseInt(itemSplit[0]);
						featureValues[i - 1] = Double.parseDouble(itemSplit[1]);

					}
				}
				temp.add(new DatasetEntry(target, featureIndices, featureValues));
			}
		});
		
		// XXX remove me
		Collections.shuffle(temp);
		
		this.entries = Collections.unmodifiableList(temp);

		// determine the maximum index
		int maxFeatureIndex = 0;
		for (DatasetEntry entry : entries) {
			for (int featureIndex : entry.featureIndices) {
				maxFeatureIndex = Math.max(maxFeatureIndex, featureIndex);
			}
		}

		// build the feature information
		FeatureInformationBuilder featureInfoBuilder = new FeatureInformationBuilder();
		for (int i = 1; i <= maxFeatureIndex; i++) {
			featureInfoBuilder.set("" + i, ValueDefinitions.doubleValue());
		}
		this.featureInformation = featureInfoBuilder.create();

	}

	@Override
	public CloseableIterator<Instance> iterator() {
		final Iterator<DatasetEntry> iterator = entries.iterator();
		return new CloseableIteratorAdapter<>(new AbstractIterator2<Instance>() {
			@Override
			protected Instance getNext() {
				if (iterator.hasNext()) {
					DatasetEntry entry = iterator.next();
					InstanceBuilder instanceBuilder = new InstanceBuilder();
					for (int i = 0; i < entry.featureIndices.length; i++) {
						instanceBuilder.set("" + entry.featureIndices[i], entry.featureValues[i]);
					}
					return instanceBuilder.create("" + entry.target);
				}
				return finished();
			}
		});
	}

	@Override
	public FeatureInformation getFeatureInformation() {
		return featureInformation;
	}

	@Override
	public long size() {
		return entries.size();
	}

}
