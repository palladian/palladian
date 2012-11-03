/**
 * Created on: 23.10.2012 11:16:17
 */
package ws.palladian.extraction.feature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.Port;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Writes the {@link FeatureVector} of the processed document to a CSV file.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class CsvWriter extends AbstractPipelineProcessor<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvWriter.class);

    private final List<String> featurePaths;
    private final String csvFilePath;

    /**
     * <p>
     * 
     * </p>
     * 
     * @throws IOException
     * 
     */
    public CsvWriter(String csvFilePath, Collection<String> featurePaths) throws IOException {
        super(Arrays.asList(new Port<?>[] {new Port<Object>(DEFAULT_INPUT_PORT_IDENTIFIER)}), new ArrayList<Port<?>>());

        this.featurePaths = new ArrayList<String>(featurePaths);
        this.csvFilePath = csvFilePath;
        StringBuffer header = new StringBuffer("");
        for (String featurePath : this.featurePaths) {
            header.append("\"" + featurePath + "\",");
        }
        header.replace(header.length() - 1, header.length(), "\n");
        FileUtils.write(new File(csvFilePath), header);
    }

    public CsvWriter(String csvFilePath, String... featurePaths) throws IOException {
        this(csvFilePath, Arrays.asList(featurePaths));
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        try {
            StringBuffer dataLine = new StringBuffer("");
            for (String featurePath : featurePaths) {
                Feature<?> feature = getDefaultInput().getFeatureVector().getFeature(featurePath);
                if (feature == null) {
                    LOGGER.warn("Unable to find feature for feature path: " + featurePath);
                    dataLine.append("?,");
                } else {
                    dataLine.append(feature.getValue() + ",");
                }
            }
            dataLine.replace(dataLine.length() - 1, dataLine.length(), "\n");
            FileUtils.write(new File(csvFilePath), dataLine, true);
        } catch (IOException e) {
            throw new DocumentUnprocessableException(e);
        }
    }

}
