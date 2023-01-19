package ws.palladian.kaggle.restaurants.dataset;

import ws.palladian.helper.functional.Predicates;

import java.io.File;
import java.util.function.Predicate;

public final class FilenameIndexRangeFilter implements Predicate<File> {

    private final int maxIndex;

    public static Predicate<File> until(int index) {
        return new FilenameIndexRangeFilter(index);
    }

    public static Predicate<File> above(int index) {
        return Predicates.not(until(index));
    }

    private FilenameIndexRangeFilter(int maxIndex) {
        this.maxIndex = maxIndex;
    }

    @Override
    public boolean test(File item) {
        String numberPart = item.getName().replaceAll("[^\\d]", "");
        try {
            int filenameIndex = Integer.parseInt(numberPart);
            return filenameIndex <= maxIndex;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
