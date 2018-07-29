package ws.palladian.core.dataset;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.csv.CsvDatasetWriter;
import ws.palladian.core.value.Value;
import ws.palladian.extraction.feature.FeatureRenamer;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.io.CloseableIterator;

public final class DatasetJoiner {

	/**
	 * Join the columns of two datasets on the given <tt>joinColumn</tt>. The
	 * category of the second dataset will be used (first dataset's category is
	 * ignored).
	 * 
	 * @param d1
	 *            First dataset, not <code>null</code>.
	 * @param d2
	 *            Second dataset, not <code>null</code>.
	 * @param joinColumn
	 *            Name of the column which is used for joining.
	 * @param result
	 *            The path to the result file.
	 * @return 
	 * @throws IOException
	 *             In case anything I/O goes nuts.
	 */
	public static Dataset join(Dataset d1, Dataset d2, String joinColumn) {
		Validate.notNull(d1, "d1 must not be null");
		Validate.notNull(d2, "d2 must not be null");
		Validate.notEmpty(joinColumn, "joinColumn must not be null");

		ProgressReporter progress = new ProgressMonitor();
		progress.startTask("Joining datasets", -1);

		ProgressReporter p1 = progress.createSubProgress(.5);
		p1.startTask("Reading d1", d1.size());

		MultiMap<Value, FeatureVector> d1vectors = DefaultMultiMap.createWithList();
		for (Instance i1 : d1) {
			Value key = i1.getVector().get(joinColumn);
			d1vectors.add(key, i1.getVector());
			p1.increment();
		}

		ProgressReporter p2 = progress.createSubProgress(.5);
		p2.startTask("Reading d2", d2.size());
		
		return new AbstractDataset() {
			@Override
			public long size() {
				return -1;
			}
			@Override
			public CloseableIterator<Instance> iterator() {
				return new CloseableIterator<Instance>() {
					CloseableIterator<Instance> i2 = d2.iterator();
					Queue<FeatureVector> vectorsToJoin = new LinkedList<>();
					Instance instance2;

					@Override
					public boolean hasNext() {
						return vectorsToJoin.size() > 0 || i2.hasNext();
					}
					@Override
					public Instance next() {
						FeatureVector vector1 = vectorsToJoin.poll();
						if (vector1 != null) {
							return join(vector1, instance2);
						}
						for (;;) {
							instance2 = i2.next();
							vectorsToJoin.addAll(d1vectors.get(instance2.getVector().get(joinColumn)));
							vector1 = vectorsToJoin.poll();
							if (vector1 != null) {
								return join(vector1, instance2);
							}
						}
					}
					@Override
					public void close() throws IOException {
						i2.close();
					}
				};
			}
			@Override
			public FeatureInformation getFeatureInformation() {
				FeatureInformationBuilder builder = new FeatureInformationBuilder();
				builder.add(d1.getFeatureInformation());
				builder.add(d2.getFeatureInformation());
				return builder.create();
			}
		};
	}
	
	private static Instance join(FeatureVector v1, Instance i2) {
		InstanceBuilder builder = new InstanceBuilder();
		builder.add(v1);
		builder.add(i2.getVector());
		return builder.create(i2.getCategory());
	}

	private DatasetJoiner() {
		// no op
	}

	public static void main(String[] args) throws IOException {
		FeatureRenamer applyActPrefixes = new FeatureRenamer("^((?!people|activity|outcome).+)$", "activity_$1");

		Builder config = CsvDatasetReaderConfig
				.filePath(new File("/Users/pk/Desktop/kaggle-red-hat-business-value/act_train.csv"));
		config.setFieldSeparator(',');
		config.readHeader(true);
		config.readClassFromLastColumn(true);
		config.treatAsNullValue("");
		Dataset actTrainDataset = config.create();
		actTrainDataset = actTrainDataset.transform(applyActPrefixes);

		config = CsvDatasetReaderConfig
				.filePath(new File("/Users/pk/Desktop/kaggle-red-hat-business-value/act_test.csv"));
		config.setFieldSeparator(',');
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.treatAsNullValue("");
		Dataset actTestDataset = config.create();
		actTestDataset = actTestDataset.transform(applyActPrefixes);

		config = CsvDatasetReaderConfig
				.filePath(new File("/Users/pk/Desktop/kaggle-red-hat-business-value/people.csv"));
		config.setFieldSeparator(',');
		config.readHeader(true);
		config.readClassFromLastColumn(false);
		config.treatAsNullValue("");
		Dataset peopleDataset = config.create();
		peopleDataset = peopleDataset.transform(new FeatureRenamer("^((?!people).+)$", "people_$1"));

//		join(peopleDataset, actTrainDataset, "people_id",
//				new File("/Users/pk/Desktop/kaggle-red-hat-business-value/act_train_people_joined.csv"));
		Dataset joinedDataset = join(peopleDataset, actTestDataset, "people_id");
		new CsvDatasetWriter(new File("/Users/pk/Desktop/kaggle-red-hat-business-value/act_test_people_joined.csv")).write(joinedDataset);
	}

}
