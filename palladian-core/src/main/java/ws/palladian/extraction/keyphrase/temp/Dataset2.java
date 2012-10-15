package ws.palladian.extraction.keyphrase.temp;

import java.io.File;
import java.util.ArrayList;

import ws.palladian.processing.PipelineDocument;

/**
 * <p>
 * A dataset typically used for classification evaluation tasks, it consists of multiple {@link DatasetItem}s.
 * </p>
 * 
 * @author Philipp Katz
 * @deprecated Replace by {@link Iterable} with {@link PipelineDocument}.
 */
@Deprecated
public class Dataset2 extends ArrayList<DatasetItem> {

    private static final long serialVersionUID = 1L;

    private final File filePath;

    public Dataset2(File filePath) {
        this.filePath = filePath;
    }
    
    public Dataset2() {
        this(null);
    }

    public File getFilePath() {
        return filePath;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dataset2 [filePath=");
        builder.append(filePath);
        builder.append(", size()=");
        builder.append(size());
        builder.append("]");
        return builder.toString();
    }

}
