package ws.palladian.core.dataset;

import ws.palladian.helper.ProgressReporter;

public interface DatasetWriter {
	
	void write(Dataset dataset);
	
	void write(Dataset dataset, ProgressReporter progress);
	
}
