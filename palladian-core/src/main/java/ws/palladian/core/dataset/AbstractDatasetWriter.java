package ws.palladian.core.dataset;

import ws.palladian.helper.NoProgress;

public abstract class AbstractDatasetWriter implements DatasetWriter {
	
	@Override
	public void write(Dataset dataset) {
		write(dataset, NoProgress.INSTANCE);
	}

}
