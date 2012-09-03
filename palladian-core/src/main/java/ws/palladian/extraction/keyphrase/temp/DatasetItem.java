package ws.palladian.extraction.keyphrase.temp;

import java.io.File;
import java.util.Arrays;

/**
 * <p>
 * An item in a {@link Dataset2}, typically used for classification evaluation tasks.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DatasetItem {

    private final File file;
    private final String[] categories;

    /**
     * <p>
     * Create a new {@link DatasetItem}, pointing to the specified {@link File} with the specified categories.
     * </p>
     * 
     * @param file The file which holds the text content of this {@link DatasetItem}.
     * @param categories The category or categories assigned to this {@link DatasetItem}.
     */
    public DatasetItem(File file, String... categories) {
        this.file = file;
        this.categories = categories;
    }

    /**
     * <p>
     * Get the {@link File} associated with this {@link DatasetItem}, i.e. the {@link File} holding the actual text
     * content.
     * 
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     * <p>
     * Get the categories assigned to this {@link DatasetItem}.
     * </p>
     * 
     * @return
     */
    public String[] getCategories() {
        return categories;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DatasetItem [file=");
        builder.append(file);
        builder.append(", categories=");
        builder.append(Arrays.toString(categories));
        builder.append("]");
        return builder.toString();
    }

}
