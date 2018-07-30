package ws.palladian.kaggle.restaurants.dataset;

import static ws.palladian.helper.functional.Filters.ALL;
import static ws.palladian.helper.functional.Filters.and;
import static ws.palladian.helper.functional.Filters.fileExtension;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.dataset.ImageValue;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.AbstractIterator;
import java.util.function.Predicate;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;

public class DirectoryDatasetReader implements Iterable<Instance> {
	private final File directory;
	private List<File> imageFiles;

	public DirectoryDatasetReader(File directory) {
		this(directory, ALL);
	}

	public DirectoryDatasetReader(File directory, Predicate<? super File> filter) {
		this.directory = directory;
		this.imageFiles = FileHelper.getFiles(directory, and(fileExtension("jpg"), filter), Filters.ALL);
	}

	@Override
	public Iterator<Instance> iterator() {
		Iterator<File> fileIterator = imageFiles.iterator();
		ProgressReporter progressReporter = new ProgressMonitor();
		progressReporter.startTask(directory.toString(), imageFiles.size());
		return new AbstractIterator<Instance>() {
			@Override
			protected Instance getNext() throws Finished {
				if (fileIterator.hasNext()) {
					File imageFile = fileIterator.next();
					String category = imageFile.getParentFile().getName().toString();
					progressReporter.increment();
					return new InstanceBuilder().set("image", new ImageValue(imageFile)).create(category);
				}
				throw FINISHED;
			}
		};
	}

	@Override
	public String toString() {
		return this.getClass().getName() + " [" + directory + "]";
	}
	public List<File> getImageFiles() {
		return Collections.unmodifiableList(imageFiles);
	}

}
