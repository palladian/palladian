package ws.palladian.core.dataset;

import static ws.palladian.helper.io.FileHelper.NEWLINE_CHARACTER;

import java.io.IOException;
import java.io.Writer;

import ws.palladian.core.Instance;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;

public abstract class AbstractDatasetWriter implements DatasetWriter {
	
	/**
	 * Common {@link DatasetAppender} implementation based on a {@link Writer}.
	 * 
	 * @author pk
	 */
	protected static abstract class AbstractDatasetAppender implements DatasetAppender {
		private final Writer writer;
		protected AbstractDatasetAppender(Writer writer) {
			this.writer = writer;
		}
		@Override
		public void close() throws IOException {
			writer.close();
		}
		protected void writeLine(CharSequence charSequence) {
			try {
				writer.write(charSequence.toString());
				writer.write(NEWLINE_CHARACTER);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	@Override
	public void write(Dataset dataset) {
		write(dataset, NoProgress.INSTANCE);
	}
	
	@Override
	public void write(Dataset dataset, ProgressReporter progress) {
		try (DatasetAppender appender = write(dataset.getFeatureInformation())) {
			progress.startTask("Writing dataset", dataset.size());
			for (Instance instance : dataset) {
				appender.append(instance);
				progress.increment();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
