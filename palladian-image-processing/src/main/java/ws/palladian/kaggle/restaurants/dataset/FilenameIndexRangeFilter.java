package ws.palladian.kaggle.restaurants.dataset;

import java.io.File;

import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

public final class FilenameIndexRangeFilter implements Filter<File> {

	private final int maxIndex;

	public static Filter<File> until(int index) {
		return new FilenameIndexRangeFilter(index);
	}

	public static Filter<File> above(int index) {
		return Filters.not(until(index));
	}

	private FilenameIndexRangeFilter(int maxIndex) {
		this.maxIndex = maxIndex;
	}

	@Override
	public boolean accept(File item) {
		String numberPart = item.getName().replaceAll("[^\\d]", "");
		try {
			int filenameIndex = Integer.parseInt(numberPart);
			return filenameIndex <= maxIndex;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
