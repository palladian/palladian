package ws.palladian.classification.evaluation;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.split.AbstractFilterSplit;
import ws.palladian.helper.functional.Filter;

/**
 * Instead of splitting % 2 the ID, split in chunks. This way, each subset still
 * contains predecessor/successor instances, and thus allows to learn additional
 * features.
 * 
 * @author pk
 *
 */
public class ChunkBasedSplit extends AbstractFilterSplit {

	public static final int CHUNK_SIZE = 100;

	public ChunkBasedSplit(Dataset dataset) {
		super(dataset);
	}

	@Override
	protected Filter<? super Instance> createFilter() {
		return new Filter<Instance>() {
			@Override
			public boolean accept(Instance item) {
				String id = item.getVector().getNominal("Id").getString();
				return Integer.parseInt(id) / CHUNK_SIZE % 2 == 0;
			}
		};
	}

//	public static void main(String[] args) {
//		Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(Config.getNumericTrain());
//		csvConfigBuilder.fieldSeparator(",");
//		csvConfigBuilder.treatAsNullValue("");
//		csvConfigBuilder.parser("Id", ValueDefinitions.stringValue());
//		csvConfigBuilder.skipColumns(not(equal("Id")));
//		Dataset numericTrain = csvConfigBuilder.create();
//
//		ChunkBasedSplit split = new ChunkBasedSplit(numericTrain);
//		 System.out.println("| train | = " + split.getTrain().size());
//		 System.out.println("| test | = " + split.getTest().size());
//	}

}
